/*
 * Member.java - Ruby file structure member
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

import java.util.ArrayList;
import java.util.List;

/**
 * Ruby file structure member
 * @author robmckinnon at users.sourceforge.net
 */
public class Member {

    private static final Member[] EMPTY_MEMBER_ARRAY = new Member[0];

    private String namespace;
    private String name;
    private int offset;
    private Member parentMember;
    private ArrayList<Member> childMembers;

    public Member(String name) {
        this.name = name;
    }

    public Member(String name, int offset) {
        this.offset = offset;
        this.name = name;
    }

    public void visitMember(Member.Visitor visitor) {

    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDisplayName() {
        if(namespace == null) {
            return name;
        } else {
            return namespace + name;
        }
    }

    public String getName() {
        return name;
    }

    public String getLowerCaseName() {
        return name;
    }

    public int getOffset() {
        return offset;
    }

    public String toString() {
        return getDisplayName();
    }

    public boolean hasChildMembers() {
        return childMembers != null;
    }

    public Member[] getChildMembers() {
        if(hasChildMembers()) {
            return childMembers.toArray(EMPTY_MEMBER_ARRAY);
        } else {
            return null;
        }
    }

    public List<Member> getChildMembersAsList() {
        return childMembers;
    }

    public void addChildMember(Member member) {
        if(childMembers == null) {
            childMembers = new ArrayList<Member>();
        }
        childMembers.add(member);
        member.setParentMember(this);
    }

    public Member getParentMember() {
        return parentMember;
    }

    private void setParentMember(Member parentMember) {
        this.parentMember = parentMember;
    }

    public static interface Visitor {
        public void handleModule();
        public void handleClass();
        public void handleMethod();
    }

    public static class Module extends Member {
        public Module(String name, int offset) {
            super(name, offset);
        }

        public void visitMember(Visitor visitor) {
            visitor.handleModule();
        }
    }

	public static class Class extends Member {
        public Class(String name, int offset) {
            super(name, offset);
        }

        public void visitMember(Visitor visitor) {
            visitor.handleClass();
        }
	}

	public static class Method extends Member {
        public Method(String name, int offset) {
            super(name, offset);
        }

        public void visitMember(Visitor visitor) {
            visitor.handleMethod();
        }
	}

}