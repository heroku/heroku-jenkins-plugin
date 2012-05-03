package com.heroku;

import com.heroku.api.http.UserAgentValueProvider;

/**
 * @author Ryan Brainard
 */
public class JenkinsUserAgentValueProvider extends UserAgentValueProvider.DEFAULT {

    private final String localUserAgent;

    public JenkinsUserAgentValueProvider() {
        this.localUserAgent = "heroku-jenkins-plugin/" + HerokuPlugin.get().getPluginVersion();
    }

    public String getLocalUserAgent() {
        return localUserAgent;
    }

    public String getHeaderValue(String customPart) {
        return localUserAgent + " " + super.getHeaderValue(customPart);
    }
}
