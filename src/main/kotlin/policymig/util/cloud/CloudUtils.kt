@file:JvmName("CloudUtils")
package policymig.util.cloud

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.InstanceAggregatedList
import com.google.api.services.compute.model.InstancesScopedList
import policymig.db.Instance
import policymig.db.instance
import policymig.util.AWS_REGIONS
import policymig.util.misc.logError
import policymig.util.misc.logWarning
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.*
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.security.GeneralSecurityException
import java.time.LocalDateTime

const val FILENAME = "CloudUtils"

/**
 * Creates a [com.google.api.services.compute.Compute] instance from given credentials
 *
 * @param credentialsFile path to JSON file that holds service account credentials
 * @return [com.google.api.services.compute.Compute] instance
 *
 * @throws IOException
 * @throws GeneralSecurityException
 */
@Throws(IOException::class, GeneralSecurityException::class)
fun createComputeService(credentialsFile: String): Compute {
    require(Files.exists(Paths.get(credentialsFile))) { "$credentialsFile doesn't exist" }

    var credentials: GoogleCredential = GoogleCredential.fromStream(File(credentialsFile).inputStream())
    if (credentials.createScopedRequired()) {
        credentials = credentials.createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
    }
    return Compute.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(),
        credentials
    )
        .setApplicationName("PolicyMig")
        .build()
}

/**
 * Fetches VM instances on GCP cloud for [project]
 *
 * @param project name of project (unique identifier) on GCP
 * @param computeService [com.google.api.services.compute.Compute] instance obtained from [createComputeService] function
 * @return list of [policymig.db.Instance] on the cloud
 */
fun fetchInstancesFromGcp(project: String, computeService: Compute): List<Instance> {
    val request = computeService.instances().aggregatedList(project)

    var response: InstanceAggregatedList
    val cloudInstancesMap: MutableMap<String, InstancesScopedList> = mutableMapOf()
    do {
        response = request.execute()
        if (response.items == null) {
            continue
        }
        for (item in response.items.entries) {
            if (item.value.warning == null) {
                cloudInstancesMap[item.key] = item.value
            }
        }
        request.pageToken = response.nextPageToken
    } while (response.nextPageToken != null)

    val instances: MutableList<Instance> = mutableListOf()
    val internalIps: MutableList<String> = mutableListOf()
    val instanceTags: MutableList<Pair<String, String>> = mutableListOf()

    cloudInstancesMap.forEach {
        internalIps.clear()
        instanceTags.clear()

        it.value.instances.forEach { cloudInstance ->
            cloudInstance.networkInterfaces.forEach { nif ->
                val ip = nif.networkIP.split(".").toMutableList().apply {
                    this[2] = "0"
                    this[3] = "0"
                }.joinToString(separator = ".", postfix = "/24")
                internalIps += ip
            }
            cloudInstance.metadata.items?.forEach { item ->
                instanceTags += item.key to item.value
            }
            instances += instance {
                instanceId = cloudInstance.id.toString()
                accountId = project
                region = cloudInstance.zone.split("/").last()
                target = "gcp"
                privateIps = internalIps
                tags = instanceTags
            }
        }
    }
    return instances
}

/**
 * Fetches EC2 instances from AWS cloud
 *
 * AWS credentials provider chain that looks for credentials in this order:
 * <ol>
 *   <li>Java System Properties - <code>aws.accessKeyId</code> and <code>aws.secretKey</code></li>
 *   <li>Environment Variables - <code>AWS_ACCESS_KEY_ID</code> and <code>AWS_SECRET_ACCESS_KEY</code></li>
 *   <li>Credential profiles file at the default location (~/.aws/credentials) shared by all AWS SDKs and the AWS CLI</li>
 *   <li>Credentials delivered through the Amazon EC2 container service if "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" environment
 *   variable is set and security manager has permission to access the variable,</li>
 *   <li>Instance profile credentials delivered through the Amazon EC2 metadata service</li>
 * </ol>
 */
fun fetchEc2Instances(): List<Instance> {
    val profileCredentials: DefaultCredentialsProvider = DefaultCredentialsProvider.create()

    val instances: MutableList<Instance> = mutableListOf()
    val nifIds: MutableList<String> = mutableListOf()
    val internalIps: MutableList<String> = mutableListOf()
    val instanceTags: MutableList<Pair<String, String>> = mutableListOf()
    var ec2: Ec2Client
    var nextToken: String?
    var request: DescribeInstancesRequest
    var response: DescribeInstancesResponse

    AWS_REGIONS.forEach { awsRegion ->
        try {
            ec2 = Ec2Client.builder()
                .credentialsProvider(profileCredentials)
                .region(Region.of(awsRegion))
                .build()

            nextToken = null
            do {
                request = DescribeInstancesRequest.builder().nextToken(nextToken).build()
                response = ec2.describeInstances(request)

                response.reservations().forEach { reservation ->
                    reservation.instances().forEach { ec2Instance ->
                        internalIps.clear()
                        instanceTags.clear()

                        ec2Instance.networkInterfaces().forEach { nif ->
                            nifIds += nif.networkInterfaceId()
                            nif.privateIpAddresses().forEach {
                                val ip = it.privateIpAddress().split(".").toMutableList().apply {
                                    this[2] = "0"
                                    this[3] = "0"
                                }.joinToString(separator = ".", postfix = "/24")
                                internalIps += ip
                            }
                        }
                        ec2Instance.tags().forEach { instanceTags += it.key() to it.value() }

                        instances += instance {
                            instanceId = ec2Instance.instanceId()
                            accountId = profileCredentials.resolveCredentials().accessKeyId()
                            region = awsRegion
                            target = "aws"
                            networkInterfaceIds = nifIds
                            privateIps = internalIps
                            tags = instanceTags
                        }
                    }
                }
                nextToken = response.nextToken()
            } while (nextToken != null)
        } catch (e: Ec2Exception) {
            // Occurs if a region that has not been enabled is accessed (gov, cn, etc)
            logWarning(FILENAME) { "$awsRegion: ${e.message}" }
        } catch (e: SdkClientException) {
            // Normally occurs when accessing global regions and during SDK client configuration
            if (e.message!!.contains("Unable to load credentials")) {
                logError(FILENAME) {
                    "Credentials must be provided in either of the following locations:\n" +
                            "• Java System Properties - aws.accessKeyId and aws.secretKey\n" +
                            "• Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY\n" +
                            "• Credential profiles file at the default location (~/.aws/credentials) shared by all AWS SDKs and the AWS CLI\n" +
                            "• Credentials delivered through the Amazon EC2 container service if \"AWS_CONTAINER_CREDENTIALS_RELATIVE_URI\" environment\n" +
                            "• variable is set and security manager has permission to access the variable\n" +
                            "• Instance profile credentials delivered through the Amazon EC2 metadata service"
                }
            }
            logWarning(FILENAME) { "$awsRegion: ${e.message}" }
        }
    }
    return instances
}

fun fetchSecurityGroupIds(): List<String> {
    val profileCredentials: DefaultCredentialsProvider = DefaultCredentialsProvider.create()

    val securityGroupIds: MutableList<String> = mutableListOf()
    var ec2: Ec2Client
    var nextToken: String?
    var request: DescribeSecurityGroupsRequest
    var response: DescribeSecurityGroupsResponse

    AWS_REGIONS.forEach { awsRegion ->
        try {
            ec2 = Ec2Client.builder()
                .credentialsProvider(profileCredentials)
                .region(Region.of(awsRegion))
                .build()

            nextToken = null
            do {
                request = DescribeSecurityGroupsRequest.builder()
                    .nextToken(nextToken)
                    .build()
                response = ec2.describeSecurityGroups(request)

                response.securityGroups().forEach { securityGroup ->
                    val createdAt: LocalDateTime = securityGroup.tags()
                        .filter { it.key() == "createdAt" }
                        .map { LocalDateTime.parse(it.value()) }
                        .first()
                    val now: LocalDateTime = LocalDateTime.now()
                    if (now.minusMinutes(5) <= createdAt && createdAt <= now) {
                        securityGroupIds += securityGroup.groupId()
                    }
                }
                nextToken = response.nextToken()
            } while (nextToken != null)
        } catch (e: Ec2Exception) {
            // Occurs if a region that has not been enabled is accessed (gov, cn, etc)
            logWarning(FILENAME) { "$awsRegion: ${e.message}" }
        } catch (e: SdkClientException) {
            // Normally occurs when accessing global regions
            logWarning(FILENAME) { "$awsRegion: ${e.message}" }
        }
    }
    return securityGroupIds
}

fun attachSecurityGroups(networkInterfaceId: String, securityGroupId: List<String>) {
    val profileCredentials: DefaultCredentialsProvider = DefaultCredentialsProvider.create()
    val ec2: Ec2Client = Ec2Client.builder().credentialsProvider(profileCredentials).build()

    val request: ModifyNetworkInterfaceAttributeRequest = ModifyNetworkInterfaceAttributeRequest.builder()
        .groups(securityGroupId)
        .networkInterfaceId(networkInterfaceId)
        .build()
    ec2.modifyNetworkInterfaceAttribute(request)
}