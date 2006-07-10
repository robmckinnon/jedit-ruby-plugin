/*
 * RubyCache.java - Cache of Ruby methods, classes, modules
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

import org.jedit.ruby.ast.*;
import org.jedit.ruby.parser.RubyParser;
import org.jedit.ruby.RubyPlugin;

import java.util.*;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class RubyCache {

    private static RubyCache instance;

    private final NameToMethods nameToMethods;
    private final NameToParents nameToParents;
    private final MethodToParents methodToParents;
    private final ParentToMethods parentToMethods;
    private final ParentToImmediateMethods parentToImmediateMethods;
    private final Map<String, RubyMembers> pathToMembers;

    public static synchronized void resetCache() {
        instance = new RubyCache();
    }

    public static synchronized RubyCache instance() {
        return instance;
    }

    private RubyCache() {
        nameToMethods = new NameToMethods();
        nameToParents = new NameToParents();
        methodToParents = new MethodToParents();
        parentToMethods = new ParentToMethods();
        parentToImmediateMethods = new ParentToImmediateMethods();
        pathToMembers = new HashMap<String, RubyMembers>();
        nameToParents.setParentToImmediateMethods(parentToImmediateMethods);
        parentToMethods.setNameToParents(nameToParents);
    }

    public final synchronized void addMembers(String text, String path) {
        RubyMembers members = RubyParser.getMembers(text, path, null, true);
        addMembers(members, path);
    }

    public final synchronized void addMembers(RubyMembers members, String path) {
        if (!members.containsErrors()) {
            add(members, path);
        }
    }

    public final synchronized void addClass(ClassMember parent, String path) {
        Member[] members = new Member[1];
        members[0] = parent;
        RubyMembers rubyMembers = new RubyMembers(members, new ArrayList<Problem>(), 0);
        RubyCache.instance().addMembers(rubyMembers, path);
    }

    public final synchronized ClassMember getClass(String className) {
        return nameToParents.getClass(className);
    }

    public final synchronized ParentMember getParentMember(String parentMemberName) {
        return nameToParents.getMember(parentMemberName);
    }

    public final synchronized List<Method> getMethods(String method) {
        return new ArrayList<Method>(nameToMethods.getMethods(method));
    }

    public final synchronized Set<Member> getMembersWithMethod(String method) {
        return new HashSet<Member>(methodToParents.getParentSet(method));
    }

    public final synchronized Set<Method> getMethodsOfMember(String memberName) {
        return new HashSet<Method>(parentToMethods.getMethodSet(memberName));
    }

    public final synchronized Set<Method> getAllMethods() {
        return parentToMethods.getAllMethods();
    }

    public final synchronized List<Member> getAllImmediateMembers() {
        return new ArrayList<Member>(nameToParents.getAllMembers());
    }

    public final synchronized List<Member> getMembersWithMethodAsList(String method) {
        return new ArrayList<Member>(methodToParents.getParentList(method));
    }

    public final synchronized List<Method> getMethodsOfMemberAsList(String memberName) {
        return new ArrayList<Method>(parentToMethods.getMethodList(memberName));
    }

    public final synchronized void populateSuperClassMethods() {
        Collection<ParentMember> allParents = nameToParents.getAllParents();

        for (ParentMember member : allParents) {
            member.accept(new MemberVisitorAdapter() {
                public void handleClass(ClassMember classMember) {
                    populateSuperClassMethods(classMember, classMember);
                }
            });
        }

        if (nameToParents.getMember("ActionController::Base") != null) {
            ClassMember appController = new ClassMember("ApplicationController");
            appController.setSuperClassName("ActionController::Base");
            appController.setEndOffset(0);
            appController.setDocumentationComment(
                    "<p>ApplicationController is the parent of all the controller classes in a Rails application." +
                    " By default in app/controllers there is a file called application.rb that contains an empty" +
                    " definition of class ApplicationController.</p>" +
                    "<p>This class is used to establish a context for the entire application." +
                    " Filters added to this controller will be run for all controllers in the application." +
                    " Likewise, all the methods added will be available for all controllers.</p>");

            addClass(appController, "ApplicationController");
            populateSuperClassMethods(appController, appController);
        }

        Set<Method> methods = getAllMethods();

        for (Method method : methods) {
            method.populateReturnTypes();
        }
    }

    private void add(RubyMembers members, String path) {
        parentToMethods.resetAllMethodsList();
        pathToMembers.put(path, members);
        members.visitMembers(new MemberVisitorAdapter() {
            public void handleModule(Module module) {
                parentToImmediateMethods.add(module);
                parentToMethods.add(module);
                nameToParents.add(module);
            }

            public void handleClass(ClassMember classMember) {
                parentToImmediateMethods.add(classMember);
                parentToMethods.add(classMember);
                nameToParents.add(classMember);
            }

            public void handleMethod(Method method) {
                methodToParents.add(method);
                nameToMethods.add(method);
            }
        });
    }

    private final static String[] ACTION_CONTROLLER_BASE_INCLUDES = new String[]{
            "ActionController::Filters",
            "ActionController::Layout",
            "ActionController::Flash",
            "ActionController::Benchmarking",
            "ActionController::Rescue",
            "ActionController::Dependencies",
            "ActionController::Pagination",
            "ActionController::Scaffolding",
            "ActionController::Helpers",
            "ActionController::Cookies",
            "ActionController::Caching",
            "ActionController::Components",
            "ActionController::Verification",
            "ActionController::Streaming",
            "ActionController::SessionManagement",
            "ActionController::Macros::AutoComplete",
            "ActionController::Macros::InPlaceEditing"
    };

    private final static String[] ACTIVE_RECORD_BASE_INCLUDES = new String[]{
            "ActiveRecord::Validations",
            "ActiveRecord::Locking",
            "ActiveRecord::Callbacks",
            "ActiveRecord::Observing",
            "ActiveRecord::Timestamp",
            "ActiveRecord::Associations",
            "ActiveRecord::Aggregations",
            "ActiveRecord::Transactions",
            "ActiveRecord::Reflection",
            "ActiveRecord::Acts::Tree",
            "ActiveRecord::Acts::List",
            "ActiveRecord::Acts::NestedSet"
    };

    private void populateSuperClassMethods(ClassMember member, ClassMember memberOrSuperclass) {
        if (memberOrSuperclass.hasSuperClassName()) {
            String superClassName = memberOrSuperclass.getSuperClassName();
            ClassMember superClass = nameToParents.getClass(superClassName);
            if (superClass != null) {
                addSuperClassMethods(member, superClass.getMethods());
                populateSuperClassMethods(member, superClass);
            }

            addIncludes(member, "ActiveRecord::Base", ACTIVE_RECORD_BASE_INCLUDES);
            addIncludes(member, "ActionController::Base", ACTION_CONTROLLER_BASE_INCLUDES);
        }
    }

    private void addIncludes(ClassMember member, String fullName, String[] includeModules) {
        if (member.getFullName().equals(fullName)) {
            for (String include : includeModules) {
                ClassMember classMethodClass = nameToParents.getClass(include + "::ClassMethods");
                if (classMethodClass != null && classMethodClass.getMethods().size() > 0) {
                    includeClassMethods(member, classMethodClass);
                } else {
                    classMethodClass = nameToParents.getClass(include);
                    if (classMethodClass != null) {
                        includeClassMethods(member, classMethodClass);
                    }
                }
            }
        }
    }

    private void includeClassMethods(ClassMember member, ClassMember classMethodClass) {
        Set<Method> methods = classMethodClass.getMethods();
        Set<Method> classMethods = new HashSet<Method>();

        for (Method classMethod : methods) {
            classMethod.setClassMethod(true);
            classMethods.add(classMethod);
        }
        addSuperClassMethods(member, classMethods);
    }

    private void addSuperClassMethods(ClassMember member, Set<Method> superClassMethods) {
        for (Method method : superClassMethods) {
            methodToParents.add(method, member);
        }
        superClassMethods = filterOutDuplicates(member, superClassMethods);
        parentToMethods.add(member, superClassMethods);
    }

    private Set<Method> filterOutDuplicates(ParentMember member, Set<Method> parentMethods) {
        Set<String> methodNames = getMethodNames(member);
        Iterator<Method> parentMethodIterator = parentMethods.iterator();

        while (parentMethodIterator.hasNext()) {
            Method parentMethod = parentMethodIterator.next();
            if (methodNames.contains(parentMethod.getName())) {
                parentMethodIterator.remove();
            }
        }
        return parentMethods;
    }

    private Set<String> getMethodNames(ParentMember member) {
        Set<Method> methods = parentToMethods.getMethodSet(member.getName());
        Set<String> methodNames = new HashSet<String>();
        for (Method method : methods) {
            methodNames.add(method.getName());
        }
        return methodNames;
    }

    public List<ParentMember> getParentsStartingWith(String partialClass, boolean ignoreCase) {
        List<ParentMember> members = new ArrayList<ParentMember>();
        List<String> names = nameToParents.getAllParentNames();

        String lowerCasePartial = null;
        if (ignoreCase && partialClass != null) {
            lowerCasePartial = partialClass.toLowerCase();
        }

        for (String name : names) {
            if (name.startsWith(partialClass)) {
                ParentMember member = nameToParents.getMember(name);
                members.add(member);
            } else if (ignoreCase && lowerCasePartial != null) {
                if (name.toLowerCase().startsWith(lowerCasePartial)) {
                    ParentMember member = nameToParents.getMember(name);
                    members.add(member);
                }
            }
        }

        return members;
    }
}
