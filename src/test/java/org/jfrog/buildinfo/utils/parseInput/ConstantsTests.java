package org.jfrog.buildinfo.utils.parseInput;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.jfrog.buildinfo.utils.Utils.parseInput;
import static org.junit.Assert.assertEquals;

/**
 * @author yahavi
 */
@RunWith(Parameterized.class)
public class ConstantsTests {

    @Parameterized.Parameters(name = "{index}: parseInput({0})={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"{{}}", ""},
                {"{{abc}}", ""},
                {"{{abc|def}}", ""},
                {"{{A|B|C|def}}", ""},
                {"{{\"\"}}", ""},
                {"{{}}{{\"\"}}", ""},
                {"{{\"\"}}{{}}", ""},
                {"{{\"abc\"}}", "abc"},
                {"{{\"abc|\"def\"}}", "def"},
                {"{{\"abc\"}}def{{\"\"}}", "abcdef"},
                {"{{\"abc\"}}def{{\"zzz\"}}", "abcdefzzz"},
                {"{{\"abc\"}}def{{\"zzz|uuu\"}}", "abcdef"},
                {"{{\"abc|xxx\"}}def{{\"zzz|uuu\"}}", "def"},
                {"{{A|B|C|\"def\"}}", "def"},
                {"{{A|B}}_{{D|E}}", "_"},
                {"{{A|B}}_{{D|E|\"f\"}}", "_f"},
                {"{{A|B|\"c\"}}_{{D|E}}", "c_"},
                {"{{A|B|\"c\"}}_{{D|E|\"ee\"}}", "c_ee"},
                {"{{JAVA_HOME2|EDITOR2}}", ""},
                {"{{JAVA_HOME2|EDITOR2|\"a\"}}", "a"},
                {"{{A|EDITOR2|B|\"aa\"}}", "aa"},
                {"aa{{}}bb", "aabb"},
                {"aa{{\"\"}}", "aa"},
                {"aa{{\"abc\"}}", "aaabc"},
                {"aa{{abc}}", "aa"},
                {"aa{{\"abc|def\"}}", "aa"},
                {"aa{{\"abc|\"def\"}}", "aadef"},
                {"aa{{abc|\"def\"}}", "aadef"},
                {"aa{{}}{{\"\"}}", "aa"},
                {"aa{{\"\"}}{{}}", "aa"},
                {"aa{{\"abc\"}}def{{\"\"}}", "aaabcdef"},
                {"aa{{\"abc\"}}def{{\"\"}}{{qqq}}", "aaabcdef"},
                {"aa{{\"abc\"}}def{{\"zzz\"}}", "aaabcdefzzz"},
                {"aa{{\"abc\"}}def{{\"zzz|uuu\"}}", "aaabcdef"},
                {"aa{{\"abc|xxx\"}}def{{\"zzz|uuu\"}}", "aadef"},
                {"aa{{A|B|C|\"def\"}}", "aadef"},
                {"aa{{A|B|C|def}}", "aa"},
                {"aa{{A|B}}_{{D|E}}", "aa_"},
                {"aa{{A|B}}_{{D|E|\"f\"}}", "aa_f"},
                {"aa{{A|B|\"c\"}}_{{D|E}}", "aac_"},
                {"aa{{A|B|\"c\"}}_{{D|E|\"ee\"}}", "aac_ee"},
                {"aa{{JAVA_HOME2|EDITOR2}}", "aa"},
                {"aa{{JAVA_HOME2|EDITOR2|\"\"}}", "aa"},
                {"aa{{JAVA_HOME2|EDITOR2|\"x\"}}", "aax"},
                {"aa{{A|EDITOR2|B|\"rr\"}}", "aarr"},
                {"aa{{A|C|B|\"rr\"}}z{ff}{{d}}", "aarrz{ff}"}
        });
    }

    @Parameterized.Parameter
    public String expression;

    @Parameterized.Parameter(1)
    public String expected;

    @Test
    public void testParseInput() {
        assertEquals(expected, parseInput(expression));
    }
}
