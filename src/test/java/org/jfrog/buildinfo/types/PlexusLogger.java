package org.jfrog.buildinfo.types;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;

/**
 * @author yahavi
 */
public class PlexusLogger extends AbstractLogger {

    private final Log log;

    public PlexusLogger(Log log) {
        super(LEVEL_INFO, "Artifactory Maven plugin tests");
        this.log = log;
    }

    @Override
    public void debug(String s, Throwable throwable) {
        log.debug(s, throwable);
    }

    @Override
    public void info(String s, Throwable throwable) {
        log.info(s, throwable);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        log.warn(s, throwable);
    }

    @Override
    public void error(String s, Throwable throwable) {
        log.error(s, throwable);
    }

    @Override
    public void fatalError(String s, Throwable throwable) {
        log.error(s, throwable);
    }

    @Override
    public Logger getChildLogger(String s) {
        return null;
    }
}
