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
import org.jedit.ruby.ast.ParentMember;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.structure.AutoIndentAndInsertEnd;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class CodeAnalyzer {

    private static final RE COMPLETE_REG_EXP = new CompleteRegularExpression();
    private static final String DEMARKERS = "~`!@#$%^&*-=_+|\\:;\"',.?/";

    private static String LAST_COMPLETED;
    private static Set<Member> LAST_RETURN_TYPES;

    private Buffer buffer;
    private JEditTextArea textArea;
    private String textWithoutLine;
    private String partialMethod;
    private String restOfLine;
    private String name;

    public CodeAnalyzer(JEditTextArea textArea, Buffer buffer) {
        this.textArea = textArea;
        this.buffer = buffer;
        String line = getLineUpToCaret(textArea);
        RubyPlugin.log("line: "+line, getClass());
        REMatch match = COMPLETE_REG_EXP.getMatch(line);

        if (match != null) {
            name = match.toString(1);
            RubyPlugin.log("name: " + name, getClass());
            restOfLine = match.toString(1) + match.toString(2) + match.toString(3) + match.toString(4);
            if(match.toString(5).length() > 0) {
                partialMethod = match.toString(5);
            }
        }
    }

    public static boolean isInsertionPoint(JEditTextArea textArea) {
        String lineUpToCaret = getLineUpToCaret(textArea);
        boolean match = COMPLETE_REG_EXP.isMatch(lineUpToCaret);
        return match;
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

    public boolean isInsertionPoint() {
        RubyPlugin.log("insertion? " + String.valueOf(name) + " " + String.valueOf(LAST_COMPLETED), CodeAnalyzer.class);
        boolean insertionPoint = name != null;

        if (partialMethod != null) {
            insertionPoint = insertionPoint && !partialMethod.equals(LAST_COMPLETED);
        }

        return insertionPoint;
    }

    public String getName() {
        return name;
    }

    static String getLineUpToCaret(JEditTextArea textArea) {
        int lineIndex = textArea.getCaretLine();
        int start = textArea.getLineStartOffset(lineIndex);
        int end = textArea.getCaretPosition();
        String line = textArea.getText(start, end - start);
        return line;
    }

    public boolean isClass() {
        try {
            RE expression = new RE("^[A-Z]\\w*");
            boolean isClass = expression.isMatch(name);
            RubyPlugin.log("isClass: " + isClass, getClass());
            return isClass;
        } catch (REException e) {
            e.printStackTrace();
            return false;
        }
    }

    String getClassName() {
        String className = null;

        if (isClass()) {
            className = name;
        } else {
            className = findClassName();
        }

        if (className == null && name != null && name.length() > 0) {
            className = determineClassName(name);
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

        } else {
            return null;
        }
    }

    private boolean isFixNum(String name) {
        try {
            Long.parseLong(name);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isFloat(String name) {
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

    private boolean demarked(String rest) {
        boolean demarked = matches("{", "}", rest) || matches("(", ")", rest) || matches("[", "]", rest);

        if (!demarked) {
            int index = DEMARKERS.indexOf(rest.charAt(0));
            demarked = index != -1 && rest.endsWith(""+DEMARKERS.charAt(index));
        }
        return demarked;
    }

    private boolean matches(String prefix, String suffix, String name) {
        return name.startsWith(prefix) && name.endsWith(suffix);
    }

    String getTextWithoutLine() {
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

    String findClassName() {
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
        RE expression = new RE("(" + name + " *= *)" + pattern);
        REMatch[] matches = expression.getAllMatches(text);
        int count = matches.length;
        if(count > 0) {
            return matches[count - 1].toString(2);
        } else {
            return null;
        }
    }

    public String getPartialMethod() {
        return partialMethod;
    }

    public String getRestOfLine() {
        return restOfLine;
    }

    List<String> getMethods() {
        return getMethods(getTextWithoutLine(), name);
    }

    /**
     * Returns array of methods invoked on the variable
     */
    public static List<String> getMethods(String text, String name) {
        try {
            List<String> methods = new ArrayList<String>();

            addMatches(text, methods, "("+name+"(\\.|#))(\\w+\\??)");
//            addMatches(name, text, methods, "("+name+"\\.|#)(\\+\\?+)");

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

    private static class CompleteRegularExpression extends AutoIndentAndInsertEnd.RegularExpression {
        protected String getPattern() {
            return "((@@|@|$)?\\S+(::\\w+)?)(\\.|::|#)(\\S*)$";
        }
    }

}
