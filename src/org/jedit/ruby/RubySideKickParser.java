/*
 * RubySideKickParser.java - Side Kick Parser for Ruby
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
package org.jedit.ruby;

import sidekick.SideKickParser;
import sidekick.SideKickParsedData;
import org.gjt.sp.jedit.Buffer;
import errorlist.DefaultErrorSource;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RubySideKickParser extends SideKickParser {

    public RubySideKickParser() {
        super("ruby");
    }

    public SideKickParsedData parse(Buffer buffer, DefaultErrorSource errorSource) {
        String text = buffer.getText(0, buffer.getLength());
        Member[] members = RubyParser.getMembers(text);
        SideKickParsedData data = new SideKickParsedData(buffer.getName());

        DefaultMutableTreeNode parentNode = data.root;
        addNodes(parentNode, members, buffer);

        return data;
    }

    private void addNodes(DefaultMutableTreeNode parentNode, Member[] members, Buffer buffer) {
        for(Member member : members) {
            MemberNode node = new MemberNode(member);
            node.start = buffer.createPosition(member.getOffset());
            node.end = buffer.createPosition(member.getOffset());
            DefaultMutableTreeNode treeNode = node.createTreeNode();
            if(member.hasChildMembers()) {
                Member[] childMembers = member.getChildMembers();
                addNodes(treeNode, childMembers, buffer);
            }
            parentNode.add(treeNode);
        }
    }
}
