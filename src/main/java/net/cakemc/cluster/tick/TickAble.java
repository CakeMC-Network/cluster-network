package net.cakemc.cluster.tick;

/**
 * The {@code TickAble} interface represents a contract for classes that
 * require a periodic update mechanism. Implementing this interface
 * allows an object to define its own behavior during each tick of
 * a timer or game loop.
 */
public interface TickAble {

	/**
	 * Performs an update operation. This method is typically called
	 * at regular intervals to allow the implementing class to
	 * execute logic such as updating state, processing events, or
	 * managing resources.
	 */
	void tick();
}
