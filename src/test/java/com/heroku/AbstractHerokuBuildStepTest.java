package com.heroku;

import com.heroku.api.HerokuAPI;
import com.heroku.api.exception.RequestFailedException;
import net.sf.json.JSONObject;

/**
 * @author Ryan Brainard
 */
public class AbstractHerokuBuildStepTest extends BaseHerokuBuildStepTest {


    public void testGetEffectiveApiKey_OnlyDefaultSet() throws Exception {
        final String defaultApiKey = "DEFAULT API KEY";
        final JSONObject formData = new JSONObject();
        formData.put("defaultApiKey", defaultApiKey);
        HerokuPlugin.get().configure(null, formData);

        final AbstractHerokuBuildStep step = new AbstractHerokuBuildStep(null, null) {
        };
        assertEquals(defaultApiKey, step.getEffectiveApiKey());
    }

    public void testGetEffectiveApiKey_OnlyOverrideSet() throws Exception {
        final AbstractHerokuBuildStep step = new AbstractHerokuBuildStep(apiKey, null) {
        };
        assertEquals(apiKey, step.getEffectiveApiKey());
    }

    public void testGetEffectiveApiKey_OverrideWinWithBothSet() throws Exception {
        final String defaultApiKey = "DEFAULT API KEY";
        final JSONObject formData = new JSONObject();
        formData.put("defaultApiKey", defaultApiKey);
        HerokuPlugin.get().configure(null, formData);

        final AbstractHerokuBuildStep step = new AbstractHerokuBuildStep(apiKey, null) {
        };
        assertEquals(apiKey, step.getEffectiveApiKey());
    }

    public void testGetEffectiveApiKey_NoneSetThrows() throws Exception {
        final AbstractHerokuBuildStep step = new AbstractHerokuBuildStep(null, null) {
        };

        try {
            step.getEffectiveApiKey();
            fail();
        } catch (HerokuJenkinsHandledException e) {
            assertStringContains(e.getMessage(), "Heroku API key not specified");
        }
    }

    public void testGetAppName() throws Exception {
        final AbstractHerokuBuildStep step = new AbstractHerokuBuildStep(null, appName) {
        };
        assertEquals(appName, step.getAppName());
    }

    public void testGetOrCreateApp_BadApiKey() throws Exception {
        final String nonNullBadApiKey = "NON_NULL_BAD_API_KEY";
        final HerokuAPI badApi = new HerokuAPI(api.getConnection(), nonNullBadApiKey);

        final String newAppName = "test" + System.currentTimeMillis();
        assertFalse("Precondition: App should not already exist", api.appExists(newAppName));

        final AbstractHerokuBuildStep step = new AbstractHerokuBuildStep(nonNullBadApiKey, newAppName) {};

        try {
            step.getOrCreateApp(listener, badApi).getName();
            fail();
        } catch (HerokuJenkinsHandledException e){
            assertStringContains(e.getMessage(), "No access to create Heroku app");
        }
    }

    public void testGetOrCreateApp_AlreadyExists_WithoutAccess() throws Exception {
        final String existingAppWithoutAccess = "java";
        assertTrue("Precondition: App should already exist", api.appExists(existingAppWithoutAccess));

        try {
            api.getApp(existingAppWithoutAccess).getName();
            fail("Precondition: User should not have access");
        } catch (RequestFailedException e) {
            assertStringContains(e.getResponseBody(), "You do not have access to the app");
        }

        final AbstractHerokuBuildStep step = new AbstractHerokuBuildStep(apiKey, existingAppWithoutAccess) {};

        try {
            step.getOrCreateApp(listener, api).getName();
            fail();
        } catch (HerokuJenkinsHandledException e){
            assertStringContains(e.getMessage(), "No access to Heroku app '" + existingAppWithoutAccess);
        }
    }

    public void testGetOrCreateApp_AlreadyExists() throws Exception {
        assertEquals("Precondition: App should already exist", appName, api.getApp(appName).getName());
        final AbstractHerokuBuildStep step = new AbstractHerokuBuildStep(apiKey, appName) {
        };
        assertEquals(appName, step.getOrCreateApp(listener, api).getName());
    }

    public void testGetOrCreateApp_NewAppCreated() throws Exception {
        final String newAppName = "testapp" + System.currentTimeMillis();
        try {
            api.getApp(newAppName);
            fail();
        } catch (RequestFailedException e) {
            assertTrue("Precondition: App should not already exist", e.getMessage().contains("Unable to get app"));
        }

        final AbstractHerokuBuildStep step = new AbstractHerokuBuildStep(apiKey, newAppName) {
        };
        try {
            assertEquals(newAppName, step.getOrCreateApp(listener, api).getName());
        } finally {
            api.destroyApp(newAppName);
        }
    }
}
