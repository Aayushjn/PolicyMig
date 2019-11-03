@file:JvmName("LogUtils")
package policymig.util.misc

import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Logs to DEBUG level
 *
 * @param name name of logger
 * @param msg msg to be logged
 */
inline fun logDebug(name: String = APP_NAME, msg: () -> Any) = with(LoggerFactory.getLogger(name)) {
    if (isDebugEnabled) {
        debug(msg().toString())
    }
}

/**
 * Logs to INFO level
 *
 * @param name name of logger
 * @param msg msg to be logged
 */
inline fun logInfo(name: String = APP_NAME, msg: () -> Any) = LoggerFactory.getLogger(name).info(msg().toString())

/**
 * Logs to WARN level
 *
 * @param name name of logger
 * @param msg msg to be logged
 */
inline fun logWarning(name: String = APP_NAME, msg: () -> Any) = LoggerFactory.getLogger(name).warn(msg().toString())

/**
 * Logs to ERROR level
 *
 * @param name name of logger
 * @param msg msg to be logged
 */
inline fun logError(name: String = APP_NAME, exitCode: Int = UNCAUGHT_EXCEPTION, msg: () -> Any) {
    LoggerFactory.getLogger(name).error(msg().toString())
    exitProcess(exitCode)
}

/**
 * Extension function to get stacktrace as string
 *
 * @return stacktrace as a string
 */
fun Throwable.stackTraceAsString(): String = with(StringWriter()) {
    printStackTrace(PrintWriter(this, true))
    return buffer.toString()
}