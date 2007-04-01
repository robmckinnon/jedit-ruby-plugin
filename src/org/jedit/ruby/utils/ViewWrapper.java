/*
 * ViewWrapper.java
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
package org.jedit.ruby.utils;

import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.syntax.KeywordMap;
import org.gjt.sp.util.StandardUtilities;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.parser.RubyParser;

import java.util.*;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class ViewWrapper implements EditorView {

    private final View view;

    public ViewWrapper(View view) {
        this.view = view;
    }

    private Buffer buffer() {
        return view.getBuffer();
    }

    private JEditTextArea textArea() {
        return view.getTextArea();
    }

    public final int getCaretPosition() {
        return textArea().getCaretPosition();
    }

    public final String getLineUpToCaret() {
        int lineIndex = textArea().getCaretLine();
        int start = textArea().getLineStartOffset(lineIndex);
        int end = textArea().getCaretPosition();
        return textArea().getText(start, end - start);
    }

    public final String getLineUpToCaretLeftTrimmed() {
        String line = getLineUpToCaret();
        while (line.length() > 0 && Character.isWhitespace(line.charAt(0))) {
            line = line.substring(1);
        }
        return line;
    }

    public final void replaceLineUpToCaret(String newText) {
        int caretPosition = getCaretPosition();
        String oldText = getLineUpToCaret();
        Selection.Range range = new Selection.Range(caretPosition - oldText.length(), caretPosition);
        textArea().setSelection(range);
        textArea().setSelectedText(newText);
    }

    public final String getText(int start, int length) {
        return buffer().getText(start, length);
    }

    public final int getLength() {
        return buffer().getLength();
    }

    public final String getTextWithoutLine() {
        int caretPosition = textArea().getCaretPosition();
        int line = textArea().getLineOfOffset(caretPosition);
        int start = textArea().getLineStartOffset(line);
        int end = textArea().getLineEndOffset(line);
        StringBuffer text = new StringBuffer();
        text.append(buffer().getText(0, start));
        if (buffer().getLength() > end) {
            text.append(buffer().getText(end, buffer().getLength() - end));
        }
        return text.toString();
    }

    /**
     * If there are no errors in fresh parse
     * returns members from fresh parse.
     * If there are errors in fresh parse returns
     * members from last good parse if there was one,
     * else returns members from fresh parse.
     *
     * @return RubyMembers
     */
    public final RubyMembers getMembers() {
        RubyMembers members = RubyParser.getMembers(textArea().getView());
        boolean useLastGoodParse = members.containsErrors() && RubyParser.hasLastGoodMembers(buffer());

        return useLastGoodParse ? RubyParser.getLastGoodMembers(buffer()) : members;
    }

    /**
     * Returns {@link Member} at caret position, if caret position
     * is outside a Ruby member then the file's {@link org.jedit.ruby.ast.Root}
     * member is returned. Returns null if there are errors in the parse.
     *
     * @return {@link Member} at caret or null
     */
    public final Member getMemberAtCaretPosition() {
        RubyMembers members = getMembers();
        boolean errorsPresent = members.containsErrors();

        return errorsPresent ? null : members.getMemberAt(getCaretPosition());
    }

    public final View getView() {
        return view;
    }

    public final int getNonSpaceStartOffset(int line) {
        int offset = buffer().getLineStartOffset(line);
        int end = buffer().getLineEndOffset(line);
        String text = buffer().getLineText(line);
        int length = text.length();

        if (length > 0) {
            int index = 0;
            while (index < length
                    && (text.charAt(index) == ' ' || text.charAt(index) == '\t')
                    && (offset - index) < end) {
                index++;
            }
            offset += index;
        }

        return offset;
    }

    public final int getEndOffset(int line) {
        return buffer().getLineEndOffset(line) - 1;
    }

    public final int getEndOfFileOffset() {
        return buffer().getLineEndOffset(buffer().getLineCount() - 1);
    }

    public final int getStartOffset(int line) {
        return buffer().getLineStartOffset(line);
    }

    public final int getLineAtOffset(int offset) {
        return buffer().getLineOfOffset(offset);
    }

    public final int getLineAtCaret() {
        return textArea().getCaretLine();
    }

    public final int getTextLength() {
        return textArea().getText().length();
    }

    public List<String> getKeywords() {
        KeywordMap keywordMap = buffer().getKeywordMapAtOffset(getCaretPosition());
        String[] keywords = keywordMap.getKeywords();
        List<String> list = new ArrayList<String>(Arrays.asList(keywords));
        list.add("defined?");
        return list;
    }

    public int getCaretOffsetInLine() {
        return getCaretPosition() - getStartOffset(getLineAtCaret());
    }

    public List<String> getWords(String partialName) {
        Set<String> words = new TreeSet<String>(new Comparator<String>() {
            public int compare(String string, String otherString) {
                return StandardUtilities.compareStrings(string, otherString, false);
            }
	    });
        Set<Buffer> buffers = new HashSet<Buffer>();
        View views = jEdit.getFirstView();
        while (views != null) {
            EditPane[] panes = views.getEditPanes();
            for (EditPane pane : panes) {
                Buffer buffer = pane.getBuffer();

                if (!buffers.contains(buffer)) {
                    buffers.add(buffer);
                    int offset = (buffer == buffer() ? getCaretPosition() : 0);
                    getCompletions(buffer, partialName, "$@:_", offset, words);
                }
            }
            views = views.getNext();
        }
        return new ArrayList<String>(words);
    }

    private static void getCompletions(Buffer buffer, String partialName, String noWordSep, int caret, Set<String> completions) {
        for (int i = 0; i < buffer.getLineCount(); i++) {
            String line = buffer.getLineText(i);
            int start = buffer.getLineStartOffset(i);

            if (line.startsWith(partialName) && caret != start + partialName.length()) {
                String word = completeWord(line, 0, noWordSep);
                if (!completions.contains(word)) {
                    completions.add(word);
                }
            }

            int length = partialName.length();
            int len = line.length() - partialName.length();
            for (int j = 0; j < len; j++) {
                char c = line.charAt(j);
                if (!Character.isLetterOrDigit(c) && noWordSep.indexOf(c) == -1) {
                    if (line.regionMatches(j + 1, partialName, 0, length) && caret != start + j + partialName.length() + 1) {
                        String _word = completeWord(line, j + 1, noWordSep);
                        if (!completions.contains(_word)) {
                            completions.add(_word);
                        }
                    }
                }
            }
        }
    }

    private static String completeWord(String line, int offset, String noWordSep) {
        // '+ 1' so that findWordEnd() doesn't pick up the space at the start
        int wordEnd = TextUtilities.findWordEnd(line, offset + 1, noWordSep);
        return line.substring(offset, wordEnd);
    }

}
