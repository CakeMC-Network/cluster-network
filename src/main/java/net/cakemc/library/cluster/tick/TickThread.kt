package net.cakemc.library.cluster.tick

import net.cakemc.library.cluster.tick.TickAble

/**
 * A thread that periodically calls the tick method of a [TickAble] instance.
 * The tick method is executed at a fixed rate, defined by the TICK_RATE constant.
 * This is useful for tasks that need to be updated regularly in a separate thread.
 */
class TickThread(
    /** The object that will be ticked.  */
    private val tickAble: TickAble
) : Runnable {
    /** The thread that runs the tick loop.  */
    private val tickThread = Thread(this)

    /** Indicates whether the tick thread is running.  */
    private var running = true

    /**
     * Creates a new TickThread for the given [TickAble] instance.
     *
     * @param tickAble The instance to be ticked periodically.
     */
    init {
        tickThread.priority = Thread.NORM_PRIORITY
        tickThread.name = "tick-thread"
        tickThread.isDaemon = true
        tickThread.start()
    }

    /**
     * The main loop of the tick thread.
     * It repeatedly calls the tick method of the TickAble instance at a fixed rate.
     */
    override fun run() {
        while (running) {
            val start = System.currentTimeMillis()

            tickAble.tick()

            val end = System.currentTimeMillis()
            val duration = end - start

            // Sleep for the remaining time to maintain the tick rate.
            if (duration < TICK_RATE) {
                try {
                    Thread.sleep(TICK_RATE - duration)
                } catch (ignored: InterruptedException) {
                    // Thread was interrupted, can safely ignore and exit.
                }
            }
        }
    }

    /**
     * Shuts down the tick thread, stopping the periodic ticking.
     * This method should be called to cleanly terminate the thread.
     */
    fun shutdown() {
        this.running = false

        // Interrupt the thread if it is still running.
        if (!tickThread.isInterrupted) {
            tickThread.interrupt()
        }
    }

    companion object {
        /** The rate at which to tick, in milliseconds.  */
        const val TICK_RATE: Int = 50
    }
}
