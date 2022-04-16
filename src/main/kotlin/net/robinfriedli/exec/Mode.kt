package net.robinfriedli.exec

/**
 * Helper class to build a mode with the given [ModeWrapper] applied
 */
open class Mode() {

    private constructor(currentWrapper: ModeWrapper?) : this() {
        this.currentWrapper = currentWrapper
    }

    private var currentWrapper: ModeWrapper? = null

    companion object {
        @JvmStatic
        val empty = create().immutable()

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
     * @return this mode with the new wrapper applied, or a new Mode instance if this Mode was immutable
     */
    open fun with(wrapper: ModeWrapper): Mode {
        currentWrapper = if (currentWrapper != null) {
            currentWrapper!!.combine(wrapper)
        } else {
            wrapper
        }
        return this
    }

    open fun getWrapper(): ModeWrapper? {
        return currentWrapper
    }

    /**
     * Clone this Mode so that changes made to the returned Mode are not reflected by this instance.
     */
    open fun fork(): Mode {
        return Mode(currentWrapper?.fork())
    }

    /**
     * Wraps this Mode into a [ImmutableMode], prohibiting modifications to this instance by making calls to [Mode.with]
     * fork the Mode by calling [Mode.fork] and operating on the cloned instance. Note that instances returned by
     * [ImmutableMode.with] are not immutable.
     */
    fun immutable(): ImmutableMode {
        return ImmutableMode(this)
    }

    class ImmutableMode(private val mode: Mode) : Mode() {
        override fun with(wrapper: ModeWrapper): Mode {
            return mode.fork().with(wrapper)
        }

        override fun getWrapper(): ModeWrapper? {
            return mode.getWrapper()
        }

        override fun fork(): Mode {
            return mode.fork()
        }
    }

}
