package io.github.trethore.jcefgithub;

/**
 * Thrown when JCef failed to initialize.
 *
 * @author Fritz Windisch
 */
public class CefInitializationException extends Exception {
    public CefInitializationException(String message) {
        super(message);
    }

    public CefInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
