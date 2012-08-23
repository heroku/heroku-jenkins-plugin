package com.heroku;

/**
 * @author Ryan Brainard
 */
public class HerokuJenkinsHandledException extends RuntimeException {

    public HerokuJenkinsHandledException(String message) {
        super(message);
    }
}
