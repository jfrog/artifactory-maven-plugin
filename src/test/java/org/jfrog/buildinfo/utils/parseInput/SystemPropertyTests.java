package org.jfrog.buildinfo.utils.parseInput;

import org.junit.BeforeClass;
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
public class SystemPropertyTests {

    @BeforeClass
    public static void setUp() {
        System.setProperty("SOME_PROP", "SOME_VAL");
    }

    @Parameterized.Parameters(name = "{index}: parseInput({0})={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"{{SOME_PROP}}", "SOME_PROP"},
                {"{{A|SOME_PROP|B}}", "SOME_PROP"},
                {"{{SOME_PROP|SOME_PROP}}", "SOME_PROP"},
                {"{{AAA|SOME_PROP}}", "SOME_PROP"},
                {"{{SOME_PROP|BBB}}", "SOME_PROP"},
                {"{{SOME_PROP}}|{{SOME_PROP}}", "SOME_PROP|SOME_PROP"}
        });
    }

    @Parameterized.Parameter
    public String expression;

    @Parameterized.Parameter(1)
    public String expected;

    @Test
    public void testParseInput() {
        expected = Arrays.stream(expected.split("\\|")).map(System::getProperty).collect(Collectors.joining("|"));
        assertEquals(expected, parseInput(expression));
    }
}
