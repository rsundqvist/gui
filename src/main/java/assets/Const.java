package assets;

/**
 * Constant container class.
 *
 * @author Richard Sundqvist
 */
public abstract class Const {
    private Const () {
    } // Not to be instantiated.

    public static final long VERSION_NUMBER = Long.MAX_VALUE;

    public static final String PROJECT_NAME = "Lorem Ipsum";
    public static final String PROJECT_SLOGAN = "DATx02-16-23";
    public static final String PROGRAM_NAME = PROJECT_NAME + " - JavaFX Desktop Visualization";
    public static final String DEFAULT_CHANNEL = "DATx02-16-23";
}
