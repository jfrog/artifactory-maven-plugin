package org.jfrog.buildinfo.utils.parseInput;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.jfrog.buildinfo.utils.Utils.parseInput;
import static org.junit.Assert.assertEquals;

/**
 * @author yahavi
 */
@RunWith(Parameterized.class)
public class EnvironmentVariableTests {

    @Parameterized.Parameters(name = "{index}: parseInput({0})={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"{{JAVA_HOME}}", "JAVA_HOME"},
                {"{{A|JAVA_HOME|B}}", "JAVA_HOME"},
                {"{{JAVA_HOME|JAVA_HOME}}", "JAVA_HOME"},
                {"{{AAA|JAVA_HOME}}", "JAVA_HOME"},
                {"{{JAVA_HOME|BBB}}", "JAVA_HOME"},
                {"{{JAVA_HOME}}|{{JAVA_HOME}}", "JAVA_HOME|JAVA_HOME"}
        });
    }

    @Parameterized.Parameter
    public String expression;

    @Parameterized.Parameter(1)
    public String expected;

    @Test
    public void testParseInput() {
        expected = Arrays.stream(expected.split("\\|")).map(System::getenv).collect(Collectors.joining("|"));
        assertEquals(expected, parseInput(expression));
    }
}
