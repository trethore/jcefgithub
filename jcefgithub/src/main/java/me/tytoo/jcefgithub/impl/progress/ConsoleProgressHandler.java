package me.tytoo.jcefgithub.impl.progress;

import me.tytoo.jcefgithub.EnumProgress;
import me.tytoo.jcefgithub.IProgressHandler;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation for the {@link IProgressHandler} interface.
 *
 * @author Fritz Windisch
 */
public class ConsoleProgressHandler implements IProgressHandler {
    private static final Logger LOGGER = Logger.getLogger(ConsoleProgressHandler.class.getName());

    @Override
    public void handleProgress(EnumProgress state, float percent) {
        Objects.requireNonNull(state, "state cannot be null");
        if (percent != -1f && (percent < 0f || percent > 100f)) {
            throw new RuntimeException("percent has to be -1f or between 0f and 100f. Got " + percent + " instead");
        }
        LOGGER.log(Level.INFO, state + " |> " + (percent == -1f ? "In progress..." : percent));
    }
}
