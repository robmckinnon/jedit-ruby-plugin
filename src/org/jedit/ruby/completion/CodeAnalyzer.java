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

import gnu.regexp.RE;
import gnu.regexp.REMatch;
import gnu.regexp.REException;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.structure.AutoIndentAndInsertEnd;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class CodeAnalyzer {

    private static final String DEMARKERS = "~`!@#$%^&*-=_+|\\:;\"',.?/";

    private static String LAST_COMPLETED;
    private static Set<Member> LAST_RETURN_TYPES;

    private final Buffer buffer;
    private final JEditTextArea textArea;
    private String textWithoutLine;
    private String partialMethod;
    private String restOfLine;
    private String methodCalledOnThis;

    public CodeAnalyzer(JEditTextArea textArea, Buffer buffer) {
        this.textArea = textArea;
        this.buffer = buffer;
        String line = getLineUpToCaret(textArea);
        RubyPlugin.log("line: "+line, getClass());
        REMatch match = CompleteRegExp.instance.getMatch(line);

        if (match != null) {
            methodCalledOnThis = match.toString(1);
            RubyPlugin.log("methodCalledOnThis: " + methodCalledOnThis, getClass());
            restOfLine = match.toString(1) + match.toString(2) + match.toString(3) + match.toString(4);

            int parenthesisIndex = methodCalledOnThis.indexOf('(');

            if (parenthesisIndex != -1) {
                methodCalledOnThis = methodCalledOnThis.substring(parenthesisIndex + 1);
                restOfLine = restOfLine.substring(parenthesisIndex + 1);
            }

            if(match.toString(5).length() > 0) {
                partialMethod = match.toString(5);
            }
        }
    }

    public static boolean isInsertionPoint(JEditTextArea textArea) {
        String lineUpToCaret = getLineUpToCaret(textArea);
        return CompleteRegExp.instance.isMatch(lineUpToCaret.trim());
    }

    public static void setLastReturnTypes(Set<Member> type) {
        RubyPlugin.log("set last return type: " + String.valueOf(type), CodeAnalyzer.class);
        LAST_RETURN_TYPES = type;
    }

    public static boolean hasLastReturnTypes() {
        return LAST_RETURN_TYPES != null && LAST_RETURN_TYPES.size() > 0;
    }

    public static Set<Member> getLastReturnTypes() {
        return LAST_RETURN_TYPES;
    }

    public static void setLastCompleted(String text) {
        RubyPlugin.log("last completed: " + String.valueOf(text), CodeAnalyzer.class);
        LAST_COMPLETED = text;
    }

    public final boolean isInsertionPoint() {
        RubyPlugin.log("insertion? " + String.valueOf(methodCalledOnThis) + " " + String.valueOf(LAST_COMPLETED), CodeAnalyzer.class);
        boolean insertionPoint = methodCalledOnThis != null;

        if (partialMethod != null) {
            insertionPoint = insertionPoint && !partialMethod.equals(LAST_COMPLETED);
        }

        return insertionPoint;
    }

    public final String getMethodCalledOnThis() {
        return methodCalledOnThis;
    }

    private static String getLineUpToCaret(JEditTextArea textArea) {
        int lineIndex = textArea.getCaretLine();
        int start = textArea.getLineStartOffset(lineIndex);
        int end = textArea.getCaretPosition();
        return textArea.getText(start, end - start);
    }

    public final boolean isClass() {
        return isClass(methodCalledOnThis);
    }

    public static boolean isClass(String methodCalledOnThis) {
        boolean isClass = ClassNameRegExp.instance.isMatch(methodCalledOnThis);
        RubyPlugin.log("isClass: " + isClass, CodeAnalyzer.class);
        return isClass;
    }

    final String getClassName() {
        String className;

        if (isClass()) {
            className = methodCalledOnThis;
        } else {
            className = findClassName();
        }

        if (className == null && methodCalledOnThis != null && methodCalledOnThis.length() > 0) {
            className = determineClassName(methodCalledOnThis);
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

    private static boolean isFloat(String name) {
        try {
            Double.parseDouble(name);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDemarked(String name, int start) {
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
        if(textWithoutLine == null) {
            int caretPosition = textArea.getCaretPosition();
            int line = textArea.getLineOfOffset(caretPosition);
            int start = textArea.getLineStartOffset(line);
            int end = textArea.getLineEndOffset(line);
            StringBuffer text = new StringBuffer();
            text.append(buffer.getText(0, start));
            if(buffer.getLength() > end) {
                text.append(buffer.getText(end, buffer.getLength() - end));
            }
            textWithoutLine = text.toString();
        }

        return textWithoutLine;
    }

    private String findClassName() {
        String text = getTextWithoutLine();
        try {
            String className = findAssignment(text, "([A-Z]\\w+(::\\w+)?)((\\.|::)new)");

            if (className == null) {
                String value = findAssignment(text, "(\\S+)");
                if (value != null) {
                    className = determineClassName(value);
                }
            }

            return className;
        } catch (REException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String findAssignment(String text, String pattern) throws REException {
        RE expression = new RE("(" + methodCalledOnThis + " *= *)" + pattern);
        REMatch[] matches = expression.getAllMatches(text);
        int count = matches.length;
        if(count > 0) {
            return matches[count - 1].toString(2);
        } else {
            return null;
        }
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
        try {
            List<String> methods = new ArrayList<String>();

            addMatches(text, methods, "("+name+"(\\.|#))(\\w+\\??)");
//            addMatches(methodCalledOnThis, text, methods, "("+methodCalledOnThis+"\\.|#)(\\+\\?+)");

            return methods;
        } catch (REException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void addMatches(String text, List<String> methods, String pattern) throws REException {
        RE expression = new RE(pattern);
        REMatch[] matches = expression.getAllMatches(text);

        for (REMatch match : matches) {
            String method = match.toString(3);
            methods.add(method);
        }
    }

    private static final class CompleteRegExp extends AutoIndentAndInsertEnd.RegularExpression {
        private static final RE instance = new CompleteRegExp();
        protected final String getPattern() {
            return "((@@|@|$)?\\S+(::\\w+)?)(\\.|::|#)(\\S*)$";
        }
    }

    private static final class SymbolRegExp extends AutoIndentAndInsertEnd.RegularExpression {
        private static final RE instance = new SymbolRegExp();
        protected final String getPattern() {
            return ":\\w+";
        }
    }

    public static final class ClassNameRegExp extends AutoIndentAndInsertEnd.RegularExpression {
        private static final RE instance = new ClassNameRegExp();
        protected final String getPattern() {
            return "^([A-Z]\\w*(::)?)+";
        }
    }
}
