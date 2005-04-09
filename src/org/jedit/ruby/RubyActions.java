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
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Method;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.parser.RubyParser;
import org.jedit.ruby.sidekick.RubySideKickParser;

import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.awt.Point;
import java.io.IOException;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public class RubyActions {

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

        RubyPlugin.log("looking for methods named: " + text);
        List<Method> methods = RubyCache.getMethods(text);
        RubyPlugin.log("found: " + methods.size());

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
