/*
 * TestCodeAnalyzer.java - 
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
import org.jedit.ruby.completion.CodeAnalyzer;
import org.jedit.ruby.utils.EditorView;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class TestCodeAnalyzer extends TestCase {

    private static final String TEXT = "a.respond_to?() \n" +
            "a#respond_to?() \n" +
            "a#respond_to? tree\n" +
            "a#respond_to(tree)\n" +
                    "a.[] \n" +
                    "3 \n";

    public final void testMethodCalledOnThis() {
        String line = "        a.g";
        MatchResult match = CodeAnalyzer.getMatch(line);
        String methodCalledOnThis = CodeAnalyzer.getMethodCalledOnThis(match);
        assertEquals("Assert method called on this correct.", "a", methodCalledOnThis);
    }

    public final void testOutsideMember() {
        CodeAnalyzer analyzer = new CodeAnalyzer(new MockEditorView("", 0));
        assertCorrect(analyzer, null, null, false);
    }

    public final void testPartialClass() {
        String clas = "Arr";
        CodeAnalyzer analyzer = new CodeAnalyzer(new MockEditorView(clas, clas.length()));
        assertCorrect(analyzer, null, clas, false);
    }

    public final void testDotCompletePartialClass() {
        String clas = "Red::arr";
        CodeAnalyzer analyzer = new CodeAnalyzer(new MockEditorView(clas, clas.length()));
        assertCorrect(analyzer, "arr", null, true);
    }

    public final void testPartialNamespaceClass() {
        String clas = "Red::Arr";
        CodeAnalyzer analyzer = new CodeAnalyzer(new MockEditorView(clas, clas.length()));
        assertCorrect(analyzer, null, clas, false);
    }

    public final void testPartialMethod() {
        String method = "to_";
        CodeAnalyzer analyzer = new CodeAnalyzer(new MockEditorView(method, 3));
        assertCorrect(analyzer, method, null, false);
    }

    public final void testDotCompleteMatchFalse() {
        boolean match = CodeAnalyzer.DotCompleteRegExp.instance.isMatch("ActiveRecord:");
        assertEquals("Assert dot completion match correct.", false, match);
    }

    public final void testDotCompleteMatch() {
        boolean match = CodeAnalyzer.DotCompleteRegExp.instance.isMatch("ActiveRecord::");
        assertEquals("Assert dot completion match correct.", true, match);
    }

    public final void testPartialClassPlusColonDotCompleteMatchFalse() {
        String text = "ActiveRecord:";
        CodeAnalyzer analyzer = new CodeAnalyzer(new MockEditorView(text, text.length()));
        assertCorrect(analyzer, null, text, false);
    }

    public final void testPartialClassPlusColon() {
        String text = "ActiveRecord::";
        CodeAnalyzer analyzer = new CodeAnalyzer(new MockEditorView(text, text.length()));
        assertCorrect(analyzer, null, text, true);
    }

    public final void testClassMatch() {
        Pattern pattern = Pattern.compile("((@@|@|$)?\\S+(::\\w+)?)(\\.|::|#)((?:[^A-Z]\\S*)?)$");
        Matcher matcher = pattern.matcher("      a.");
        boolean match = matcher.find();

        assertEquals("Assert class match correct.", true, match);
    }

    public final void testClassMatchOld() {
        boolean match = CodeAnalyzer.ClassNameRegExp.instance.isMatch("ActiveRecord:");
        assertEquals("Assert class match correct.", true, match);
        match = CodeAnalyzer.ClassNameRegExp.instance.isMatch("ActiveRecord::");
        assertEquals("Assert class match correct.", true, match);
    }

    private static void assertCorrect(CodeAnalyzer analyzer, String partialMethod, String partialClass, boolean isDotCompletion) {
        assertEquals("Assert dot completion point correct", isDotCompletion, analyzer.isDotInsertionPoint());
        assertEquals("Assert partial method correct", partialMethod, analyzer.getPartialMethod());
        assertEquals("Assert partial class correct", partialClass, analyzer.getPartialClass());
    }

    public final void testFindMethod1() {
        List<String> methods = CodeAnalyzer.getMethodsCalledOnVariable(TEXT, "a");
        assertEquals("assert found method", "respond_to?", methods.get(0));
    }

    public final void testFindMethod2() {
        List<String> methods = CodeAnalyzer.getMethodsCalledOnVariable(TEXT, "a");
        assertEquals("assert found method", "respond_to?", methods.get(1));
    }

    public final void testFindMethod3() {
        List<String> methods = CodeAnalyzer.getMethodsCalledOnVariable(TEXT, "a");
        assertEquals("assert found method", "respond_to?", methods.get(2));
    }

    public final void testFindMethod4() {
        List<String> methods = CodeAnalyzer.getMethodsCalledOnVariable(TEXT, "a");
        assertEquals("assert found method", "respond_to", methods.get(3));
    }

    public final void testClassName() {
        boolean isClass = CodeAnalyzer.isClass("REXML::Element");
        assertEquals("assert class name recognized", true, isClass);
    }

    public final void testClassName2() {
        boolean isClass = CodeAnalyzer.isClass("IO::puts");
        assertEquals("assert class method recognized", false, isClass);
    }

    public final void testIsDotInsertionPointWithIfStatement() {
        assertTrue(CodeAnalyzer.isDotInsertionPoint("if a."));
    }

    public final void testIsDotInsertionPointWithOpenParenthesis() {
        assertTrue(CodeAnalyzer.isDotInsertionPoint("(a."));
    }

    public final void testIsDotInsertionPointWithOpenBrace() {
        assertTrue(CodeAnalyzer.isDotInsertionPoint("{a."));
    }

    public final void testIsDotInsertionPointWithOpenBracket() {
        assertTrue(CodeAnalyzer.isDotInsertionPoint("[a."));
    }

    public final void testIsDotInsertionPointWithKeyAssignmentOperator() {
        assertTrue(CodeAnalyzer.isDotInsertionPoint("b=>a."));
    }

    public final void testIsDotInsertionPointWithArrayComma() {
        assertTrue(CodeAnalyzer.isDotInsertionPoint("c,a."));
    }

    public final void testFindMethod() {
        MatchResult match = CodeAnalyzer.DotCompleteRegExp.instance.lastMatch("        [a.adapter_name,b.", 5);
        assertEquals("b", match.group(1));
    }

    private static final class MockEditorView extends EditorView.NullEditorView {
        private final String text;
        private final int caret;

        public MockEditorView(String text, int caret) {
            this.text = text;
            this.caret = caret;
        }

        public int getCaretPosition() {
            return caret;
        }

        public String getLineUpToCaret() {
            return text.substring(0, caret);
        }

        public String getLineAfterCaret() {
            int start = caret + 1;
            int end = text.length() - 1;
            if (end > start) {
                return text.substring(start, end);
            } else {
                return "";
            }
        }

        public String getText(int start, int length) {
            return text.substring(start, start+length);
        }

        public int getLength() {
            return text.length();
        }
    }
}
