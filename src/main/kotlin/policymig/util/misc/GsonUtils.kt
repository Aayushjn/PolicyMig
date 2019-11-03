@file:JvmName("GsonUtils")
package policymig.util.misc

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import policymig.model.Policy
import policymig.model.Rule
import policymig.util.dsl.policy
import java.lang.reflect.Type

/**
 * [com.google.gson.Gson] object for writing JSON files
 */
val jsonWriter: Gson = GsonBuilder()
    .registerTypeAdapter(Pair::class.java, PairSerializer())
    .setPrettyPrinting()
    .create()

/**
 * [com.google.gson.Gson] object for reading JSON files
 */
val jsonReader: Gson = GsonBuilder()
    .registerTypeAdapter(Pair::class.java, PairDeserializer())
    .registerTypeAdapter(Rule::class.java, RuleDeserializer())
    .registerTypeAdapter(Policy::class.java, PolicyDeserializer())
    .create()

/**
 * JSON serializer for [Pair] of [String] to [String]
 */
class PairSerializer: JsonSerializer<Pair<String, String>> {
    override fun serialize(src: Pair<String, String>?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonObject().apply {
            if (src != null) {
                addProperty(src.first, src.second)
            } else {
                addProperty("null", "null")
            }
        }
    }
}

/**
 * JSON deserializer for [Pair] of [String] to [String]
 */
class PairDeserializer: JsonDeserializer<Pair<String, String>> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Pair<String, String> {
        return if (json != null) {
            val jsonObject = json.asJsonObject
            val key = jsonObject.keySet().first()
            key to jsonObject[key].asString
        } else {
            "null" to "null"
        }
    }
}

/**
 * JSON deserializer for [policymig.model.Policy]
 */
class PolicyDeserializer: JsonDeserializer<Policy> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Policy {
        requireNotNull(json)

        val jsonObject = json.asJsonObject

        return policy {
            name = jsonObject["name"].asString
            description = jsonObject["description"].asString
            target = jsonObject["target"].asString
            direction = jsonObject["direction"].asString
            network = if (jsonObject.has("network")) jsonObject["network"].asString else null
            region = if (jsonObject.has("region")) jsonObject["region"].asString else null
            sourceIps = if (jsonObject.has("sourceIps")) context?.deserialize(jsonObject["sourceIps"], object : TypeToken<List<String>?>() {}.type) else null
            sourceTags = if (jsonObject.has("sourceTags")) context?.deserialize(jsonObject["sourceTags"], object : TypeToken<List<Pair<String, String>>?>() {}.type) else null
            targetIps = if (jsonObject.has("targetIps")) context?.deserialize(jsonObject["targetIps"], object : TypeToken<List<String>?>() {}.type) else null
            targetTags = if (jsonObject.has("targetTags")) context?.deserialize(jsonObject["targetTags"], object : TypeToken<List<Pair<String, String>>?>() {}.type) else null
            rules = context?.deserialize(jsonObject["rules"], object : TypeToken<MutableList<Rule>>() {}.type)!!
        }
    }
}

/**
 * JSON deserializer for [policymig.model.Rule]
 */
class RuleDeserializer: JsonDeserializer<Rule> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Rule {
        requireNotNull(json)

        val jsonObject = json.asJsonObject
        return Rule(
            context?.deserialize(jsonObject["ports"], object : TypeToken<List<String>>() {}.type)!!,
            jsonObject["action"].asString,
            jsonObject["protocol"].asString
        )
    }
}