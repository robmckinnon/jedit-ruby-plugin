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
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.jedit.ruby.ast.Method;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.MemberVisitorAdapter;
import org.jedit.ruby.ri.RDocViewer;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.utils.EditorView;
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
public final class RubyCompletion extends SideKickCompletion {

    private static final String NO_DOT_METHOD_STARTS = "=<>%*-+/|~&^";

    private static boolean CONTINUE_COMPLETING = false;

    private final List members;
    private final String partialMethod;
    private final String partialClass;
    private JWindow frame;
    private EditorView editorView;

    public RubyCompletion(EditorView view, String partialClass, String partialMethod, List members) {
        super(view.getView(), partialMethod == null ? "." : "." + partialMethod, members);
        this.members = members;
        this.partialMethod = partialMethod;
        this.partialClass = partialClass;
        frame = new JWindow((Frame)null);
        frame.setFocusable(false);
        JTextPane textPane = new JTextPane();
        textPane.setEditorKit(new HTMLEditorKit());
        JScrollPane scroller = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        frame.getContentPane().add(scroller, BorderLayout.CENTER);
        frame.setSize(400,400);
        editorView = view;
    }

    /**
     * Returns true if should continue completing.
     */
    public final boolean handleKeystroke(int selectedIndex, char keyChar) {
        RubyPlugin.log("handle keystroke", getClass());
        final boolean emptyPopup = selectedIndex == -1;
        final boolean backspace = keyChar == '\b';
        final boolean space = keyChar == ' ';
        final boolean dot = keyChar == '.';
        boolean continueCompleting = !space && !dot && keyChar != '\t' && keyChar != '\n';

        if (backspace) {
            continueCompleting = handleBackspace();

        } else if (continueCompleting || emptyPopup) {
            textArea.userInput(keyChar);
        } else {
            Member member = (Member)members.get(selectedIndex);
            Completion completion = insert(member);
            CompletionVisitor visitor = new CompletionVisitor(dot, space, completion.showDot, textArea);
            member.accept(visitor);
            continueCompleting = visitor.continueCompletion;
        }

        if(!continueCompleting) {
            CodeAnalyzer.setLastReturnTypes(null);
        }
        RubyPlugin.log("continue completing: " + continueCompleting, getClass());

        CONTINUE_COMPLETING = continueCompleting;
        return continueCompleting;
    }

    public static boolean continueCompleting() {
        return CONTINUE_COMPLETING;
    }

    private boolean handleBackspace() {
        RubyPlugin.log("handle backspace", getClass());
        String text = textArea.getLineText(textArea.getCaretLine());
        if (text.length() > 0) {
            int caretPosition = textArea.getCaretPosition();
            textArea.selectLine();
            textArea.setSelectedText(text.substring(0, text.length() - 1));
            textArea.setCaretPosition(caretPosition - 1);
        }
        return CodeAnalyzer.isDotInsertionPoint(editorView) || CodeAnalyzer.isClassCompletionPoint(editorView);
    }

    public final void insert(int index) {
        insert((Member)members.get(index));
    }

    /**
     * Overriden super class method in order
     * to set Ruby docs in Ruby doc viewer
     */
    public final String getCompletionDescription(int index) {
        RDocViewer.setMemberInViewer((Member)members.get(index));
        return null;
    }

    private Completion insert(Member member) {
        Buffer buffer = view.getBuffer();
        RubyPlugin.log("insert member: " + member.getName(), getClass());
        int caretPosition = textArea.getCaretPosition();
        int offset = caretPosition;

        CompletionCreatorVisitor completionCreator = new CompletionCreatorVisitor(offset, buffer, partialMethod, partialClass);
        member.accept(completionCreator);
        Completion completion = completionCreator.completion;
        offset = completionCreator.offset;

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
        RubyPlugin.log("completion text: " + completion.text, getClass());
        textArea.setCaretPosition(textArea.getCaretPosition() + completion.caretAdjustment);
        frame.setVisible(false);
        frame.dispose();
        frame = null;
        return completion;
    }

    private static class CompletionVisitor extends MemberVisitorAdapter {
        final boolean dot;
        final boolean space;
        final boolean showDot;
        JEditTextArea textArea;
        boolean continueCompletion;

        public CompletionVisitor(boolean dot, boolean space, boolean showDot, JEditTextArea textArea) {
            this.dot = dot;
            this.space = space;
            this.showDot = showDot;
            this.textArea = textArea;
        }

        public void handleDefault(Member member) {
            if (space) {
                textArea.userInput(' ');
            } else if (dot && showDot) {
                textArea.userInput('.');
            }
        }

        public void handleMethod(Method method) {
            if (space && !method.hasParameters()) {
                textArea.userInput(' ');
            } else if (dot && !method.hasParameters() && showDot) {
                textArea.userInput('.');
                this.continueCompletion = true;
                Set<Member> returnTypes = method.getReturnTypes();
                CodeAnalyzer.setLastReturnTypes(returnTypes);
            }
        }
    }

    private static class CompletionCreatorVisitor extends MemberVisitorAdapter {
        private final String partialMethod;
        private final String partialClass;
        private final Buffer buffer;
        private Completion completion;
        private int offset;

        private CompletionCreatorVisitor(int offset, Buffer buffer, String partialMethod, String partialClass) {
            this.offset = offset;
            this.buffer = buffer;
            this.partialMethod = partialMethod;
            this.partialClass = partialClass;
        }

        public void handleDefault(Member member) {
            remove(partialClass);
            completion = new Completion(member, member.getFullName(), 0, true);
            CodeCompletor.setLastCompleted(partialClass, completion.member);
        }

        public void handleMethod(Method method) {
            remove(partialMethod);
            String name = method.getName();

            if (name.equals("each")) {
                completion = new Completion(method, "each do ||", -1, true);

            } else if (name.startsWith("[")) {
                completion = new Completion(method, name, -1, false);

            } else if (NO_DOT_METHOD_STARTS.indexOf(name.charAt(0)) != -1) {
                completion = new Completion(method, name + " ", 0, false);

            } else if (name.endsWith("=") && name.length() > 1) {
                name = name.substring(0, name.length() - 1) + " = ";
                completion = new Completion(method, name, 0, true);

            } else if (name.endsWith("?") && name.length() > 1) {
                completion = new Completion(method, name + " ", 0, true);

            } else if (method.hasParameters()) {
                completion = new Completion(method, name + "()", -1, true);

            } else {
                completion = new Completion(method, name, 0, true);
            }
            CodeCompletor.setLastCompleted(partialMethod, completion.member);
        }

        private void remove(String partialName) {
            if (partialName != null) {
                offset -= partialName.length();
                buffer.remove(offset, partialName.length());
            }
        }
    }

    private static final class Completion {
        final Member member;
        final String text;
        final int caretAdjustment;
        final boolean showDot;

        public Completion(Member member, String text, int caretPositionAdjustment, boolean showDot) {
            this.caretAdjustment = caretPositionAdjustment;
            this.member = member;
            this.text = text;
            this.showDot = showDot;
        }
    }

}