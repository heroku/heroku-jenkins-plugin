package com.heroku;

import com.heroku.api.App;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;

import java.util.Map;
import java.util.Set;

/**
 * @author Ryan Brainard
 */
public class ConfigAddTest extends BaseHerokuBuildStepTest {

    public void testConfigAdd() throws Exception {
        runWithNewApp(new AppRunnable() {
            public void run(App app) throws Exception {
                final String appName = app.getName();
                assertTrue(api.listConfig(appName).isEmpty());

                FreeStyleProject project = createFreeStyleProject();
                project.getBuildersList().add(new ConfigAdd(apiKey, appName, "A=a"));
                project.scheduleBuild2(0).get();

                final Map<String, String> configAfter = api.listConfig(appName);
                assertEquals("a", configAfter.get("A"));
            }
        });
    }
}
