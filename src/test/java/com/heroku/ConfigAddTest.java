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

    public void testPerform() throws Exception {
        runWithNewApp(new AppRunnable() {
            public void run(App app) throws Exception {
                // clean up config
                for (Map.Entry<String, String> c : api.listConfig(app.getName()).entrySet()) {
                    api.removeConfig(app.getName(), c.getKey());
                }
                assertTrue(api.listConfig(app.getName()).isEmpty());

                FreeStyleProject project = createFreeStyleProject();
                project.getBuildersList().add(new ConfigAdd(apiKey, app.getName(), "A=a"));
                project.scheduleBuild2(0).get();

                final Map<String, String> configAfter = api.listConfig(app.getName());
                assertEquals("a", configAfter.get("A"));
            }
        });
    }
}
