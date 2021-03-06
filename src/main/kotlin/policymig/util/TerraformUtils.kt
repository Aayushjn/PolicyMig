@file:JvmName("TerraformUtils")
package policymig.util

import policymig.model.Policy
import policymig.util.db.DbUtils
import policymig.util.misc.COMMAND_FAILURE
import policymig.util.misc.logError
import policymig.util.misc.logInfo
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

private const val FILENAME = "TerraformUtils"

const val DIRECTORY: String = "terraform-resources"
const val GCP_PROVIDER_VERSION: String = "2.15.0"
const val AWS_PROVIDER_VERSION: String = "2.28.1"

private val GCP_PROVIDER_BLOCK: MutableList<String> = mutableListOf(
    "provider \"google\" {\n\tversion = \"~> ${GCP_PROVIDER_VERSION}\"\n\tproject = \"",
    "\"\n\tcredentials = file(\"",
    "\")\n}\n"
)

private val GCP_FIREWALL_TEMPLATE: MutableList<String> = mutableListOf(
    "\" {\n\tname = \"",
    "\"\n\tnetwork = \"",
    "\"\n\tdirection = \"",
    "\"\n\tdescription = \"",
    "\"",
    "\n\tsource_ranges = ",
    "\n\tdestination_ranges = ",
    "\n}\n\n"
)

private val ALLOW_TEMPLATE: MutableList<String> = mutableListOf(
    "\n\tallow {\n\t\tprotocol = \"",
    "\"\n\t\tports = ",
    "\n\t}"
)

private val DENY_TEMPLATE: MutableList<String> = mutableListOf(
    "\n\tdeny {\n\t\tprotocol = \"",
    "\"\n\t\tports = ",
    "\n\t}"
)

private val AWS_PROVIDER_BLOCK: MutableList<String> = mutableListOf(
    "provider \"aws\" {\n\tversion = \"~> ${AWS_PROVIDER_VERSION}\"\n\tregion = \"",
    "\"\n}\n"
)

private val SECURITY_GROUP_BLOCK: MutableList<String> = mutableListOf(
    "\" {\n\tname = \"",
    "\"\n\tdescription = \"",
    "\"\n\ttags = {\n\t\tcreatedAt = \"",
    "\"\n\t}",
    "\n}\n\n"
)

private val INGRESS_BLOCK: MutableList<String> = mutableListOf(
    "\n\tingress {\n\t\tfrom_port = ",
    "\n\t\tto_port = ",
    "\n\t\tprotocol = \"",
    "\"\n\t\tcidr_blocks = ",
    "\n\t}"
)

private val EGRESS_BLOCK: MutableList<String> = mutableListOf(
    "\n\tegress {\n\t\tfrom_port = ",
    "\n\t\tto_port = ",
    "\n\t\tprotocol = \"",
    "\"\n\t\tcidr_blocks = ",
    "\n\t}"
)

/**
 * Creates a provider block for GCP as defined by Terraform
 *
 * @param project Name of project on GCP
 * @param credentialsFile path to JSON credentials file
 */
fun createGcpProviderBlock(project: String, credentialsFile: String) =
    with(Paths.get(DIRECTORY + File.separator + "gcp")) {
        if (!Files.exists(this)) {
            Files.createDirectory(this)
        }
        val clonedBlock = ArrayList(GCP_PROVIDER_BLOCK).apply {
            add(1, project)
            add(3, credentialsFile)
        }

        with (File(this.toString(), "provider.tf")) {
            writeText("")
            clonedBlock.forEach {
                appendText(it.toString())
            }
        }
    }

/**
 * Creates a "google_compute_firewall" resource block as defined by Terraform
 * The number of blocks created depends on the number of rules the [policy] contains
 *
 * @param policy Policy to be written as a "google_compute_firewall" resource
 */
fun createGcpFirewallBlock(policy: Policy) {
    require(policy.target == "gcp") { "Only GCP policies allowed!" }

    val path = Paths.get(DIRECTORY + File.separator + "gcp")
    if (!Files.exists(path)) {
        Files.createDirectory(path)
    }

    var clonedBlock: MutableList<String>
    policy.rules.forEachIndexed { i, rule ->
        clonedBlock = ArrayList(GCP_FIREWALL_TEMPLATE).apply {
            add(0, "resource \"google_compute_firewall\" \"firewall-${randomHexString()}")
            add(2, policy.name + i)
            add(4, policy.network)
            add(6, policy.direction)
            add(8, policy.description)
            if (policy.sourceTags == null) {
                if (policy.sourceIps == null) {
                    removeAt(10)
                } else {
                    add(11, "[" +
                            policy.sourceIps.joinToString { item -> "\"$item\"" } +
                            "]")
                }
            } else {
                val ips = DbUtils.fetchTagsAsIp(policy.target, policy.sourceTags)
                add(11, "[" +
                        ips.joinToString { item -> "\"$item\"" } +
                        "]")
            }
            if (policy.targetTags == null) {
                if (policy.targetIps == null) {
                    removeAt(12)
                } else {
                    add(11, "[" +
                            policy.targetIps.joinToString { item -> "\"$item\"" } +
                            "]")
                }
            } else {
                val ips = DbUtils.fetchTagsAsIp(policy.target, policy.targetTags)
                add(11, "[" +
                        ips.joinToString { item -> "\"$item\"" } +
                        "]")
            }

            val template: MutableList<String> = if (rule.action == "allow") {
                ArrayList(ALLOW_TEMPLATE)
            } else {
                ArrayList(DENY_TEMPLATE)
            }.apply {
                add(1, rule.protocol)
                add(3, "[" +
                        rule.ports.joinToString { port -> "\"$port\"" } +
                        "]")
            }
            add(size - 1, template.joinToString(""))
        }

        with(File(path.toString(), "firewalls.tf")) {
            clonedBlock.forEach {
                appendText(it)
            }
        }
    }
}

/**
 * Creates a "aws_security_group" resource block as defined by Terraform
 * The number of blocks created depends on the number of rules the [policy] contains
 *
 * @param policy Policy to be written as a "aws_security_group" resource
 */
fun createAwsSecurityGroupBlock(policy: Policy) {
    require(policy.target == "aws") { "Only AWS policies allowed!" }

    var template: MutableList<String>

    val outerPath = Paths.get(DIRECTORY + File.separator + "aws")
    if (!Files.exists(outerPath)) {
        Files.createDirectory(outerPath)
    }

    val innerPath = Paths.get(outerPath.toString() + File.separator + policy.region)
    if (!Files.exists(innerPath)) {
        Files.createDirectory(innerPath)
    }

    val providerBlock = ArrayList(AWS_PROVIDER_BLOCK).apply { add(1, policy.region) }

    with(File(innerPath.toString(), "provider.tf")) {
        writeText("")
        providerBlock.forEach { line -> appendText(line) }
    }

    var clonedBlock: MutableList<String>
    policy.rules.forEachIndexed { i, rule ->
        clonedBlock = ArrayList(SECURITY_GROUP_BLOCK).apply {
            add(0, "resource \"aws_security_group\" \"firewall-${randomHexString()}")
            add(2, policy.name + i)
            add(4, policy.description)

            rule.ports.forEach { port ->
                template = if (policy.direction.toUpperCase() == "INGRESS") {
                    ArrayList(INGRESS_BLOCK)
                } else {
                    ArrayList(EGRESS_BLOCK)
                }
                if (port.split("-").size == 2) {
                    template.apply {
                        val (from, to) = port.split("-", limit = 2)
                        add(1, from)
                        add(3, to)
                    }
                } else {
                    template.apply {
                        add(1, port)
                        add(3, port)
                    }
                }
                template.apply {
                    if (rule.protocol == "all") {
                        add(5, "-1")
                    } else {
                        add(5, rule.protocol)
                    }
                    if (policy.sourceTags == null) {
                        policy.sourceIps?.let { ip ->
                            add(7, "[${ip.joinToString { item -> "\"$item\"" }}]")
                        }
                    } else {
                        val ips = DbUtils.fetchTagsAsIp(policy.target, policy.sourceTags)
                        add(7, "[${ips.joinToString { item -> "\"$item\"" }}]")
                    }

                    if (policy.targetTags == null) {
                        policy.targetIps?.let { ip ->
                            add(7, "[${ip.joinToString { item -> "\"$item\"" }}]")
                        }
                    } else {
                        val ips = DbUtils.fetchTagsAsIp(policy.target, policy.targetTags)
                        add(7, "[${ips.joinToString { item -> "\"$item\"" }}]")
                    }
                }
                add(size - 1, template.joinToString(""))
            }
            add(6, LocalDateTime.now().toString())
        }

        with(File(innerPath.toString(), "firewalls.tf")) {
            clonedBlock.forEach { appendText(it) }
        }
    }
}

/**
 * Perform Terraform commands for GCP
 */
fun terraformGcp() {
    val initCommand = runCommand("terraform init", "$DIRECTORY/gcp/")
    logInfo(FILENAME) { initCommand.first }
    if (initCommand.second != "") {
        logError(FILENAME, COMMAND_FAILURE) { initCommand.second }
    }
    val planCommand = runCommand("terraform plan -out plan.out", "$DIRECTORY/gcp/")
    logInfo(FILENAME) { planCommand.first }
    if (planCommand.second != "") {
        logError(FILENAME, COMMAND_FAILURE) { planCommand.second }
    }
    val applyCommand = runCommand("terraform apply plan.out", "$DIRECTORY/gcp/", 180)
    logInfo(FILENAME) { applyCommand.first }
    if (applyCommand.second != "") {
        logError(FILENAME, COMMAND_FAILURE) { applyCommand.second }
    }
}

/**
 * Perform Terraform commands for AWS
 */
fun terraformAws() {
    var initCommand: Pair<String, String>
    var planCommand: Pair<String, String>
    var applyCommand: Pair<String, String>

    File(DIRECTORY).listFiles { pathname -> pathname.isDirectory }?.forEach { file ->
        file.listFiles { pathname -> pathname.isDirectory }?.forEach { dir ->
            logInfo { "Performing Terraform commands in $dir" }
            initCommand = runCommand("terraform init", dir.toString())
            logInfo(FILENAME) { initCommand.first }
            if (initCommand.second != "") {
                logError(FILENAME, COMMAND_FAILURE) { initCommand.second }
            }
            planCommand = runCommand("terraform plan -out plan.out", dir.toString())
            logInfo(FILENAME) { planCommand.first }
            if (planCommand.second != "") {
                logError(FILENAME, COMMAND_FAILURE) { planCommand.second }
            }
            applyCommand = runCommand("terraform apply plan.out", dir.toString(), 180)
            logInfo(FILENAME) { applyCommand.first }
            if (applyCommand.second != "") {
                logError(FILENAME, COMMAND_FAILURE) { applyCommand.second }
            }
        }
    }
}

/**
 * Runs an external shell command
 * @param command Shell command to be executed
 * @param workingDirectory directory in which command must be executed. By default, it is $HOME
 * @param timeout seconds to wait for command to finish execution. By default, it waits for a minute
 *
 * @return Returns a pair of the command's outputs to [System.out] and [System.err]
 */
internal fun runCommand(command: String, workingDirectory: String = System.getenv("HOME"), timeout: Long = 60): Pair<String, String> {
    logInfo(FILENAME) { "Running command: $command" }
    val process = ProcessBuilder(listOf("/bin/sh", "-c", *command.split("\\s").toTypedArray()))
        .directory(File(workingDirectory))
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
        .apply { waitFor(timeout, TimeUnit.SECONDS) }

    return process.inputStream.bufferedReader().readText().trim() to process.errorStream.bufferedReader().readText().trim()
}

/**
 * Generates a random hex string of  length 8
 *
 * @return Randomly generated hex string
 */
private fun randomHexString(): String {
    val hexChars = ('a'..'z') + ('0'..'9')
    return hexChars.shuffled().take(8).joinToString("")
}