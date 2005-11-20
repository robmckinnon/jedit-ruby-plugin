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

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.KeyEventWorkaround;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.structure.RubyToken;
import org.jedit.ruby.utils.CharCaretListener;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages our key bindings.
 */
public final class RubyKeyBindings extends KeyAdapter {

    private static final List<String> pairs = Arrays.asList(new String[]{"()", "[]", "{}", "''", "\"\"", "//"});

    private static final char NIL_CHAR = (char)-1;
    private static final List<Edit> EDITS = new ArrayList<Edit>();
    private static int editLocationIndex = 0;

    private final JEditTextArea textArea;
    private static boolean previousEditAction;

    public RubyKeyBindings(JEditTextArea textArea) {
        this.textArea = textArea;
    }

    public static void gotoPreviousEdit(View view) {
        previousEditAction = true;
        if (!EDITS.isEmpty()) {
            if (editLocationIndex < EDITS.size()) {
                int index = (EDITS.size() - 1) - editLocationIndex;
                editLocationIndex++;
                final Edit edit = EDITS.get(index);

                JEditTextArea textArea;

                if (!edit.file.equals(view.getBuffer().getPath())) {
                    Buffer buffer = jEdit.openFile(view, edit.file);
                    if (buffer != null) {
                        view.goToBuffer(buffer);

                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                JEditTextArea textArea = jEdit.getActiveView().getTextArea();
                                int offset = textArea.getLineStartOffset(edit.line) + edit.offsetInLine;
                                textArea.setCaretPosition(offset);
                            }
                        });
                    }
                } else {
                    textArea = view.getTextArea();
                    int offset = textArea.getLineStartOffset(edit.line) + edit.offsetInLine;
                    textArea.setCaretPosition(offset);
                }
            }
        }
    }

    public void keyPressed(KeyEvent e) {
        if (!previousEditAction) {
            editLocationIndex = 0;
        }
    }

    public void keyReleased(KeyEvent e) {
        if (!previousEditAction) {
            editLocationIndex = 0;
        }
        previousEditAction = false;
    }

    public final void keyTyped(KeyEvent e) {
        e = KeyEventWorkaround.processKeyEvent(e);

        if (isRuby()) {
            if (isBackspace(e)) {
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
            } else if (isCharacter(e)) {
                boolean done = false;

                if (!ignoreCharacter()) {
                    for (String pair : pairs) {
                        done = addPair(pair);
                        if (done) break;
                    }
                    if (!done) {
                        if (lastBehind() == '{' && behind() == '|' && ahead() == '}') {
                            addPair("||");
                        }
                    }
                }

                if (!done) {
                    for (String pair : pairs) {
                        done = removeExtraEnd(pair);
                        if (done) break;
                    }
                }
            }
        }

        if (e != null && !e.isActionKey()) {
            String file = textArea.getBuffer().getPath();
            int line = textArea.getCaretLine();
            int offsetInLine = textArea.getCaretPosition() - textArea.getLineStartOffset(line);
            Edit edit = new Edit(file, line, offsetInLine);
            EDITS.remove(edit);
            EDITS.add(edit);
        }

        if (!previousEditAction) {
            editLocationIndex = 0;
        }
    }

    private boolean removePair(String pair) {
        if (charLastBehind() == pair.charAt(0) && charAhead() == pair.charAt(1)) {
            int position = textArea.getCaretPosition();
            Selection.Range range = new Selection.Range(position, position + 1);
            textArea.setSelection(range);
            textArea.setSelectedText("");
            return true;
        } else {
            return false;
        }
    }

    private boolean addPair(String pair) {
        char start = pair.charAt(0);
        char end = pair.charAt(1);

        if (behind() == start && ahead() != end && (start != end || lastBehind() != start)) {
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

        if (lastBehind() == start && behind() == end && ahead() == end) {
            int position = textArea.getCaretPosition();
            Selection.Range range = new Selection.Range(position, position + 1);
            textArea.setSelection(range);
            textArea.setSelectedText("");
            return true;
        } else {
            return false;
        }
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
        return token.isComment() || (token.isLiteral() && lastToken != null && lastToken.isLiteral());
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

    public static class Edit {
        String file;
        int line;
        int offsetInLine;

        public Edit(String file, int line, int offsetInLine) {
            this.file = file;
            this.line = line;
            this.offsetInLine = offsetInLine;
        }

        public boolean equals(Object obj) {
            Edit edit = (Edit)obj;
            return file.equals(edit.file) && line == edit.line;
        }

        public int hashCode() {
            return file.hashCode() + line;
        }
    }
}
