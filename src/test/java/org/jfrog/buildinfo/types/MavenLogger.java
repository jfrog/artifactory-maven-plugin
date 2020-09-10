package org.jfrog.buildinfo.types;

import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * @author yahavi
 */
public class MavenLogger extends SystemStreamLog {
    @Override
    public void debug(CharSequence content) {
    }
}
