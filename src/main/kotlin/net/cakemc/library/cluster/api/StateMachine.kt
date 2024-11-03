package net.cakemc.library.cluster.api

class StateMachine {
    /**
     * Returns the current state as an integer value.
     *
     * @return the current state.
     */
    // This int will hold the current combination of states
    var currentState: Int = 0
        private set

    /**
     * Adds a state to the current state using bitwise OR.
     *
     * @param state the state to add (e.g. FIRST, SECOND)
     */
    fun addState(state: Int) {
        currentState = currentState or state
    }

    /**
     * Removes a state from the current state using bitwise AND and bitwise NOT.
     *
     * @param state the state to remove (e.g. FIRST, SECOND)
     */
    fun removeState(state: Int) {
        currentState = currentState and state.inv()
    }

    /**
     * Checks if a state is present in the current state.
     *
     * @param state the state to check (e.g. FIRST, SECOND)
     * @return true if the state is present, false otherwise
     */
    fun hasState(state: Int): Boolean {
        return (currentState and state) == state
    }

    /**
     * Checks if all specified states are present in the current state.
     *
     * @param states a combination of states to check (e.g. FIRST | SECOND)
     * @return true if all the specified states are present, false otherwise
     */
    fun hasAllStates(states: Int): Boolean {
        return (currentState and states) == states
    }

    /**
     * Resets all states to their initial value.
     */
    fun reset() {
        currentState = 0
    }
}
