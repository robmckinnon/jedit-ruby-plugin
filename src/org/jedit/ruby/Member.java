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
import java.util.Collections;

/**
 * Ruby file structure member
 * @author robmckinnon at users.sourceforge.net
 */
public class Member {

    private static final Member[] EMPTY_MEMBER_ARRAY = new Member[0];
    private String receiverName;
    private int parentCount;
    private List<Member> parentPath;

    public static class Root extends Member {
        public Root() {
            super("root", 0);
        }
    }

    private Member parentMember;
    private List<Member> childMembers;

    private String namespace;
    private String name;
    private int offset;

    public Member(String name) {
        this.name = name;
        parentCount = -1;
    }

    public Member(String name, int offset) {
        this(name);
        this.offset = offset;
    }

    public void visitMember(Member.Visitor visitor) {

    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setReceiver(String receiverName) {
        this.receiverName = receiverName;
        if(name.startsWith(receiverName)) {
            name = name.substring(name.indexOf('.') + 1);
        }
    }

    public String getDisplayName() {
        if(namespace == null) {
            if(receiverName == null) {
                return name;
            } else {
                return receiverName + '.' + name;
            }
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

    public boolean equals(Object obj) {
        if(obj instanceof Member) {
            Member member = ((Member) obj);
            return name.equals(member.name) && offset == member.offset;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return name.hashCode() + offset;
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

    public boolean hasParentMember() {
        return parentMember != null && !(parentMember instanceof Member.Root);
    }

    public Member getTopMostParent() {
        if(hasParentMember()) {
            return getTopMostParentOrSelf(getParentMember());
        } else {
            return null;
        }
    }

    private Member getTopMostParentOrSelf(Member member) {
        if (member.hasParentMember()) {
            return getTopMostParentOrSelf(member.getParentMember());
        } else {
            return member;
        }
    }

    /**
     * Returns list of member parent hierarchy
     * starting with top most parent, ending
     * with the member itself.
     *
     * If this member has no parent, the list only
     * contains this member.
     */ 
    public List<Member> getMemberPath() {
        if (parentPath == null) {
            List<Member> path = new ArrayList<Member>();
            Member current = this;
            path.add(current);

            while(current.hasParentMember()) {
                current = current.getParentMember();
                path.add(current);
            }

            Collections.reverse(path);
            parentPath = path;
        }

        return parentPath;
    }

    public int getParentCount() {
        if (parentCount == -1) {
            parentCount = getMemberPath().size() - 1;
        }

        return parentCount;
    }

    public Member getParentMember() {
        return parentMember;
    }

    public void setParentMember(Member parentMember) {
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