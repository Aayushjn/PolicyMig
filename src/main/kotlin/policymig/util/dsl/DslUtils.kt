package policymig.util.dsl

import policymig.model.Policy
import policymig.model.Rule

/**
 * Internal DSL for [policymig.model.Policy] and [policymig.model.Rule]
 */

@DslMarker
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class PolicyDsl

@PolicyDsl
class PolicyBuilder {
    var name: String = ""
    var description: String = ""
    var target: String = ""
    var direction: String = ""
    var network: String? = null
    var region: String? = null
    var sourceIps: List<String>? = null
    var sourceTags: Map<String, String>? = null
    var targetIps: List<String>? = null
    var targetTags: Map<String, String>? = null
    var rules: MutableList<Rule> = mutableListOf()

    fun rules(block: @PolicyDsl RULES.() -> Unit) = rules.addAll(RULES().apply(block))

    fun build(): Policy =
        Policy(name, description, target, direction, network, region, sourceIps, sourceTags, targetIps, targetTags, rules)
}

@PolicyDsl
class RuleBuilder {
    var ports: List<String> = emptyList()
    var action: String = "allow"
    var protocol: String = "tcp"

    fun build(): Rule = Rule(ports, action, protocol)
}

@PolicyDsl
class RULES: ArrayList<Rule>() {
    fun rule(block: @PolicyDsl RuleBuilder.() -> Unit) = add(RuleBuilder().apply(block).build())
}

fun policy(block: @PolicyDsl PolicyBuilder.() -> Unit): Policy = PolicyBuilder().apply(block).build()