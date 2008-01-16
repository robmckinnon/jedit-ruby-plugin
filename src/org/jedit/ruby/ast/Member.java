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

import java.util.*;

/**
 * Ruby file structure member
 *
 * @author robmckinnon at users.sourceforge.net
 */
public abstract class Member implements Comparable<Member> {

    protected static final String SELF = "self";

    private static final Member[] EMPTY_MEMBER_ARRAY = new Member[0];

    private List<Member> parentPath;
    private int parentCount;

    private String parentMemberName;
    private Member parentMember;
    private List<Member> childMembers;

    private String shortName;
    private String lowerCaseName;
    private String name;
    private String namespace;
    private String compositeNamespace;
    private String documentation;
    private int startOuterOffset;
    private int startOffset;
    private int endOffset;

    public Member(String name) {
        parentCount = -1;
        setName(name);
        boolean noParameters = name.indexOf("(") == -1;
        shortName = noParameters ? name : (new StringTokenizer(name, " (")).nextToken();
    }

    public int compareTo(Member member) {
        return getFullName().compareTo(member.getFullName());
    }

    public final void setStartOuterOffset(int outerOffset) {
        startOuterOffset = outerOffset;
    }

    public final void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public final void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public abstract void accept(MemberVisitor visitor);

    final void visitChildren(MemberVisitor visitor) {
        if (hasChildMembers()) {
            for (Member member : getChildMembersAsList()) {
                member.accept(visitor);
            }
        }
    }

    public final String getNamespace() {
        return namespace;
    }

    public final void setCompositeNamespace(String namespace) {
        compositeNamespace = namespace;
    }

    public final void setNamespace(String namespace) {
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

    public String getDisplayName() {
        return getFullName();
    }

    /**
     * Returns composite name for members
     * that have been defined in code
     * in the form
     * module Www::Xxx or class Yyy::Zzz.
     */
    public final String getCompositeName() {
        if (compositeNamespace == null) {
            return name;
        } else {
            return compositeNamespace + name;
        }
    }

    /**
     * Returns member name excluding
     * any namespace or receiver prefix.
     */
    public String getName() {
        return name;
    }

    void setName(String name) {
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

    protected void setShortName(String name) {
        shortName = name;
    }

    public final String getLowerCaseName() {
        if (lowerCaseName == null) {
            String lowerCase = name.toLowerCase();
            if (name.equals(lowerCase)) {
                lowerCaseName = name;
            } else {
                lowerCaseName = lowerCase;
            }
        }
        return lowerCaseName;
    }

    public final int getStartOuterOffset() {
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
                equal = !hasParentMember() || parentMember.equals(member.parentMember);
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

    public final String toString() {
        return getFullName();
    }

    public final boolean hasChildMembers() {
        return childMembers != null;
    }

    public final Member[] getChildMembers() {
        if (hasChildMembers()) {
            return childMembers.size() == 0 ? EMPTY_MEMBER_ARRAY : childMembers.toArray(new Member[childMembers.size()]);
        } else {
            return null;
        }
    }

    public final List<Member> getChildMembersAsList() {
        return childMembers;
    }

    public final void addChildMember(Member member) {
        if (childMembers == null) {
            childMembers = new ArrayList<Member>();
        }
        childMembers.add(member);
        member.setParentMember(this);
    }

    public final boolean hasParentMember() {
        return parentMember != null && !(parentMember instanceof Root);
    }

    public final Member getTopMostParent() {
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
    public final List<Member> getMemberPath() {
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

    public final int getParentCount() {
        if (parentCount == -1) {
            parentCount = getMemberPath().size() - 1;
        }

        return parentCount;
    }

    public final Member getParentMember() {
        return parentMember;
    }

    public final boolean hasParentMemberName() {
        return parentMemberName != null && parentMemberName.length() > 0;
    }

    public final String getParentMemberName() {
        return parentMemberName;
    }

    public final void setParentMemberName(String parentClass) {
        this.parentMemberName = parentClass;
    }

    public final void setParentMember(Member parentMember) {
        this.parentMember = parentMember;
    }

    public final void setDocumentationComment(String documentation) {
        int index = documentation.indexOf("|lt;");
        while (index != -1) {
            documentation = documentation.substring(0, index) + "&lt;" + documentation.substring(index+4);
            index = documentation.indexOf("|lt;");
        }
        this.documentation = documentation;
    }

    public String getDocumentation() {
        return documentation;
    }

}