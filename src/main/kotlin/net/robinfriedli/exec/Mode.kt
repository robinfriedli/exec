package net.robinfriedli.exec

/**
 * Helper class to build a mode with the given [ModeWrapper] applied
 */
class Mode {

    private var currentWrapper: ModeWrapper? = null

    companion object {
        @JvmStatic
        fun create(): Mode {
            return Mode()
        }
    }

    /**
     * Add an additional mode. Depending on the implementation of the [ModeWrapper.combine] method,
     * this normally wraps the new wrapper within the current wrapper, meaning the task of the added wrapper will run
     * inside the task of the current wrapper.
     *
     * @param wrapper the new wrapper to add, normally wrapping it inside the current wrapper
     * @return this mode with the new wrapper applied
     */
    fun with(wrapper: ModeWrapper): Mode {
        if (currentWrapper != null) {
            currentWrapper = currentWrapper!!.combine(wrapper)
        } else {
            currentWrapper = wrapper
        }
        return this
    }

    fun getWrapper(): ModeWrapper? {
        return currentWrapper
    }

}