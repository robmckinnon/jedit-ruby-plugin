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

import org.jedit.ruby.ast.*;
import org.jedit.ruby.cache.RubyCache;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.utils.EditorView;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class CodeCompletor {

    private static final ArrayList EMPTY_LIST = new ArrayList();
    private static final MethodFinderVisitor METHOD_FINDER = new MethodFinderVisitor();
    private static final Set<Method> kernelMethods = getMethodsOfParentMember("Kernel", false, true);
    private static final Set<Method> moduleMethods = getMethodsOfParentMember("Module", false, true);
    private static final Set<String> kernelMethodsNames = new HashSet<String>();
    private static final Set<String> moduleMethodsNames = new HashSet<String>();

    static {
        for (Method method : kernelMethods) {
            kernelMethodsNames.add(method.getShortName());
        }
        for (Method method : moduleMethods) {
            moduleMethodsNames.add(method.getShortName());
        }
    }

    private final List<ParentMember> classesAndModules;
    private final List<Method> methods;
    private final List<KeywordMember> keywords;
    private final CodeAnalyzer analyzer;
    private final EditorView view;

    private boolean foundMethodsFromPosition;

    public CodeCompletor(EditorView editorView) {
        RubyPlugin.log("completing", getClass());
        foundMethodsFromPosition = false;
        view = editorView;
        analyzer = new CodeAnalyzer(editorView);
        methods = findMethods();
        keywords = findKeywords(foundMethodsFromPosition);

//        if (isClassComplete()) {
            classesAndModules = findClassesAndModules();
//        } else {
//            classesAndModules = null;
//        }
    }

    private List<KeywordMember> findKeywords(boolean foundMethodsFromPosition) {
        List<KeywordMember> keywords;

        if (foundMethodsFromPosition) {
            keywords = getMatchingKeywords();
            keywords.addAll(getMatchesFromFile());
            Collections.sort(keywords, new KeywordComparator());
        } else {
            keywords = null;
        }

        return keywords;
    }

    private List<KeywordMember> getMatchingKeywords() {
        List<KeywordMember> keywords;
        keywords = new ArrayList<KeywordMember>();
        String partialName = getPartialMethod() != null ? getPartialMethod() : getPartialClass();
        for (String keyword : view.getKeywords()) {
            if (partialName == null || keyword.startsWith(partialName)) {
                if (!kernelMethodsNames.contains(keyword) && !moduleMethodsNames.contains(keyword)) {
                    keywords.add(new KeywordMember(keyword));
                }
            }
        }
        return keywords;
    }

    private List<KeywordMember> getMatchesFromFile() {
        String partialName = getPartialMethod() != null ? getPartialMethod() : getPartialClass();
        List<KeywordMember> words = new ArrayList<KeywordMember>();
        if (partialName != null && partialName.length() > 0) {
            for (String word : view.getWords(partialName)) {
                if (!kernelMethodsNames.contains(word) && !moduleMethodsNames.contains(word)) {
                    words.add(new KeywordMember(word));
                }
            }
        }
        return words;
    }

    private String getPartialMethod() {
        return analyzer.getPartialMethod();
    }

    private String getPartialClass() {
        return analyzer.getPartialClass();
    }

    public final boolean isDotInsertionPoint() {
        return analyzer.isDotInsertionPoint();
    }

    public boolean hasCompletion() {
        boolean haveMethods = methods.size() > 0 && !analyzer.isLastCompleted();
        return haveMethods || (classesAndModules != null && classesAndModules.size() > 0) || (keywords != null && keywords.size() > 0);
    }

    public RubyCompletion getDotCompletion() {
        List members = methods;
        if (classesAndModules != null && classesAndModules.size() > 0) {
            members = new ArrayList(members);
            members.addAll(classesAndModules);
        }

        if (members.size() == 0 && analyzer.getClassMethodCalledFrom() == null) {
            members.addAll(getMatchesFromFile());
            members.addAll(convertToList(filterMethods(RubyCache.instance().getAllMethods())));
            members.removeAll(getMatchingKeywords());
        }

        return new RubyCompletion(view, getPartialClassIgnoreCase(), getPartialMethod(), members);
    }

    public RubyCompletion getEmptyCompletion() {
        return new RubyCompletion(view, getPartialClass(), getPartialMethod(), EMPTY_LIST);
    }

    public RubyCompletion getCompletion() {
        Collection<? extends Member> members;

        if (methods.size() == 0 && classesAndModules != null) {
            members = classesAndModules;
        } else {
            members = methods;
        }

        if (keywords != null) {
            Collection<? extends Member> temp = members;
            members = keywords;
            members.addAll((Collection)temp);
        }

        return new RubyCompletion(view, getPartialClassIgnoreCase(), getPartialMethod(), (List<? extends Member>)members);
    }

    private List<ParentMember> findClassesAndModules() {
        RubyCache cache = RubyCache.instance();
        String partialName = getPartialClassIgnoreCase();

        if (partialName != null) {
            List<ParentMember> members = cache.getParentsStartingWith(partialName, true);

            if (members.size() > 0) {
                ParentCompletionComparator.instance.setPartialName(getPartialClass());
                Collections.sort(members, ParentCompletionComparator.instance);
                return members;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private String getPartialClassIgnoreCase() {
        String partialName = getPartialClass();

        if (partialName == null && analyzer.getClassMethodCalledFrom() != null) {
            partialName = analyzer.getClassMethodCalledFrom()+"::"+getPartialMethod();
        }
        return partialName;
    }

    private List<Method> findMethods() {
        Set<Method> methods;

        if (CodeAnalyzer.hasLastReturnTypes()) {
            methods = getMethodsOfParents(CodeAnalyzer.getLastReturnTypes());
            methods = filterMethods(methods);
        } else {
            String methodCalledOnThis = analyzer.getMethodCalledOnThis();
            boolean demarkerChar = CodeAnalyzer.isDemarkerChar(methodCalledOnThis);

            if (methodCalledOnThis != null) {
                if (!demarkerChar) {
                    if (methodCalledOnThis.indexOf('.') == -1 || CodeAnalyzer.isFloat(methodCalledOnThis)) {
                        methods = findMethodsFromCallee();
                    } else {
                        methods = filterMethods(RubyCache.instance().getAllMethods());
                    }
                } else {
                    methods = new HashSet<Method>();
                }

            } else if (getPartialClass() == null) {
                methods = findMethodsFromPosition();

            } else {
                methods = new HashSet<Method>();
            }
        }

        return convertToList(methods);
    }

    private List<Method> convertToList(Set<Method> methods) {
        List<Method> methodList = new ArrayList<Method>(methods);

        if (methods.size() > 0) {
            MethodCompletionComparator.instance.setObjectMethodsLast(methods.size() > 8);
            Collections.sort(methodList, MethodCompletionComparator.instance);
        }
        return methodList;
    }

    private Set<Method> findMethodsFromPosition() {
        Member member = view.getMemberAtCaretPosition();
        Set<Method> methods = null;

        if (member != null) {
            methods = METHOD_FINDER.getMethods(member, this);
        }

        if (methods == null) {
            methods = getKernelMethods();
        }

        foundMethodsFromPosition = true;

        return methods;
    }

    private Set<Method> getKernelMethods() {
        return filterMethods(new HashSet<Method>(kernelMethods));
    }

    private Set<Method> getModuleMethods() {
        return filterMethods(new HashSet<Method>(moduleMethods));
    }

    public static void setLastCompleted(String partialName, Member member) {
        MethodCompletionComparator.instance.addLastCompleted(member);
        ParentCompletionComparator.instance.addLastCompleted(partialName, member);
        CodeAnalyzer.setLastCompleted(member.getName());
    }

    private Set<Method> findMethods(String parentName, boolean removeInstanceMethods) {
        return findMethods(parentName, removeInstanceMethods, false);
    }

    private Set<Method> findMethods(String parentName, boolean removeInstanceMethods, boolean removeCommonClassMethods) {
        Set<Method> methods = getMethodsOfParentMember(parentName, removeInstanceMethods, removeCommonClassMethods);
        return filterMethods(methods);
    }

    private Set<Method> findMethodsFromCallee() {
        Set<Method> methods;
        String className = analyzer.getClassMethodCalledFrom();
        if (className != null) {
            methods = getMethodsOfParentMember(className, analyzer.isClass(), false);
        } else {
            methods = completeUsingMethods(analyzer.getMethodsCalledOnVariable());
        }

        return filterMethods(methods);
    }

    private Set<Method> filterMethods(Set<Method> methods) {
        String partialMethod = getPartialMethod();
        if (partialMethod != null) {
            for (Iterator<Method> iterator = methods.iterator(); iterator.hasNext();) {
                Method method = iterator.next();

                if (!method.getShortName().startsWith(partialMethod)) {
                    iterator.remove();
                }
            }
        }

        return methods;
    }

    private static Set<Method> getMethodsOfParentMember(String parentMember, boolean removeInstanceMethods, boolean removeCommonClassMethods) {
        RubyPlugin.log("parent: " + parentMember, CodeCompletor.class);
        Set<Method> methods = RubyCache.instance().getMethodsOfMember(parentMember);
        RubyPlugin.log("methods: " + methods.size(), CodeCompletor.class);

        for (Iterator<Method> iterator = methods.iterator(); iterator.hasNext();) {
            Method method = iterator.next();

            if (method.isClassMethod()) {
                if (removeCommonClassMethods) {
                    String name = method.getName();
                    if (name.equals("new") || name.equals("[]")) {
                        iterator.remove();
                    }
                }
            } else if (removeInstanceMethods) {
                iterator.remove();
            }
        }

        return methods;
    }

    private static Set<Method> completeUsingMethods(List<String> methods) {
        Set<Member> members = null;

        for (String method : methods) {
            Set<Member> classes = RubyCache.instance().getMembersWithMethod(method);
            if (members != null) {
                members = intersection(members, classes);
            } else {
                members = classes;
            }
        }

        return getMethodsOfParents(members);
    }

    private static Set<Method> getMethodsOfParents(Set<Member> members) {
        Set<Method> results = new HashSet<Method>();

        if (members != null) {
            for (Member member : members) {
                results.addAll(RubyCache.instance().getMethodsOfMemberAsList(member.getFullName()));
            }
        } else {
            results.addAll(RubyCache.instance().getAllMethods());
        }

        return results;
    }

    private static Set<Member> intersection(Set<Member> list, Set<Member> otherList) {
        Set<Member> intersection = new HashSet<Member>();

        if (!list.isEmpty()) {
            intersection.addAll(list);
        }

        if (!intersection.isEmpty()) {
            intersection.retainAll(otherList);
        }

        return intersection;
    }

    private static final class MethodFinderVisitor extends MemberVisitorAdapter {
        private Set<Method> methods;
        private CodeCompletor completor;

        public Set<Method> getMethods(Member member, CodeCompletor completor) {
            this.completor = completor;
            member.accept(this);
            return methods;
        }

        public final void handleModule(Module module) {
            methods = completor.findMethods(module.getFullName(), true);
            methods.addAll(completor.getModuleMethods());
        }

        public final void handleClass(ClassMember classMember) {
            methods = completor.findMethods(classMember.getFullName(), true);
            String superClass = classMember.getSuperClassName();

            if (superClass != null) {
                methods.addAll(completor.findMethods(superClass, true));
            }

            methods.addAll(completor.getModuleMethods());
        }

        public final void handleMethod(Method method) {
            if (method.hasParentMember()) {
                method.getParentMember().accept(this);
            } else {
                methods = completor.findMethods(method.getParentMemberName(), false);
            }
            methods.addAll(completor.getKernelMethods());
        }

        public final void handleRoot(Root root) {
            methods = completor.getKernelMethods();
        }
    }

    private static final class ParentCompletionComparator implements Comparator<ParentMember> {
        private static final ParentCompletionComparator instance = new ParentCompletionComparator();
        private final Map<String, Member> partialNameToMember = new HashMap<String, Member>();
        private String partialName;

        public final int compare(ParentMember parent, ParentMember otherParent) {
            Member member = partialNameToMember.get(partialName);
            if (member != null) {
                boolean isLastCompleted = parent.equals(member);
                boolean isOtherLastCompleted = otherParent.equals(member);

                if (isLastCompleted && !isOtherLastCompleted) {
                    return -1;
                } else if (!isLastCompleted && isOtherLastCompleted) {
                    return 1;
                } else {
                    return nameCompare(parent, otherParent);
                }
            } else {
                return nameCompare(parent, otherParent);
            }
        }

        private int nameCompare(ParentMember parent, ParentMember otherParent) {
            return parent.getFullName().compareTo(otherParent.getFullName());
        }

        public void addLastCompleted(String partialName, Member member) {
            if (partialName != null) {
                while (partialName.length() > 0) {
                    partialNameToMember.put(partialName, member);
                    partialName = partialName.substring(0, partialName.length() - 1);
                }
            }
        }

        public void setPartialName(String partialName) {
            this.partialName = partialName;
        }
    }

    private static final class MethodCompletionComparator extends MemberVisitorAdapter implements Comparator<Method> {
        private static final MethodCompletionComparator instance = new MethodCompletionComparator();

        private final Map<String, Method> classToLastCompletedMethod = new HashMap<String, Method>();
        private boolean objectMethodsLast;

        public final int compare(Method method, Method otherMethod) {
            boolean onObjectClass = onObjectClass(method);
            boolean otherOnObjectClass = onObjectClass(otherMethod);

            if (objectMethodsLast && onObjectClass && !otherOnObjectClass) {
                return 1;
            } else if (objectMethodsLast && !onObjectClass && otherOnObjectClass) {
                return -1;
            } else {
                boolean isLastCompleted = isLastCompleted(method);
                boolean isOtherLastCompleted = isLastCompleted(otherMethod);

                if (isLastCompleted && !isOtherLastCompleted) {
                    return -1;
                } else if (!isLastCompleted && isOtherLastCompleted) {
                    return 1;
                } else {
                    int compare = method.getName().compareTo(otherMethod.getName());

                    if (compare == 0) {
                        return method.getFullName().compareTo(otherMethod.getFullName());
                    } else {
                        return compare;
                    }
                }
            }
        }

        private boolean isLastCompleted(Method method) {
            if (method.hasParentMemberName()) {
                Method last = classToLastCompletedMethod.get(method.getParentMemberName());
                return method.equals(last);
            } else {
                return false;
            }
        }

        private boolean onObjectClass(Method method) {
            return method.getParentMember().getName().equals("Object");
        }

        public final void setObjectMethodsLast(boolean objectMethodsLast) {
            this.objectMethodsLast = objectMethodsLast;
        }

        public void handleMethod(Method method) {
            RubyPlugin.log("add last completed: " + String.valueOf(method), getClass());
            if (method != null && method.hasParentMemberName()) {
                RubyPlugin.log("add last completed: " + method.getParentMemberName() + "." + method.getName(), getClass());
                classToLastCompletedMethod.put(method.getParentMemberName(), method);
            }
        }

        public void addLastCompleted(Member member) {
            member.accept(this);
        }
    }

    private static class KeywordComparator implements Comparator<KeywordMember> {
        public int compare(KeywordMember keyword, KeywordMember other) {
            boolean global = keyword.startsWith("$") || keyword.startsWith("_");
            boolean otherGlobal = other.startsWith("$") || other.startsWith("_");

            if (global && !otherGlobal) {
                return 1;
            } else if(!global && otherGlobal) {
                return -1;
            } else {
                return keyword.getLowerCaseName().compareTo(other.getLowerCaseName());
            }
        }
    }
}