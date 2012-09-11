package com.heroku;

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
        // clean up config
        for (Map.Entry<String, String> c : api.listConfig(appName).entrySet()) {
            api.removeConfig(appName, c.getKey());
        }
        assertTrue(api.listConfig(appName).isEmpty());

        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new ConfigAdd(apiKey, appName, "A=a"));
        project.scheduleBuild2(0).get();

        final Map<String, String> configAfter = api.listConfig(appName);
        assertEquals("a", configAfter.get("A"));
    }
}
