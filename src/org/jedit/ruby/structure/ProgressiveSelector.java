/*
 * ProgressiveSelector.java - 
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

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.TextArea;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.utils.CommandUtils;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.parser.RubyParser;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class ProgressiveSelector {

    private static final RubyTokenHandler tokenHandler = new RubyTokenHandler();

    public static void doProgressiveSelection(View view) {
        JEditTextArea textArea = view.getTextArea();
        String text = textArea.getText();

        if (text.length() > 0) {
            doProgressiveSelection(textArea, text, view);
        }
    }

    private static void doProgressiveSelection(JEditTextArea textArea, String text, View view) {
        int caretPosition = textArea.getCaretPosition();

        Selection[] selections = textArea.getSelection();
        Selection selection = selections.length > 0 ? selections[0] : null;

        textArea.selectNone();

        boolean needToSelectMoreDefault = true;

        if (caretPosition == text.length() || !matchesLiteralChar(text.charAt(caretPosition))) {
            if (!(caretPosition > 0 && matchesLiteralChar(text.charAt(caretPosition - 1)))) {
                needToSelectMoreDefault = false;
                selectWord(textArea);

                if (textArea.getSelection().length == 0) {
                    selectBeyondLine(view, textArea, selection);
                }
            }
        }

        if (needToSelectMore(textArea, selection, needToSelectMoreDefault)) {
            selectMore(caretPosition, textArea, selection, view);
        }
    }

    private static void selectMore(int caretPosition, JEditTextArea textArea, Selection selection, View view) {
        Buffer buffer = view.getBuffer();

        try {
            handleLiteral(buffer, caretPosition, textArea, selection);

        } catch (Exception e) {
            e.printStackTrace();
            RubyPlugin.log(e.getMessage(), ProgressiveSelector.class);
        }

        if (needToSelectMore(textArea, selection)) {
            selectLineExcludingWhitespace(textArea);
        }

        if (needToSelectMore(textArea, selection)) {
            selectLine(textArea);
        }

        if (needToSelectMore(textArea, selection)) {
            selectBeyondLine(view, textArea, selection);
        }
    }

    private static void handleLiteral(Buffer buffer, int caretPosition, JEditTextArea textArea, Selection selection) {
        RubyToken first = tokenHandler.getTokenAtCaret(buffer, caretPosition);
        RubyToken second = first.getNextToken();

        RubyToken prior = first;
        RubyToken current = second;

        if (prior.isLiteral() || current.isLiteral()) {
            if (current.isLiteral() && (selection != null || !prior.isLiteral())) {
                while (current.isNextLiteral()) {
                    current = current.getNextToken();
                }
            }

            if (prior.isLiteral() && (selection != null || !current.isLiteral())) {
                while (prior.isPreviousLiteral()) {
                    prior = prior.getPreviousToken();
                }
            }

            int lineStartOffset = textArea.getLineStartOffset(textArea.getCaretLine());

            int start = lineStartOffset;
            int end = lineStartOffset;

            if (prior.isLiteral()) {
                start += prior.offset;
            } else {
                start += second.offset;
            }

            if (current.isLiteral()) {
                end += current.offset + current.length;
            } else {
                end += first.offset + first.length;
            }

            RubyPlugin.log("prior " + prior + " current " + current, ProgressiveSelector.class);
            RubyPlugin.log("start " + start + " end " + end, ProgressiveSelector.class);
            if (selection != null) {
                RubyPlugin.log("sstart " + selection.getStart() + " send " + selection.getEnd(), ProgressiveSelector.class);
            }

            boolean unselectQuotes = true;
            boolean emptyString = start == (end - 2);

            if (selection != null) {
                if (selection.getStart() == start) {
                    unselectQuotes = false;
                } else if ((start + 1) == selection.getStart() && (end - 1) == selection.getEnd()) {
                    unselectQuotes = false;
                }
            } else {
                unselectQuotes = false;

                if(!emptyString) {
                    if (first.length == 1 && first.isLiteral() && !first.isPreviousLiteral()) {
                        start++;
                    } else if(second.length == 1 && second.isLiteral() && !second.isNextLiteral()) {
                        end--;
                    }
                }
            }

            if (!current.isLiteral() || !prior.isLiteral()) {
                unselectQuotes = false;
            }

            if (unselectQuotes && !emptyString) {
                start++;
                end--;
            }
            setSelection(start, end, textArea);
        }
    }

    private static boolean matchesLiteralChar(char character) {
        switch (character) {
            case '\'':
            case '"':
            case '[':
            case ']':
                return true;
            default:
                return false;
        }
    }

    private static void selectBeyondLine(View view, JEditTextArea textArea, Selection selection) {
        if (RubyPlugin.isRuby(view.getBuffer())) {
            try {
                try {
                    RubyMembers members = RubyParser.getMembers(view);
                    Member member = members.getMemberAt(textArea.getCaretPosition());
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
//            if (insideMember(textArea, member)) {
//                selectParagraphInMember(textArea, member, selection);
//            } else {
                selectMemberOrParent(member, textArea, selection);
//            }
        }
    }

    private static boolean insideMember(JEditTextArea textArea, Member member) {
        int caretLine = textArea.getCaretLine();
        int memberStartLine = textArea.getLineOfOffset(member.getStartOffset());
        int memberEndLine = textArea.getLineOfOffset(member.getEndOffset());

        return memberStartLine < caretLine && caretLine < memberEndLine;
    }

    private static void selectParagraphInMember(JEditTextArea textArea, Member member, Selection selection) {
        selectParagraph(textArea);
        Selection paragraphSelection = textArea.getSelection()[0];
        if (paragraphSelection != null) {
            boolean hitMemberStart = paragraphSelection.getStart() <= member.getStartOffset();
            boolean hitMemberEnd = paragraphSelection.getEnd() >= member.getEndOffset();

            if (!(hitMemberStart && hitMemberEnd)) {
                if (hitMemberStart) {
                    int line = textArea.getLineOfOffset(member.getStartOffset());
                    int offset = textArea.getLineStartOffset(line + 1);
                    setSelection(offset, paragraphSelection.getEnd(), textArea);
                } else if (hitMemberEnd) {
                    int line = textArea.getLineOfOffset(member.getEndOffset());
                    int offset = textArea.getLineEndOffset(line - 1);
                    setSelection(paragraphSelection.getStart(), offset, textArea);
                }
            }
            if (needToSelectMore(textArea, selection)) {
                selectMemberOrParent(member, textArea, selection);
            }
        } else {
            selectMemberOrParent(member, textArea, selection);
        }
    }

    private static void selectBeyondLineNonRuby(JEditTextArea textArea, Selection selection) {
        selectParagraph(textArea);

        if (textArea.getSelection().length == 0 || needToSelectMore(textArea, selection)) {
            selectAll(textArea);
        }
    }

    private static void selectMemberOrParent(Member member, JEditTextArea textArea, Selection selection) {
        if (insideMember(textArea, member)) {
            selectMemberContents(member, textArea);
        }

        if (needToSelectMore(textArea, selection)) {
            selectMember(member, textArea);
        }

        if (needToSelectMore(textArea, selection)) {
            if (member.hasParentMember()) {
                member = member.getParentMember();
                selectMemberOrParent(member, textArea, selection);
            } else {
                selectAll(textArea);
            }
        }
    }

    private static void selectMemberContents(Member member, JEditTextArea textArea) {
        int start = member.getStartOffset();
        int line = textArea.getLineOfOffset(start) + 1;
        start = textArea.getLineStartOffset(line);

        int end = member.getEndOffset();
        line = textArea.getLineOfOffset(end) - 1;
        end = textArea.getLineEndOffset(line);

        if (start < end) {
            setSelection(start, end, textArea);
        }
    }

    private static void selectMember(Member member, JEditTextArea textArea) {
        int start = member.getStartOffset();
        int line = textArea.getLineOfOffset(start);
        start = textArea.getLineStartOffset(line);
        int end = member.getEndOffset();
        char character = textArea.getText(end, 1).charAt(0);
        if (character != '\n' && character != '\r') {
            end++;
        }
        setSelection(start, end, textArea);
    }

    private static void setSelection(int start, int end, JEditTextArea textArea) {
        Selection.Range range = new Selection.Range(start, end);
        textArea.setSelection(range);
    }

    private static boolean needToSelectMore(JEditTextArea textArea, Selection originalSelection) {
        return needToSelectMore(textArea, originalSelection, false);
    }

    private static boolean needToSelectMore(JEditTextArea textArea, Selection originalSelection, boolean defaultNeed) {
        if (originalSelection != null && !defaultNeed) {
            Selection[] selections = textArea.getSelection();
            if (selections == null || selections.length == 0) {
                return true;
            } else {
                Selection selection = selections[0];
                int start = originalSelection.getStart();
                int end = originalSelection.getEnd();
                return selection.getStart() >= start && selection.getEnd() <= end;
            }
        } else {
            return defaultNeed;
        }
    }

    /**
     * Selects the word at the caret position.
     *
     * @since jEdit 2.7pre2
     */
    private static void selectWord(TextArea textArea) {
        int line = textArea.getCaretLine();
        int lineStart = textArea.getLineStartOffset(line);
        int offset = textArea.getCaretPosition() - lineStart;

        if (textArea.getLineLength(line) == 0)
            return;

        String lineText = textArea.getLineText(line);
        String noWordSep = ((Buffer)CommandUtils.getBuffer(textArea)).getStringProperty("noWordSep");

        if (offset == textArea.getLineLength(line))
            offset--;

        int wordStart = TextUtilities.findWordStart(lineText, offset, noWordSep);
        int wordEnd = TextUtilities.findWordEnd(lineText, offset + 1, noWordSep);

        Selection s = new Selection.Range(lineStart + wordStart, lineStart + wordEnd);
        addToSelection(textArea, s);
    }

    /**
     * Selects the paragraph at the caret position.
     *
     * @since jEdit 2.7pre2
     */
    private static void selectParagraph(JEditTextArea textArea) {
        int caretLine = textArea.getCaretLine();

        if (textArea.getLineLength(caretLine) == 0) {
            textArea.getToolkit().beep();
            return;
        }

        int start = caretLine;
        int end = caretLine;

        while (start >= 0) {
            if (textArea.getLineLength(start) == 0 || textArea.getLineText(start).trim().length() == 0)
                break;
            else
                start--;
        }

        while (end < textArea.getLineCount()) {
            if (textArea.getLineLength(end) == 0 || textArea.getLineText(end).trim().length() == 0)
                break;
            else
                end++;
        }

        int selectionStart = (start != textArea.getLineCount()-1) ? textArea.getLineStartOffset(start + 1) : textArea.getLineEndOffset(start);
        int selectionEnd = (end - 1 >= 0) ? textArea.getLineEndOffset(end - 1) - 1 : textArea.getLineStartOffset(end);
        if (selectionEnd > selectionStart) {
            Selection s = new Selection.Range(selectionStart, selectionEnd);
            addToSelection(textArea, s);
        }
    }

    /**
     * Selects the current line.
     *
     * @since jEdit 2.7pre2
     */
    private static void selectLine(JEditTextArea textArea) {
        int caretLine = textArea.getCaretLine();
        int start = textArea.getLineStartOffset(caretLine);
        int end = textArea.getLineEndOffset(caretLine) - 1;
        Selection s = new Selection.Range(start, end);
        addToSelection(textArea, s);
    }

    private static void selectLineExcludingWhitespace(JEditTextArea textArea) {
        int caretLine = textArea.getCaretLine();
        int start = textArea.getLineStartOffset(caretLine);
        int end = textArea.getLineEndOffset(caretLine) - 1;

        int nonSpaceStartOffset = RubyPlugin.getNonSpaceStartOffset(caretLine);
        if (textArea.getCaretPosition() >= nonSpaceStartOffset) {
            start = nonSpaceStartOffset;
        }

        Selection s = new Selection.Range(start, end);
        addToSelection(textArea, s);
    }

    private static void addToSelection(TextArea textArea, Selection s) {
        if (textArea.isMultipleSelectionEnabled()) {
            textArea.addToSelection(s);
        } else {
            textArea.setSelection(s);
        }
    }

    /**
     * Selects all text in the buffer.
     */
    private static void selectAll(JEditTextArea textArea) {
        textArea.setSelection(new Selection.Range(0, textArea.getBufferLength()));
    }

}
