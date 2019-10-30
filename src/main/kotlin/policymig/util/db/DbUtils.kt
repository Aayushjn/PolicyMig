package policymig.util.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import policymig.db.*
import policymig.util.misc.logInfo

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
            addLogger(Slf4jSqlDebugLogger)

            // Creates tables if they don't already exist
            SchemaUtils.create(InstanceTable, PrivateIpTable, TagsTable, NetworkInterfacesTable)

            /*
                TODO: Add deletion support for instances that don't exist on cloud
                @author: aayush
                @date: 30/09/19
                @time: 6:47 PM
             */
//            val awsInstances = instances.filter { it.target == "aws" }
//            val gcpInstances = instances.filter { it.target == "gcp" }
//            val toDelete: MutableList<String> = mutableListOf()
//
//            val awsStored = selectAllAwsInstances()
//            awsStored.forEach { ins ->
//                awsInstances.forEach {
//                    if (it.instanceId == ins.instanceId) {
//                        toDelete.add(it.instanceId)
//                    }
//                }
//            }
//            toDelete.forEach {
//                InstanceTable.deleteWhere { InstanceTable.instanceId eq it }
//            }
//            toDelete.clear()
//            val gcpStored = selectAllGcpInstances()
//            gcpStored.forEach { ins ->
//                gcpInstances.forEach {
//                    if (it.instanceId == ins.instanceId) {
//                        toDelete.add(it.instanceId)
//                    }
//                }
//            }
//            toDelete.forEach {
//                InstanceTable.deleteWhere { InstanceTable.instanceId eq it }
//            }

            instances.forEach { instance ->
                // Insert into DB if not already exists
                if (InstanceTable.select { (InstanceTable.instanceId eq instance.instanceId) and (InstanceTable.target eq instance.target) }.toList().isEmpty()) {
                    InstanceTable.insert {
                        it[instanceId] = instance.instanceId
                        it[accountId] = instance.accountId
                        it[region] = instance.region
                        it[target] = instance.target
                    }
                    instance.networkInterfaceIds.forEach { nifId ->
                        NetworkInterfacesTable.insert {
                            it[nif] = nifId
                            it[instanceId] = instance.instanceId
                        }
                    }
                    instance.privateIps.forEach { ipAddress ->
                        PrivateIpTable.insert {
                            it[ip] = ipAddress
                            it[instanceId] = instance.instanceId
                        }
                    }
                    instance.tags.forEach { instanceTag ->
                        TagsTable.insert {
                            it[tag] = "${instanceTag.first}=${instanceTag.second}"
                            it[instanceId] = instance.instanceId
                        }
                    }
                } else {
                    // If entry exists in DB, update it
                    InstanceTable.update {
                        it[instanceId] = instance.instanceId
                        it[accountId] = instance.accountId
                        it[region] = instance.region
                        it[target] = instance.target
                    }
                    instance.networkInterfaceIds.forEach { nifId ->
                        NetworkInterfacesTable.insert {
                            it[nif] = nifId
                            it[instanceId] = instance.instanceId
                        }
                    }
                    instance.privateIps.forEach { ipAddress ->
                        PrivateIpTable.update {
                            it[ip] = ipAddress
                            it[instanceId] = instance.instanceId
                        }
                    }
                    instance.tags.forEach { instanceTag ->
                        TagsTable.update {
                            it[tag] = "${instanceTag.first}=${instanceTag.second}"
                            it[instanceId] = instance.instanceId
                        }
                    }
                }
            }
        }
    }

    /**
     * Converts tags to private IPs
     *
     * @param target cloud target to which instance belongs to (one of [policymig.util.TARGETS])
     * @param tags key-value pairs that are metadata of the instance
     *
     * @return list of IPs
     */
    fun fetchTagsAsIp(target: String, tags: List<Pair<String, String>>): List<String> {
        val instances: List<Instance> = if (target == "gcp") {
            selectAllGcpInstances()
        } else {
            selectAllAwsInstances()
        }

        return instances
            .filter { instance -> tags.any { tag -> tag in instance.tags } }
            .map { instance -> instance.privateIps }
            .flatten()
    }

    /**
     * Fetches all GCP instances stored in DB
     *
     * @return list of [policymig.db.Instance]
     */
    private fun selectAllGcpInstances(): List<Instance> {
        logInfo("DbUtils") { "Opening connection to ${db.url} to fetch GCP instances" }

        val instances: MutableList<Instance> = mutableListOf()
        val nifIds: MutableList<String> = mutableListOf()
        val internalIps: MutableList<String> = mutableListOf()
        val instanceTags: MutableList<Pair<String, String>> = mutableListOf()

        transaction {
            addLogger(Slf4jSqlDebugLogger)

            InstanceTable.select { InstanceTable.target eq "gcp" }.forEach { result ->
                nifIds.clear()
                internalIps.clear()
                instanceTags.clear()

                NetworkInterfacesTable.select { NetworkInterfacesTable.instanceId eq result[InstanceTable.instanceId] }.forEach {
                    nifIds += it[NetworkInterfacesTable.nif]
                }
                PrivateIpTable.select { PrivateIpTable.instanceId eq result[InstanceTable.instanceId] }.forEach {
                    internalIps += it[PrivateIpTable.ip]
                }
                TagsTable.select { TagsTable.instanceId eq result[InstanceTable.instanceId] }.forEach {
                    val (key: String, value: String) = it[TagsTable.tag].split("=")
                    instanceTags += key to value
                }

                instances += instance {
                    instanceId = result[InstanceTable.instanceId]
                    accountId = result[InstanceTable.accountId]
                    region = result[InstanceTable.region]
                    target = result[InstanceTable.target]
                    networkInterfaceIds = nifIds
                    privateIps = internalIps
                    tags = instanceTags
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
    private fun selectAllAwsInstances(): List<Instance> {
        logInfo("DbUtils") { "Opening connection to ${db.url} to fetch AWS instances" }

        val instances: MutableList<Instance> = mutableListOf()
        val nifIds: MutableList<String> = mutableListOf()
        val internalIps: MutableList<String> = mutableListOf()
        val instanceTags: MutableList<Pair<String, String>> = mutableListOf()

        transaction {
            addLogger(Slf4jSqlDebugLogger)

            InstanceTable.select { InstanceTable.target eq "aws" }.forEach { result ->
                nifIds.clear()
                internalIps.clear()
                instanceTags.clear()

                NetworkInterfacesTable.select { NetworkInterfacesTable.instanceId eq result[InstanceTable.instanceId] }.forEach {
                    nifIds += it[NetworkInterfacesTable.nif]
                }
                PrivateIpTable.select { PrivateIpTable.instanceId eq result[InstanceTable.instanceId] }.forEach {
                    internalIps += it[PrivateIpTable.ip]
                }
                TagsTable.select { TagsTable.instanceId eq result[InstanceTable.instanceId] }.forEach {
                    val (key: String, value: String) = it[TagsTable.tag].split("=")
                    instanceTags += key to value
                }

                instances += instance {
                    instanceId = result[InstanceTable.instanceId]
                    accountId = result[InstanceTable.accountId]
                    region = result[InstanceTable.region]
                    target = result[InstanceTable.target]
                    networkInterfaceIds = nifIds
                    privateIps = internalIps
                    tags = instanceTags
                }
            }
        }
        return instances
    }

    /**
     * Drops all tables utilized by the app
     */
    fun dropAllTables() {
        logInfo("DbUtils") { "Opening connection to ${db.url} to drop all tables" }

        transaction {
            addLogger(Slf4jSqlDebugLogger)

            SchemaUtils.drop(PrivateIpTable, TagsTable, NetworkInterfacesTable, InstanceTable)
        }
    }
}