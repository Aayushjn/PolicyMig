package policymig.util.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import policymig.db.*
import policymig.util.Slf4jSqlInfoLogger
import policymig.util.logInfo

internal const val DB_NAME: String = "policy_mig"
internal const val DB_USER: String = "root"
internal const val DB_PASS: String = "root"
internal const val DB_DRIVER: String = "com.mysql.cj.jdbc.Driver"

/**
 * Exposes methods to access database and perform SQL queries
 */
object DbUtils {
    /**
     * Lazily instantiated database
     * Multiple [Database.connect] calls cause leaks, thus lazy instantiation is used
     */
    private val db: Database by lazy { Database.connect("jdbc:mysql://localhost:3306/$DB_NAME", DB_DRIVER, DB_USER, DB_PASS) }

    /**
     * Inserts list of [policymig.db.Instance] into db
     *
     * @param instances list of [policymig.db.Instance]
     */
    fun insertIntoTable(instances: List<Instance>) {
        logInfo("DbUtils") { "Opening connection to ${db.url} for insert/update" }

        transaction {
            addLogger(Slf4jSqlInfoLogger)

            // Creates tables if they don't already exist
            SchemaUtils.create(InstanceTable, PrivateIpTable, PublicIpTable, TagsTable)

            // If DB contains entries that don't exist on the cloud, they must be deleted
            val toDelete: MutableList<String> = mutableListOf()
            InstanceTable.slice(InstanceTable.instanceId).selectAll().toList().forEach {
                if (!instances.contains(it[InstanceTable.instanceId])) {
                    toDelete.add(it[InstanceTable.instanceId])
                }
            }

            instances.forEach { instance ->
                // Insert into DB if not already exists
                if (InstanceTable.select { InstanceTable.instanceId eq instance.instanceId }.toList().isEmpty()) {
                    InstanceTable.insert {
                        it[instanceId] = instance.instanceId
                        it[accountId] = instance.accountId
                        it[region] = instance.region
                    }
                    for (ipAddress in instance.privateIps) {
                        PrivateIpTable.insert {
                            it[ip] = ipAddress
                            it[instanceId] = instance.instanceId
                        }
                    }
                    for (ipAddress in instance.publicIps) {
                        PublicIpTable.insert {
                            it[ip] = ipAddress
                            it[instanceId] = instance.instanceId
                        }
                    }
                    for (instanceTag in instance.tags) {
                        TagsTable.insert {
                            it[tag] = "${instanceTag.key}=${instanceTag.value}"
                            it[instanceId] = instance.instanceId
                        }
                    }
                } else {
                    // If entry exists in DB, update it
                    InstanceTable.update {
                        it[instanceId] = instance.instanceId
                        it[accountId] = instance.accountId
                        it[region] = instance.region
                    }
                    for (ipAddress in instance.privateIps) {
                        PrivateIpTable.update {
                            it[ip] = ipAddress
                            it[instanceId] = instance.instanceId
                        }
                    }
                    for (ipAddress in instance.publicIps) {
                        PublicIpTable.update {
                            it[ip] = ipAddress
                            it[instanceId] = instance.instanceId
                        }
                    }
                    for (instanceTag in instance.tags) {
                        TagsTable.update {
                            it[tag] = "${instanceTag.key}=${instanceTag.value}"
                            it[instanceId] = instance.instanceId
                        }
                    }
                }
            }

            // Delete entries that don't exist on cloud
            toDelete.forEach { id ->
                InstanceTable.deleteWhere { InstanceTable.instanceId eq id }
            }
        }
    }

    /**
     * Fetches all GCP instances stored in DB
     *
     * @return list of [policymig.db.Instance]
     */
    fun selectAllGcpInstances(): List<Instance> {
        logInfo("DbUtils") { "Opening connection to ${db.url} to fetch GCP instances" }

        val instances: MutableList<Instance> = mutableListOf()
        val internalIps: MutableList<String> = mutableListOf()
        val natIps: MutableList<String> = mutableListOf()
        val instanceTags: MutableMap<String, String> = mutableMapOf()

        transaction {
            addLogger(Slf4jSqlInfoLogger)

            InstanceTable.selectAll().forEach { result ->
                internalIps.clear()
                natIps.clear()
                instanceTags.clear()

                // GCP instances have unique identifiers that contain alphanumerics and hyphens
                if (!result[InstanceTable.accountId].isNumeric()) {
                    PrivateIpTable.select { PrivateIpTable.instanceId eq result[InstanceTable.instanceId] }.forEach {
                        internalIps.add(it[PrivateIpTable.ip])
                    }
                    PublicIpTable.select { PublicIpTable.instanceId eq result[InstanceTable.instanceId] }.forEach {
                        natIps.add(it[PublicIpTable.ip])
                    }
                    TagsTable.select { TagsTable.instanceId eq result[InstanceTable.instanceId] }.forEach {
                        val (key: String, value: String) = it[TagsTable.tag].split("=")
                        instanceTags[key] = value
                    }

                    instances.add(
                        instance {
                            instanceId = result[InstanceTable.instanceId]
                            accountId = result[InstanceTable.accountId]
                            region = result[InstanceTable.region]
                            privateIps = internalIps
                            publicIps = natIps
                            tags = instanceTags
                        }
                    )
                }
            }
        }
        return instances
    }

    /**
     * Fetches all AWS instances stored in DB
     *
     * @return list of [policymig.db.Instance]
     */
    fun selectAllAwsInstances(): List<Instance> {
        logInfo("DbUtils") { "Opening connection to ${db.url} to fetch AWS instances" }

        val instances: MutableList<Instance> = mutableListOf()
        val internalIps: MutableList<String> = mutableListOf()
        val natIps: MutableList<String> = mutableListOf()
        val instanceTags: MutableMap<String, String> = mutableMapOf()

        transaction {
            addLogger(Slf4jSqlInfoLogger)

            InstanceTable.selectAll().forEach { result ->
                internalIps.clear()
                natIps.clear()
                instanceTags.clear()

                // AWS unique identifiers are completely numeric
                if (result[InstanceTable.accountId].isNumeric()) {
                    PrivateIpTable.select { PrivateIpTable.instanceId eq result[InstanceTable.instanceId] }.forEach {
                        internalIps.add(it[PrivateIpTable.ip])
                    }
                    PublicIpTable.select { PublicIpTable.instanceId eq result[InstanceTable.instanceId] }.forEach {
                        natIps.add(it[PublicIpTable.ip])
                    }
                    TagsTable.select { TagsTable.instanceId eq result[InstanceTable.instanceId] }.forEach {
                        val (key: String, value: String) = it[TagsTable.tag].split("=")
                        instanceTags[key] = value
                    }

                    instances.add(
                        instance {
                            instanceId = result[InstanceTable.instanceId]
                            accountId = result[InstanceTable.accountId]
                            region = result[InstanceTable.region]
                            privateIps = internalIps
                            publicIps = natIps
                            tags = instanceTags
                        }
                    )
                }
            }
        }
        return instances
    }

    fun emptyTables() {
        logInfo("DbUtils") { "Opening connection to ${db.url} to drop all tables" }

        transaction { SchemaUtils.drop(InstanceTable, PrivateIpTable, PublicIpTable, TagsTable) }
    }
}