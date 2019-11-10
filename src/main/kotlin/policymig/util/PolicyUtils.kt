@file:JvmName("PolicyUtils")
package policymig.util

import policymig.model.Policy
import policymig.util.dsl.policy

val TARGETS: Array<String> = arrayOf("aws", "gcp")

val AWS_REGIONS: Array<String> = arrayOf(
    "us-east-2", "us-east-1", "us-west-1", "us-west-2", "ap-east-1", "ap-south-1", "ap-northeast-3", "ap-northeast-2",
    "ap-southeast-1", "ap-southeast-2", "ap-northeast-1", "ca-central-1", "cn-north-1", "cn-northwest-1",
    "eu-central-1", "eu-west-1", "eu-west-2", "eu-west-3", "eu-north-1", "me-south-1", "sa-east-1", "us-gov-east-1",
    "us-gov-west-1"
)

val NETWORK_REGEX: Regex = "[a-z0-9-]+".toRegex()
val IP_REGEX: Regex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/([0-9]|[1-2][0-9]|3[0-2]))\$".toRegex()
val PORT_REGEX: Regex = "^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])\$".toRegex()
val TAG_REGEX: Regex = "[a-z0-9A-Z.-]+=[a-z0-9A-Z.-]+".toRegex()

val DIRECTIONS: Array<String> = arrayOf("INGRESS", "EGRESS")
val ACTIONS: Array<String> = arrayOf("allow", "deny")
val PROTOCOLS: Array<String> = arrayOf("tcp", "udp", "icmp", "esp", "ah", "sctp", "all")

const val FILE_EXTENSION: String = ".json"
const val FILE_ERROR: String = "Only $FILE_EXTENSION file type is supported!"

/**
 * Creates a sample policy
 * @param _target target environment must be one of [policymig.util.TARGETS]
 * @param _direction inbound/outbound (must be one of [policymig.util.DIRECTIONS])
 * @return instance of [policymig.model.Policy]
 */
internal fun getSamplePolicy(_target: String, _direction: String): Policy {
    require(_target in TARGETS) { "Target must be one of ${TARGETS.contentDeepToString()}" }
    require(_direction.toUpperCase() in DIRECTIONS) { "Invalid direction specified! Must be one of $DIRECTIONS" }

    val _region = if (_target == "aws") "us-west-2" else null
    val _network = if (_target == "aws") null else "default"

    return if (_direction == "INGRESS") {
        policy {
            name = "test-policy"
            description = "Testing policy"
            direction = _direction
            target = _target
            region = _region
            network = _network
            sourceTags = listOf("app" to "App", "name" to "Name")
            rules {
                rule {
                    ports = listOf("8080", "5500-5600")
                    action = "allow"
                    protocol = "tcp"
                }
                rule {
                    ports = listOf("0")
                    action = "allow"
                    protocol = "udp"
                }
            }
        }
    } else {
        policy {
            name = "test-policy"
            description = "Testing policy"
            direction = _direction
            target = _target
            region = _region
            network = _network
            targetTags = listOf("app" to "App", "name" to "Name")
            rules {
                rule {
                    ports = listOf("8080", "5500-5600")
                    action = "allow"
                    protocol = "tcp"
                }
                rule {
                    ports = listOf("0")
                    action = "allow"
                    protocol = "udp"
                }
            }
        }
    }
}