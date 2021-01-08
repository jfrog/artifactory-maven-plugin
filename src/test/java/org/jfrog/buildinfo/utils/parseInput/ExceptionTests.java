package org.jfrog.buildinfo.utils.parseInput;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.jfrog.buildinfo.utils.Utils.parseInput;
import static org.junit.Assert.assertThrows;

/**
 * @author yahavi
 */
@RunWith(Parameterized.class)
public class ExceptionTests {

    @Parameterized.Parameters(name = "{index}: parseInput({0})")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"{{"},
                {"abc{{"},
                {"abc{{def"},
                {"abc{{def{{"},
                {"abc{{def{{ghi"}
        });
    }

    @Parameterized.Parameter
    public String expression;

    @Test
    public void testParseInput() {
        assertThrows(IllegalArgumentException.class, () -> parseInput(expression));
    }
}
