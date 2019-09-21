package policymig.util

import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.exposedLogger
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.slf4j.LoggerFactory

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
 * Logs SQL queries using Slf4j at INFO level
 */
object Slf4jSqlInfoLogger: SqlLogger {
    override fun log(context: StatementContext, transaction: Transaction) {
        if (exposedLogger.isInfoEnabled) {
            exposedLogger.info(context.expandArgs(TransactionManager.current()))
        }
    }
}