package model;

/**
 * @author Richard Sundqvist
 *
 */
public interface CallbackListener {

    /**
     * Called by various components when something of note occurs.
     * @param message
     *            The message being sent.
     * @param foo
     *            The associated handling class.
     */
    public void callback (String message, CallbackLevel foo);

    /**
     * 
     * @author Richard Sundqvist
     *
     */
    public static enum CallbackLevel {
        NORMAL, SEVERE;
    }
}
