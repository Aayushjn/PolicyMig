@file:JvmName("FileUtils")
package policymig.util.io

import com.google.gson.reflect.TypeToken
import policymig.model.Policy
import policymig.util.FILE_ERROR
import policymig.util.FILE_EXTENSION
import policymig.util.misc.jsonReader
import policymig.util.misc.jsonWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Writes policies to a file
 *
 * @param filename file to be written to
 */
fun List<Policy>.writeToFile(filename: String) {
    require(filename.endsWith(FILE_EXTENSION)) { FILE_ERROR }

    val policies = readFromFile(filename)
    policies.addAll(this)
    FileWriter(filename).use { jsonReader.toJson(policies, it) }
}

/**
 * Read policies from a file
 *
 * @param filename file to be read
 * @return list of policies read from file
 */
fun readFromFile(filename: String): MutableList<Policy> {
    require(filename.endsWith(FILE_EXTENSION)) { FILE_ERROR }

    // If file is empty or doesn't exist, return empty list
    if (File(filename).length() == 0L) {
        return mutableListOf()
    }

    return FileReader(filename).use { jsonWriter.fromJson(it, object : TypeToken<MutableList<Policy>>() {}.type) }
}