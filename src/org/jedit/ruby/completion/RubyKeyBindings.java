/*
 * RubyKeyBindings.java
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

import org.gjt.sp.jedit.gui.KeyEventWorkaround;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.structure.RubyToken;
import org.jedit.ruby.structure.BufferChangeHandler;
import org.jedit.ruby.utils.CharCaretListener;
import org.jedit.ruby.utils.EditorView;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

/**
 * Manages our key bindings.
 */
public final class RubyKeyBindings extends KeyAdapter {

    private static final List<String> pairs = Arrays.asList("()", "[]", "{}", "''", "\"\"", "//");

    private static final char NIL_CHAR = (char)-1;

    private final JEditTextArea textArea;

    public RubyKeyBindings(JEditTextArea textArea) {
        this.textArea = textArea;
    }

    public void keyPressed(KeyEvent e) {
        BufferChangeHandler.instance().setGotoPreviousEdit(false);
    }

    public void keyReleased(KeyEvent e) {
    }

    public final void keyTyped(KeyEvent e) {
        if (isRuby()) {
            e = KeyEventWorkaround.processKeyEvent(e);
            keyTypedInRubyText(e);
        }
    }

    private void keyTypedInRubyText(KeyEvent e) {
        if (isBackspace(e)) {
            handleBackspace();
        } else if (isCharacter(e)) {
            handleCharacter();
        }
    }

    private void handleBackspace() {
        if (CharCaretListener.hasCharLastBehind() && !ignoreBackspace()) {
            boolean done = false;
            for (String pair : pairs) {
                done = removePair(pair);
                if (done) break;
            }
            if (!done) {
                removePair("||");
            }
        }
    }

    private void handleCharacter() {
        boolean done = false;

        if (!ignoreCharacter()) {
            for (String pair : pairs) {
                done = addPair(pair);
                if (done) break;
            }
            if (!done) {
                if (behind() == '|') {
                    if (lastBehind() == '{' && ahead() == '}') {
                        done = addPair("||");
                    } else {
                        EditorView view = RubyPlugin.getActiveView();
                        int doIndex = view.getLineUpToCaret().indexOf(" do ");
                        if (view.getCaretPosition() == doIndex + 5) {
                            done = addPair("||");
                        }
                    }
                }
            }
        }

        if (!done) {
            for (String pair : pairs) {
                done = removeExtraEnd(pair);
                if (done) break;
            }
            if(!done) {
                removeExtraEnd("||");
            }
        }
    }

    private boolean removePair(String pair) {
        if (charLastBehind() == pair.charAt(0) && charAhead() == pair.charAt(1)) {
            removeNextChar();
            return true;
        } else {
            return false;
        }
    }

    private boolean addPair(String pair) {
        char start = pair.charAt(0);
        char end = pair.charAt(1);

        if (!Character.isLetter(ahead()) && behind() == start && ahead() != end && (start != end || lastBehind() != start)) {
            int position = textArea.getCaretPosition();
            Selection.Range range = new Selection.Range(position - 1, position);
            textArea.setSelection(range);
            textArea.setSelectedText(pair);
            textArea.setCaretPosition(position);
            return true;
        } else {
            return false;
        }
    }

    private boolean removeExtraEnd(String pair) {
        char start = pair.charAt(0);
        char end = pair.charAt(1);
        boolean done = false;

        if (behind() == end && ahead() == end) {
            if (lastBehind() == start) {
                removeNextChar();
                done = true;
            } else if(ahead() == '|') {
                String line = RubyPlugin.getActiveView().getLineUpToCaret();
                if(line.indexOf(" do |") != -1 || line.indexOf("{|") != -1 || line.indexOf("{ |") != -1) {
                    removeNextChar();
                    done = true;
                }
            } else {
                String line = RubyPlugin.getActiveView().getLineUpToCaret();
                int startIndex = line.indexOf(start);
                if (startIndex != -1 && startIndex != (line.length() - 1)) {
                    removeNextChar();
                    done = true;
                }
            }
        }

        return done;
    }

    private void removeNextChar() {
        int position = textArea.getCaretPosition();
        Selection.Range range = new Selection.Range(position, position + 1);
        textArea.setSelection(range);
        textArea.setSelectedText("");
    }

    private char lastBehind() {
        int position = textArea.getCaretPosition();
        String text = textArea.getText();
        return position > 1 ? text.charAt(position - 2) : NIL_CHAR;
    }

    private char behind() {
        int position = textArea.getCaretPosition();
        String text = textArea.getText();
        return position > 0 ? text.charAt(position - 1) : NIL_CHAR;
    }

    private char ahead() {
        int position = textArea.getCaretPosition();
        String text = textArea.getText();
        return position < text.length() ? text.charAt(position) : NIL_CHAR;
    }

    private static char charLastBehind() {
        return CharCaretListener.getCharLastBehind();
    }

    private static char charBehind() {
        return CharCaretListener.getCharBehind();
    }

    private static char charAhead() {
        return CharCaretListener.getCharAhead();
    }

    private static boolean ignoreBackspace() {
        RubyToken token = CharCaretListener.getCurrentToken();
        return token.isComment() || (token.isLiteral() && !(charBehind() == '\n' || charBehind() == '\r'));
    }

    private static boolean ignoreCharacter() {
        RubyToken token = CharCaretListener.getCurrentToken();
        RubyToken lastToken = CharCaretListener.getLastToken();
        return token == null || token.isComment() || (token.isLiteral() && lastToken != null && lastToken.isLiteral());
    }

    private static boolean isCharacter(KeyEvent e) {
        return e != null && !Character.isWhitespace(e.getKeyChar());
    }

    private static boolean isBackspace(KeyEvent e) {
        return e != null && e.getKeyChar() == '\b' && e.getModifiers() == 0;
    }

    private boolean isRuby() {
        return RubyPlugin.isRuby(textArea);
    }

}
