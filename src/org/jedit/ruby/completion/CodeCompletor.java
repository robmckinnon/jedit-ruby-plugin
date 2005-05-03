/*
 * CodeCompletor.java - 
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
package org.jedit.ruby.completion;

import java.util.*;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Method;
import org.jedit.ruby.completion.*;
import org.jedit.ruby.completion.CodeAnalyzer;
import org.jedit.ruby.cache.RubyCache;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class CodeCompletor {

    private JEditTextArea textArea;
    private Buffer buffer;
    private org.jedit.ruby.completion.CodeAnalyzer analyzer;
    private List<Method> methods;

    public CodeCompletor(View view) {
        textArea = view.getTextArea();
        buffer = view.getBuffer();
        analyzer = new org.jedit.ruby.completion.CodeAnalyzer(textArea, buffer);
        methods = findMethods();
    }

    public List<Method> getMethods() {
        return methods;
    }

    public String getRestOfLine() {
        return analyzer.getRestOfLine();
    }

    public String getPartialMethod() {
        return analyzer.getPartialMethod();
    }

    public boolean isInsertionPoint() {
        String partialMethod = getPartialMethod();
        if (partialMethod != null && partialMethod.length() > 2) {
            for (Method method : methods) {
                if (method.getShortName().equals(partialMethod)) {
                    return false;
                }
            }
        }
        return analyzer.isInsertionPoint();
    }

    private List<Method> findMethods() {
        if (analyzer.getName() != null) {
            String className = analyzer.getClassName();

            List<Method> methods;
            if (className != null) {
                methods = RubyCache.instance().getMethodsOfMember(className);
                if (analyzer.isClass()) {
                    for (Iterator<Method> iterator = methods.iterator(); iterator.hasNext();) {
                        Method method = iterator.next();
                        if (!method.isClassMethod()) {
                            iterator.remove();
                        }
                    }
                }
            } else {
                methods = completeUsingMethods(analyzer.getMethods());
            }

            if (getPartialMethod() != null) {
                for (Iterator<Method> iterator = methods.iterator(); iterator.hasNext();) {
                    Method method = iterator.next();

                    if (!method.getShortName().startsWith(getPartialMethod())) {
                        iterator.remove();
                    }
                }
            }
            return methods;
        } else {
            return new ArrayList<Method>();
        }
    }

    private List<Method> completeUsingMethods(List<String> methods) {
        List<Member> members = null;

        for (String method : methods) {
            List<Member> classes = RubyCache.instance().getMembersWithMethod(method);
            if (members != null) {
                intersection(members, classes);
            } else {
                members = classes;
            }
        }

        List<Method> results = new ArrayList<Method>();

        if (members != null) {
            for (Member member : members) {
                results.addAll(RubyCache.instance().getMethodsOfMember(member.getFullName()));
            }
        } else {
            results.addAll(RubyCache.instance().getAllMethods());
        }

        return results;
    }

    List<Member> intersection(List<Member> list, List<Member> otherList) {
        List<Member> intersection = new ArrayList<Member>();

        if (!list.isEmpty()) {
            intersection.addAll(list);
        }

        if (!intersection.isEmpty()) {
            intersection.retainAll(otherList);
        }

        return intersection;
    }

}