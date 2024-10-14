package net.cakemc.library.cluster.api;

public class StateMachine {

    // This int will hold the current combination of states
    private int currentState = 0;

    /**
     * Adds a state to the current state using bitwise OR.
     * 
     * @param state the state to add (e.g. FIRST, SECOND)
     */
    public void addState(int state) {
        currentState |= state;
    }

    /**
     * Removes a state from the current state using bitwise AND and bitwise NOT.
     * 
     * @param state the state to remove (e.g. FIRST, SECOND)
     */
    public void removeState(int state) {
        currentState &= ~state;
    }

    /**
     * Checks if a state is present in the current state.
     * 
     * @param state the state to check (e.g. FIRST, SECOND)
     * @return true if the state is present, false otherwise
     */
    public boolean hasState(int state) {
        return (currentState & state) == state;
    }

    /**
     * Checks if all specified states are present in the current state.
     * 
     * @param states a combination of states to check (e.g. FIRST | SECOND)
     * @return true if all the specified states are present, false otherwise
     */
    public boolean hasAllStates(int states) {
        return (currentState & states) == states;
    }

    /**
     * Resets all states to their initial value.
     */
    public void reset() {
        currentState = 0;
    }

    /**
     * Returns the current state as an integer value.
     * 
     * @return the current state.
     */
    public int getCurrentState() {
        return currentState;
    }

}
