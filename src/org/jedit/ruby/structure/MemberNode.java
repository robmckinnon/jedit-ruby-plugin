/*
 * MemberNode.java - 
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

import sidekick.Asset;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.Icon;

import org.jedit.ruby.ast.Member;
import org.jedit.ruby.icons.MemberIcon;

/**
 * @author robmckinnon at users.sourceforge.net
 */
final class MemberNode extends Asset {

    private final Icon icon;

    public MemberNode(Member member) {
        super(member.getDisplayName());
        MemberIcon memberIcon = new MemberIcon(member);
        icon = memberIcon.getIcon();
    }

    public final DefaultMutableTreeNode createTreeNode() {
       return new DefaultMutableTreeNode(this, true);
    }

    public final Icon getIcon() {
        return icon;
    }

    public final String getShortString() {
        return getName();
    }

    public final String getLongString() {
        return getShortString();
    }

    public final String toString() {
        return getName() + " " + getStart() + " " + getEnd();
    }

}
