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

    protected final String appName = getSystemPropertyOrEnvVar("heroku.appName", "HEROKU_APP_NAME");
    protected final String apiKey = getSystemPropertyOrEnvVar("heroku.apiKey", "HEROKU_API_KEY");

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
    
    private static String getSystemPropertyOrEnvVar(String systemProperty, String envVar) {
        if (System.getProperties().containsKey(systemProperty)) {
            return  System.getProperty(systemProperty);
        } else if (System.getenv().containsKey(envVar)) {
            return System.getenv(envVar);
        } else {
            throw new RuntimeException("Could not log system property [" + systemProperty + "]" +
                                       " or environment variable [" + envVar + "]");
        }
    }
}
