/*
 * RubyParser.java - Parses ruby file
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

import gnu.regexp.REException;
import gnu.regexp.REMatch;
import gnu.regexp.RE;

import java.util.*;

import org.gjt.sp.jedit.search.RESearchMatcher;

/**
 * Parses ruby file
 * @author robmckinnon at users.sourceforge.net
 */
public class RubyParser {

    private static final Member[] EMPTY_MEMBER_ARRAY = new Member[0];

    public static interface Matcher {
        List<REMatch> getMatches(String text) throws REException;
    }

//    private static final Matcher moduleMatcher = new Matcher() {
//        public List<REMatch> getMatches(String text) throws REException {
//            return getMatchList("([ ]*)(module[ ]+)(\\w+.*)", text);
//        }
//    };

    private static final Matcher classMatcher = new Matcher() {
        public List<REMatch> getMatches(String text) throws REException {
            return getMatchList("([ ]*)(class[ ]+)(\\w+.*)", text);
        }
    };

    private static final Matcher methodMatcher = new Matcher() {
        public List<REMatch> getMatches(String text) throws REException {
            return getMatchList("([ ]*)(def[ ]+)(\\w+.*)", text);
        }
    };

    public static Member[] getMembers(String text) {
        Member[] members;

        try {
            List<Member> memberList = getMemberList(text);
            members = memberList.toArray(EMPTY_MEMBER_ARRAY);
            if(members.length > 0) {
                members = JRubyParser.getMembers(text, members);
            }
        } catch (REException e) {
            members = EMPTY_MEMBER_ARRAY;
            e.printStackTrace();
        }

        return members;
    }

    private static List<Member> getMemberList(String text) throws REException {
//        return getMemberSubList(text, moduleMatcher, classMatcher, 0);
        return getMemberSubList(text, classMatcher, methodMatcher, 0);
    }

    private static List<Member> getMemberSubList(String text, Matcher matcher, Matcher subMatcher, int indexAdjustment) throws REException {
        List<Member> memberList = new ArrayList<Member>();
        List<REMatch> memberMatches = matcher.getMatches(text);

        if (memberMatches.size() == 0) {
//            if (matcher == moduleMatcher) {
//                memberList = getMemberSubList(text, classMatcher, methodMatcher, 0);
//            } else {
                List<REMatch> matches = subMatcher.getMatches(text);
                List<Member> members = createMembers(matches, 3, false, indexAdjustment);
                memberList.addAll(members);
//            }
        } else {
            Iterator<REMatch> matches = memberMatches.iterator();
            REMatch memberMatch = matches.next();
            while (matches.hasNext()) {
                REMatch nextMemberMatch = matches.next();
                int end = nextMemberMatch.getStartIndex(3);
                memberList = addMembers(memberMatch, text, end, matcher, memberList, subMatcher, indexAdjustment);
                memberMatch = nextMemberMatch;
            }

            memberList = addMembers(memberMatch, text, text.length(), matcher, memberList, subMatcher, indexAdjustment);
        }

        return memberList;
    }

    private static List<Member> addMembers(REMatch memberMatch, String text, int end, Matcher matcher, List<Member> memberList, Matcher subMatcher, int indexAdjustment) throws REException {
        int start = memberMatch.getStartIndex(3);
        indexAdjustment += start;
        String subText = text.substring(start, end);

//        if (matcher == moduleMatcher) {
//            List<Member> subMembers = getMemberSubList(subText, classMatcher, methodMatcher, indexAdjustment);
//            memberList = addMemberAndSubMembers(memberMatch, start, memberList, subMembers);
//        } else {
            memberList = addSubMembers(memberMatch, indexAdjustment, subText, memberList, subMatcher);
//        }

        return memberList;
    }

    private static REMatch[] getMatches(String expression, String text) throws REException {
        RE re = new RE(expression, 0, RESearchMatcher.RE_SYNTAX_JEDIT);
        return re.getAllMatches(text);
    }

    private static List<REMatch> getMatchList(String pattern, String text) throws REException {
        REMatch[] matches = getMatches(pattern, text);
        List<REMatch> matchList = new ArrayList<REMatch>();

        for(REMatch match : matches) {
            String openingText = match.toString(1).trim();
            boolean isClass = openingText.length() == 0;

            if(isClass) {
                matchList.add(match);
            }
        }

        return matchList;
    }

    private static List<Member> sortMembers(List<Member> members) {
        Collections.sort(members, new Comparator<Member>() {
            public int compare(Member member, Member otherItem) {
                return member.getLowerCaseName().compareTo(otherItem.getLowerCaseName());
            }
        });
        return members;
    }

    private static List<Member> createMembers(List<REMatch> matchList, int matchIndex, boolean indent, int indexAdjustment) {
        List<Member> members = new ArrayList<Member>();

        for(REMatch match : matchList) {
            String name = match.toString(matchIndex);
            int index = indexAdjustment + match.getStartIndex(matchIndex);
            members.add(new Member(name, index));
        }

        sortMembers(members);
        return members;
    }

    private static List<Member> addSubMembers(REMatch memberMatch, int start, String text, List<Member> membersList, Matcher matcher) throws REException {
        List<REMatch> subMemberMatches = matcher.getMatches(text);
        List<Member> subMembers = createMembers(subMemberMatches, 3, true, start);
        return addMemberAndSubMembers(memberMatch, start, membersList, subMembers);
    }

    private static List<Member> addMemberAndSubMembers(REMatch memberMatch, int start, List<Member> membersList, List<Member> subMembers) {
        String memberName = memberMatch.toString(3);
        Member member = new Member(memberName, start);
        membersList.add(member);
        membersList.addAll(subMembers);
        return membersList;
    }

}
