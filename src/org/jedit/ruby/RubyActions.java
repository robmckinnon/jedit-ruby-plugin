/*
 * RubyActions.java - Actions for Ruby plugin
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

import errorlist.ErrorSource;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.Macros;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Method;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.parser.RubyParser;
import org.jedit.ruby.sidekick.RubySideKickParser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.io.IOException;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public class RubyActions {

    public static void progressiveSelection(View view) {
        JEditTextArea textArea = view.getTextArea();

        Selection[] selections = textArea.getSelection();
        Selection selection = selections.length > 0 ? selections[0] : null;

        textArea.selectNone();
        if (selection != null) {
            textArea.setCaretPosition(selection.getStart());
        }

        textArea.selectWord();

        if(textArea.getSelection().length == 0) {
            selectBeyondLine(view, textArea, selection);
        }

        if (needToSelectMore(textArea, selection)) {
            textArea.selectLine();
            if (needToSelectMore(textArea, selection)) {
                selectBeyondLine(view, textArea, selection);
            }
        }
    }

    private static void selectBeyondLine(View view, JEditTextArea textArea, Selection selection) {
        if (RubyPlugin.isRubyFile(view.getBuffer())) {
            try {
                Member member = null;
                try {
                    RubyMembers members = RubyParser.getMembers(view);
                    member = members.getCurrentMember(textArea.getCaretPosition());
                    selectBeyondLineRuby(textArea, selection, member);
                } catch (Exception e) {
                    selectBeyondLineNonRuby(textArea, selection);
                }
            } catch (Exception e) {
                selectBeyondLineNonRuby(textArea, selection);
            }
        } else {
            selectBeyondLineNonRuby(textArea, selection);
        }
    }

    private static void selectBeyondLineRuby(JEditTextArea textArea, Selection selection, Member member) {
        if (member == null) {
            selectBeyondLineNonRuby(textArea, selection);
        } else {
            selectMemberOrParent(member, textArea, selection);
        }
    }

    private static void selectBeyondLineNonRuby(JEditTextArea textArea, Selection selection) {
        textArea.selectParagraph();

        if (textArea.getSelection().length == 0 || needToSelectMore(textArea, selection)) {
            textArea.selectAll();
        }
    }

    private static void selectMemberOrParent(Member member, JEditTextArea textArea, Selection selection) {
        selectMember(member, textArea);

        if (needToSelectMore(textArea, selection)) {
            if (member.hasParentMember()) {
                member = member.getParentMember();
                selectMemberOrParent(member, textArea, selection);
            } else {
                textArea.selectAll();
            }
        }
    }

    private static void selectMember(Member member, JEditTextArea textArea) {
        int start = member.getStartOffset();
        int line = textArea.getLineOfOffset(start);
        start = textArea.getLineStartOffset(line);
        int end = member.getEndOffset();
        char character = textArea.getText(end, 1).charAt(0);
        if (character != '\n' || character != '\r') {
            end++;
        }
        Selection.Range range = new Selection.Range(start, end);
        textArea.setSelection(range);
    }

    private static boolean needToSelectMore(JEditTextArea textArea, Selection originalSelection) {
        if(originalSelection != null) {
            Selection selection = textArea.getSelection()[0];
            int start = originalSelection.getStart();
            int end = originalSelection.getEnd();
            return selection.getStart() >= start && selection.getEnd() <= end;
        } else {
            return false;
        }
    }

    public static void searchDocumentation(View view) {
        try {
            RDocSeacher.doSearch(view);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void findDeclaration(View view) {
        JEditTextArea textArea = view.getTextArea();
        int caretPosition = textArea.getCaretPosition();
        textArea.selectWord();
        String text = textArea.getSelectedText();
        textArea.setCaretPosition(caretPosition);

        RubyPlugin.log("looking for methods named: " + text, RubyActions.class);
        List<Method> methods = RubyCache.getMethods(text);
        RubyPlugin.log("found: " + methods.size(), RubyActions.class);

        if (methods.size() > 0) {
            Member[] displayMembers = methods.toArray(new Member[0]);
            new TypeAheadPopup(view, displayMembers, displayMembers[0], TypeAheadPopup.FIND_DECLARATION_POPUP);
        } else {
            Macros.message(textArea, jEdit.getProperty("ruby.find-declaration.no-matches.label"));
        }
    }
    
    public static void fileStructurePopup(View view) {
        FileStructurePopup fileStructurePopup = new FileStructurePopup(view);
        fileStructurePopup.show();
    }

    public static void structureBrowser(View view) {
        DockableWindowManager windowManager = view.getDockableWindowManager();
        windowManager.showDockableWindow("sidekick-tree");
    }

    public static void autoIndentAndInsertEnd(View view) {
// start = System.currentTimeMillis();
        AutoIndentAndInsertEnd.performIndent(view);
// end = System.currentTimeMillis();
// Macros.message(view, "" + (end - start));
    }

    public static void nextMethod(View view) {
        RubyMembers members = RubyParser.getMembers(view);

        if (!members.containsErrors()) {
            JEditTextArea textArea = view.getTextArea();
            int caretPosition = textArea.getCaretPosition();
            Member member = members.getNextMember(caretPosition);

            if (member != null) {
                textArea.setCaretPosition(member.getStartOffset(), true);
            } else {
                textArea.setCaretPosition(textArea.getBufferLength() - 1, true);
            }
        }
    }

    public static void previousMethod(View view) {
        RubyMembers members = RubyParser.getMembers(view);

        if (!members.containsErrors()) {
            JEditTextArea textArea = view.getTextArea();
            int caretPosition = textArea.getCaretPosition();
            Member member = members.getCurrentMember(caretPosition);
            if (member != null && caretPosition == member.getStartOffset()) {
                member = members.getPreviousMember(caretPosition);
            }

            if (member != null) {
                textArea.setCaretPosition(member.getStartOffset(), true);
            } else {
                textArea.setCaretPosition(0, true);
            }
        }
    }

    public static void nextError(JEditTextArea textArea) {
        int caretPosition = textArea.getCaretPosition();
        ErrorSource.Error[] errors = RubySideKickParser.getErrors();

        for (ErrorSource.Error error : errors) {
            int offset = RubyPlugin.getNonSpaceStartOffset(error.getLineNumber());
            if (caretPosition < offset) {
                textArea.setCaretPosition(offset, true);
                break;
            }
        }
    }

    public static void previousError(JEditTextArea textArea) {
        int caretPosition = textArea.getCaretPosition();
        List<ErrorSource.Error> errors = Arrays.asList(RubySideKickParser.getErrors());
        Collections.reverse(errors);

        for (ErrorSource.Error error : errors) {
            int offset = RubyPlugin.getNonSpaceStartOffset(error.getLineNumber());
            if (caretPosition > offset) {
                textArea.setCaretPosition(offset, true);
                break;
            }
        }
    }

}
