package io.github.trethore.jcefgithub.impl.progress;

import io.github.trethore.jcefgithub.EnumProgress;
import io.github.trethore.jcefgithub.IProgressHandler;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation for the {@link IProgressHandler} interface.
 *
 * @author Titouan Réthoré
 */
public class ConsoleProgressHandler implements IProgressHandler {
    private static final Logger LOGGER = Logger.getLogger(ConsoleProgressHandler.class.getName());

    @Override
    public void handleProgress(EnumProgress state, float percent) {
        Objects.requireNonNull(state, "state cannot be null");
        if (percent != EnumProgress.NO_ESTIMATION && (percent < 0f || percent > 100f)) {
            throw new IllegalArgumentException(
                    "percent has to be " + EnumProgress.NO_ESTIMATION + " or between 0f and 100f. Got " + percent
                            + " instead");
        }
        LOGGER.log(Level.INFO, state + " |> " + (percent == EnumProgress.NO_ESTIMATION ? "In progress..." : percent));
    }
}
