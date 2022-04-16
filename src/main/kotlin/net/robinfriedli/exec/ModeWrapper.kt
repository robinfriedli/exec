package net.robinfriedli.exec

import java.util.concurrent.Callable

/**
 * Applies a mode to a task by wrapping the task in a new callable. When applying a mode to a task the ModeWrappers added
 * to the Mode will be iterated inside out from the last ModeWrapper that has been added. For example consider the
 * following mode:
 * ```
 * var mode = Mode.create()
 *     .with(modeWrapper1)
 *     .with(modeWrapper2)
 *     .with(modeWrapper3)
 * ```
 *
 * When applying this mode to a task, the task will first be wrapped into the modeWrapper3 which will then be nested
 * inside modeWrapper2 which will then be nested inside modeWrapper1, provided all those modes extend [AbstractNestedModeWrapper].
 * The top level callable that ends up being executed is the one returned by modeWrapper1#wrap().
 */
interface ModeWrapper : Iterable<ModeWrapper> {

    /**
     * Applies the mode this ModeWrapper represents by wrapping the given [Callable] in a new Callable
     */
    fun <T> wrap(callable: Callable<T>): Callable<T>

    /**
     * Returns a new [ModeWrapper] that represents a combination of this and the given ModeWrapper. In case of an
     * [AbstractNestedModeWrapper] this sets this ModeWrapper as the outer ModeWrapper of the given ModeWrapper. When
     * applying a mode to a task the ModeWrappers will be iterated inside out to combine a nested Callable that calls
     * the outermost ModeWrapper first in the order they were added to the mode.
     */
    fun combine(mode: ModeWrapper): ModeWrapper

    fun getOuter(): ModeWrapper?

    fun setOuter(mode: ModeWrapper?)

    /**
     * Clones this ModeWrapper for use in a new [Mode]. Changes to the returned ModeWrapper are not reflected by this instance.
     *
     * If the [ModeWrapper.combine] implementation does not modify this instance, meaning this implementation is practically
     * immutable, this method does not have to do anything and can return the current instance.
     */
    fun fork(): ModeWrapper

    override fun iterator(): Iterator<ModeWrapper> {
        return object : Iterator<ModeWrapper> {

            private var current: ModeWrapper? = null

            override fun hasNext(): Boolean {
                // current == null means the iterator has not been accessed yet, the iterator always has at least
                // the current ModeWrapper as item
                return current == null || current!!.getOuter() != null
            }

            override fun next(): ModeWrapper {
                current = if (current == null) {
                    // the iterator has not been accessed before
                    this@ModeWrapper
                } else {
                    current!!.getOuter()
                }
                return current!!
            }

        }
    }
}
