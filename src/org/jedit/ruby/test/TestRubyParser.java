package org.jedit.ruby.test;

import junit.framework.TestCase;
import org.jedit.ruby.RubyParser;
import org.jedit.ruby.Member;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class TestRubyParser extends TestCase {
    
    public static final String DEF = "def red\n" +
            "end\n";

    public static final String EMPTY_CLASS = "class Green\n" +
            "end\n";

    public static final String CLASS = "class Green\n" +
            DEF +
            "end\n";

    public static final String CLASS_AND_DEF = "class Green\n" +
            "end\n" +
            DEF;

    public static final String EMPTY_MODULE = "module Blue\n" +
            "end\n";

    public static final String EMPTY_CLASS_IN_MODULE = "module Blue\n" +
            EMPTY_CLASS +
            "end\n";

    public static final String CLASS_IN_MODULE = "module Blue\n" +
            CLASS +
            "end\n";

    public static final String DEF_IN_MODULE = "module Blue\n" +
            DEF +
            "end\n";

    public void testEmptyClassInModuleSize() {
        assertSizeCorrect(1, EMPTY_CLASS_IN_MODULE);
    }

    public void testEmptyClassInModule() {
        Member[] members = RubyParser.getMembers(EMPTY_CLASS_IN_MODULE);
//        assertCorrect(0, "blue", 7, members);
        assertCorrect(0, "Blue::Green", 18, members);
    }

    public void testClassInModuleSize() {
        assertSizeCorrect(2, CLASS_IN_MODULE);
    }

    public void testClassInModule() {
        Member[] members = RubyParser.getMembers(CLASS_IN_MODULE);
//        assertCorrect(0, "blue", 7, members);
        assertCorrect(0, "Blue::Green", 18, members);
        assertCorrect(1, "red", 28, members);
    }

    public void testDefInModuleSize() {
        assertSizeCorrect(1, DEF_IN_MODULE);
    }

    public void testDefInModule() {
        Member[] members = RubyParser.getMembers(DEF_IN_MODULE);
//        assertCorrect(0, "blue", 7, members);
        assertCorrect(0, "red", 16, members);
    }

    public void testParseDefSize() {
        assertSizeCorrect(1, DEF);
    }

    public void testParseDef() {
        Member[] members = RubyParser.getMembers(DEF);
        assertCorrect(0, "red", 4, members);
    }

    public void testParseEmptyClassSize() {
        assertSizeCorrect(1, EMPTY_CLASS);
    }

    public void testParseEmptyClass() {
        Member[] members = RubyParser.getMembers(EMPTY_CLASS);
        assertCorrect(0, "Green", 6, members);
    }

    public void testParseEmptyModuleSize() {
        assertSizeCorrect(0, EMPTY_MODULE);
    }

//    public void testParseEmptyModule() {
//        Member[] members = RubyParser.getMembers(EMPTY_MODULE);
//        assertCorrect(0, "blue", 7, members);
//    }

    public void testParseClassSize() {
        assertSizeCorrect(2, CLASS);
    }

    public void testParseClass() {
        Member[] members = RubyParser.getMembers(CLASS);
        assertCorrect(0, "Green", 6, members);
        assertCorrect(1, "red", 16, members);
    }

    public void testParseClassAndDefSize() {
        assertSizeCorrect(2, CLASS_AND_DEF);
    }

    public void testParseClassAndDef() {
        Member[] members = RubyParser.getMembers(CLASS_AND_DEF);
        assertCorrect(0, "Green", 6, members);
        assertCorrect(1, "red", 20, members);
    }

    private void assertSizeCorrect(int resultSize, String content) {
        Member[] members = RubyParser.getMembers(content);
        assertEquals("assert result size is correct", resultSize, members.length);
    }

    private void assertCorrect(int index, String name, int offset, Member[] members) {
        try {
            Member member = members[index];
            assertEquals("Assert name correct", name, member.getDisplayName());
            assertEquals("Assert offset correct", offset, member.getOffset());
        } catch (Exception e) {
            fail("Member not in result: " + name);
        }
    }

}
