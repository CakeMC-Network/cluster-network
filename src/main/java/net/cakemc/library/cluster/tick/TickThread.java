package net.cakemc.library.cluster.tick;

/**
 * A thread that periodically calls the tick method of a {@link TickAble} instance.
 * The tick method is executed at a fixed rate, defined by the TICK_RATE constant.
 * This is useful for tasks that need to be updated regularly in a separate thread.
 */
public class TickThread implements Runnable {

	/** The rate at which to tick, in milliseconds. */
	static final int TICK_RATE = 50;

	/** The object that will be ticked. */
	private final TickAble tickAble;

	/** The thread that runs the tick loop. */
	private final Thread tickThread;

	/** Indicates whether the tick thread is running. */
	private boolean running;

	/**
	 * Creates a new TickThread for the given {@link TickAble} instance.
	 *
	 * @param tickAble The instance to be ticked periodically.
	 */
	public TickThread(TickAble tickAble) {
		this.tickAble = tickAble;
		running = true;

		tickThread = new Thread(this);
		tickThread.setPriority(Thread.NORM_PRIORITY);
		tickThread.setName("tick-thread");
		tickThread.setDaemon(true);
		tickThread.start();
	}

	/**
	 * The main loop of the tick thread.
	 * It repeatedly calls the tick method of the TickAble instance at a fixed rate.
	 */
	@Override
	public void run() {
		while (running) {
			long start = System.currentTimeMillis();

			tickAble.tick();

			long end = System.currentTimeMillis();
			long duration = end - start;

			// Sleep for the remaining time to maintain the tick rate.
			if (duration < TICK_RATE) {
				try {
					Thread.sleep(TICK_RATE - duration);
				} catch (InterruptedException ignored) {
					// Thread was interrupted, can safely ignore and exit.
				}
			}
		}
	}

	/**
	 * Shuts down the tick thread, stopping the periodic ticking.
	 * This method should be called to cleanly terminate the thread.
	 */
	public void shutdown() {
		this.running = false;

		// Interrupt the thread if it is still running.
		if (!tickThread.isInterrupted()) {
			tickThread.interrupt();
		}
	}
}
