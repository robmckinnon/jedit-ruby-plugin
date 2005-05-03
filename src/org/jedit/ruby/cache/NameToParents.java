/*
 * ParentToMethods.java - 
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
package org.jedit.ruby.cache;

import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.ParentMember;

import java.util.*;

/**
 * @author robmckinnon at users.sourceforge.net
 */
class NameToParents {

    private Map<String, Member> fullNameToMember = new HashMap<String, Member>();
    private Map<String, Member> nameToMember = new HashMap<String, Member>();
    private ParentToMethods parentToMethods;
    private List<Member> allMembers;

    void setParentToMethods(ParentToMethods parentToMethods) {
        this.parentToMethods = parentToMethods;
    }

    void add(ParentMember member) {
        String fullName = member.getFullName();
        String name = member.getName();
        fullNameToMember.put(fullName, member);
        nameToMember.put(name, member);
    }

    void clear() {
        fullNameToMember.clear();
        nameToMember.clear();
    }

    List<Member> getAllMembers() {
        if (allMembers == null) {
            allMembers = new ArrayList<Member>();

            for (String parentName : getAllParentNames()) {
                allMembers.add(fullNameToMember.get(parentName));
                allMembers.addAll(parentToMethods.getMethodList(parentName));
            }
        }

        return allMembers;
    }

    List<String> getAllParentNames() {
        List<String> names = new ArrayList<String>(fullNameToMember.keySet());
        Collections.sort(names);
        return names;
    }

    void reset() {
        allMembers = null;
    }
}
