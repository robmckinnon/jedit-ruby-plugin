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
package org.jedit.ruby;

import gnu.regexp.RE;
import gnu.regexp.REMatch;
import gnu.regexp.REException;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import java.util.List;
import java.util.ArrayList;

/**
 * @author robmckinnon at users.sourceforge.net
 */
class CodeAnalyzer {

    private String name;
    private String partialMethod;
    private String restOfLine;
    private String textWithoutLine;
    private JEditTextArea textArea;
    private Buffer buffer;

    public CodeAnalyzer(JEditTextArea textArea, Buffer buffer) {
        this.textArea = textArea;
        this.buffer = buffer;
        String line = getLineUpToCaret();
        try {
            RE expression = new RE("((@@|@|$)?\\w+(::\\w+)?)(\\.|::|#)(\\w*)$");
            REMatch match = expression.getMatch(line);
            if (match != null) {
                name = match.toString(1);
                restOfLine = match.toString(1) + match.toString(2) + match.toString(3) + match.toString(4);
                if(match.toString(5).length() > 0) {
                    partialMethod = match.toString(5);
                }
            }
        } catch (REException e) {
            e.printStackTrace();
        }
    }

    public boolean isInsertionPoint() {
        return name != null;
    }

    private int getLineStartIndex() {
        int lineIndex = textArea.getCaretLine();
        int start = textArea.getLineStartOffset(lineIndex);
        return start;
    }

    public String getName() {
        return name;
    }

    String getLineUpToCaret() {
        int start = getLineStartIndex();
        int end = textArea.getCaretPosition();
        String line = textArea.getText(start, end - start);
        return line;
    }

    boolean isClass() {
        try {
            RE expression = new RE("^[A-Z]\\w*");
            return expression.isMatch(name);
        } catch (REException e) {
            e.printStackTrace();
            return false;
        }
    }

    String getClassName() {
        if(isClass()) {
            return name;
        } else {
            return findClassName();
        }
    }

    String getTextWithoutLine() {
        if(textWithoutLine == null) {
            int caretPosition = textArea.getCaretPosition();
            int line = textArea.getLineOfOffset(caretPosition);
            int start = textArea.getLineStartOffset(line);
            int end = textArea.getLineEndOffset(line);
            StringBuffer text = new StringBuffer();
            text.append(buffer.getText(0, start));
            text.append(buffer.getText(end, buffer.getLength() - end));
            textWithoutLine = text.toString();
        }

        return textWithoutLine;
    }

    String findClassName() {
        String text = getTextWithoutLine();
        try {
            RE expression = new RE("(" + name + " *= *)([A-Z]\\w+(::\\w+)?)((\\.|::)new)");
            REMatch[] matches = expression.getAllMatches(text);
            if(matches.length > 0) {
                return matches[0].toString(2);
            } else {
                return null;
            }
        } catch (REException e) {
            e.printStackTrace();
            return  null;
        }
    }

    public String getPartialMethod() {
        return partialMethod;
    }

    public String getRestOfLine() {
        return restOfLine;
    }

    //
    // Returns array of methods invoked on the variable
    //
    List<String> getMethods() {
        try {
            RE expression = new RE("("+name+"\\.|#)(\\w+)");
            REMatch[] matches = expression.getAllMatches(getTextWithoutLine());

            List<String> methods = new ArrayList<String>();

            for (REMatch match : matches) {
                String method = match.toString(2);
                methods.add(method);
            }

            return methods;
        } catch (REException e) {
            e.printStackTrace();
            return null;
        }
    }
}
