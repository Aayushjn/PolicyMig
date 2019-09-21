package policymig.util

val TARGETS: Array<String> = arrayOf("aws", "gcp")

val AWS_REGIONS: Array<String> = arrayOf(
    "us-east-2", "us-east-1", "us-west-1", "us-west-2", "ap-east-1", "ap-south-1", "ap-northeast-3", "ap-northeast-2",
    "ap-southeast-1", "ap-southeast-2", "ap-northeast-1", "ca-central-1", "cn-north-1", "cn-northwest-1",
    "eu-central-1", "eu-west-1", "eu-west-2", "eu-west-3", "eu-north-1", "me-south-1", "sa-east-1", "us-gov-east-1",
    "us-gov-west-1"
)

const val NETWORK_REGEX: String = "[a-z0-9-]+"
const val IP_REGEX: String = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/([0-9]|[1-2][0-9]|3[0-2]))\$"
const val TAG_REGEX: String = "[a-z0-9A-Z.-]+=[a-z0-9A-Z.-]+"

val DIRECTIONS: Array<String> = arrayOf("INGRESS", "EGRESS")
val ACTIONS: Array<String> = arrayOf("allow", "deny")
val PROTOCOLS: Array<String> = arrayOf("tcp", "udp", "icmp", "esp", "ah", "sctp", "all")

const val FILE_EXTENSION: String = ".pcl"
const val FILE_ERROR: String = "Only $FILE_EXTENSION file type is supported!"