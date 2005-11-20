/*
 * FileStructurePopup.java - File structure popup
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
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Problem;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.parser.RubyParser;
import org.jedit.ruby.RubyPlugin;

/**
 * Shows file structure popup to allow user to navigate
 * to member locations within a file.
 *
 * @author robmckinnon at users,sourceforge,net
 */
public final class FileStructurePopup {

    private final View view;
    private long start;

    public FileStructurePopup(View view) {
        this.view = view;
    }

    public final void show() {
        if (RubyPlugin.isRuby(view.getBuffer())) {
            try {
                log("showing file structure popup");
                view.showWaitCursor();
                showPopup(view);
            } catch (Exception e) {
                RubyPlugin.error(e, getClass());
            } finally {
                view.hideWaitCursor();
            }
        } else {
            RubyPlugin.showMessage("ruby.file-structure-popup.label", "ruby.not-ruby-file.message", view);
        }
    }

    private void showPopup(View view) {
        start = now();

        Buffer buffer = view.getBuffer();
        RubyMembers members = RubyParser.getMembers(view);

        if (!members.containsErrors() && members.size() > 0) {
            showPopup(view, members, members.getMembers());

        } else if(RubyParser.hasLastGoodMembers(buffer)) {
            RubyMembers lastGoodMembers = RubyParser.getLastGoodMembers(buffer);
            Member[] displayMembers = lastGoodMembers.combineMembersAndProblems(buffer.getLength());
            showPopup(view, displayMembers, displayMembers[displayMembers.length - 1]);

        } else {
            Problem[] problems = members.getProblems();

            if(problems.length > 0) {
                showPopup(view, problems, problems[0]);
            } else {
                RubyPlugin.showMessage("ruby.file-structure-popup.label", "ruby.file-structure-popup.no-structure.label", view);
            }
        }
    }

    private void showPopup(View view, RubyMembers members, Member[] displayMembers) {
        int caretPosition = view.getTextArea().getCaretPosition();
        Member selectedMember = members.getLastMemberBefore(caretPosition);
        showPopup(view, displayMembers, selectedMember);
    }

    private void showPopup(View view, Member[] displayMembers, Member selectedMember) {
        try {
            new org.jedit.ruby.structure.TypeAheadPopup(view, displayMembers, selectedMember, TypeAheadPopup.FILE_STRUCTURE_POPUP);
        } catch (Exception e) {
            RubyPlugin.error(e, getClass());
        }
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private void log(String msg) {
        RubyPlugin.log(msg + (now() - start), getClass());
        start = now();
    }

}
