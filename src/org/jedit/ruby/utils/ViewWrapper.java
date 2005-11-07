package org.jedit.ruby.utils;

import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.parser.RubyParser;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class ViewWrapper implements EditorView {

    private JEditTextArea textArea;
    private Buffer buffer;

    public ViewWrapper(View view) {
        this.textArea = view.getTextArea();
        this.buffer = view.getBuffer();
    }

    public int getCaretPosition() {
        return textArea.getCaretPosition();
    }

    public String getLineUpToCaret() {
        int lineIndex = textArea.getCaretLine();
        int start = textArea.getLineStartOffset(lineIndex);
        int end = textArea.getCaretPosition();
        return textArea.getText(start, end - start);
    }

    public String getText(int start, int length) {
        return buffer.getText(start, length);
    }

    public int getLength() {
        return buffer.getLength();
    }

    public String getTextWithoutLine() {
        int caretPosition = textArea.getCaretPosition();
        int line = textArea.getLineOfOffset(caretPosition);
        int start = textArea.getLineStartOffset(line);
        int end = textArea.getLineEndOffset(line);
        StringBuffer text = new StringBuffer();
        text.append(buffer.getText(0, start));
        if(buffer.getLength() > end) {
            text.append(buffer.getText(end, buffer.getLength() - end));
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
    public RubyMembers getMembers() {
        RubyMembers members = RubyParser.getMembers(textArea.getView());
        boolean useLastGoodParse = members.containsErrors() && RubyParser.hasLastGoodMembers(buffer);

        return useLastGoodParse ? RubyParser.getLastGoodMembers(buffer) : members;
    }

    /**
     * Returns {@link Member} at caret position, if caret position
     * is outside a Ruby member then the file's {@link org.jedit.ruby.ast.Root}
     * member is returned. Returns null if there are errors in the parse.
     *
     * @return {@link Member} at caret or null
     */
    public Member getMemberAtCaretPosition() {
        RubyMembers members = getMembers();
        boolean errorsPresent = members.containsErrors();

        return errorsPresent ? null : members.getMemberAt(getCaretPosition());
    }

    public View getView() {
        return textArea.getView();
    }
}
