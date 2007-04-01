/*
 * RubyStructureMatcher.java - 
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

import org.gjt.sp.jedit.textarea.StructureMatcher;
import org.gjt.sp.jedit.textarea.TextArea;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.utils.EditorView;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.MemberVisitorAdapter;
import org.jedit.ruby.ast.Root;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class RubyStructureMatcher extends MemberVisitorAdapter implements StructureMatcher {

    private Match match;

    public final Match getMatch(TextArea textArea) {
        if (isRuby(textArea)) {
            match = null;
            EditorView view = RubyPlugin.getActiveView();
            Member member = view.getMemberAtCaretPosition();

            if (member != null) {
                member.accept(this);
            }
            return match;
        } else {
            return null;
        }
    }

    public final void handleDefault(Member member) {
        EditorView view = RubyPlugin.getActiveView();
        int position = view.getCaretPosition();
        int start = member.getStartOuterOffset();
        int startInner = member.getStartOffset();
        int nameEnd = startInner + member.getCompositeName().length();
        int end = member.getEndOffset();

        boolean endIsPresent = end <= view.getTextLength() && view.getText(end - 3, 3).equals("end");

        if (endIsPresent) {
            boolean showStart = position > end - 4;
            boolean showEnd = position >= start && position <= startInner - 1;

            if (showStart) {
                int startLine = view.getLineAtOffset(start);
                match = new Match(this, startLine, start, view.getLineAtOffset(nameEnd), nameEnd);
            } else if (showEnd) {
                int endLine = view.getLineAtOffset(end);
                match = new Match(this, endLine, end - 3, endLine, end);
            }
        }
    }

    public final void handleRoot(Root root) {
    }


    public final void selectMatch(TextArea textArea) {
        if (isRuby(textArea)) {
            RubyPlugin.log("Select match", getClass());
        }
    }

    private static boolean isRuby(TextArea textArea) {
        return RubyPlugin.isRuby(textArea);
    }
}
