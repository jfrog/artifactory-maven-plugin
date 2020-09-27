package org.jfrog.buildinfo.types;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

/**
 * Used as an execution event in BuildInfoRecorderTest.
 *
 * @author yahavi
 */
public class TestExecutionEvent implements ExecutionEvent {

    private final MavenSession session;
    private final MavenProject project;

    public TestExecutionEvent(MavenSession session, MavenProject project) {
        this.session = session;
        this.project = project;
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public MavenSession getSession() {
        return session;
    }

    @Override
    public MavenProject getProject() {
        return project;
    }

    @Override
    public MojoExecution getMojoExecution() {
        return null;
    }

    @Override
    public Exception getException() {
        return null;
    }
}
