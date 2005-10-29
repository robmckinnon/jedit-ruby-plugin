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
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Method;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.parser.RubyParser;
import org.jedit.ruby.structure.RubySideKickParser;
import org.jedit.ruby.structure.*;
import org.jedit.ruby.structure.TypeAheadPopup;
import org.jedit.ruby.structure.FileStructurePopup;
import org.jedit.ruby.structure.AutoIndentAndInsertEnd;
import org.jedit.ruby.cache.*;
import org.jedit.ruby.ri.*;
import org.jedit.ruby.utils.CommandUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.io.IOException;

import sidekick.SideKickActions;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public final class RubyActions {

    public static void progressiveSelection(View view) {
        ProgressiveSelector.doProgressiveSelection(view);
    }

    public static void searchDocumentation(View view) {
        try {
            if (CommandUtils.isRubyInstalled()) {
                RDocSeacher.doSearch(view);
            }
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
        List<Method> methods = RubyCache.instance().getMethods(text);
        RubyPlugin.log("found: " + methods.size(), RubyActions.class);

        if (methods.size() > 0) {
            Member[] displayMembers = methods.toArray(new Member[0]);
            new TypeAheadPopup(view, displayMembers, displayMembers[0], org.jedit.ruby.structure.TypeAheadPopup.FIND_DECLARATION_POPUP);
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
        if(isRubyFile(view)) {
// start = System.currentTimeMillis();
        AutoIndentAndInsertEnd.performIndent(view);
// end = System.currentTimeMillis();
// Macros.message(view, "" + (end - start));
        } else {
            view.getTextArea().insertEnterAndIndent();
        }
    }

    public static void nextMethod(View view) {
        if(isRubyFile(view)) {
            RubyMembers members = RubyParser.getMembers(view);

            if (!members.containsErrors()) {
                JEditTextArea textArea = view.getTextArea();
                int caretPosition = textArea.getCaretPosition();
                Member member = members.getNextMember(caretPosition);

                if (member != null) {
                    textArea.setCaretPosition(member.getStartOffset(), true);
                } else {
                    textArea.setCaretPosition(textArea.getBufferLength(), true);
                }
            }
        } else {
            SideKickActions.goToNextAsset(view);
        }
    }

    public static void previousMethod(View view) {
        if(isRubyFile(view)) {
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
        } else {
            SideKickActions.goToPrevAsset(view);
        }
    }

    public static void nextError(View view) {
        if(isRubyFile(view)) {
            JEditTextArea textArea = view.getTextArea();
            int caretPosition = textArea.getCaretPosition();
            ErrorSource.Error[] errors = org.jedit.ruby.structure.RubySideKickParser.getErrors();

            for (ErrorSource.Error error : errors) {
                int offset = RubyPlugin.getNonSpaceStartOffset(error.getLineNumber());
                if (caretPosition < offset) {
                    textArea.setCaretPosition(offset, true);
                    break;
                }
            }
        }
    }

    public static void previousError(View view) {
        if(isRubyFile(view)) {
            JEditTextArea textArea = view.getTextArea();
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

    private static boolean isRubyFile(View view) {
        return RubyPlugin.isRubyFile(view.getBuffer());
    }
}
