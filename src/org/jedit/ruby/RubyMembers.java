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
package org.jedit.ruby;

import java.util.ArrayList;
import java.util.List;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RubyMembers {

    private Member[] members;
    private List<Member> memberList;

    public RubyMembers(Member[] members) {
        this.members = members;
        memberList = new ArrayList<Member>();
        populateMemberList(members, memberList);
    }

    private void populateMemberList(Member[] members, List<Member> list) {
        for(Member member : members) {
            list.add(member);
            if(member.hasChildMembers()) {
                populateMemberList(member.getChildMembers(), list);
            }
        }
    }

    public int size() {
        return members.length;
    }

    public Member getNextMember(int caretPosition) {
        int index = getCurrentMemberIndex(caretPosition);
        if (index == -1) {
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

    public Member getPreviousMember(int caretPosition) {
        int index = getCurrentMemberIndex(caretPosition);
        if (index > 0) {
            return memberList.get(index - 1);
        } else {
            return null;
        }
    }

    public Member getCurrentMember(int caretPosition) {
        int index = getCurrentMemberIndex(caretPosition);
        if (index == -1) {
            return null;
        } else {
            return memberList.get(index);
        }
    }

    private int getCurrentMemberIndex(int caretPosition) {
        int memberIndex = -1;

        for (int i = 0; memberIndex == -1 && i < memberList.size(); i++) {
            Member member = memberList.get(i);
            int offset = member.getStartOffset();

            if(caretPosition >= offset) {
                if(i < memberList.size() - 1) {
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

    public Member[] getMembers() {
        return members;
    }

    public Member get(int index) {
        return members[index];
    }

    public List<Member> getClasses() {
        final List<Member> classes = new ArrayList<Member>();
        visitMembers(new Member.VisitorAdapter() {
            public void handleClass(Member.Class classMember) {
                classes.add(classMember);
            }
        });
        return classes;
    }

    public void visitMembers(Member.Visitor visitor) {
        for(Member member : memberList) {
            member.accept(visitor);
        }
    }
}