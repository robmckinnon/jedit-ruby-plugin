/*
 * BufferChangeHandler.java - 
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

package org.jedit.ruby.structure;

import org.gjt.sp.jedit.buffer.BufferAdapter;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.utils.EditorView;

import java.util.*;
import java.util.List;
import java.awt.*;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class BufferChangeHandler extends BufferAdapter {

    private static final BufferChangeHandler instance = new BufferChangeHandler();

    private final Map<String, Map<Integer, Edit>> fileToEdit = new HashMap<String, Map<Integer, Edit>>();
    private final List<Edit> allEdits = new ArrayList<Edit>();

    private boolean previousEditAction;
    private int editLocationIndex = 0;

    private BufferChangeHandler() {
    }

    public static BufferChangeHandler instance() {
        return instance;
    }

    public void preContentRemoved(Buffer buffer, int startLine, int offset, int numLines, int length) {
    }

    public void transactionComplete(Buffer buffer) {
        EditorView view = RubyPlugin.getActiveView();
        int line = view.getLineAtCaret();
        Map<Integer, Edit> lineEdits = getLineEdits(buffer.getPath());

        if (lineEdits.containsKey(line)) {
            lineEdits.get(line).offsetInLine = view.getCaretOffsetInLine();
        }
    }

    public void contentInserted(Buffer buffer, int startLine, int offset, int numLines, int length) {
        if (numLines > 0) {
            Map<Integer, Edit> lineEdits = getLineEdits(buffer.getPath());
            Collection<Edit> edits = new ArrayList<Edit>(lineEdits.values());

            for (Edit edit : edits) {
                if (edit.line >= startLine) {
                    lineEdits.remove(edit.line);
                    edit.incrementLine(numLines);
                    lineEdits.put(edit.line, edit);
                }
            }

        }
        if (numLines == 0) {
            updateEdits(buffer, startLine, offset + length - buffer.getLineStartOffset(startLine));
        } else {
            updateEdits(buffer, startLine, offset - buffer.getLineStartOffset(startLine));
        }
    }

    public void contentRemoved(Buffer buffer, int startLine, int offset, int numLines, int length) {
        if (numLines > 0) {
            Map<Integer, Edit> lineEdits = getLineEdits(buffer.getPath());

            for (int line = startLine; line < startLine + numLines; line++) {
                Edit edit = lineEdits.remove(line);
                allEdits.remove(edit);
            }

            Collection<Edit> edits = new ArrayList<Edit>(lineEdits.values());
            for (Edit edit : edits) {
                if (edit.line >= startLine + numLines) {
                    lineEdits.remove(edit.line);
                    edit.decrement(numLines);
                    lineEdits.put(edit.line, edit);
                }
            }
        }
        updateEdits(buffer, startLine, offset - buffer.getLineStartOffset(startLine));
    }

    private void updateEdits(Buffer buffer, int line, int offsetInLine) {
        String file = buffer.getPath();
        Edit edit = new Edit(file, line, offsetInLine);
        allEdits.remove(edit);
        allEdits.add(edit);

        Map<Integer, Edit> lineEdits = getLineEdits(file);
        lineEdits.put(line, edit);

        if (line > 0 && lineEdits.containsKey(line - 1)) {
            removeEditAt(line - 1, lineEdits);
        }

        if (line < (buffer.getLineCount() - 1) && lineEdits.containsKey(line + 1)) {
            removeEditAt(line + 1, lineEdits);
        }
    }

    private void removeEditAt(int line, Map<Integer, Edit> lineEdits) {
        Edit previousEdit = lineEdits.remove(line);
        allEdits.remove(previousEdit);
    }

    public void gotoPreviousEdit(View view) {
        setGotoPreviousEdit(true);

        if (!allEdits.isEmpty() && editLocationIndex < allEdits.size()) {
            editLocationIndex++;
            final Edit edit = allEdits.get(allEdits.size() - editLocationIndex);

            if (edit.file.equals(view.getBuffer().getPath())) {
                gotoEdit(view.getTextArea(), edit);

            } else {
                Buffer buffer = jEdit.openFile(view, edit.file);
                if (buffer != null) {
                    view.goToBuffer(buffer);

                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            gotoEdit(jEdit.getActiveView().getTextArea(), edit);
                        }
                    });
                } else {
                    setGotoPreviousEdit(false);
                }
            }
        } else {
            setGotoPreviousEdit(false);
        }
    }

    private void gotoEdit(JEditTextArea textArea, Edit edit) {
        textArea.setCaretPosition(edit.getEditOffset(textArea));
        setGotoPreviousEdit(false);
    }

    public boolean isGotoPreviousEditAction() {
        return previousEditAction;
    }

    public void resetEditLocationIndex() {
        editLocationIndex = 0;
    }

    public void setGotoPreviousEdit(boolean action) {
        previousEditAction = action;
    }

    private Map<Integer, Edit> getLineEdits(String file) {
        if (!fileToEdit.containsKey(file)) {
            Map<Integer, Edit> lineToEdit = new HashMap<Integer, Edit>();
            fileToEdit.put(file, lineToEdit);
        }

        return fileToEdit.get(file);
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

        private int getEditOffset(JEditTextArea textArea) {
            if (line < textArea.getLineCount()) {
                String lineText = textArea.getLineText(line);
                int offsetInLineText = offsetInLine <= lineText.length() ? offsetInLine : lineText.length();
                return textArea.getLineStartOffset(line) + offsetInLineText;
            } else {
                return textArea.getText().length();
            }
        }

        public boolean equals(Object obj) {
            Edit edit = (Edit)obj;
            return file.equals(edit.file) && line == edit.line;
        }

        public int hashCode() {
            return file.hashCode() + line;
        }

        public void incrementLine(int numLines) {
            line += numLines;
        }

        public void decrement(int numLines) {
            line -= numLines;
        }
    }

}
