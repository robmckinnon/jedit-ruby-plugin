/*
 * RubyMembers.java - Represents members in a Ruby file
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

import java.util.ArrayList;
import java.util.List;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class RubyMembers {

    private static final int ROOT = -1;
    private final Member[] members;

    private List<Member> memberList;
    private List<Problem> problems;
    private Root rootMember;

    public RubyMembers(Member[] memberArray, List<Problem> problems, int textLength) {
        members = memberArray;
        setProblems(problems);

        if (members != null) {
            memberList = new ArrayList<Member>();
            populateMemberList(memberArray, memberList);
            rootMember = new Root(textLength);
        }
    }

    public final void setProblems(List<Problem> problems) {
        this.problems = problems;
    }

    public final boolean containsErrors() {
        return members == null;
    }

    public final Problem[] getProblems() {
        return problems.toArray(new Problem[problems.size()]);
    }

    private void populateMemberList(Member[] members, List<Member> list) {
        for(Member member : members) {
            list.add(member);
            if(member.hasChildMembers()) {
                populateMemberList(member.getChildMembers(), list);
            }
        }
    }

    /**
     * @throws RuntimeException if {@link #containsErrors()} returns true
     * @return size
     */
    public final int size() {
        return members.length;
    }

    public final Member getNextMember(int caretPosition) {
        int index = getLastMemberIndexBefore(caretPosition);
        if (index == ROOT) {
            if(memberList.size() > 0) {
                return memberList.get(0);
            } else {
                return null;
            }
        } else if (index < memberList.size() - 1) {
            return memberList.get(index + 1);
        } else {
            return null;
        }
    }

    public final Member getPreviousMember(int caretPosition) {
        int index = getLastMemberIndexBefore(caretPosition);
        Member lastMember = memberList.get(index);
        if (index > 0) {
            Member member = memberList.get(index - 1);
            if (member.getEndOffset() < lastMember.getEndOffset() || caretPosition == lastMember.getStartOffset()) {
                return member;
            } else {
                return lastMember;
            }
        } else if(lastMember.getStartOffset() < caretPosition) {
            return lastMember;
        } else {
            return null;
        }
    }

    /**
     * Returns member at caret position, if caret position
     * is outside a Ruby member then the file's {@link Root}
     * member is returned.
     *
     * @param caretPosition caret position
     * @return {@link Member} at caret or file {@link Root} member
     */
    public final Member getMemberAt(int caretPosition) {
        int index = getMemberIndexAt(caretPosition);
        return index == ROOT ? rootMember : memberList.get(index);
    }

    public final Member getLastMemberBefore(int caretPosition) {
        int index = getLastMemberIndexBefore(caretPosition);
        return index == ROOT ? null : memberList.get(index);
    }

    private int getLastMemberIndexBefore(int caretPosition) {
        int lastIndex = memberList.size() - 1;
        int memberIndex = ROOT;

        for (int i = 0; memberIndex == ROOT && i < memberList.size(); i++) {
            Member member = memberList.get(i);
            int start = member.getStartOffset();

            if (caretPosition >= start) {
                if (i < lastIndex) {
                    Member nextMember = memberList.get(i + 1);
                    int nextOffset = nextMember.getStartOffset();
                    if (caretPosition < nextOffset) {
                        memberIndex = i;
                    }
                } else {
                    memberIndex = i;
                }
            }
        }

        return memberIndex;
    }

    private int getMemberIndexAt(int caretPosition) {
        int memberIndex = ROOT;

        for (int i = 0; i < memberList.size(); i++) {
            Member member = memberList.get(i);
            int offset = member.getStartOuterOffset();

            if (caretPosition >= offset && caretPosition <= member.getEndOffset()) {
                memberIndex = i;
            }
        }

        return memberIndex;
    }

    public final Member[] getMembers() {
        return members;
    }

    public final Member[] combineMembersAndProblems(int offsetLimit) {
        if (members != null && problems != null) {
            List<Member> accesibleMembers = new ArrayList<Member>();

            for (Member member : members) {
                if(member.getStartOffset() < offsetLimit) {
                    accesibleMembers.add(member);
                }
            }

            Member[] combined = new Member[accesibleMembers.size() + problems.size()];
            int index = 0;
            for (Member member : accesibleMembers) {
                combined[index++] = member;
            }
            for (Problem problem : problems) {
                combined[index++] = problem;
            }
            return combined;
        } else {
            throw new IllegalStateException("Can only call when members and problems are non-null arrays");
        }
    }

    public final Member get(int index) {
        return members[index];
    }

    public final List<Member> getClasses() {
        final List<Member> classes = new ArrayList<Member>();
        visitMembers(new MemberVisitorAdapter() {
            public void handleClass(org.jedit.ruby.ast.ClassMember classMember) {
                classes.add(classMember);
            }
        });
        return classes;
    }

    public final void visitMembers(MemberVisitor visitor) {
        for(Member member : memberList) {
            member.accept(visitor);
        }
    }

}