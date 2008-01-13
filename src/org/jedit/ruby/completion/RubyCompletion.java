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
import org.jedit.ruby.ast.*;
import org.jedit.ruby.ast.Error;
import org.jedit.ruby.ri.RDocViewer;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.utils.EditorView;
import sidekick.SideKickCompletion;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.util.List;
import java.util.Set;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class RubyCompletion extends SideKickCompletion {

    private static final CompletionNameRenderer COMPLETION_NAME_RENDERER = new CompletionNameRenderer();
    private static final CompletorCreatorVisitor COMPLETOR_CREATOR = new CompletorCreatorVisitor();
    private static final String NO_DOT_METHOD_STARTS = "=<>%*-+/|~&^";
    private static boolean CONTINUE_COMPLETING = false;

    private final List<? extends Member> members;
    private final String partialMethod;
    private final String partialClass;
    private JWindow frame;
    private final EditorView editorView;

    public RubyCompletion(EditorView view, String partialClass, String partialMethod, List<? extends Member> members) {
        super(view.getView(), "", members);
        this.members = members;
        this.partialMethod = partialMethod;
        this.partialClass = partialClass;
        frame = new JWindow((Frame)null);
        frame.setFocusable(false);
        JTextPane textPane = new JTextPane();
        textPane.setEditorKit(new HTMLEditorKit());
        JScrollPane scroller = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        frame.getContentPane().add(scroller, BorderLayout.CENTER);
        frame.setSize(400, 400);
        editorView = view;
    }

    public ListCellRenderer getRenderer() {
        return COMPLETION_NAME_RENDERER;
    }

    public static boolean continueCompleting() {
        return CONTINUE_COMPLETING;
    }

    private void setContinueCompleting(boolean continueCompleting) {
        CONTINUE_COMPLETING = continueCompleting;
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
            Member member = members.get(selectedIndex);
            continueCompleting = insert(member, space, dot);
        }

        RubyPlugin.log("continue completing: " + continueCompleting, getClass());
        if (!continueCompleting) {
            CodeAnalyzer.setLastReturnTypes(null);
        }

        setContinueCompleting(continueCompleting);
        return continueCompleting;
    }

    public final void insert(int index) {
        setContinueCompleting(insert(members.get(index), false, false));
    }

    private boolean insert(Member member, boolean space, boolean dot) {
        Buffer buffer = view.getBuffer();
        RubyPlugin.log("insert member: " + member.getName(), getClass());
        Completor completor = COMPLETOR_CREATOR.getCompletor(partialMethod, partialClass, space, dot, member);
        super.text = completor.partialName;
        completor.completeCode(buffer, textArea, dot, space);

        frame.setVisible(false);
        frame.dispose();
        frame = null;
        return completor.continueCompleting;
    }

    private boolean handleBackspace() {
        RubyPlugin.log("handle backspace", getClass());
        String text = textArea.getLineText(textArea.getCaretLine());
        char deletedChar = (char)-1;
        if (text.length() > 0) {
            int caretPositionInLine = editorView.getCaretOffsetInLine();
            deletedChar = text.charAt(caretPositionInLine - 1);
//            String upToCaret = text.substring(0, caretPositionInLine - 1);
//            String afterCaret = (caretPositionInLine == text.length()) ? "" : text.substring(caretPositionInLine, text.length());
//            RubyPlugin.log("text:        " + text,        getClass());
//            RubyPlugin.log("upToCaret:   " + upToCaret,   getClass());
//            RubyPlugin.log("deletedChar: " + deletedChar, getClass());
//            RubyPlugin.log("afterCaret:  " + afterCaret,  getClass());
//            textArea.selectLine();
//            textArea.setSelectedText(upToCaret + afterCaret);
//            textArea.setCaretPosition(textArea.getCaretPosition() - 2);
            textArea.backspace();
        }
        boolean dotInsertionPoint = CodeAnalyzer.isDotInsertionPoint(editorView) && deletedChar != '.' && deletedChar != ':';
        boolean classCompletionPoint = CodeAnalyzer.isClassCompletionPoint(editorView);
        RubyPlugin.log("dot? " + dotInsertionPoint + " class? " + classCompletionPoint + " " + deletedChar, getClass());
        return dotInsertionPoint || classCompletionPoint;
    }

    /**
     * Overriden super class method in order
     * to set Ruby docs in Ruby doc viewer
     */
    public final String getCompletionDescription(int index) {
        RDocViewer.setMemberInViewer(members.get(index));
        return null;
    }

    private static final class CompletorCreatorVisitor extends MemberVisitorAdapter {
        private String partialMethod;
        private String partialClass;
        private Completor completor;
        private boolean space;
        private boolean dot;

        private Completor getCompletor(String partialMethod, String partialClass, boolean space, boolean dot, Member member) {
            this.partialMethod = partialMethod;
            this.partialClass = partialClass;
            this.space = space;
            this.dot = dot;
            member.accept(this);
            return completor;
        }

        public final void handleDefault(Member member) {
            completor = new Completor(member, member.getFullName(), 0, true, partialClass);
            CodeCompletor.setLastCompleted(partialClass, member);
        }

        public void handleKeyword(KeywordMember keyword) {
            String partial = partialMethod != null ? partialMethod : partialClass;
            completor = new Completor(keyword, keyword.getFullName(), 0, true, partial);
            CodeCompletor.setLastCompleted(partial, keyword);
        }

        public final void handleMethod(Method method) {
            String name = method.getName();

            if (name.equals("each")) {
                completor = new Completor(method, "each do ||", -1, true, partialMethod);

            } else if (name.startsWith("[")) {
                completor = new Completor(method, name, -1, false, partialMethod);

            } else if (NO_DOT_METHOD_STARTS.indexOf(name.charAt(0)) != -1) {
                completor = new Completor(method, name + " ", 0, false, partialMethod);

            } else if (name.endsWith("=") && name.length() > 1) {
                name = name.substring(0, name.length() - 1) + " = ";
                completor = new Completor(method, name, 0, true, partialMethod);

            } else if (name.endsWith("?") && name.length() > 1) {
                completor = new Completor(method, name + " ", 0, true, partialMethod);

            } else if (!dot && !space && method.hasParameters()) {
//                completor = new Completor(method, name + "()", -1, true, partialMethod);
                completor = new Completor(method, name, 0, true, partialMethod);

            } else {
                completor = new Completor(method, name, 0, true, partialMethod);
            }
            CodeCompletor.setLastCompleted(partialMethod, method);
        }

    }

    private static final class Completor extends MemberVisitorAdapter {
        final Member member;
        final String text;
        final String partialName;
        final boolean showDot;
        final int caretAdjustment;
        boolean continueCompleting;

        public Completor(Member member, String text, int caretPositionAdjustment, boolean showDot, String partialName) {
            this.caretAdjustment = caretPositionAdjustment;
            this.member = member;
            this.text = text;
            this.showDot = showDot;
            this.partialName = partialName;
            continueCompleting = false;
        }

        public void completeCode(Buffer buffer, JEditTextArea textArea, boolean dot, boolean space) {
            int caretPosition = textArea.getCaretPosition();
            int offset = removePartialName(buffer, caretPosition);

            if (!showDot) {
                offset = removeDot(textArea, offset);
            }

            buffer.insert(offset, text);
            textArea.setCaretPosition(textArea.getCaretPosition() + caretAdjustment);

            if (space) {
                textArea.userInput(' ');
            } else if (dot) {
                textArea.userInput('.');
                member.accept(this);
            }
        }

        public void handleMethod(Method method) {
            this.continueCompleting = true;
            Set<Member> returnTypes = method.getReturnTypes();
            CodeAnalyzer.setLastReturnTypes(returnTypes);
        }

        private int removeDot(JEditTextArea textArea, int offset) {
            int caretPosition;
            caretPosition = textArea.getCaretPosition();
            Selection.Range range = new Selection.Range(caretPosition - 1, caretPosition);
            textArea.setSelection(range);
            if (caretAdjustment == 0) {
                textArea.setSelectedText(" ");
            } else {
                textArea.setSelectedText("");
                offset--;
            }
            return offset;
        }

        public int removePartialName(Buffer buffer, int offset) {
            if (partialName != null) {
                offset -= partialName.length();
                buffer.remove(offset, partialName.length());
            }
            return offset;
        }
    }

    private static class CompletionNameRenderer extends DefaultListCellRenderer implements MemberVisitor {
        String text;

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof Member) {
                Member member = (Member)value;
                member.accept(this);
            } else {
                text = String.valueOf(value);
            }
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        }

        public void handleModule(Module module) {
            text = module.getFullName();
        }

        public void handleClass(ClassMember classMember) {
            text = classMember.getFullName();
        }

        public void handleMethod(Method method) {
            if (method.getNamespace() != null) {
                text = method.getName() + "  (" + method.getNamespace() + ")";
            } else {
                text = method.getName();
            }
        }

        public void handleMethodCallWithSelfAsAnImplicitReceiver(MethodCallWithSelfAsAnImplicitReceiver methodCall) {
        }

        public void handleKeyword(KeywordMember keywordMember) {
            text = keywordMember.getName();
        }

        public void handleWarning(Warning warning) {
        }

        public void handleError(Error warning) {
        }

        public void handleRoot(Root root) {
        }
    }
}