/*
 * Method.java - 
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

import org.jedit.ruby.parser.LineCounter;
import org.jedit.ruby.cache.RubyCache;

import java.util.Set;
import java.util.HashSet;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class Method extends Member {

    private Set<Member> returnTypes;
    private final String filePath;
    private final String fileName;
    private String receiverName;
    private String parameters;
    private String blockParameters;
    private String fullDocumentation;
    private boolean isClassMethod;
    private boolean hasParameters;

    public Method(String name, String params, String filePath, String fileName, boolean classMethod) {
        super(params == null ? name : name + params);
        this.filePath = filePath;
        this.fileName = fileName;
        isClassMethod = classMethod;
        hasParameters = true;
    }

    public final void accept(MemberVisitor visitor) {
        visitor.handleMethod(this);
    }

    public final Set<Member> getReturnTypes() {
        return returnTypes;
    }

    /**
     * Returns member name including any
     * namespace or receiver prefix.
     */
    public final String getFullName() {
        if (getNamespace() == null) {
            if (receiverName == null) {
                return getName();
            } else {
                return receiverName + getMethodDelimiter() + getName();
            }
        } else {
            return getNamespace() + getMethodDelimiter() + getName();
        }
    }

    private String getMethodDelimiter() {
        if(isClassMethod()) {
            return "::";
        } else {
            return "#";
        }
    }

    public void setReceiverToSelf(String methodName) {
        setReceiver(Member.SELF, methodName);
    }

    public final void setReceiver(String receiverName, String methodName) {
        String shortName = getShortName().replace(receiverName, "").replace(".","").replace("::","");
        setShortName(shortName);
        if (methodName != null) {
            setName(methodName);
        }
        setClassMethod(true);
        this.receiverName = receiverName;
    }

    
    public final void setClassMethod(boolean classMethod) {
        isClassMethod = classMethod;
    }

    public final boolean isClassMethod() {
        return isClassMethod;
    }

    public final String getFilePath() {
        return filePath;
    }

    public final String getFileName() {
        return fileName;
    }

    public final int compareTo(Member member) {
        int comparison = super.compareTo(member);
        if(comparison == 0 && member instanceof Method) {
            org.jedit.ruby.ast.Method method = (Method)member;
            comparison = fileName.compareTo(method.fileName);
        }
        return comparison;
    }

    public boolean equals(Object obj) {
        boolean equal = obj instanceof Method && super.equals(obj);

        if (equal) {
            return isClassMethod() == ((Method)obj).isClassMethod();
        } else {
            return equal;
        }
    }

    public int hashCode() {
        return super.hashCode() + (isClassMethod() ? 1 : 0);
    }

    public final void setName(String name) {
        super.setName(name);
    }

    public final String getDocumentation() {
        if (fullDocumentation == null) {
            StringBuffer buffer = new StringBuffer();
            String parameters = getDocumentationParameters();
            if (parameters.length() == 0) {
                parameters = getDocumentationBlockParameters();
            }

            if (parameters.length() != 0) {
                buffer.append("<hr><pre class=\"param\">").append(parameters).append("</pre><hr><br>");
            }
            buffer.append(super.getDocumentation());
            fullDocumentation = buffer.toString();
        }

        return fullDocumentation;
    }

    public final boolean hasParameters() {
        return hasParameters;
    }

    public final void setDocumentationParams(String parameters) {
        parameters = parameters.trim();

        if (parameters.indexOf("(") == -1) {
            hasParameters = false;
            
        } else if (parameters.startsWith("(") && parameters.endsWith(")")) {
            if(parameters.length() == 2) {
                hasParameters = false;
            } else {
                String parameterList = parameters.substring(1, parameters.length() - 1).trim();
                hasParameters = parameterList.length() > 0;
            }
            parameters = getShortName() + parameters;

        } else if (parameters.indexOf(getShortName() + " ") != -1) {
            hasParameters = false;
        }

        this.parameters = parameters;
    }

    public final void populateReturnTypes() {
        if (parameters.indexOf("=>") != -1) {
            returnTypes = guessReturnTypes(parameters, "=>");

        } else if (parameters.indexOf("->") != -1) {
            returnTypes = guessReturnTypes(parameters, "->");
        }
    }

    private Set<Member> guessReturnTypes(String parameters, String pointer) {
        Set<Member> types = null;
        LineCounter lineCounter = new LineCounter(parameters);

        for (int i = 0; i < lineCounter.getLineCount(); i++) {
            String line = lineCounter.getLine(i);
            if (line.indexOf(pointer) != -1) {
                int start = line.indexOf(pointer) + 2;
                String value = line.substring(start).trim();

                if (value.length() > 0) {
                    if (types == null) {
                        types = new HashSet<Member>();
                    }
                    types = addTypes(value, types);
                    boolean cannotDetermineType = types == null;
                    if (cannotDetermineType) {
                        break;
                    }
                }
            }
        }

        return types;
    }

    private Set<Member> addTypes(String value, Set<Member> types) {
        Result result = new Result(value);

        if (result.canDetermineType()) {
            addDeterminedType(result, types);

        } else {
            if (result.has("result") &&
                    (is("modulo") || is("quo") || is("remainder"))) {
                addType("Fixnum", types);
                addType("Bignum", types);
                addType("Float", types);

                if (is("quo")) {
                    addType("Rational", types);
                }

            } else if(result.has("self")) {
                addType(getParentMemberName(), types);

            } else {
                types = null;
            }
        }

        return types;
    }

    private boolean is(String name) {
        return getShortName().equals(name);
    }

    private void addDeterminedType(Result result, Set<Member> types) {
        addType("Array", "array", result, types);
        addType("NilClass", "nil", result, types);
        addType("TrueClass", "bool", result, types);
        addType("FalseClass", "bool", result, types);
        addType("TrueClass", "true", result, types);
        addType("FalseClass", "false", result, types);
        addType("String", "str", result, types);
        addType("Numeric", "num", result, types);
        addType("Fixnum", "fixnum", result, types);
        addType("Fixnum", "int", result, types);
        addType("Fixnum", "1", result, types);
        addType("Fixnum", "0", result, types);
        addType("Fixnum", "old_seed", result, types);
        addType("Fixnum", "numeric", result, types);
        addType("Fixnum", "number", result, types);
        addType("Bignum", "numeric", result, types);
        addType("Bignum", "number", result, types);
        addType("Bignum", "bignum", result, types);
        addType("Float", "numeric", result, types);
        addType("Float", "number", result, types);
        addType("Float", "float", result, types);
        addType("Float", "flt", result, types);
        addType("Float", "0.0", result, types);
        addType("Dir", "dir", result, types);
        addType("Enumerable", "enum", result, types);
        addType("Exception", "exception", result, types);
        addType("Time", "time", result, types);
        addType("String", "file_ame", result, types);
        addType("String", "_name", result, types);
        addType("String", "path", result, types);
        addType("String", "$_", result, types);
        addType("File", "file", result, types);
        addType("File::Stat", "stat", result, types);
        addType("Symbol", "sym", result, types);
        addType("Hash", "hsh", result, types);
        addType("Hash", "hash", result, types);
        addType("IO", "io", result, types);
        addType("Proc", "proc", result, types);
        addType("Proc", "prc", result, types);
        addType("Binding", "binding", result, types);
        addType("UnboundMethod", "unbound_method", result, types);
        addType("Module", "mod", result, types);
        addType("Method", "method", result, types);
        addType("NameError", "name_error", result, types);
        addType("NoMethodError", "no_method_error", result, types);
        addType("Thread", "thread", result, types);
        addType("Struct::Tms", "StructTms", result, types);
        addType("Range", "rng", result, types);
        addType("Range", "range", result, types);
        addType("MatchData", "matchdata", result, types);
        addType("Regexp", "regexp", result, types);
        addType("Struct", "struct", result, types);
        addType("SystemCallError", "system_call_error_subclass", result, types);
        addType("Errno", "system_call_error_subclass", result, types);
        addType("SystemExit", "system_exit", result, types);
        addType("Thread", "thr", result, types);
        addType("ThreadGroup", "thgrp", result, types);

        addType("Array", "[", "]", result, types);
        addType("String", "\"", "\"", result, types);

        removeType("Dir", "dir_name", result, types);
        removeType("Method", "unbound_method", result, types);
        removeType("Fixnum", "0.0", result, types);
    }

    private static void removeType(String parentMemberName, String match, Result result, Set<Member> types) {
        if (result.has(match)) {
            types.remove(getParentMember(parentMemberName));
        }
    }

    private static void addType(String parentMemberName, String begin, String end, Result result, Set<Member> types) {
        if (result.wrapped(begin, end)) {
            addType(parentMemberName, types);
        }
    }

    private static void addType(String parentMemberName, String match, Result result, Set<Member> types) {
        if (result.has(match)) {
            addType(parentMemberName, types);
        }
    }

    private static void addType(String parentMemberName, Set<Member> types) {
        types.add(getParentMember(parentMemberName));
    }

    private static ParentMember getParentMember(String parentMemberName) {
        return RubyCache.instance().getParentMember(parentMemberName);
    }

    private static final class Result {
        private final String result;

        public Result(String result) {
            this.result = result.toLowerCase();
        }

        boolean has(String name) {
            return result.indexOf(name) != -1 && !wrapped("\"", "\"") && !wrapped("[","]");
        }

        boolean wrapped(String begin, String end) {
            return result.startsWith(begin) && result.endsWith(end);
        }

        public boolean canDetermineType() {
            return !(has("obj")
                    || has("class")
                    || has("klass")
                    || has("value")
                    || has("self")
                    || has("key")
                    || has("result"));
        }
    }


    public final void setDocumentationBlockParams(String blockParameters) {
        this.blockParameters = blockParameters;
    }

    private String getDocumentationBlockParameters() {
        return blockParameters;
    }

    private String getDocumentationParameters() {
        return parameters;
    }
}
