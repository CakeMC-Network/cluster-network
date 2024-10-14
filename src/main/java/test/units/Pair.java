package test.units;

/**
 * The {@code Pair} class is a generic container that holds two related objects.
 * This class provides a way to group two objects together without creating a specific
 * class for the pair.
 *
 * @param <T1> the type of the first element
 * @param <T2> the type of the second element
 */
public class Pair<T1, T2> {
	private final T1 first;
	private final T2 second;

	/**
	 * Constructs a new {@code Pair} with the specified elements.
	 *
	 * @param first  the first element of the pair
	 * @param second the second element of the pair
	 */
	public Pair(T1 first, T2 second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * Returns the first element of the pair.
	 *
	 * @return the first element
	 */
	public T1 getFirst() {
		return first;
	}

	/**
	 * Returns the second element of the pair.
	 *
	 * @return the second element
	 */
	public T2 getSecond() {
		return second;
	}

	/**
	 * Returns a string representation of the pair in the format "(first, second)".
	 *
	 * @return a string representation of the pair
	 */
	@Override
	public String toString() {
		return "(" + first + ", " + second + ")";
	}
}
