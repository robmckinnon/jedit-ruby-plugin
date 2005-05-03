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
public class MemberNode extends Asset {

    private Icon icon;

    public MemberNode(Member member) {
        super(member.getName());
        MemberIcon memberIcon = new MemberIcon(member);
        icon = memberIcon.getIcon();
    }

    public DefaultMutableTreeNode createTreeNode() {
       return new DefaultMutableTreeNode(this, true);
    }

    public Icon getIcon() {
        return icon;
    }

    public String getShortString() {
        return name;
    }

    public String getLongString() {
        return getShortString();
    }

    public String toString() {
        return name + " " + start + " " + end;
    }

}
