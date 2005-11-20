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
import org.jedit.ruby.ast.ClassMember;
import org.jedit.ruby.ast.MemberVisitorAdapter;

import java.util.*;

/**
 * Manages mapping of module or class name
 * to cached {@link ParentMember} object
 * that have that name.
 * 
 * @author robmckinnon at users.sourceforge.net
 */
final class NameToParents {

    private final Map<String, ParentMember> fullNameToMember = new HashMap<String, ParentMember>();
    private final Map<String, ParentMember> nameToMember = new HashMap<String, ParentMember>();
    private final ClassVisitor classVisitor = new ClassVisitor();

    private List<Member> allMembers;
    private ParentToImmediateMethods parentToImmediateMethods;

    final void add(ParentMember member) {
        String fullName = member.getFullName();
        String name = member.getName();
        fullNameToMember.put(fullName, member);
        nameToMember.put(name, member);
    }

    final ClassMember getClass(String name) {
        ParentMember member = getMember(name);

        if (member == null) {
            return null;
        } else {
            member.accept(classVisitor);
            return classVisitor.classMember;
        }
    }

    final ParentMember getMember(String name) {
        if (fullNameToMember.containsKey(name)) {
            return fullNameToMember.get(name);
        } else if(nameToMember.containsKey(name)) {
            return nameToMember.get(name);
        } else {
            return null;
        }
    }

    final void clear() {
        fullNameToMember.clear();
        nameToMember.clear();
    }

    final List<Member> getAllMembers() {
        if (allMembers == null) {
            allMembers = new ArrayList<Member>();

            for (String parentName : getAllParentNames()) {
                allMembers.add(fullNameToMember.get(parentName));
                allMembers.addAll(parentToImmediateMethods.getImmediateMethodList(parentName));
            }
        }

        return allMembers;
    }

    final List<String> getAllParentNames() {
        List<String> names = new ArrayList<String>(fullNameToMember.keySet());
        Collections.sort(names);
        return names;
    }

    final Collection<ParentMember> getAllParents() {
        return fullNameToMember.values();
    }

    final void reset() {
        allMembers = null;
    }

    public final void setParentToImmediateMethods(ParentToImmediateMethods parentToImmediateMethods) {
        this.parentToImmediateMethods = parentToImmediateMethods;
    }

    private static final class ClassVisitor extends MemberVisitorAdapter {
        ClassMember classMember = null;

        public final void handleClass(ClassMember clas) {
            classMember = clas;
        }
    }
}
