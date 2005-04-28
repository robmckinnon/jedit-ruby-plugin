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
package org.jedit.ruby.ast;

import javax.swing.text.Utilities;
import java.util.*;

/**
 * Ruby file structure member
 *
 * @author robmckinnon at users.sourceforge.net
 */
public abstract class Member implements Comparable<Member> {

    private static final Member[] EMPTY_MEMBER_ARRAY = new Member[0];

    private List<Member> parentPath;
    private int parentCount;

    private Member parentMember;
    private List<Member> childMembers;

    private String namespace;
    private String name;
    private String shortName;
    private String documentation;
    private int startOuterOffset;
    private int startOffset;
    private int endOffset;

    public Member(String name, int startOuterOffset, int startOffset) {
        parentCount = -1;
        this.startOffset = startOffset;
        this.startOuterOffset = startOuterOffset;
        setEndOffset(startOffset);
        setName(name);
        shortName = (new StringTokenizer(name, " (")).nextToken();
    }

    public int compareTo(Member member) {
        return getFullName().compareTo(member.getFullName());
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public abstract void accept(MemberVisitor visitor);

    public void visitChildren(MemberVisitor visitor) {
        if (hasChildMembers()) {
            for (Member member : getChildMembersAsList()) {
                member.accept(visitor);
            }
        }
    }

    protected String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Returns member name including any
     * namespace prefix.
     */
    public String getFullName() {
        if (namespace == null) {
            return name;
        } else {
            return namespace + name;
        }
    }

    /**
     * Returns member name excluding
     * any namespace or receiver prefix.
     */
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    /**
     * Returns member name excluding
     * any namespace and excluding
     * any parameter list or class
     * extended from.
     */
    public String getShortName() {
        return shortName;
    }

    public String getLowerCaseName() {
        return name;
    }

    public int getStartOuterOffset() {
        return startOuterOffset;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    /**
     * Returns true if supplied object
     * is a member with the same display name
     * and parent member as this member.
     */
    public boolean equals(Object obj) {
        boolean equal = false;
        if (obj instanceof Member) {
            Member member = ((Member)obj);
            boolean displayNamesEqual = getFullName().equals(member.getFullName());

            if (displayNamesEqual) {
                if (hasParentMember()) {
                    equal = parentMember.equals(member.getParentMember());
                } else {
                    equal = true;
                }
            }
        }
        return equal;
    }

    public int hashCode() {
        int code = getFullName().hashCode();
        if (hasParentMember()) {
            code += getParentMember().hashCode();
        }
        return code;
    }

    public String toString() {
        return getFullName();
    }

    public boolean hasChildMembers() {
        return childMembers != null;
    }

    public Member[] getChildMembers() {
        if (hasChildMembers()) {
            return childMembers.toArray(EMPTY_MEMBER_ARRAY);
        } else {
            return null;
        }
    }

    public List<Member> getChildMembersAsList() {
        return childMembers;
    }

    public void addChildMember(Member member) {
        if (childMembers == null) {
            childMembers = new ArrayList<Member>();
        }
        childMembers.add(member);
        member.setParentMember(this);
    }

    public boolean hasParentMember() {
        return parentMember != null && !(parentMember instanceof Root);
    }

    public Member getTopMostParent() {
        if (hasParentMember()) {
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
     * <p/>
     * If this member has no parent, the list only
     * contains this member.
     */
    public List<Member> getMemberPath() {
        if (parentPath == null) {
            List<Member> path = new ArrayList<Member>();
            Member current = this;
            path.add(current);

            while (current.hasParentMember()) {
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

    public void setDocumentation(String comment) {
        this.documentation = comment;
    }

    public String getDocumentation() {
        return documentation;
    }
}