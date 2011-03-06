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
import org.gjt.sp.jedit.*;
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

import java.util.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;

import sidekick.SideKickActions;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public final class RubyActions {

    public static void progressiveSelection(View view) {
        ProgressiveSelector.doProgressiveSelection(view);
    }

    public static void pasteLine(View view, JEditTextArea textArea) {
        if (!view.getBuffer().isReadOnly()) {
            if (isLineInClipBoard() && textArea.getSelectionCount() == 0) {
                int lineLength = jEdit.getIntegerProperty("ruby.clipboard-line-length", 0);
                String clipBoardText = Registers.getRegister('$').toString();

                if (clipBoardText.length() == lineLength) {
                    int position = textArea.getCaretPosition();
                    int line = textArea.getCaretLine();
                    int startOffset = textArea.getLineStartOffset(line);
                    view.getBuffer().insert(startOffset, clipBoardText);
                    textArea.setCaretPosition(position + clipBoardText.length());
                } else {
                    setLineInClipBoard(false);
                    pasteFromClipBoard(textArea);
                }
            } else {
                pasteFromClipBoard(textArea);
            }
        }
    }

    public static void copyLine(JEditTextArea textArea) {
        if (textArea.getSelectionCount() > 0) {
            copySelectionToClipBoard(textArea);
        } else {
            putLineInClipBoard(textArea);
        }
    }

    public static void cutLine(JEditTextArea textArea) {
        if (textArea.getSelectionCount() > 0) {
            cutSelectionToClipBoard(textArea);
        } else {
            putLineInClipBoard(textArea);
            textArea.deleteLine();
        }
    }

    private static void putLineInClipBoard(JEditTextArea textArea) {
        int line = textArea.getCaretLine();
        String text = textArea.getLineText(line) + jEdit.getProperty("buffer.lineSeparator");
        Registers.getRegister('$').setTransferable( new StringSelection(text) );
        jEdit.setProperty("ruby.clipboard-line-length", Integer.toString(text.length()));
        setLineInClipBoard(true);
    }

    private static void copySelectionToClipBoard(JEditTextArea textArea) {
        Registers.copy(textArea, '$');
        setLineInClipBoard(false);
    }

    private static void cutSelectionToClipBoard(JEditTextArea textArea) {
        Registers.cut(textArea, '$');
        setLineInClipBoard(false);
    }

    private static void pasteFromClipBoard(JEditTextArea textArea) {
        Registers.paste(textArea, '$');
    }

    private static boolean isLineInClipBoard() {
        return jEdit.getBooleanProperty("ruby.line-in-clipboard", false);
    }

    private static void setLineInClipBoard(boolean isLine) {
        jEdit.setProperty("ruby.line-in-clipboard", Boolean.valueOf(isLine).toString());
    }

    public static void introduceVariable(View view) {
        Debug.DUMP_KEY_EVENTS = true;

        String prompt = jEdit.getProperty("ruby.introduce-variable.message");
        String name = Macros.input(view, prompt);

        if (name != null && name.length() > 0) {
            JEditTextArea textArea = view.getTextArea();
            cutSelectionToClipBoard(textArea);
            textArea.setSelectedText(name);
            textArea.goToPrevLine(false);
            textArea.goToEndOfWhiteSpace(false);
            RubyActions.autoIndentAndInsertEnd(view);
            textArea.setSelectedText(name + " = ");
            pasteFromClipBoard(textArea);
            textArea.goToNextLine(false);
        }
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
            Member[] displayMembers = methods.toArray(new Member[methods.size()]);
            new TypeAheadPopup(view, displayMembers, displayMembers[0], org.jedit.ruby.structure.TypeAheadPopup.FIND_DECLARATION_POPUP);
        } else {
            Macros.message(textArea, jEdit.getProperty("ruby.find-declaration.no-matches.label"));
        }
    }

    public static void fileStructurePopup(View view) {
        FileStructurePopup fileStructurePopup = new FileStructurePopup(view);
        fileStructurePopup.show();
    }

    public static void toggleSpec(View view) {
        SpecToggler specToggler = new SpecToggler(view);
        specToggler.toggleSpec();
    }

    public static void structureBrowser(View view) {
        DockableWindowManager windowManager = view.getDockableWindowManager();
        windowManager.showDockableWindow("sidekick-tree");
    }

    public static void autoIndentAndInsertEnd(View view) {
        if (isRubyFile(view)) {
// start = System.currentTimeMillis();
            AutoIndentAndInsertEnd.performIndent(view);
// end = System.currentTimeMillis();
// Macros.message(view, "" + (end - start));
        } else {
            view.getTextArea().insertEnterAndIndent();
        }
    }

    public static void nextMethod(View view) {
        if (isRubyFile(view)) {
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
            try {
                SideKickActions.goToNextAsset(view);
            } catch (IllegalArgumentException e) {
                // bug in SideKick
                JEditTextArea textArea = view.getTextArea();
                textArea.goToNextLine(false);
            }
        }
    }

    public static void previousMethod(View view) {
        if (isRubyFile(view)) {
            RubyMembers members = RubyParser.getMembers(view);

            if (!members.containsErrors()) {
                JEditTextArea textArea = view.getTextArea();
                int caretPosition = textArea.getCaretPosition();
                Member member = members.getLastMemberBefore(caretPosition);
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
            try {
                SideKickActions.goToPrevAsset(view);
            } catch (NullPointerException e) {
                // bug in SideKick, do nothing
            }
        }
    }

    public static void previousEdit(View view) {
        BufferChangeHandler.instance().gotoPreviousEdit(view);
    }

    public static void nextError(View view) {
        if (isRubyFile(view)) {
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
        if (isRubyFile(view)) {
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
        return RubyPlugin.isRuby(view.getBuffer());
    }
}