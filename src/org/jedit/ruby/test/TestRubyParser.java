/*
 * TestRubyParser.java -
 *
 * Copyright 2005 Robert McKinnon
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.jedit.ruby.test;

import junit.framework.TestCase;
import org.jedit.ruby.*;
import org.jedit.ruby.cache.*;
import org.jedit.ruby.parser.RubyParser;
import org.jedit.ruby.parser.LineCounter;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.ast.ClassMember;
import org.jedit.ruby.ast.MethodCallWithSelfAsAnImplicitReceiver;
import org.jruby.lexer.yacc.ISourcePosition;

import java.util.List;
import java.util.ArrayList;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class TestRubyParser extends TestCase {

    private static final String ERROR_CODE = "fmodule Red class Head\n" +
            "  end\n" +
            "end";

    private static final String ARR_DEF = "def []\n" +
            "end\n";

    private static final String DEF = "def red\n" +
            "end\n";

    private static final String EMPTY_CLASS = "class  Green\n" +
            "end\n";

    private static final String MODULE_MODULE = "module Red::Green\n" +
            "end\n";

    private static final String MODULE_CLASS = "class Red::Green\n" +
            "end\n";

    private static final String MMODULE_CLASS = "class Blue::Red::Green\n" +
            "end\n";

    public static final String CLASS = "class Green\n" +
            DEF +
            "end\n";

    private static final String NESTED_CLASS = "class Greener\n" + //14
            EMPTY_CLASS +
            "end\n";

    private static final String WIN_CLASS = "class Green\r\n" +
            DEF +
            "end\r\n";

    public static final String ARR_CLASS = "class Green\n" +
            ARR_DEF +
            "end\n";

    private static final String CLASS_AND_DEF = "class Green\n" +
            "end\n" +
            DEF;

    private static final String EMPTY_MODULE = "module Blue\n" +
            "end\n";

    private static final String DOUBLE_MODULE = "module Blue ; module Purple\n" +
            "end\n" +
            "end\n";

    private static final String TRIPLE_MODULE = "module Blue ; module Purple; module Mauve\n" +
            "end\n" +
            "end end\n";

    private static final String QUAD_MODULE = "module Blue ; module Purple; module Mauve;module Pink\n" +
            "end\n" +
            "end end end\n";

    private static final String MODULE_METHOD = "module Blue\n" + //12
            "  def Blue.deep\n" +
            "  end\n" +
            "end\n";

    private static final String EMPTY_CLASS_IN_MODULE = "module Blue\n" +
            EMPTY_CLASS +
            "end\n";

    private static final String CLASS_IN_MODULE = "module Blue\n" +
            CLASS +
            "end\n";

    private static final String ARR_CLASS_IN_MODULE = "module Blue\n" +
            ARR_CLASS +
            "end\n";

    public static final String DEF_IN_MODULE = "module Blue\n" +
            DEF +
            "end\n";

    private static final String PATH = "this/path/";

    private static final String DUCK = "class Duck\n" +
            "  def quack\n" +
            "      \n" +
            "  end\n" +
            "end";

    private static final String ITER = "b.each do |i|\n" +
            " \n" +
            "end";

    private static final String EVAL_IN_METHOD = "      def method_missing(method, *args, &block)\n" +
            "          remote = eval %{\n" +
            "            result = lambda { |block, *args| #{method}(*args, &block) }\n" +
            "            def result.call_with_block(*args, &block)\n" +
            "              call(block, *args)\n" +
            "            end\n" +
            "          }\n" +
            "      end\n" +
            "      def find_me\n" +
            "      end\n";

    private static final String ONE_LINER = "def count; @contents.split.size; end";

    private static final String ANOTHER_ONE_LINER = "  def bark() puts 'bark' end";

    private static final String SINGLE_LINE_CLASS = "  \nclass Red; " + ONE_LINER + "; end";

    private static final String SINGLE_LINE_MODULE = "module Ship; class Green; def blue; end; end; end";

    private static final String TWO_SINGLE_LINE_MODULES = "\nmodule Ships; class Greens; def blues; end; end; end\n\n"+
            SINGLE_LINE_MODULE + "\n";

    private static final String PARAM_METHOD = "def red(blue, green)\n"
    +"end";

    private static final String PARAM_METHOD2 = "def red blue, green\n"
    +"end";

    private static final String PARAM_METHOD3 = "def red(r)\n"
    +"end";

    private static final String PARAM_METHOD4 = "def red r # jam hot\n"
    +"end";

    private static final String PARAM_METHOD5 = "  def waddle  a,\\\n" +
            "    b, \\\n" +
            "    c # blue\n" +
            "    puts \"waddling\"\n" +
            "  end";

    private static final String PARAM_METHOD6 = "  def waddle  a,\\\n" +
            "    b; \\\n" +
            "    c # blue\n" +
            "    puts \"waddling\"\n" +
            "  end";

    private static final String METHOD_CALL_WITH_SELF_AS_AN_IMPLICIT_RECEIVER = "it 'should' do\n" +
            "  puts 'hi'\n" +
            "end";

    private static final String NESTED_METHOD_CALL_WITH_SELF_AS_AN_IMPLICIT_RECEIVER = "describe Model, 'when' do\n" +
            METHOD_CALL_WITH_SELF_AS_AN_IMPLICIT_RECEIVER + "\n" +
            "end";            

    private static final String CLASS_WITH_METHOD_WITH_SELF_AS_AN_IMPLICIT_RECEIVER = "class WordTokenizer\n" +
            "  attr_reader :words\n" +
            "end";

    private static final String CLASS_METHOD_DOT = "class Red\n" +
            "  def Red.hot\n" + // 12, 20
            "  end\n" +
            "end";

    private static final String CLASS_METHOD_DOUBLE_COLON = "class Red\n" +
            "  def Red::hot\n" + // 12, 21
            "  end\n" +
            "end";

    private static final String SELF_METHOD_DOT = "class Red\n" +
            "  def self.hot\n" + // 12, 21
            "  end\n" +
            "end";

    private static final String SELF_METHOD_DOUBLE_COLON = "class Red\n" +
            "  def self::hot\n" + // 12, 22
            "  end\n" +
            "end";

    private static final String FILE_WITH_REQUIRE_AT_END = "module ActiveResource\n" +
            "end\n" +
            "require 'active_resource/formats/json_format'";

    private static final String METHOD_TO_CLASS_SELF = "class HttpMock\n" +
            "  class << self\n" +
            "    def requests\n" +
            "    end\n" +
            "  end\n" +
            "end";

    private static final String RAKE_TASK = "  task :download_hansard => :environment do\n" +
            "  end";

    private String code;

    public void setUp() {
        RubyCache.resetCache();
    }

    private static void assertNameCorrect(String expected, List<Member> members) {
        assertEquals("Assert method name correct", expected, members.get(0).getName());
    }

    public final void testParseExtendedClass() {
        assertSuperClassCorrect("Less");
    }

    public final void testParseExtendedClass2() {
        assertSuperClassCorrect("Base::Less");
    }

    public final void testParseExtendedClass3() {
        assertSuperClassCorrect("ActiveRecord::Base::Less");
    }

    private void assertSuperClassCorrect(String superClass) {
        List<Member> members = getMembersList("class Red < "+superClass +"\nend");
        ClassMember classMember = (ClassMember)members.get(0);
        assertEquals("Assert superclass name correct.", superClass, classMember.getSuperClassName());
    }

    public final void testParseParamMethod() {
        List<Member> members = getMembersList(PARAM_METHOD);
        assertNameCorrect("red(blue, green)", members);
    }

    public final void testParseParamMethod2() {
        List<Member> members = getMembersList(PARAM_METHOD2);
        assertNameCorrect("red(blue, green)", members);
    }

    public final void testParseParamMethod3() {
        List<Member> members = getMembersList(PARAM_METHOD3);
        assertNameCorrect("red(r)", members);
    }

    public final void testParseParamMethod4() {
        List<Member> members = getMembersList(PARAM_METHOD4);
        assertNameCorrect("red(r)", members);
    }

    public final void testParseParamMethod5() {
        List<Member> members = getMembersList(PARAM_METHOD5);
        assertNameCorrect("waddle(a,b,c)", members);
    }

    public final void testParseParamMethod6() {
        List<Member> members = getMembersList(PARAM_METHOD6);
        assertNameCorrect("waddle(a,b)", members);
    }

    public final void testEvalInMethod() {
        List<Member> members = getMembersList(EVAL_IN_METHOD);
        assertCorrect(0, "method_missing(method, *args, &block)", null, 6, 10, 271, members);
        assertCorrect(1, "find_me", null, 278, 282, 299, members);
    }

    public final void testIterParse() {
        try {
            RubyParser.getMembers(ITER, getUniquePath());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass().getName());
        }
    }

    public final void testRakeTask() {
        Member member = getMembersList(RAKE_TASK).get(0);
        assertTrue(member instanceof MethodCallWithSelfAsAnImplicitReceiver);
        MethodCallWithSelfAsAnImplicitReceiver call = (MethodCallWithSelfAsAnImplicitReceiver) member;
        assertEquals(":download_hansard", call.getFirstArgument());        
    }

    public final void testClassWithMethodWithSelfAsAnImplicitReceiver() {
        List<Member> members = getMembersList(CLASS_WITH_METHOD_WITH_SELF_AS_AN_IMPLICIT_RECEIVER);
        assertCorrect(0, "WordTokenizer", null, 0, 6, 44, members);
        assertCorrect(0, "attr_reader", "WordTokenizer", 22, 34, 40, getChildMembers(members), false);
        MethodCallWithSelfAsAnImplicitReceiver call = (MethodCallWithSelfAsAnImplicitReceiver) getChildMembers(members).get(0);
        assertEquals(":words", call.getFirstArgument());
    }

    public final void testRSpecItRecognition() {
        List<Member> members = getMembersList(METHOD_CALL_WITH_SELF_AS_AN_IMPLICIT_RECEIVER);
        Member member = members.get(0);
        assertTrue(member instanceof MethodCallWithSelfAsAnImplicitReceiver);
        assertCorrect(0, "it", null, 0, 3, 30, members);
        MethodCallWithSelfAsAnImplicitReceiver call = (MethodCallWithSelfAsAnImplicitReceiver) member;
        assertEquals("'should'", call.getFirstArgument());
        assertFalse("assert has no child members", members.get(0).hasChildMembers());
    }

    public final void testRSpecDescribeRecognition() {
        List<Member> members = getMembersList(NESTED_METHOD_CALL_WITH_SELF_AS_AN_IMPLICIT_RECEIVER);
        Member member = members.get(0);
        assertTrue(member instanceof MethodCallWithSelfAsAnImplicitReceiver);
        MethodCallWithSelfAsAnImplicitReceiver call = (MethodCallWithSelfAsAnImplicitReceiver) member;
        assertEquals("Model", call.getFirstArgument());
        assertEquals("'when'", call.getArgument(1));
        assertCorrect(0, "describe", null, 0, 9, 60, members);
        Member childMember = member.getChildMembers()[0];
        assertTrue(childMember instanceof MethodCallWithSelfAsAnImplicitReceiver);
        assertChildrenCorrect(members, "it", 26, 29, 56, "describe");
    }

    public final void testOneLiner() {
        LineCounter lineCounter = new LineCounter(ONE_LINER);
        String line = lineCounter.getLine(0);
        assertTrue("assert line end correct", line.endsWith("end"));
        List<Member> membersAsList = getMembersList(ONE_LINER);
        assertCorrect(0, "count", null, 0, 4, 36, membersAsList);
    }

    public final void testSingleLineClass() {
        List<Member> membersAsList = getMembersList(SINGLE_LINE_CLASS);
        assertCorrect(0, "Red", null, 3, 9, 55, membersAsList);
        assertChildrenCorrect(membersAsList, "count", 14, 18, 50, "Red");
    }

    public final void testTwoSingleLineModules() {
        List<Member> membersAsList = getMembersList(TWO_SINGLE_LINE_MODULES);
        assertCorrect(0, "Ships", null, 1, 8, 53, membersAsList);
        assertChildrenCorrect(membersAsList, "Ships::Greens", 15, 21, 48, "Ships");
        assertChildrenCorrect(getChildMembers(membersAsList), "blues", 29, 33, 43, "Ships");

        assertCorrect(1, "Ship", null, 55, 62, 104, membersAsList);
        Member member = membersAsList.get(1).getChildMembersAsList().get(0);
        assertEquals("Assert position correct.", 74, member.getStartOffset());
        member = member.getChildMembersAsList().get(0);
        assertEquals("Assert position correct.", 85, member.getStartOffset());
    }

    public final void testSingleLineModule() {
        List<Member> membersAsList = getMembersList(SINGLE_LINE_MODULE);
        assertCorrect(0, "Ship", null, 0, 7, 49, membersAsList);
        assertChildrenCorrect(membersAsList, "Ship::Green", 13, 19, 44, "Ship");
        assertChildrenCorrect(getChildMembers(membersAsList), "blue", 26, 30, 39, "Ship");
    }

    public final void testOneLiner2() {
        LineCounter lineCounter = new LineCounter(ANOTHER_ONE_LINER);
        String line = lineCounter.getLine(0);
        assertTrue("assert line end correct", line.endsWith("end"));
        String code = ANOTHER_ONE_LINER;
        List<Member> membersAsList = getMembersList(code);
        assertCorrect(0, "bark", null, 2, 6, 28, membersAsList);
    }

    public final void testEndOffsets() {
        RubyMembers members = RubyParser.getMembers(DUCK, getUniquePath());
        Member member = members.getLastMemberBefore(6);
        assertEquals("Member correct.", "Duck", member.getName());
        assertEquals("End char correct.", DUCK.charAt(38), 'd');
        assertEquals("Class offset correct.", 39, member.getEndOffset());

        member = members.getLastMemberBefore(13);
        assertEquals("Member correct.", "Duck", member.getName());
        member = members.getLastMemberBefore(14);
        assertEquals("Member correct.", "Duck", member.getName());
        member = members.getLastMemberBefore(17);
        assertEquals("Member correct.", "quack", member.getName());
        member = members.getLastMemberBefore(18);
        assertEquals("Member correct.", "quack", member.getName());
        assertEquals("End char correct.", DUCK.charAt(35), '\n');
        assertEquals("Class offset correct.", 35, member.getEndOffset());
    }

    public final void testErrors() {
        RubyMembers members = RubyParser.getMembers(ERROR_CODE, getUniquePath());
        assertTrue("Assert errors exist", members.containsErrors());
        assertEquals("Assert error count correct", 3, members.getProblems().length);
//        assertEquals("Assert error count correct", 1, members.getProblems().length);
    }

    private static String getUniquePath() {
        return PATH + System.currentTimeMillis();
    }

    public final void testCodeNotEndingWithEnd() {
        List<Member> members = getMembersList(FILE_WITH_REQUIRE_AT_END);
        assertCorrect(0, "ActiveResource", null, 0, 7, 25, members);
        assertCorrect(1, "require", null, 26, 34, 71, members, false);
    }

    public final void testMethodAddedToClassSelf() {
        List<Member> members = getMembersList(METHOD_TO_CLASS_SELF);
        assertCorrect(0, "HttpMock", null, 0, 6, 65, members);
        assertChildrenCorrect(members, "requests", 35, 39, 55, "HttpMock");
    }

    public final void testParentOfDef() {
        List<Member> members = getMembersList(CLASS);
        Member method = members.get(0).getChildMembers()[0];
        assertTrue("Assert has parent", method.hasParentMember());
        assertEquals("Assert parent correct", "Green", method.getParentMember().getFullName());
    }

    public final void testParseModuleMethod() {
        List<Member> members = getMembersList(MODULE_METHOD);
        assertCorrect(0, "Blue", null, 0, 7, 37, members);
        assertChildrenCorrect(members, "Blue::deep", 14, 23, 33, "Blue");
    }

    public final void testBigFile() {
        List<Member> members = RubyParser.getMembersAsList(bigFile, getUniquePath(), new TestListener());
        members.toString();
    }

    public final void testEmptyClassInModuleSize() {
        assertSizeCorrect(1, EMPTY_CLASS_IN_MODULE);
    }

    public final void testEmptyClassInModule() {
        List<Member> members = getMembersList(EMPTY_CLASS_IN_MODULE);
        assertCorrect(0, "Blue", null, 0, 7, 32, members);
        assertChildrenCorrect(members, "Blue::Green", 12, 19, 28, "Blue");
    }

    public final void testPreviousMemberBeforeModuleMember() {
        assertPreviousMemberCorrect(EMPTY_CLASS_IN_MODULE, 6, null);
    }

    public final void testPreviousMemberAfterModuleMember() {
        assertPreviousMemberCorrect(EMPTY_CLASS_IN_MODULE, 8, "Blue");
    }

    public final void testPreviousMemberAfterClassMember() {
        assertPreviousMemberCorrect(EMPTY_CLASS_IN_MODULE, 19, "Blue::Green");
    }

    public final void testPreviousMemberAtMember() {
        assertPreviousMemberCorrect(EMPTY_CLASS_IN_MODULE, 7, "Blue");
    }

    public final void testPreviousMemberInModuleClass() {
        assertPreviousMemberCorrect(bigFile, 2832, "with_http");
    }

    private List<Member> assertChildrenCorrect(List<Member> members, String name, int outerOffset, int offset, int endOffset, String parentName) {
        members = getChildMembers(members);
        assertCorrect(0, name, parentName, outerOffset, offset, endOffset, members);
        return members;
    }

    public final void testClassInModuleSize() {
        assertSizeCorrect(1, CLASS_IN_MODULE);
    }

    public final void testParseClassInModule() {
        List<Member> members = getMembersList(CLASS_IN_MODULE);
        assertCorrect(0, "Blue", null, 0, 7, 43, members);
        members = assertChildrenCorrect(members, "Blue::Green", 12, 18, 39, "Blue");
        assertChildrenCorrect(members, "red", 24, 28, 35, "Blue");
    }

    public final void testParseArrClassInModule() {
        List<Member> members = getMembersList(ARR_CLASS_IN_MODULE);
        assertCorrect(0, "Blue", null, 0, 7, 42, members);
        members = assertChildrenCorrect(members, "Blue::Green", 12, 18, 38, "Blue");
        assertChildrenCorrect(members, "[]", 24, 28, 34, "Blue");
    }

    public final void testDefInModuleSize() {
        assertSizeCorrect(1, DEF_IN_MODULE);
    }

    public final void testParseDefInModule() {
        List<Member> members = getMembersList(DEF_IN_MODULE);
        assertCorrect(0, "Blue", null, 0, 7, 27, members);
        assertChildrenCorrect(members, "red", 12, 16, 23, "Blue");
    }

    public final void testParseClassInClass() {
        List<Member> members = getMembersList(NESTED_CLASS);
        assertCorrect(0, "Greener", null, 0, 6, 34, members);
        assertChildrenCorrect(members, "Greener::Green", 14, 21, 30, "Greener");
    }

    public final void testParseDefSize() {
        assertSizeCorrect(1, DEF);
    }

    public final void testParseDef() {
        List<Member> members = getMembersList(DEF);
        assertCorrect(0, "red", null, 0, 4, 11, members);
    }

    public final void testPreviousDefSingleLineModule() {
        RubyMembers members = RubyParser.getMembers(SINGLE_LINE_MODULE, getUniquePath());
        System.out.println(members);
        assertPreviousMemberCorrect(members, 48, "blue");
        assertPreviousMemberCorrect(members, 29, "Green");
        assertPreviousMemberCorrect(members, 30, "Green");
        assertPreviousMemberCorrect(members, 18, "Ship");
    }

    private static void assertPreviousMemberCorrect(RubyMembers members, int caretPosition, String expected) {
        String name = members.getPreviousMember(caretPosition).getName();
        assertEquals("Assert previous member from "+caretPosition+" correct.", expected, name);
    }

    public final void testNextDef() {
        code = "\n" + DEF + "\n";
        RubyMembers members = RubyParser.getMembers(code, getUniquePath());
        Member nextMember = members.getNextMember(0);
        List<Member> list = new ArrayList<Member>();
        list.add(nextMember);
        assertCorrect(0, "red", null, 1, 5, 12, list);
    }

    public final void testParseArrDefSize() {
        assertSizeCorrect(1, ARR_DEF);
    }

    public final void testParseArrDef() {
        List<Member> members = getMembersList(ARR_DEF);
        assertCorrect(0, "[]", null, 0, 4, 10, members);
    }

    public final void testParseEmptyClassSize() {
        assertSizeCorrect(1, EMPTY_CLASS);
    }

    public final void testParseEmptyClass() {
        List<Member> members = getMembersList(EMPTY_CLASS);
        assertCorrect(0, "Green", null, 0, 7, 16, members);
    }

    public final void testParseModuleModule() {
        List<Member> members = getMembersList(MODULE_MODULE);
        assertCorrect(0, "Red::Green", null, 0, 7, 21, members);
    }

    public final void testParseModuleClass() {
        List<Member> members = getMembersList(MODULE_CLASS);
        assertCorrect(0, "Red::Green", null, 0, 6, 20, members);
    }

    public final void testParseModuleModuleClass() {
        List<Member> members = getMembersList(MMODULE_CLASS);
        assertCorrect(0, "Blue::Red::Green", null, 0, 6, 26, members);
    }

    public final void testParseEmptyModuleSize() {
        assertSizeCorrect(1, EMPTY_MODULE);
    }

    public final void testParseDoubleModuleSize() {
        assertSizeCorrect(1, DOUBLE_MODULE);
    }

    public final void testParseEmptyModule() {
        List<Member> members = getMembersList(EMPTY_MODULE);
        assertCorrect(0, "Blue", null, 0, 7, 15, members);
    }

    public final void testParseDoubleModule() {
        List<Member> members = getMembersList(DOUBLE_MODULE);
        assertCorrect(0, "Blue", null, 0, 7, 35, members);
        assertChildrenCorrect(members, "Blue::Purple", 14, 21, 31, "Blue");
    }

    public final void testParseTripleModule() {
        List<Member> members = getMembersList(TRIPLE_MODULE);
        assertCorrect(0, "Blue", null, 0, 7, 53, members);
        assertChildrenCorrect(members, "Blue::Purple", 14, 21, 49, "Blue");
        assertChildrenCorrect(getChildMembers(members), "Blue::Purple::Mauve", 29, 36, 45, "Blue");
    }

    public final void testParseQuadModule() {
        List<Member> members = getMembersList(QUAD_MODULE);
        assertCorrect(0, "Blue", null, 0, 7, 69, members);
        assertChildrenCorrect(members, "Blue::Purple", 14, 21, 65, "Blue");
        assertChildrenCorrect(getChildMembers(members), "Blue::Purple::Mauve", 29, 36, 61, "Blue");
        assertChildrenCorrect(getChildMembers(getChildMembers(members)), "Blue::Purple::Mauve::Pink", 42, 49, 57, "Blue");
    }

    public final void testParseClassSize() {
        assertSizeCorrect(1, CLASS);
    }

    public final void testParseClass() {
        List<Member> members = getMembersList(CLASS);
        assertCorrect(0, "Green", null, 0, 6, 27, members);
        assertChildrenCorrect(members, "red", 12, 16, 23, "Green");
    }

    public final void testParseClassMethod() {
        List<Member> members = getMembersList(CLASS_METHOD_DOT);
        assertCorrect(0, "Red", null, 0, 6, 33, members);
        assertChildrenCorrect(members, "Red::hot", 12, 20, 29, "Red");
    }

    public final void testParseClassMethodDoubleColon() {
        List<Member> members = getMembersList(CLASS_METHOD_DOUBLE_COLON);
        assertCorrect(0, "Red", null, 0, 6, 34, members);
        assertChildrenCorrect(members, "Red::hot", 12, 21, 30, "Red");
    }

    public final void testParseSelfMethod() {
        List<Member> members = getMembersList(SELF_METHOD_DOT);
        assertCorrect(0, "Red", null, 0, 6, 34, members);
        assertChildrenCorrect(members, "self::hot", 12, 21, 30, "Red");
    }

    public final void testParseSelfMethodDoubleColon() {
        List<Member> members = getMembersList(SELF_METHOD_DOUBLE_COLON);
        assertCorrect(0, "Red", null, 0, 6, 35, members);
        assertChildrenCorrect(members, "self::hot", 12, 22, 31, "Red");
    }

    public final void testParseWinClass() {
        List<Member> members = getMembersList(WIN_CLASS);
        assertCorrect(0, "Green", null, 0, 6, 28, members);
        assertChildrenCorrect(members, "red", 13, 17, 24, "Green");
    }

    public final void testParseArrClass() {
        List<Member> members = getMembersList(ARR_CLASS);
        assertCorrect(0, "Green", null, 0, 6, 26, members);
        assertChildrenCorrect(members, "[]", 12, 16, 22, "Green");
    }

    public final void testParseClassAndDefSize() {
        assertSizeCorrect(2, CLASS_AND_DEF);
    }

    public final void testParseClassAndDef() {
        List<Member> members = getMembersList(CLASS_AND_DEF);
        assertCorrect(0, "Green", null, 0, 6, 15, members);
        assertCorrect(1, "red", null, 16, 20, 27, members);
    }

    public final void testClassMethodSize() {
        assertSizeCorrect(2, classMethodFile);
    }

    public final void testClassMethodCall() {
        List<Member> members = getMembersList(classMethodFile);
        assertCorrect(0, "One", null, 0, 6, 67, members);
        assertChildrenCorrect(members, "to_yaml(opts)", 11, 15, 63, "One");
        assertCorrect(1, "Two", null, 68, 74, 81, members);
    }

    private void assertSizeCorrect(int resultSize, String content) {
        List<Member> members = getMembersList(content);
        assertEquals("assert result size is correct", resultSize, members.size());
    }

    private static List<Member> getChildMembers(List<Member> members) {
        assertTrue("assert has child members", members.get(0).hasChildMembers());
        return members.get(0).getChildMembersAsList();
    }

    private static final class TestListener implements RubyParser.WarningListener {
//        public final void warn(SourcePosition position, String message) {
        public void warn(ISourcePosition position, String message) {
            RubyPlugin.log(message, getClass());
        }

        public final void warn(String message) {
            RubyPlugin.log(message, getClass());
        }

//        public final void warning(SourcePosition position, String message) {
        public final void warning(ISourcePosition position, String message) {
            RubyPlugin.log(message, getClass());
        }

        public final void warning(String message) {
            RubyPlugin.log(message, getClass());
        }

//        public final void error(SourcePosition position, String message) {
        public final void error(ISourcePosition position, String message) {
            RubyPlugin.log(message, getClass());
        }

        public final void clear() {
        }

        public boolean isVerbose() {
            return false;
        }
    }

    private static void assertPreviousMemberCorrect(String text, int caretPosition, String expectedName) {
        RubyMembers members = RubyParser.getMembers(text, getUniquePath(), new TestListener(), false);
        Member member = members.getLastMemberBefore(caretPosition);

        if(expectedName == null) {
            assertNull("assert previous member is null", member);
        } else {
            assertEquals("assert previous member correct", expectedName, member.getFullName());
        }
    }

    private void assertCorrect(int index, String name, String parentName, int outerOffset, int offset, int endOffset, List<Member> members) {
        assertCorrect(index, name, parentName, outerOffset, offset, endOffset, members, true);
    }

    private void assertCorrect(int index, String name, String parentName, int outerOffset, int offset, int endOffset, List<Member> members, boolean lookForEndKeyword) {
        try {
            Member member = members.get(index);
            assertEquals("Assert name correct", name, member.getFullName());
            assertEquals("Assert start offset correct", offset, member.getStartOffset());
            assertEquals("Assert outer start offset correct", outerOffset, member.getStartOuterOffset());
            int end = member.getEndOffset();
            assertEquals("Assert end offset correct.", endOffset, end);
            if (lookForEndKeyword) {
                assertEquals("End char correct.", 'e', code.charAt(end-3));
                assertEquals("End char correct.", 'n', code.charAt(end-2));
                assertEquals("End char correct.", 'd', code.charAt(end-1));
            }

            List<Member> memberPath = member.getMemberPath();

            if(parentName == null) {
                assertEquals("assert empty list", 1, memberPath.size());
                assertEquals("assert self path member correct", name, memberPath.get(0).getFullName());
            } else {
                assertEquals("assert top path member correct", parentName, memberPath.get(0).getFullName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getClass().getName() + ", " + e.getMessage());
        }
    }

    private List<Member> getMembersList(String code) {
        this.code = code;
        return RubyParser.getMembersAsList(code, getUniquePath(), null);
    }

    public final void testGlobalIf() {
        String code = globalIfFile;
        List<Member> members = getMembersList(code);
        assertCorrect(0, "File", null, 0, 6, 63, members);
        assertChildrenCorrect(members, "File::open", 27, 36, 54, "File");
        assertCorrect(1, "Test", null, 64, 70, 99, members);
        assertCorrect(0, "initialize", "Test", 76, 80, 95, members.get(1).getChildMembersAsList());
    }

    private static final String globalIfFile = "class File\n" + //11
            " if ($debug)\n" + //24
            "   def File.open(*args)\n" +
            "   end\n" +
            " end\n" +
            "end\n" +
            "class Test\n" +
            " def initialize\n" +
            " end\n" +
            "end";

    private static final String classMethodFile = "class One\n" +
            "\tdef to_yaml(opts)\n" +
            "\t\tself.class if Hash === opts\n" +
            "\tend\n" +
            "end\n" +
            "class Two\n" +
            "end";

    private static final String bigFile = "module HTTPCache\n" +
            "\n" +
            "\tVERSION = '0.1.0'\n" +
            "\tHOMEDIR = ENV['HOME'] || ENV['USERPROFILE'] || ENV['HOMEPATH']\n" +
            "\tCACHEDIR = File.join(HOMEDIR, '.httpcache')\n" +
            "\tDir.mkdir(CACHEDIR) unless File.exists? CACHEDIR\n" +
            "\n" +
            "\t##\n" +
            "\t# Obtains HTTP response based on the supplied url,\n" +
            "\t# and returns <code>CachedResponse</code>.\n" +
            "\t# Uses ETag and Last-Modified date to retrieve\n" +
            "\t# the content only when it has really changed.\n" +
            "\t# If not changed the cached content is returned.\n" +
            "\t#\n" +
            "\t# Params:\n" +
            "\t#  url, e.g. 'http://www.ruby-lang.org/'\n" +
            "\t#  proxy_address, optional e.g. 'localhost'\n" +
            "\t#  proxy_port, optional e.g. '8080'\n" +
            "\t#\n" +
            "\t# Example 1:\n" +
            "\t#  url = 'http://www.ruby-lang.org/'\n" +
            "\t#  cache = HTTPCache.get(url)\n" +
            "\t#  p cache.content\n" +
            "\t#\n" +
            "\t# Example 2:\n" +
            "\t#  # If you're running a proxy web server at port 8080\n" +
            "\t#  proxy_address = 'localhost'\n" +
            "\t#  proxy_port = '8080'\n" +
            "\t#  url = 'http://www.ruby-lang.org/'\n" +
            "\t#  cache = HTTPCache.get(url, proxy_address, proxy_port)\n" +
            "\t#  p cache.content\n" +
            "\t#\n" +
            "\tdef HTTPCache.get url, proxy_address=nil, proxy_port=nil\n" +
            "\t\turl_dir = Digest::MD5.hexdigest url\n" +
            "\t\tcache_path = File.join CACHEDIR, url_dir\n" +
            "\t\turi = URI.parse url\n" +
            "\n" +
            "\t\tretriever = Retriever.new uri, cache_path, proxy_address, proxy_port\n" +
            "\t\tretriever.retrieve\n" +
            "\tend\n" +
            "\n" +
            "\tclass Retriever\n" +
            "\n" +
            "\t\tdef initialize uri, cache_path, proxy_address, proxy_port\n" +
            "\t\t\t@uri = uri\n" +
            "\t\t\t@cache_path = cache_path\n" +
            "\t\t\t@header_file = @cache_path + '/header'\n" +
            "\t\t\t@content_file = @cache_path + '/content'\n" +
            "\t\t\t@proxy_address = proxy_address\n" +
            "\t\t\t@proxy_port = proxy_port\n" +
            "\t\t\t@request_param = {'Accept-Encoding' => 'gzip, deflate'}\n" +
            "\t\tend\n" +
            "\n" +
            "\t\tdef retrieve\n" +
            "\t\t\tif File.exists? @cache_path\n" +
            "\t\t\t\twith_http {|http| do_followup_request(http)}\n" +
            "\t\t\telse\n" +
            "\t\t\t\twith_http {|http| do_request(http)}\n" +
            "\t\t\tend\n" +
            "\t\tend\n" +
            "\n" +
            "\t\tprivate\n" +
            "\n" +
            "\t\tdef do_request http\n" +
            "\t\t\tcreate_cache http.request_get(@uri.request_uri, @request_param)\n" +
            "\t\tend\n" +
            "\n" +
            "\t\tdef do_followup_request http\n" +
            "\t\t\tcache = YAML::load(IO.read(@header_file))\n" +
            "\n" +
            "\t\t\tif cache['etag']\n" +
            "\t\t\t\t@request_param['If-None-Match'] = cache['etag']\n" +
            "\t\t\telsif cache['last-modified']\n" +
            "\t\t\t\t@request_param['If-Modified-Since'] = cache['last-modified']\n" +
            "\t\t\telsif cache['date']\n" +
            "\t\t\t\t@request_param['If-Modified-Since'] = cache['date']\n" +
            "\t\t\tend\n" +
            "\n" +
            "\t\t\tresponse = http.request_get(@uri.request_uri, @request_param)\n" +
            "\n" +
            "\t\t\tif response.code == '304' # not modified use cache\n" +
            "\t\t\t\tcache.content = IO.read(@content_file) if File.exists? @content_file\n" +
            "\t\t\telse\n" +
            "\t\t\t\tcache = create_cache response\n" +
            "\t\t\tend\n" +
            "\n" +
            "\t\t\tcache\n" +
            "\t\tend\n" +
            "\n" +
            "\t\tdef create_cache response\n" +
            "\t\t\tcache = CachedResponse.new @uri, response\n" +
            "\n" +
            "\t\t\tDir.mkdir @cache_path unless File.exists? @cache_path\n" +
            "\t\t\tFile.open(@header_file, 'w') do |file|\n" +
            "\t\t\t\tfile.write cache.to_yaml\n" +
            "\t\t\tend\n" +
            "\n" +
            "\t\t\tcache.content = response.get_content\n" +
            "\n" +
            "\t\t\tif cache.content\n" +
            "\t\t\t\t# ruby 1.8.1 bug prevents to_yaml working on cache.content\n" +
            "\t\t\t\tFile.open(@content_file, 'w') do |file|\n" +
            "\t\t\t\t\tfile.write cache.content\n" +
            "\t\t\t\tend\n" +
            "\t\t\tend\n" +
            "\n" +
            "\t\t\tcache\n" +
            "\t\tend\n" +
            "\n" +
            "\t\tdef with_http\n" +
            "\t\t\tif @proxy_address and @proxy_port\n" +
            "\t\t\t\tNet::HTTP::Proxy(@proxy_address, @proxy_port).start(@uri.host, @uri.port) do |http|\n" +
            "\t\t\t\t\tyield http\n" +
            "\t\t\t\tend\n" +
            "\t\t\telse\n" +
            "\t\t\t\thttp = Net::HTTP.new(@uri.host, @uri.port)\n" +
            "\t\t\t\tyield http\n" +
            "\t\t\tend\n" +
            "\t\tend\n" +
            "\tend\n" +
            "\n" +
            "\tclass CachedResponse\n" +
            "\t\tattr_accessor :uri, :http_version, :code, :message, :content\n" +
            "\t\tattr_writer :header\n" +
            "\n" +
            "\t\tdef initialize uri, response\n" +
            "\t\t\t@uri = uri.to_s\n" +
            "\t\t\t@http_version = response.http_version\n" +
            "\t\t\t@code = response.code\n" +
            "\t\t\t@message = response.message.strip\n" +
            "\t\t\t@header = response.to_hash\n" +
            "\t\t\t@content = nil\n" +
            "\t\tend\n" +
            "\n" +
            "\t\t##\n" +
            "\t\t# Returns retrieved content as a\n" +
            "\t\t# <code>REXML::Document</code>\n" +
            "\t\t# if it is possible to do so.\n" +
            "\t\tdef as_xml\n" +
            "\t\t\txml = nil\n" +
            "\t\t\ttype = self['content-type']\n" +
            "\t\t\tif @content and type\n" +
            "\t\t\t\tif type =~ /.*html/\n" +
            "\t\t\t\t\tparser = HTMLTree::XMLParser.new(false, false)\n" +
            "\t\t\t\t\tparser.feed(@content)\n" +
            "\t\t\t\t\t# then you have the tree built..\n" +
            "\t\t\t\t\txml = parser.document\n" +
            "\t\t\t\telsif type =~ /.*xml/\n" +
            "\t\t\t\t\txml = REXML::Document.new @content \n" +
            "\t\t\t\tend\n" +
            "\t\t\tend\n" +
            "\t\t\txml\n" +
            "\t\tend\n" +
            "\t\n" +
            "\t\t##\n" +
            "\t\t# Iterates for each header names and values.\n" +
            "\t\tdef each_header &block\n" +
            "\t\t\t@header.each &block\n" +
            "\t\tend\n" +
            "\n" +
            "\t\t##\n" +
            "\t\t# Returns HTTP response header as a string\n" +
            "\t\tdef header\n" +
            "\t\t\theader = %Q[HTTP/#{@http_version} #{@code} #{@message}\\n]\n" +
            "\t\t\teach_header do |key,value|\n" +
            "\t\t\t\theader << %Q[#{key}: #{value}\\n]\n" +
            "\t\t\tend\n" +
            "\t\t\theader\n" +
            "\t\tend\n" +
            "\n" +
            "\t\t##\n" +
            "\t\t# Returns the header field corresponding to the case-insensitive key.\n" +
            "\t\t# Example:\n" +
            "\t\t#  type = response['Content-Type']\n" +
            "\t\tdef [] key\n" +
            "\t\t\t@header[key.downcase]\n" +
            "\t\tend\n" +
            "\tend\n" +
            "\n" +
            "end\n" +
            "\n" +
            "module Zlib\n" +
            "\tclass GzipReader\n" +
            "\t\tdef GzipReader.unzip string\n" +
            "\t\t\tgz = Zlib::GzipReader.new(StringIO.new(string))\n" +
            "\t\t\tcontent = gz.read\n" +
            "\t\t\tgz.close\n" +
            "\t\t\tcontent\n" +
            "\t\tend\n" +
            "\tend\n" +
            "end\n" +
            "\n" +
            "module Net\n" +
            "\tclass HTTPResponse\n" +
            "\t\tdef get_content\n" +
            "\t\t\tcontent = self.body\n" +
            "\n" +
            "\t\t\tif content and (encoding = self['Content-Encoding'])\n" +
            "\t\t\t\tif encoding =~ /gzip/i\n" +
            "\t\t\t\t\tcontent = Zlib::GzipReader.unzip(content)\n" +
            "\t\t\t\telsif encoding =~ /deflate/i\n" +
            "\t\t\t\t\tcontent = Zlib::Inflate.inflate(content)\n" +
            "\t\t\t\tend\n" +
            "\t\t\tend\n" +
            "\n" +
            "\t\t\tcontent\n" +
            "\t\tend\n" +
            "\tend\n" +
            "end";
}
