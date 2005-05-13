/*
 * RubyCompletion.java -
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

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.Selection;
import org.jedit.ruby.ast.Method;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ri.RDocViewer;
import org.jedit.ruby.RubyPlugin;
import sidekick.SideKickCompletion;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.List;
import java.util.Set;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RubyCompletion extends SideKickCompletion {

    private static final String NO_DOT_METHOD_STARTS = "=<>%*-+/|~&^";

    private List<Method> methods;
    private String partialMethod;
    private JWindow frame;
    private JTextPane textPane;

    public RubyCompletion(View view, String partialMethod, List<Method> methods) {
        super(view, partialMethod == null ? "." : "." + partialMethod, methods);
        this.methods = methods;
        this.partialMethod = partialMethod;
        frame = new JWindow((Frame)null);
        frame.setFocusable(false);
        textPane = new JTextPane();
        textPane.setEditorKit(new HTMLEditorKit());
		JScrollPane scroller = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        frame.getContentPane().add(scroller, BorderLayout.CENTER);
        frame.setSize(400,400);
    }

    /**
     * Returns true if should continue completing.
     */
    public boolean handleKeystroke(int selectedIndex, char keyChar) {
        boolean emptyPopup = selectedIndex == -1;
        boolean backspace = keyChar == '\b';
        boolean space = keyChar == ' ';
        boolean dot = keyChar == '.';
        boolean continueCompleting = !space && !dot && keyChar != '\t' && keyChar != '\n';

        if (backspace) {
            continueCompleting = handleBackspace();

        } else if (continueCompleting || emptyPopup) {
            textArea.userInput(keyChar);
        } else {
            Method method = methods.get(selectedIndex);
            Completion completion = insert(method);

            if (space && !method.hasParameters()) {
                textArea.userInput(' ');
            } else if (dot && !method.hasParameters() && completion.showDot) {
                textArea.userInput('.');
                continueCompleting = true;
                Set<Member> returnTypes = method.getReturnTypes();
                CodeAnalyzer.setLastReturnTypes(returnTypes);
            }
        }

        if(!continueCompleting) {
            CodeAnalyzer.setLastReturnTypes(null);
        }
        return continueCompleting;
    }

    private boolean handleBackspace() {
        String text = textArea.getLineText(textArea.getCaretLine());
        if(text.length() > 0) {
            int caretPosition = textArea.getCaretPosition();
            textArea.selectLine();
            textArea.setSelectedText(text.substring(0, text.length() - 1));
            textArea.setCaretPosition(caretPosition - 1);
        }
        return CodeAnalyzer.isInsertionPoint(textArea);
    }

    public void insert(int index) {
        insert(methods.get(index));
    }

    public String getCompletionDescription(int index) {
        RDocViewer.setMethod(methods.get(index));
        return null;
    }

    private Completion insert(Method method) {
        Buffer buffer = view.getBuffer();
        RubyPlugin.log("method: " + method.getName(), getClass());
        int caretPosition = textArea.getCaretPosition();
        int offset = caretPosition;

        if (partialMethod != null) {
            offset -= partialMethod.length();
            buffer.remove(offset, partialMethod.length());
        }

        Completion completion = getCompletion(method);
        if (!completion.showDot) {
            caretPosition = textArea.getCaretPosition();
            Selection.Range range = new Selection.Range(caretPosition - 1, caretPosition);
            textArea.setSelection(range);
            if (completion.caretAdjustment == 0) {
                textArea.setSelectedText(" ");
            } else {
                textArea.setSelectedText("");
                offset--;
            }
        }
        buffer.insert(offset, completion.text);
        CodeAnalyzer.setLastCompleted(completion.text);
        textArea.setCaretPosition(textArea.getCaretPosition() + completion.caretAdjustment);
        frame.setVisible(false);
        frame.dispose();
        frame = null;
        return completion;
    }

    private Completion getCompletion(Method method) {
        String name = method.getName();

        if (name.equals("each")) {
            return new Completion("each do ||", -1, true);

        } else if (name.startsWith("[")) {
            return new Completion(name, -1, false);

        } else if (NO_DOT_METHOD_STARTS.indexOf(name.charAt(0)) != -1) {
            return new Completion(name + " ", 0, false);

        } else if (method.hasParameters()) {
            return new Completion(name + "()", -1, true);

        } else {
            return new Completion(name, 0, true);
        }
    }

    private static final class Completion {
        String text;
        int caretAdjustment;
        boolean showDot;

        public Completion(String text, int caretPositionAdjustment, boolean showDot) {
            this.caretAdjustment = caretPositionAdjustment;
            this.text = text;
            this.showDot = showDot;
        }
    }

}