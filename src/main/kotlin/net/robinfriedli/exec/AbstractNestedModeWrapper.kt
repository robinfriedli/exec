package net.robinfriedli.exec

/**
 * Mode wrapper implementation that combines ModeWrappers by nesting additional ModeWrappers inside this one.
 * When applying a mode to a task the ModeWrappers will be iterated inside out to combine a nested Callable that calls
 * the outermost ModeWrapper first in the order they were added to the mode.
 */
abstract class AbstractNestedModeWrapper : ModeWrapper {

    private var outer: ModeWrapper? = null

    override fun combine(mode: ModeWrapper): ModeWrapper {
        mode.setOuter(this)
        return mode
    }

    override fun getOuter(): ModeWrapper? {
        return outer
    }

    override fun setOuter(mode: ModeWrapper?) {
        this.outer = mode
    }

}