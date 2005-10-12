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

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.syntax.DefaultTokenHandler;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.parser.RubyParser;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.RubyMembers;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class ProgressiveSelector {

    public static void doProgressiveSelection(View view) {
        JEditTextArea textArea = view.getTextArea();
        String text = textArea.getText();

        int caretPosition = textArea.getCaretPosition();

        Selection[] selections = textArea.getSelection();
        Selection selection = selections.length > 0 ? selections[0] : null;

        textArea.selectNone();

        boolean needToSelectMoreDefault = true;

        if (!matchesLiteralChar(text.charAt(caretPosition))) {
            if(!(caretPosition > 0 && matchesLiteralChar(text.charAt(caretPosition - 1)))) {
                needToSelectMoreDefault = false;
                selectWord(textArea);

                if (textArea.getSelection().length == 0) {
                    selectBeyondLine(view, textArea, selection);
                }
            }
        }

        if (needToSelectMore(textArea, selection, needToSelectMoreDefault)) {
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
    }

    private static void handleLiteral(Buffer buffer, int caretPosition, JEditTextArea textArea, Selection selection) {
        DefaultTokenHandler tokens = RubyPlugin.getTokens(buffer, caretPosition);
        Token priorToken = RubyPlugin.getToken(buffer, caretPosition, tokens);
        Token currentToken = priorToken.next;
        Token nextToken = currentToken.next;

        boolean priorLiteral = isLiteral(priorToken);
        boolean currentLiteral = isLiteral(currentToken);
        boolean nextLiteral = isLiteral(nextToken);

        if (priorLiteral && currentLiteral && nextLiteral) {
            selectLiteral(priorToken, currentToken, nextToken, textArea, selection);

        } else if(currentLiteral && nextLiteral && isLiteral(nextToken.next)) {
            selectLiteral(currentToken, nextToken, nextToken.next, textArea, selection, false);

        } else {
            Token previousToken = RubyPlugin.getPreviousToken(tokens, priorToken);
            boolean previousLiteral = isLiteral(previousToken);

            if (previousLiteral && priorLiteral && currentLiteral) {
                selectLiteral(previousToken, priorToken, currentToken, textArea, selection);

            } else {
                Token earlierToken = RubyPlugin.getPreviousToken(tokens, previousToken);

                if (isLiteral(earlierToken) && previousLiteral && priorLiteral) {
                    selectLiteral(earlierToken, previousToken, priorToken, textArea, selection, false);

                } else if (previousLiteral && priorLiteral) {
                    selectLiteral(previousToken, null, priorToken, textArea, selection);

                } else if (priorLiteral && currentLiteral) {
                    selectLiteral(priorToken, null, currentToken, textArea, selection);

                } else if (currentLiteral && nextLiteral) {
                    selectLiteral(currentToken, null, nextToken, textArea, selection);
                }
            }
        }
    }

    private static void selectLiteral(Token literalStart, Token literal, Token literalEnd, JEditTextArea textArea, Selection selection) {
        selectLiteral(literalStart, literal, literalEnd, textArea, selection, true);
    }

    private static void selectLiteral(Token literalStart, Token literal, Token literalEnd, JEditTextArea textArea, Selection selection, boolean inside) {
        int lineStartOffset = textArea.getLineStartOffset(textArea.getCaretLine());

        if (literal != null) {
            int offset = literal.offset + lineStartOffset;
            int end = offset + literal.length;
            setSelection(offset, end, textArea);
            if (!inside || needToSelectMore(textArea, selection)) {
                setSelection(offset - literalStart.length, end + literalEnd.length, textArea);
            }

        } else {
            int offset = literalStart.offset + lineStartOffset;
            int end = offset + literalStart.length + literalEnd.length;
            setSelection(offset, end, textArea);
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

    private static boolean isLiteral(Token token) {
        if (token == null) {
            return false;
        } else {
            switch (token.id) {
                case Token.LITERAL1:
                case Token.LITERAL2:
                case Token.LITERAL3:
                case Token.LITERAL4:
                    return true;
                default:
                    return false;
            }
        }
    }

    private static void selectBeyondLine(View view, JEditTextArea textArea, Selection selection) {
        if (RubyPlugin.isRubyFile(view.getBuffer())) {
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
            if (insideMember(textArea, member)) {
                selectParagraphInMember(textArea, member, selection);
            } else {
                selectMemberOrParent(member, textArea, selection);
            }
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
                    int offset = textArea.getLineStartOffset(line+1);
                    setSelection(offset, paragraphSelection.getEnd(), textArea);
                } else if(hitMemberEnd) {
                    int line = textArea.getLineOfOffset(member.getEndOffset());
                    int offset = textArea.getLineEndOffset(line-1);
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
        if(insideMember(textArea, member)) {
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
            Selection selection = textArea.getSelection()[0];
            int start = originalSelection.getStart();
            int end = originalSelection.getEnd();
            return selection.getStart() >= start && selection.getEnd() <= end;
        } else {
            return defaultNeed;
        }
    }

    /**
     * Selects the word at the caret position.
     * @since jEdit 2.7pre2
     */
    private static void selectWord(JEditTextArea textArea) {
        int line = textArea.getCaretLine();
        int lineStart = textArea.getLineStartOffset(line);
        int offset = textArea.getCaretPosition() - lineStart;

        if(textArea.getLineLength(line) == 0)
            return;

        String lineText = textArea.getLineText(line);
        String noWordSep = textArea.getBuffer().getStringProperty("noWordSep");

        if(offset == textArea.getLineLength(line))
            offset--;

        int wordStart = TextUtilities.findWordStart(lineText,offset,noWordSep);
        int wordEnd = TextUtilities.findWordEnd(lineText,offset+1,noWordSep);

        Selection s = new Selection.Range(lineStart + wordStart, lineStart + wordEnd);
        addToSelection(textArea, s);
    }

    /**
     * Selects the paragraph at the caret position.
     * @since jEdit 2.7pre2
     */
    private static void selectParagraph(JEditTextArea textArea) {
        int caretLine = textArea.getCaretLine();

        if(textArea.getLineLength(caretLine) == 0) {
            textArea.getToolkit().beep();
            return;
        }

        int start = caretLine;
        int end = caretLine;

        while(start >= 0) {
            if(textArea.getLineLength(start) == 0 || textArea.getLineText(start).trim().length() == 0)
                break;
            else
                start--;
        }

        while(end < textArea.getLineCount()) {
            if(textArea.getLineLength(end) == 0 || textArea.getLineText(end).trim().length() == 0)
                break;
            else
                end++;
        }

        int selectionStart = textArea.getLineStartOffset(start + 1);
        int selectionEnd = textArea.getLineEndOffset(end - 1) - 1;
        if (selectionEnd > selectionStart) {
            Selection s = new Selection.Range(selectionStart,selectionEnd);
            addToSelection(textArea, s);
        }
    }

    /**
     * Selects the current line.
     * @since jEdit 2.7pre2
     */
    private static void selectLine(JEditTextArea textArea) {
        int caretLine = textArea.getCaretLine();
        int start = textArea.getLineStartOffset(caretLine);
        int end = textArea.getLineEndOffset(caretLine) - 1;
        Selection s = new Selection.Range(start,end);
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

    private static void addToSelection(JEditTextArea textArea, Selection s) {
        if(textArea.isMultipleSelectionEnabled()) {
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
