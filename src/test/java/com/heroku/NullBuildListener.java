package com.heroku;

import hudson.console.ConsoleNote;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Result;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

/**
* @author Ryan Brainard
*/
class NullBuildListener implements BuildListener {
    public void started(List<Cause> causes) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void finished(Result result) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public PrintStream getLogger() {
        return new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                // ignore
            }
        });
    }

    public void annotate(ConsoleNote ann) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void hyperlink(String url, String text) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public PrintWriter error(String msg) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PrintWriter error(String format, Object... args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PrintWriter fatalError(String msg) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PrintWriter fatalError(String format, Object... args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
