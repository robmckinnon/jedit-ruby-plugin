package org.jedit.ruby.utils;

import org.gjt.sp.jedit.View;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.ast.Member;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public interface EditorView {

    String getLineUpToCaret();

    String getText(int start, int length);

    int getLength();

    String getTextWithoutLine();

    View getView();

    int getCaretPosition();

    RubyMembers getMembers();

    Member getMemberAtCaretPosition();
}
