package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import hudson.model.BuildListener;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.PrintStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ryan Brainard
 */
public abstract class BaseHerokuBuildStepTest extends HudsonTestCase {

    protected final String appName = System.getProperty("heroku.appName");
    protected final String apiKey = System.getProperty("heroku.apiKey");

    protected final HerokuAPI api = new HerokuAPI(apiKey);
    protected final BuildListener listener = mock(BuildListener.class);
    protected final PrintStream stream = mock(PrintStream.class);

    {
        when(listener.getLogger()).thenReturn(stream);
    }

    /**
     * Runs with a new app and then destroys the app
     *
     * @param runnable the code you want to run in the context of the new app
     */
    protected void runWithNewApp(AppRunnable runnable) throws Exception {
        App newApp = null;
        try {
            newApp = api.createApp();
            runnable.run(newApp);
        } finally {
            if (newApp != null) api.destroyApp(newApp.getName());
        }
    }

    protected interface AppRunnable {
        void run(App app) throws Exception;
    }
}
