package policymig.util

import policymig.model.Policy
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

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
    "\"",
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

    with(Paths.get(DIRECTORY + File.separator + "gcp")) {
        if (!Files.exists(this)) {
            Files.createDirectory(this)
        }

        var clonedBlock: MutableList<String>
        policy.rules.forEachIndexed { i, rule ->
            clonedBlock = ArrayList(GCP_FIREWALL_TEMPLATE).apply {
                add(0, "resource \"google_compute_firewall\" \"firewall-${randomHexString()}")
                add(2, policy.name + i)
                add(4, policy.network)
                add(6, policy.direction)
                add(8, policy.description)
                if (policy.sourceIps == null) {
                    removeAt(10)
                } else {
                    add(11, "[" +
                            policy.sourceIps.joinToString { item -> "\"$item\"" } +
                            "]")
                }
                if (policy.targetIps == null) {
                    removeAt(12)
                } else {
                    add(11, "[" +
                            policy.targetIps.joinToString { item -> "\"$item\"" } +
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

            with(File(this.toString(), "firewalls.tf")) {
                clonedBlock.forEach {
                    appendText(it)
                }
            }
        }
    }
}

fun createAwsSecurityGroupBlock(policy: Policy) {
    require(policy.target == "aws") { "Only AWS policies allowed!" }

    var template: MutableList<String>

    with(Paths.get(DIRECTORY + File.separator + "aws")) {
        if (!Files.exists(this)) {
            Files.createDirectory(this)
        }

        with(Paths.get(this.toString() + File.separator + policy.region)) {
            if (!Files.exists(this)) {
                Files.createDirectory(this)
            }

            val providerBlock = ArrayList(AWS_PROVIDER_BLOCK).apply {
                add(1, policy.region)
            }

            with (File(this.toString(), "provider.tf")) {
                writeText("")
                providerBlock.forEach {
                    appendText(it.toString())
                }
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
                            policy.sourceIps?.let { ips ->
                                add(7, "[${ips.joinToString { item -> "\"$item\"" }}]")
                            }
                            policy.targetIps?.let { ips ->
                                add(7, "[${ips.joinToString { item -> "\"$item\"" }}]")
                            }
                        }
                        add(size - 1, template.joinToString(""))
                    }
                }

                with(File(this.toString(), "firewalls.tf")) {
                    clonedBlock.forEach {
                        appendText(it)
                    }
                }
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
    val process = ProcessBuilder(listOf("/bin/sh", "-c", *command.split("\\s").toTypedArray()))
        .directory(File(workingDirectory))
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
        .apply {
            waitFor(timeout, TimeUnit.SECONDS)
        }

    return process.inputStream.bufferedReader().readText() to process.errorStream.bufferedReader().readText()
}

/**
 * Generates a random hex string of  length 8
 *
 * @return Randomly generated hex string
 */
internal fun randomHexString(): String {
    val hexChars = ('a'..'z') + ('0'..'9')
    return hexChars.shuffled().take(8).joinToString("")
}