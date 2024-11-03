package test.units

/**
 * The `Pair` class is a generic container that holds two related objects.
 * This class provides a way to group two objects together without creating a specific
 * class for the pair.
 *
 * @param <T1> the type of the first element
 * @param <T2> the type of the second element
</T2></T1> */
class Pair<T1, T2>
/**
 * Constructs a new `Pair` with the specified elements.
 *
 * @param first  the first element of the pair
 * @param second the second element of the pair
 */(
    /**
     * Returns the first element of the pair.
     *
     * @return the first element
     */
    val first: T1,
    /**
     * Returns the second element of the pair.
     *
     * @return the second element
     */
    val second: T2
) {
    /**
     * Returns a string representation of the pair in the format "(first, second)".
     *
     * @return a string representation of the pair
     */
    override fun toString(): String {
        return "($first, $second)"
    }
}
