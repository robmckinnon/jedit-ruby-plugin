/*
 * FileStructurePopup.java - File structure popup
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

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.search.RESearchMatcher;
import org.gjt.sp.util.Log;
import gnu.regexp.RE;
import gnu.regexp.REMatch;
import gnu.regexp.REException;

import java.util.*;
import java.util.List;
import java.awt.*;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public class FileStructurePopup {

    private View view;
    private long start;

    public FileStructurePopup(View view) {
        this.view = view;
    }

    public void show() {
        try {
            log("showing file structure popup");
            view.showWaitCursor();
            showPopup(view);
        } catch(Exception e) {
        } finally {
            view.hideWaitCursor();
        }
    }

    private void showPopup(View view) throws REException {
        String text = view.getTextArea().getText();
        start = now();
        List<REMatch> classMatches = getClassMatchList(text);
//        boolean useSubmenus = classMatches.size() > 1;

        List<Member> memberList = new ArrayList<Member>();

        if (classMatches.size() == 0) {
            List<REMatch> matches = getMethodMatchList(text);
            List<Member> members = createMembers(0, matches, 3, false);
            memberList.addAll(members);
        } else {
            Iterator<REMatch> matches = classMatches.iterator();
            REMatch classMatch = matches.next();
            while (matches.hasNext()) {
                REMatch nextClassMatch = matches.next();
                int end = nextClassMatch.getStartIndex(3);

                memberList = addClassMembers(classMatch, end, view, memberList);
                classMatch = nextClassMatch;
            }

            memberList = addClassMembers(classMatch, text.length(), view, memberList);
        }

        int size = memberList.size();
        if(size > 0) {
            JEditTextArea textArea = view.getTextArea();
            textArea.scrollToCaret(false);
            Point location = new Point(textArea.getSize().width / 3, textArea.getSize().height / 5);
            Member[] members = memberList.toArray(new Member[size]);
            new TypeAheadPopup(view, members, location, "_(),");
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private void log(String msg) {
        Log.log(Log.DEBUG, this, msg + (now() - start));
        start = now();
    }

    private REMatch[] getMatches(String expression, String text) throws REException {
        RE re = new RE(expression, 0, RESearchMatcher.RE_SYNTAX_JEDIT);
        return re.getAllMatches(text);
    }

    private List<REMatch> getMatchList(String pattern, String text) throws REException {
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

    private List<REMatch> getClassMatchList(String text) throws REException {
        return getMatchList("([ ]*)(class[ ]+)(\\w+.*)", text);
    }

    private List<REMatch> getMethodMatchList(String text) throws REException {
        return getMatchList("([ ]*)(def[ ]+)(\\w+.*)", text);
    }

    private List<Member> sortMembers(List<Member> members) {
        Collections.sort(members, new Comparator<Member>() {
            public int compare(Member member, Member otherItem) {
                return member.getLowerCaseName().compareTo(otherItem.getLowerCaseName());
            }
        });
        return members;
    }

    private List<Member> createMembers(int indexAdjustment, List<REMatch> matchList, int matchIndex, boolean indent) {
        List<Member> members = new ArrayList<Member>();

        for(REMatch match : matchList) {
            String name = match.toString(matchIndex);
            int index = indexAdjustment + match.getStartIndex(matchIndex);
            members.add(new Member(name, index, indent));
        }

        sortMembers(members);
        return members;
    }

    private List<Member> addClassMembers(REMatch classMatch, int end, View view, List<Member> membersList) throws REException {
        String className = classMatch.toString(3);
        int start = classMatch.getStartIndex(3);
        String text = view.getTextArea().getText(start, end - start);
        List<REMatch> methodMatchList = getMethodMatchList(text);

        Member classMember = new Member(className, start, false);
        List<Member> methodMembers = createMembers(start, methodMatchList, 3, true);
        membersList.add(classMember);
        membersList.addAll(methodMembers);
        return membersList;
    }

    public static class Member {
        String displayName;
        String name;
        int offset;

        public Member(String name, int offset, boolean indent) {
            this.offset = offset;
            if(indent) {
                displayName = "    " + name;
            } else {
                displayName = name;
            }
            this.name = name.toLowerCase();
        }

        public String getDisplayName() {
            return displayName;
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
    }


}
