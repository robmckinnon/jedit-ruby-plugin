/*
 * CodeAnalyzer.java - 
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
package org.jedit.ruby.completion;

import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.utils.RegularExpression;
import org.jedit.ruby.utils.EditorView;
import org.jedit.ruby.ast.Member;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.MatchResult;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class CodeAnalyzer {

    private static final String DEMARKERS = "~`!@#$%^&*-=_+|\\:;\"',.?/";
    private static final String MORE_DEMARKERS = "()[]{}<>";

    private static String LAST_COMPLETED;
    private static Set<Member> LAST_RETURN_TYPES;

    private final EditorView view;
    private String textWithoutLine;
    private String partialMethod;
    private String partialClass;
    private String restOfLine;
    private String methodCalledOnThis;

    public CodeAnalyzer(EditorView view) {
        this.view = view;
        String line = view.getLineUpToCaret();
        RubyPlugin.log("line: "+line, getClass());

        MatchResult match = getMatch(line);
        methodCalledOnThis = getMethodCalledOnThis(match);

        if (methodCalledOnThis != null) {
            RubyPlugin.log("methodCalledOnThis: " + methodCalledOnThis, getClass());
            restOfLine = match.group(1) + match.group(2) + match.group(3) + match.group(4);

            int parenthesisIndex = methodCalledOnThis.indexOf('(');

            if (parenthesisIndex != -1) {
                methodCalledOnThis = methodCalledOnThis.substring(parenthesisIndex + 1);
                restOfLine = restOfLine.substring(parenthesisIndex + 1);
            }

            if (match.group(5).length() > 0) {
                partialMethod = match.group(5);
            }
        } else {
            lookForClassMatch(line, true);
        }

        if (partialClass == null) {
            lookForClassMatch(line, false);
        }
    }

    public static MatchResult getMatch(String line) {
        return DotCompleteRegExp.instance.lastMatch(line, 5);
    }

    public static String getMethodCalledOnThis(MatchResult match) {
        return match == null ? null : match.group(1);
    }

    private void lookForClassMatch(String line, boolean setMethod) {
        MatchResult match = PartialNameRegExp.instance.firstMatch(line);

        if (match != null) {
            String partialName = match.group(2);
            if (ClassNameRegExp.instance.isMatch(partialName)) {
                partialClass = partialName;
            } else if (setMethod) {
                partialMethod = partialName;
            }
        }
    }

    public static boolean isDotInsertionPoint(EditorView view) {
        return isDotInsertionPoint(view.getLineUpToCaretLeftTrimmed());
    }

    public static boolean isDotInsertionPoint(String lineUpToCaret) {
        String[] parts = lineUpToCaret.split(" ");
        return DotCompleteRegExp.instance.hasMatch(parts[parts.length-1]);
    }

    public static boolean isClassCompletionPoint(EditorView view) {
        String wordUpToCaret = view.getLineUpToCaretLeftTrimmed();
        int space = wordUpToCaret.lastIndexOf(' ');
        if (space != -1 && space != wordUpToCaret.length() - 1) {
            wordUpToCaret = wordUpToCaret.substring(space + 1);
        }
        return ClassNameRegExp.instance.isMatch(wordUpToCaret);
    }

    public static boolean hasLastReturnTypes() {
        return LAST_RETURN_TYPES != null && LAST_RETURN_TYPES.size() > 0;
    }

    public static Set<Member> getLastReturnTypes() {
        return LAST_RETURN_TYPES;
    }

    public static void setLastReturnTypes(Set<Member> type) {
        if (type != LAST_RETURN_TYPES) {
            RubyPlugin.log("set last return type: " + String.valueOf(type), CodeAnalyzer.class);
            LAST_RETURN_TYPES = type;
        }
    }

    public static void setLastCompleted(String text) {
        if (text != LAST_COMPLETED) {
            RubyPlugin.log("last completed: " + String.valueOf(text), CodeAnalyzer.class);
            LAST_COMPLETED = text;
        }
    }

    public final boolean isDotInsertionPoint() {
        RubyPlugin.log("insertion? " + String.valueOf(methodCalledOnThis) + "."
                + String.valueOf(partialMethod) + " vs. "
                + String.valueOf(LAST_COMPLETED), CodeAnalyzer.class);

        boolean insertionPoint = methodCalledOnThis != null && !isLastCompleted();
        return insertionPoint && !isDemarkerChar(methodCalledOnThis);
    }

    static boolean isDemarkerChar(String text) {
        boolean isDemarkerChar = false;

        if (text != null && text.length() == 1) {
            for (char demarker : DEMARKERS.toCharArray()) {
                if (isDemarkerChar) break;
                isDemarkerChar = text.charAt(0) == demarker;
            }
            for (char demarker : MORE_DEMARKERS.toCharArray()) {
                if (isDemarkerChar) break;
                isDemarkerChar = text.charAt(0) == demarker;
            }
        }
        return isDemarkerChar;
    }

    public boolean isLastCompleted() {
        return partialMethod != null && partialMethod.equals(LAST_COMPLETED);
    }

    public final String getMethodCalledOnThis() {
        return methodCalledOnThis;
    }

    public final boolean isClass() {
        return isClass(methodCalledOnThis);
    }

    public static boolean isClass(String name) {
        boolean isClass = ClassNameRegExp.instance.isMatch(name);
        RubyPlugin.log("isClass: " + isClass, CodeAnalyzer.class);
        return isClass;
    }

    final String getClassMethodCalledFrom() {
        String className = null;

        if (methodCalledOnThis != null) {
            if (isClass()) {
                className = methodCalledOnThis;
            } else {
                className = determineClassName(methodCalledOnThis);
            }
        }

        if (className == null) {
            className = findClassName();
        }

        return className;
    }

    private String determineClassName(String name) {
        if (matches("[", "]", name)) {
            return "Array";

        } else if (matches("'", "'", name) || matches("\"", "\"", name)) {
            return "String";

        } else if (matches("{", "}", name)) {
            return "Hash";

        } else if (matches("/", "/", name)) {
            return "Regexp";

        } else if ("true".equals(name) || "TRUE".equals(name)) {
            return "TrueClass";

        } else if ("false".equals(name) || "FALSE".equals(name)) {
            return "FalseClass";

        } else if ("nil".equals(name) || "NIL".equals(name)) {
            return "NilClass";

        } else if (isFixNum(name)) {
            return "Fixnum";

        } else if (isFloat(name)) {
            return "Float";

        } else if (name.startsWith("%r") && isDemarked(name, 2)) {
            return "Regexp";

        } else if (name.startsWith("%q") && isDemarked(name, 2)) {
            return "String";

        } else if (name.startsWith("%Q") && isDemarked(name, 2)) {
            return "String";

        } else if (name.startsWith("%w") && isDemarked(name, 2)) {
            return "Array";

        } else if (name.startsWith("%") && isDemarked(name, 1)) {
            return "String";

        } else if (SymbolRegExp.instance.isMatch(name)) {
            return "Symbol";

        } else {
            return null;
        }
    }

    private static boolean isFixNum(String name) {
        try {
            Long.parseLong(name);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isFloat(String name) {
        try {
            Double.parseDouble(name);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDemarked(String name, int start) {
        boolean demarked = false;
        if (name.length() >= (start+2)) {
            String rest = name.substring(start);
            demarked = demarked(rest);
        }
        return demarked;
    }

    private static boolean demarked(String rest) {
        boolean demarked = matches("{", "}", rest) || matches("(", ")", rest) || matches("[", "]", rest);

        if (!demarked) {
            int index = DEMARKERS.indexOf(rest.charAt(0));
            demarked = index != -1 && rest.endsWith(""+DEMARKERS.charAt(index));
        }
        return demarked;
    }

    private static boolean matches(String prefix, String suffix, String name) {
        return name.startsWith(prefix) && name.endsWith(suffix);
    }

    private String getTextWithoutLine() {
        if (textWithoutLine == null) {
            textWithoutLine = view.getTextWithoutLine();
        }
        return textWithoutLine;
    }

    private String findClassName() {
        String text = getTextWithoutLine();
        String className = findAssignment(text, "([A-Z]\\w+(::\\w+)?)((\\.|::)new)");

        if (className == null) {
            String value = findAssignment(text, "(\\S+)");
            if (value != null && value.length() > 1) {
                className = determineClassName(value);
            }
        }

        return className;
    }

    private String findAssignment(String text, String pattern) {
        RegularExpression expression = new RegularExpression("(" + methodCalledOnThis + " *= *)" + pattern);

        MatchResult[] matches = expression.getAllMatchResults(text);
        int count = matches.length;
        if (count > 0) {
            return matches[count - 1].group(2);
        } else {
            return null;
        }
    }

    public String getPartialClass() {
        return partialClass;
    }

    public final String getPartialMethod() {
        return partialMethod;
    }

    public final String getRestOfLine() {
        return restOfLine;
    }

    final List<String> getMethodsCalledOnVariable() {
        return getMethodsCalledOnVariable(getTextWithoutLine(), methodCalledOnThis);
    }

    /**
     * Returns array of methods invoked on the variable
     */
    public static List<String> getMethodsCalledOnVariable(String text, String name) {
        List<String> methods = new ArrayList<String>();
        addMatches(text, methods, "(" + name + "(\\.|#))(\\w+\\??)");
        return methods;
    }

    private static void addMatches(String text, List<String> methods, String pattern) {
        RegularExpression expression = new RegularExpression(pattern);
        MatchResult[] matches = expression.getAllMatchResults(text);

        for (MatchResult match : matches) {
            String method = match.group(3);
            methods.add(method);
        }
    }

    private static final class PartialNameRegExp extends RegularExpression {
        private static final RegularExpression instance = new PartialNameRegExp();
        protected final String getPattern() {
            return "(\\s*)(\\S+)$";
        }
    }

    public static final class DotCompleteRegExp extends RegularExpression {
        public static final RegularExpression instance = new DotCompleteRegExp();
        protected final String getPattern() {
            return "((@@|@|$)?\\w+(::\\w+)?)(\\.|::|#)((?:[^A-Z]\\S*)?)$";
        }
    }

    private static final class SymbolRegExp extends RegularExpression {
        private static final RegularExpression instance = new SymbolRegExp();
        protected final String getPattern() {
            return ":\\w+";
        }
    }

    public static final class ClassNameRegExp extends RegularExpression {
        public static final RegularExpression instance = new ClassNameRegExp();
        protected final String getPattern() {
            return "^([A-Z]\\w*(::)?)+:?";
        }
    }
}
