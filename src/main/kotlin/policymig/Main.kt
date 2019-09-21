@file:JvmName("PolicyMig")

package policymig

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import policymig.db.Instance
import policymig.model.Policy
import policymig.model.cloudTargetCount
import policymig.util.*
import policymig.util.cloud.createComputeService
import policymig.util.cloud.fetchEc2Instances
import policymig.util.cloud.fetchInstancesFromGcp
import policymig.util.db.DbUtils
import policymig.util.io.readFromPcl
import policymig.util.io.writeToPcl
import java.io.File
import java.nio.file.Path

/**
 * Base CLI command
 *
 * Disables stacktrace output to console and instead prints the error message and exception type to [System.err]
 */
class PolicyMigrate: CliktCommand(
    name = "cloud-policy",
    help = "CLI tool to translate and apply policies on the cloud",
    printHelpOnEmptyArgs = true
) {
    override fun run() = Thread.setDefaultUncaughtExceptionHandler { _, e -> logError { "${e::class.java.simpleName}: ${e.message}" } }
}

/**
 * apply sub-command for CLI
 *
 * @property file CLI option for specifying policy file
 * @property credentialsFile CLI option for specifying credentials file for cloud access
 *
 * Usage:
 * ```
 * cloud-policy apply -f [file].pcl
 * ```
 */
class Apply: CliktCommand(
    help = "Apply the policies to specified targets from a given file",
    printHelpOnEmptyArgs = true
) {
    private val file: Path by option("-f", "--file", help = "Path to file to read policies from")
        .path(exists = true, folderOkay = false, readable = true)
        .required()
        .validate {
            if (!it.toString().endsWith(".pcl")) {
                fail("Only $FILE_EXTENSION file type is supported!")
            }
        }

    private val credentialsFile: Path? by option("-c", "--credential", help = "path to JSON credential file")
        .path(exists = true, folderOkay = false, readable = true)
        .validate {
            if (!it.toString().endsWith(".json")) {
                fail("Must be a JSON file!")
            }
        }

    private val project: String? by option("-p", "--project", help = "name of project (unique id on GCP)")

    override fun run() {
        val policies = readFromPcl(file.toString())
        when(policies.cloudTargetCount()) {
            1 -> if (policies[0].target == "gcp") {
                requireNotNull(project)
                requireNotNull(credentialsFile)
                createGcpProviderBlock(project!!, credentialsFile.toString())
                policies.forEach { createGcpFirewallBlock(it) }
            } else {
                /*
                    TODO: Add AWS Terraform code
                    @author: aayush
                    @date: 20/09/19
                    @time: 10:02 AM
                 */
                TODO()
            }
            2 -> {
                requireNotNull(project)
                requireNotNull(credentialsFile)
                createGcpProviderBlock(project!!, credentialsFile.toString())
                policies.forEach {
                    if (it.target == "gcp") {
                        createGcpFirewallBlock(it)
                    } else {
                        /*
                    TODO: Add AWS Terraform code
                    @author: aayush
                    @date: 20/09/19
                    @time: 10:02 AM
                 */
                        TODO()
                    }
                }
            }
        }
        val initCommand = runCommand("terraform init", "/home/aayush/IdeaProjects/PolicyMig/terraform-resources/gcp/")
        logInfo { initCommand.first }
        if (initCommand.second != "") {
            logError { initCommand.second }
            return
        }
        val planCommand = runCommand("terraform plan -out plan.out", "/home/aayush/IdeaProjects/PolicyMig/terraform-resources/gcp/")
        logInfo { planCommand.first }
        if (planCommand.second != "") {
            logError { planCommand.second }
            return
        }
        val applyCommand = runCommand("terraform apply plan.out", "/home/aayush/IdeaProjects/PolicyMig/terraform-resources/gcp/", 180)
        logInfo { applyCommand.first }
        if (applyCommand.second != "") {
            logError { applyCommand.second }
            return
        }
    }
}

/**
 * translate sub-command for CLI
 *
 * @property file CLI option for specifying policy file
 * @property target CLI option for cloud environment (must be one of [policymig.util.TARGETS])
 * @property region CLI option for AWS regions to translate the policy for (must belong to [policymig.util.AWS_REGIONS])
 * @property network CLI option for GCP network
 *
 * Usage:
 * ```
 * cloud-policy translate -t aws -r [region] -f [file].pcl
 * cloud-policy translate -t gcp -n [network] -f [file].pcl
 * ```
 */
class Translate: CliktCommand(
    help = "Translate given policy to a target",
    printHelpOnEmptyArgs = true
) {
    private val file: Path by option("-f", "-file", help = "Name of file which contains policy")
        .path(exists = true, folderOkay = false, readable = true)
        .required()
        .validate {
            if (!it.toString().endsWith(".pcl")) {
                fail("Only $FILE_EXTENSION file type is supported!")
            }
        }
    private val target: String by option("-t", "--target", help = "Name of target environment")
        .choice("aws", "gcp")
        .required()
    private val region: String? by option("-r", "--region", help = "Regions for AWS")
    private val network: String? by option("-n", "--network", help = "Network for GCP")
        .validate {
            if (!it.matches(NETWORK_REGEX.toRegex())) {
                fail("Network must contain only lowercase letters, hyphens and numbers!")
            }
        }

    override fun run() {
        if (target == "aws") {
            if (region == null) {
                echo("Regions must be specified using the option!", err = true)
                return
            }
            if (!AWS_REGIONS.contains(region)) {
                echo("$region is not a valid region!", err = true)
                return
            }
        } else {
            if (network == null) {
                echo("Network must be specified using the option!", err = true)
                return
            }
        }

        val policies = readFromPcl(file.toString())
        val translatedPolicies: MutableList<Policy> = mutableListOf()
        policies.forEach { translatedPolicies.add(it.translatePolicy(target, region, network)) }
        translatedPolicies.writeToPcl("translated_policies$FILE_EXTENSION")
    }
}

/**
 * discover sub-command for CLI
 *
 * @property target CLI option for cloud environment (must be one of [policymig.util.TARGETS])
 * @property project optional CLI option for specifying GCP project
 * @property credentialsFile optional CLI option for specifying GCP service account credentials file
 *
 * Usage:
 * ```
 * cloud-policy discover -t gcp -p [project] -c [file].json
 * cloud-policy discover -t aws
 * ```
 */
class Discover: CliktCommand(
    help = "Discovers cloud VMs and stores locally",
    printHelpOnEmptyArgs = true
) {
    private val target: String by option("-t", "--target", help = "Name of target environment")
        .choice("aws", "gcp")
        .required()
    private val project: String? by option("-p", "--project", help = "name of project (unique id on GCP)")
    private val credentialsFile: Path? by option("-c", "--credential", help = "path to JSON credential file")
        .path(exists = true, folderOkay = false, readable = true)
        .validate {
            if (!it.toString().endsWith(".json")) {
                fail("Must be a JSON file!")
            }
        }

    override fun run() {
        val instances: List<Instance>
        if (target == "gcp") {
            if (project == null) {
                echo("Project name must be specified!", err = true)
                return
            }
            instances = fetchInstancesFromGcp(project!!, createComputeService(credentialsFile.toString()))
            if (instances.isEmpty()) {
                logInfo { "No instances on GCP" }
                return
            }
        } else {
            instances = fetchEc2Instances()
            if (instances.isEmpty()) {
                logInfo { "No instances on AWS" }
                return
            }
        }
        DbUtils.insertIntoTable(instances)
    }
}

/**
 * clean sub-command for CLI
 *
 * Usage:
 * ```
 * cloud-policy clean
 * ```
 */
class Clean: CliktCommand(
    help = "Frees all resources used by this tool (Databases, File System, etc)"
) {
    override fun run() {
        DbUtils.emptyTables()
        logInfo { "All tables dropped!" }

        File(DIRECTORY).deleteRecursively()
        logInfo { "All Terraform resources deleted!" }
    }
}

fun main(args: Array<String>) = PolicyMigrate()
    .subcommands(Apply(), Translate(), Discover(), Clean())
    .main(args)

//fun main() {
//    createGcpProviderBlock("pelagic-cycle-239905", "/home/aayush/IdeaProjects/PolicyMig/gcp_auth.json")
//    val policy = policy {
//        name = "test-policy"
//        description = "Testing policy"
//        target = "gcp"
//        network = "default"
//        direction = "EGRESS"
////        sourceIps = listOf("192.168.2.0/16", "10.53.25.192/24")
//        targetIps = listOf("0.0.0.0/0")
//        rules {
//            rule {
//                ports = listOf("8080", "5500-5600")
//                action = "allow"
//                protocol = "sctp"
//            }
//            rule {
//                ports = listOf("8080", "5500-5600")
//                action = "deny"
//                protocol = "udp"
//            }
//        }
//    }
//    println(policy)
//    createGcpFirewallBlock(policy)
//
//    println(runCommand("terraform init", "/home/aayush/IdeaProjects/PolicyMig/terraform-resources/gcp/"))
//    println(runCommand("terraform plan -out plan.out", "/home/aayush/IdeaProjects/PolicyMig/terraform-resources/gcp/"))
//    println(runCommand("terraform apply plan.out", "/home/aayush/IdeaProjects/PolicyMig/terraform-resources/gcp/", 180))
//}