/*
 * TypeAheadPopupListCellRenderer.java - 
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

import org.jedit.ruby.ast.Member;
import org.jedit.ruby.icons.MemberIcon;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.util.List;

/**
 * @author robmckinnon at users.sourceforge.net
 */
final class TypeAheadPopupListCellRenderer extends DefaultListCellRenderer {

    private final Member gotoParentMember;
    private boolean allHaveSameName;
    private final boolean showAll;

    public TypeAheadPopupListCellRenderer(Member toParentMember, boolean showAllMembers) {
        gotoParentMember = toParentMember;
        showAll = showAllMembers;
        allHaveSameName = false;
    }

    public final Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);

        Member member = (Member)value;
        StringBuffer buffer = new StringBuffer();

        if (member == gotoParentMember) {
            buffer.append("[ " + org.jedit.ruby.structure.TypeAheadPopup.UP_TO_PARENT_TEXT + " ]");
        } else {
            if (showAll) {
                for (int i = 0; i < member.getParentCount(); i++) {
                    buffer.append("  ");
                }
                buffer.append(member.getDisplayName());
            } else {
                buffer.append(member.getDisplayName());
            }

            if (allHaveSameName && member.hasParentMember()) {
                String diplayName = member.getParentMember().getDisplayName();
                buffer.append(" (").append(diplayName).append(")");
            }
        }

        setText(buffer.toString());
        MemberIcon memberIcon = new MemberIcon(member);
        setIcon(memberIcon.getIcon());
        return this;
    }

    final void resetAllHaveSameName(List<Member> members) {
        allHaveSameName = members.size() > 1;
        String name = null;
        for (Member member : members) {
            if (name != null) {
                allHaveSameName = allHaveSameName && name.equals(member.getFullName());
            }
            name = member.getFullName();
        }
    }
}
