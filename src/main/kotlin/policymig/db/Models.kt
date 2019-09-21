package policymig.db

/**
 * Represents a cloud VM instance with relevant details
 *
 * @property instanceId unique id of each instance
 * @property accountId unique id of the account that it belongs to (helps differentiate AWS/GCP instances)
 * @property region region that the instance is located in
 * @property privateIps list of internal IPs
 * @property publicIps list of NAT IPs
 * @property tags map of key=value pairs used to "mark" certain instances
 */
data class Instance(
    val instanceId: String,
    var accountId: String,
    var region: String,
    var privateIps: List<String>,
    var publicIps: List<String>,
    var tags: Map<String, String>
)

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class InstanceDsl

/**
 * Helper class that exposes DSL way of declaring [policymig.db.Instance]
 *
 * @property instanceId unique id of each instance
 * @property accountId unique id of the account that it belongs to (helps differentiate AWS/GCP instances)
 * @property region region that the instance is located in
 * @property privateIps list of internal IPs
 * @property publicIps list of NAT IPs
 * @property tags map of key=value pairs used to "mark" certain instances
 */
@InstanceDsl
class InstanceBuilder {
    var instanceId: String = ""
    var accountId: String = ""
    var region: String = ""
    var privateIps: List<String> = listOf()
    var publicIps: List<String> = listOf()
    var tags: Map<String, String> = mapOf()

    /**
     * Builds a [policymig.db.Instance]
     *
     * @return [policymig.db.Instance]
     */
    internal fun build(): Instance = Instance(instanceId, accountId, region, privateIps, publicIps, tags)
}

/**
 * DSL approach to declaring [policymig.db.Instance]
 *
 * @param block functional block that declares all properties of [policymig.db.Instance]
 *
 * @return [policymig.db.Instance]
 */
fun instance(block: @InstanceDsl InstanceBuilder.() -> Unit): Instance = InstanceBuilder().apply(block).build()

/**
 * Extension function that checks whether the list contains a [policymig.db.Instance] with [instanceId]
 *
 * @param instanceId unique id of [policymig.db.Instance]
 * @return true if list contains [policymig.db.Instance] with [instanceId]
 */
internal fun List<Instance>.contains(instanceId: String): Boolean {
    forEach {
        if (it.instanceId == instanceId) {
            return true
        }
    }
    return false
}

/**
 * Extension function that checks whether a string is composed of only numerals or not
 *
 * @return true if the string is only numeric
 */
internal fun String.isNumeric(): Boolean {
    forEach {
        if (!it.isDigit()) {
            return false
        }
    }
    return true
}