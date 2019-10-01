@file:JvmName("Progress")
package policymig.util.misc

/**
 * Rotating progress bar for console
 *
 * @property begin message to be displayed during progress
 * @property end message to be displayed after progress complete
 * @property anim progress characters
 * @property showProgress flag to stop the loading animation
 */
private class ProgressBar(private val begin: String = "Processing", private val end: String = "Done"): Thread() {
    private val anim = "|/-\\"
    internal var showProgress = true

    override fun run() {
        var x = 0
        while (showProgress) {
            print("\r$begin ${anim[x++ % anim.length]}")
            try {
                sleep(100)
            } catch (e: Exception) {}
        }
        println("\r$end")
    }
}

/**
 * Shows progress bar for specified [function]
 *
 * @param begin message to be displayed during progress
 * @param end message to be displayed after progress complete
 * @param function function to be invoked during which progress is shown
 *
 * @return returns the return value of [function]
 */
internal fun <T> showLoading(begin: String, end: String, function: () -> T): T =
    with(ProgressBar(begin, end)) {
        start()
        val returnValue = function()
        showProgress = false
        return returnValue
    }