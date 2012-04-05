package com.heroku;

import com.heroku.api.HerokuAPI;
import hudson.model.BuildListener;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.PrintStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ryan Brainard
 */
public class BaseHerokuBuildStepTest extends HudsonTestCase {

    protected final String appName = System.getProperty("heroku.appName");
    protected final String apiKey = System.getProperty("heroku.apiKey");

    protected final HerokuAPI api = new HerokuAPI(apiKey);
    protected final BuildListener listener = mock(BuildListener.class);
    protected final PrintStream stream = mock(PrintStream.class);

    {
        when(listener.getLogger()).thenReturn(stream);
    }
}
