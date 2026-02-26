package io.github.trethore.jcefgithub.impl.step.fetch;

import io.github.trethore.jcefgithub.CefBuildInfo;
import io.github.trethore.jcefgithub.EnumPlatform;
import org.cef.CefApp;

import java.io.InputStream;

/**
 * Class used to extract natives from classpath.
 *
 * @author Fritz Windisch
 */
public class PackageClasspathStreamer {
    private static final String LOCATION = "/jcef-natives-{platform}-{tag}.tar.gz";

    public static InputStream streamNatives(CefBuildInfo info, EnumPlatform platform) {
        return CefApp.class.getResourceAsStream(LOCATION
                .replace("{platform}", platform.getIdentifier())
                .replace("{tag}", info.getReleaseTag()));
    }
}
