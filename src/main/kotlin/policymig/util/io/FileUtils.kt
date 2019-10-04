@file:JvmName("FileUtils")
package policymig.util.io

import policymig.model.Policy
import policymig.model.Rule
import policymig.util.FILE_ERROR
import policymig.util.FILE_EXTENSION
import java.io.File

/**
 * Writes policies to a file
 *
 * @param filename file to be written to
 */
fun List<Policy>.writeToPcl(filename: String) {
    require(filename.endsWith(FILE_EXTENSION)) { FILE_ERROR }
    with(File(filename)) {
        forEach { appendText(it.toString()) }
    }
}

/*
    TODO: Update .pcl deserialization
    @author: aayush
    @date: 30/09/19
    @time: 6:59 PM
 */
/**
 * Read policies from a file
 *
 * @param filename file to be read
 * @return list of policies read from file
 */
fun readFromPcl(filename: String): MutableList<Policy> {
    require(filename.endsWith(FILE_EXTENSION)) { FILE_ERROR }

    val policies: MutableList<Policy> = mutableListOf()

    val lines: List<List<List<String>>> = File(filename)
        .bufferedReader()
        .readText()
        .split("\n\n")
        .dropLast(1)                // Remove the last empty lines
        .map { line ->
            /*
            Maps each line to a list of string separated by '=', then filters out lines that have only one value

            Example:
            ```
            policy {                // -> filtered out
                name = "test"       // kept-in and mapped as ["name", "test"]
                [...]
            }                       // -> filtered out
            ```
             */
            line.split("\n")
                .map { value -> value.trim().split("=", limit = 2) }
                .filter { property -> property.size > 1 }
        }

    var name: String
    var description: String
    var target: String
    var region: String?
    var network: String?
    var direction: String
    var ports: List<String>
    var action: String
    var protocol: String
    var sourceIps: List<String>?
    var sourceTags: List<Pair<String, String>>?
    var targetIps: List<String>?
    var targetTags: List<Pair<String, String>>?
    val rules: MutableSet<Rule> = mutableSetOf()

    for (policy: List<List<String>> in lines) {
        name = ""
        description = ""
        target = ""
        region = null
        network = null
        direction = ""
        ports = emptyList()
        action = ""
        protocol = ""
        sourceIps = null
        sourceTags = null
        targetIps = null
        targetTags = null

        for (property: List<String> in policy) {
            when (property[0].trim()) {
                "name" -> name = property[1].trim()
                "description" -> description = property[1].trim()
                "target" -> target = property[1].trim()
                "network" -> network = property[1].trim()
                "region" -> region = property[1].trim()
                "direction" -> direction = property[1].trim().toUpperCase()
                "ports" -> {
                    ports = property[1].trim().split(",").map {
                        // Removes [] from the input
                        it.trim().replace("[\\[\\]]".toRegex(), "")
                    }
                    println("Ports: $protocol, $action, $ports")
                }
                "action" -> {
                    action = property[1].trim()
                    println("Action: $protocol, $action, $ports")
                }
                "protocol" -> {
                    protocol = property[1].trim()
                    println("Protocol: $protocol, $action, $ports")
                }
                "sourceIps" -> {
                    sourceIps = property[1].trim().split(",").map {
                        // Removes [] from the input
                        it.trim().replace("[\\[\\]]".toRegex(), "")
                    }
                    sourceTags = null
                    targetIps = null
                    targetTags = null
                }
                "sourceTags" -> {
                    // Creates a map of all "key=value" elements
                    sourceTags = property[1].trim().split(",").map {
                        val (left: String, right: String) = it.trim().split("=").map {x ->
                            // Removes {} from the input
                            x.trim().replace("[{}]".toRegex(), "")
                        }
                        left to right
                    }
                    sourceIps = null
                    targetIps = null
                    targetTags = null
                }
                "targetIps" -> {
                    targetIps = property[1].trim().split(",").map {
                        // Removes [] from the input
                        it.trim().replace("[\\[\\]]".toRegex(), "")
                    }
                    sourceIps = null
                    sourceTags = null
                    targetTags = null
                }
                "targetTags" -> {
                    // Creates a map of all "key=value" elements
                    targetTags = property[1].trim().split(",").map {
                        val (left: String, right: String) = it.trim().split("=").map {x ->
                            // Removes {} from the input
                            x.trim().replace("[{}]".toRegex(), "")
                        }
                        left to right
                    }
                    sourceIps = null
                    sourceTags = null
                    targetIps = null
                }
            }
            if (ports.isNotEmpty() && action != "" && protocol != "") {
                rules.add(
                    Rule(
                        ports,
                        action,
                        protocol
                    )
                )
            }
        }

        policies.add(Policy(name, description, target, direction, network, region, sourceIps, sourceTags, targetIps, targetTags, rules.toList()))
    }

    return policies
}