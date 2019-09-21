package policymig.model

import policymig.util.*

/**
 * Resembles a network security policy
 * Also supports tag-based policy enforcement strategy
 * Either [sourceIps] or [sourceTags] must be specified (not both)
 * Either [targetIps] or [targetTags] must be specified (not both)
 *
 * @property name name of policy
 * @property description brief description of policy
 * @property target target environment must be one of [policymig.util.TARGETS]
 * @property direction inbound/outbound (must be one of [policymig.util.DIRECTIONS])
 * @property network name of target network (specified only if [target] is "gcp"
 * @property region list of regions (must be one of [policymig.util.AWS_REGIONS] and is specified only if [target] is "aws"
 * @property sourceIps list of source IP addressed
 * @property sourceTags list of source key-value pairs
 * @property targetIps list of target IP addresses
 * @property targetTags list of target key-value pairs
 * @property rules list of firewall rules to be enforced
 */
data class Policy(val name: String,
                  val description: String = "",
                  var target: String,
                  var direction: String = "",
                  var network: String?,
                  var region: String?,
                  val sourceIps: List<String>?,
                  val sourceTags: Map<String, String>?,
                  val targetIps: List<String>?,
                  val targetTags: Map<String, String>?,
                  val rules: List<Rule>) {
    init {
        validate()
    }

    /**
     * Validates the given parameters to ensure that they have been passed correctly
     */
    private fun validate() {
        require(direction.toUpperCase() in DIRECTIONS) { "Invalid direction specified! Must be one of $DIRECTIONS" }

        if (direction.toUpperCase() == "INGRESS") {
            require((sourceIps != null && sourceTags == null && targetIps == null && targetTags == null) ||
                    (sourceTags != null && sourceIps == null && targetIps == null && targetTags == null))
            { "Ingress requires either source IPs or tags be set!" }
        } else {
            require((targetIps != null && sourceIps == null && sourceTags == null && targetTags == null) ||
                    (targetTags != null && sourceIps == null && sourceTags == null && targetIps == null))
            { "Egress requires either target IPs or tags be set!" }
        }

        require(TARGETS.contains(target)) { "Target must be one of ${TARGETS.contentDeepToString()}" }
        when (target) {
            "gcp" -> {
                requireNotNull(network) { "Network must be specified!" }
                rules.forEach { rule ->
                    require(rule.protocol != "all") { "${rule.protocol} is not a valid for GCP!" } }
            }
            "aws" -> {
                requireNotNull(region) { "Regions must be specified!" }
                rules.forEach { rule ->
                    require(rule.protocol != "sctp" || rule.protocol != "esp" || rule.protocol != "ah") { "${rule.protocol} is not a valid for AWS!" } }
            }
        }
        require(AWS_REGIONS.contains(region)) { "$region is not a valid region!" }

        network?.let { require(it.matches(NETWORK_REGEX.toRegex())) { "Network must contain only lowercase letters, hyphens and numbers!" } }

//        require(!((sourceIps != null && sourceTags != null) ||
//                (sourceIps == null && sourceTags == null) ||
//                (targetIps != null && targetTags != null) ||
//                (targetIps == null && targetTags == null)))
//        { "Either IP or tag MUST be specified (not both or neither)!" }

        sourceIps?.forEach {
            require(it.matches(IP_REGEX.toRegex())) { "$it is not a valid IP address!" }
        }
        targetIps?.forEach {
            require(it.matches(IP_REGEX.toRegex())) { "$it is not a valid IP address!" }
        }
    }

    /**
     * Translates a given policy to a target's policy and returns the modified policy
     *
     * @param target name of target environment (must be one of [policymig.util.TARGETS]
     * @param region list of regions (must be one of [policymig.util.AWS_REGIONS] and is specified only if [target] is "aws"
     * @param network name of target network (specified only if [target] is "gcp"
     * @return updated policy with required [target], [region], and [network]
     */
    fun translatePolicy(target: String, region: String?=null, network: String?=null): Policy {
        validate()

        val newPolicy: Policy = copy()

        newPolicy.target = target
        if (target == "aws") {
            newPolicy.network = null
            newPolicy.region = region
        } else if (target == "gcp") {
            newPolicy.network = network
            newPolicy.region = null
        }

        return newPolicy
    }

    override fun toString(): String {
        var s = "policy {\n\tname = $name\n\tdirection = $direction\n\tdescription = $description\n\ttarget = $target\n"
        network?.let { s += "\tnetwork = $network\n" }
        region?.let { s += "\tregions = $region\n" }
        sourceIps?.let { s += "\tsourceIps = $sourceIps\n" }
        sourceTags?.let { s += "\tsourceTags = $sourceTags\n" }
        targetIps?.let { s += "\ttargetIps = $targetIps\n" }
        targetTags?.let { s += "\ttargetTags = $targetTags\n" }
        s += "\trules {\n"
        rules.forEach { s += "\t\t$it" }
        s += "\t}\n}\n\n"

        return s
    }
}

/**
 * Resembles a firewall rule
 *
 * @property ports list of ports to target
 * @property action specifies allowing or denying traffic from given rule (must be one of [policymig.util.ACTIONS]
 */
data class Rule(val ports: List<String>,
                val action: String = "allow",
                val protocol: String = "tcp") {
    init {
        validate()
    }

    /**
     * Validates the given parameters to ensure that they have been passed correctly
     */
    private fun validate() {
        val portRegex: Regex = "^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])\$".toRegex()
        for (port: String in ports) {
            val splits: List<String> = port.split("-")
            splits.forEach {
                require(it.matches(portRegex)) { "Port numbers must be between 0 and 65535 (inclusive)!" }
            }
            if (splits.size == 1) {
                continue
            }
            else require(splits[0].toInt() <= splits[1].toInt()) { "2nd port number must be greater than the first!" }
        }

        require(action in ACTIONS) { "Invalid action specified!" }
        require(protocol in PROTOCOLS) { "Invalid protocol specified!" }
    }

    override fun toString(): String {
        var s = "rule {\n\t\t\tports = $ports\n\t\t\taction = $action\n\t\t\tprotocol = $protocol\n"
        s += "\t\t}\n"

        return s
    }
}

fun List<Policy>.cloudTargetCount(): Int {
    val awsCount = count { it.target == "aws" }
    val gcpCount = count { it.target == "gcp" }

    return if (awsCount == 0 || gcpCount == 0) {
        1
    } else {
        2
    }
}
