package org.jedit.ruby.utils;

import org.gjt.sp.jedit.View;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.ast.Member;

import java.util.List;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public interface EditorView {

    public final static EditorView NULL = new NullEditorView();

    String getLineUpToCaret();

    String getLineUpToCaretLeftTrimmed();

    String getText(int start, int length);

    int getLength();

    String getTextWithoutLine();

    View getView();

    int getCaretPosition();

    RubyMembers getMembers();

    Member getMemberAtCaretPosition();

    int getNonSpaceStartOffset(int line);

    int getEndOffset(int line);

    int getEndOfFileOffset();

    int getStartOffset(int line);

    int getLineAtOffset(int offset);

    int getLineAtCaret();

    int getTextLength();

    List<String> getKeywords();

    List<String> getWords(String partialName);

    int getCaretOffsetInLine();

    public static class NullEditorView implements EditorView {

        public String getLineUpToCaret() {
            return null;
        }

        public final String getLineUpToCaretLeftTrimmed() {
            return null;
        }

        public String getText(int start, int length) {
            return null;
        }

        public int getLength() {
            return 0;
        }

        public final String getTextWithoutLine() {
            return null;
        }

        public final View getView() {
            return null;
        }

        public int getCaretPosition() {
            return 0;
        }

        public final RubyMembers getMembers() {
            return null;
        }

        public final Member getMemberAtCaretPosition() {
            return null;
        }

        public final int getNonSpaceStartOffset(int line) {
            return 0;
        }

        public final int getEndOffset(int line) {
            return 0;
        }

        public final int getEndOfFileOffset() {
            return 0;
        }

        public final int getStartOffset(int line) {
            return 0;
        }

        public final int getLineAtOffset(int offset) {
            return 0;
        }

        public final int getLineAtCaret() {
            return 0;
        }

        public final int getTextLength() {
            return 0;
        }

        public List<String> getKeywords() {
            return null;
        }

        public int getCaretOffsetInLine() {
            return 0;
        }

        public List<String> getWords(String partialName) {
            return null;
        }
    }
}
