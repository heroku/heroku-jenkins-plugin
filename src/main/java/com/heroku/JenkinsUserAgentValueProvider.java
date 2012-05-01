package com.heroku;

import com.heroku.api.http.UserAgentValueProvider;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Ryan Brainard
 */
public class JenkinsUserAgentValueProvider extends UserAgentValueProvider.DEFAULT {

    private final String localUserAgent;

    public JenkinsUserAgentValueProvider() {
        this.localUserAgent = "heroku-jenkins-plugin/" + loadProjectVersion();
    }

    private static String loadProjectVersion() {
        try {
            Properties projectProperties = new Properties();
            projectProperties.load(JenkinsUserAgentValueProvider.class.getClassLoader().
                    getResourceAsStream("heroku-jenkins-plugin.properties"));
            return String.valueOf(projectProperties.get("heroku-jenkins-plugin.version"));
        } catch (IOException e) {
            return "";
        }
    }

    public String getHeaderValue(String customPart) {
        return localUserAgent + " " + super.getHeaderValue(customPart);
    }
}
