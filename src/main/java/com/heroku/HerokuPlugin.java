package com.heroku;

import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.collections.map.UnmodifiableMap;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * @author Ryan Brainard
 */
public final class HerokuPlugin extends Plugin {

    static HerokuPlugin get() {
        return Hudson.getInstance().getPlugin(HerokuPlugin.class);
    }

    /**
     * @return Unmodifiable map of project properties
     */
    private static Map<String, String> loadProjectProperties() {
        Properties projectProperties = new Properties();
        try {
            projectProperties.load(HerokuPlugin.class.getClassLoader().getResourceAsStream("heroku-jenkins-plugin.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //noinspection unchecked
        return UnmodifiableMap.decorate(projectProperties);
    }

    private Secret defaultApiKey;
    private final Map<String, String> projectProperties = loadProjectProperties();

    @Override
    public void start() throws Exception {
        load();
    }

    /**
     * For Jenkins UI
     *
     * @return encrypted api key
     */
    public String getDefaultApiKey() {
        return "".equals(Secret.toString(defaultApiKey)) ? "" : defaultApiKey.getEncryptedValue();
    }

    /**
     * For internal plugin use
     *
     * @return plain text api key
     */
    String getDefaultApiKeyPlainText() {
        return Secret.toString(defaultApiKey);
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, Descriptor.FormException {
        super.configure(req, formData);
        defaultApiKey = Secret.fromString(formData.getString("defaultApiKey"));
        save();
    }

    /**
     * @return version of this heroku-jenkins-plugin project from pom.xml
     */
    String getPluginVersion() {
        return projectProperties.get("heroku-jenkins-plugin.version");
    }

    enum Feature {
        ANVIL
    }

    boolean hasFeature(Feature feature) {
        final String features = System.getProperty("com.heroku.jenkins.features");
        return features != null && features.contains(feature.name());
    }
}
