@file:JvmName("LogUtils")
package policymig.util.misc

import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Logs to INFO level
 *
 * @param name name of logger
 * @param msg msg to be logged
 */
inline fun logInfo(name: String = "PolicyMig", msg: () -> Any) = LoggerFactory.getLogger(name).info(msg().toString())

/**
 * Logs to WARN level
 *
 * @param name name of logger
 * @param msg msg to be logged
 */
inline fun logWarning(name: String = "PolicyMig", msg: () -> Any) = LoggerFactory.getLogger(name).warn(msg().toString())

/**
 * Logs to ERROR level
 *
 * @param name name of logger
 * @param msg msg to be logged
 */
inline fun logError(name: String = "PolicyMig", msg: () -> Any) = LoggerFactory.getLogger(name).error(msg().toString())

/**
 * Extension function to get stacktrace as string
 *
 * @return stacktrace as a string
 */
fun Throwable.stackTraceAsString(): String = with(StringWriter()) {
    printStackTrace(PrintWriter(this, true))
    return buffer.toString()
}