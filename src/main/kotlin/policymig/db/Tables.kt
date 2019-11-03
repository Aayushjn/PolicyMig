@file:JvmName("Tables")
package policymig.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table
import policymig.util.IP_REGEX
import policymig.util.TAG_REGEX

/**
 * SQL table to store instance details
 *
 * @property instanceId unique id of each instance
 * @property accountId unique id of each cloud account
 * @property region region in which instance is located
 * @property target cloud target to which instance belongs to (one of [policymig.util.TARGETS])
 */
object InstanceTable: Table("instances") {
    val instanceId: Column<String> = varchar("instance_id", 20).primaryKey()
    val accountId: Column<String> = varchar("account_id", 20)
    val region: Column<String> = varchar("region", 20)
    var target: Column<String> = varchar("target", 3)
}

/**
 * SQL table to store private IPs
 *
 * @property ip private IP address
 * @property instanceId foreign key to [policymig.db.InstanceTable.instanceId]
 */
object PrivateIpTable: Table("private_ips") {
    val ip: Column<String> = varchar("ip", 19).primaryKey().check("IP") { it.regexp(IP_REGEX.toString()) }
    val instanceId: Column<String> = reference("instance_id", InstanceTable.instanceId, onDelete = CASCADE)
}

/**
 * SQL table to store VM tags
 *
 * @property tag key=value pairs
 * @property instanceId foreign key to [policymig.db.InstanceTable.instanceId]
 */
object TagsTable: Table("tags") {
    val tag: Column<String> = varchar("tag", 30).check("tag") { it.regexp(TAG_REGEX.toString()) }
    val instanceId: Column<String> = reference("instance_id", InstanceTable.instanceId, onDelete = CASCADE)
}

/**
 * SQL table to store VM network interface IDs
 *
 * @property nif unique id for each network interface card
 * @property instanceId foreign key to [policymig.db.InstanceTable.instanceId]
 */
object NetworkInterfacesTable: Table("network_interfaces") {
    val nif: Column<String> = varchar("nif", 30)
    val instanceId: Column<String> = reference("instance_id", InstanceTable.instanceId, onDelete = CASCADE)
}