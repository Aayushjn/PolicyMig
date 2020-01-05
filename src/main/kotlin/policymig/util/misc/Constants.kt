package policymig.util.misc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

const val APP_NAME = "PolicyMig"

const val UNCAUGHT_EXCEPTION = 1
const val COMMAND_FAILURE = 2
const val DISCOVERY_NOT_DONE = 3

const val APPLY_COMPLETE = "Policy application complete"

val JSON: Json = Json(JsonConfiguration.Stable.copy(prettyPrint = true))
