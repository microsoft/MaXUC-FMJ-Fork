package javax.media;

/**
 * Class specifically created for debugging audio config errors on Mac. Thrown
 * when a plugin starts before being configured (ProcessEngine.setCodecChain)
 * only, to differentiate from other uses of NotConfiguredError.
 */
public class NotConfiguredInCodecChainError extends NotConfiguredError
{
    /**
     * The serialVersionUID inherited from NotConfiguredError.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Throws a NotConfiguredInCodecChainError, which behaves just like a
     * NotConfiguredError except for being identifiable as having come from
     * ProcessEngine.setCodecChain for error reporting purposes.
     *
     * @param message The error message associated with this error
     */
    public NotConfiguredInCodecChainError(String message)
    {
        super(message);
    }
}
