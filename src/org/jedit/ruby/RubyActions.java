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
import gnu.regexp.REException;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.awt.Point;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public class RubyActions {

    public static void findDeclaration(View view) {
        JEditTextArea textArea = view.getTextArea();
        int caretPosition = textArea.getCaretPosition();
        textArea.selectWord();
        String text = textArea.getSelectedText();
        textArea.setCaretPosition(caretPosition);

        System.out.println("looking for methods named: " + text);
        List<Member.Method> methods = RubyCache.getMethods(text);
        System.out.println("found: " + methods.size());

        if (methods.size() > 0) {
            Member[] displayMembers = methods.toArray(new Member[0]);

            textArea.scrollToCaret(false);
            Point location = textArea.offsetToXY(textArea.getCaretPosition());
            location.y += textArea.getPainter().getFontMetrics().getHeight();
            SwingUtilities.convertPointToScreen(location, textArea.getPainter());

            new TypeAheadPopup(view, displayMembers, displayMembers[0], location);
        }
    }

    public static void completeMethod(View view) {
        CodeCompletor codeCompletor = new CodeCompletor(view);
        codeCompletor.completeRubyMethod();
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
        try {
            AutoIndentAndInsertEnd indenter = new AutoIndentAndInsertEnd(view);
            indenter.performIndent();
        } catch (REException e) {
            e.printStackTrace();
        }
    }

    public static void nextMethod(View view) {
        RubyMembers members = RubyParser.getMembers(view);

        if(!members.containsErrors()) {
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

        if(!members.containsErrors()) {
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
            int offset = getOffset(error, textArea);
            if(caretPosition < offset) {
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
            int offset = getOffset(error, textArea);
            if(caretPosition > offset) {
                textArea.setCaretPosition(offset, true);
                break;
            }
        }
    }

    private static int getOffset(ErrorSource.Error error, JEditTextArea textArea) {
        int line = error.getLineNumber() - 1;
        return textArea.getLineEndOffset(line);
    }
//
//    public static void goToPreviousError(View view) {
//        getErrorList(view).previousError();
//        view.getTextArea().requestFocus();
//        jEdit.getBooleanProperty("");
//    }
//
//    private static ErrorList getErrorList(View view) {
//        ErrorList errorList2 = new ErrorList(view);
//        DockableWindowManager windowManager = view.getDockableWindowManager();
//        windowManager.showDockableWindow("error-list");
//        ErrorList errorList = ((ErrorList)windowManager.getDockable("error-list"));
//        windowManager.hideDockableWindow("error-list");
//        return errorList2;
//    }
}
