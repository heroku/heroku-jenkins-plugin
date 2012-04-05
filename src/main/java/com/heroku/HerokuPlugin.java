package com.heroku;

import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * @author Ryan Brainard
 */
public final class HerokuPlugin extends Plugin {

    private Secret defaultApiKey;

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

    static HerokuPlugin get() {
        return Hudson.getInstance().getPlugin(HerokuPlugin.class);
    }
}
