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

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class Method extends Member {

    private String filePath;
    private String fileName;
    private String receiverName;
    private String parameters;
    private String blockParameters;
    private String fullDocumentation;
    private boolean isClassMethod;
    private boolean hasParameters;

    public Method(String name, String filePath, String fileName, int startOuterOffset, int startOffset, boolean classMethod) {
        super(name, startOuterOffset, startOffset);
        this.filePath = filePath;
        this.fileName = fileName;
        isClassMethod = classMethod;
        hasParameters = true;
    }

    public void accept(MemberVisitor visitor) {
        visitor.handleMethod(this);
    }

    /**
     * Returns member name including any
     * namespace or receiver prefix.
     */
    public String getFullName() {
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

    public String getMethodDelimiter() {
        if(isClassMethod()) {
            return "::";
        } else {
            return "#";
        }
    }

    public void setReceiver(String receiverName) {
        this.receiverName = receiverName;
        String name = getName();
        if (name.startsWith(receiverName)) {
            name = name.substring(name.indexOf('.') + 1);
        }
    }

    public void setClassMethod(boolean classMethod) {
        isClassMethod = classMethod;
    }

    public boolean isClassMethod() {
        return isClassMethod;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public int compareTo(Member member) {
        int comparison = super.compareTo(member);
        if(comparison == 0 && member instanceof Method) {
            org.jedit.ruby.ast.Method method = (Method)member;
            comparison = fileName.compareTo(method.fileName);
        }
        return comparison;
    }

    public void setName(String name) {
        super.setName(name);
    }

    private String format(String parameters) {
        parameters = parameters.trim();
        if (parameters.startsWith("(") && parameters.endsWith(")")) {
           parameters = getName() + parameters;
        }
        return parameters;
    }

    public String getDocumentation() {
        if (fullDocumentation == null) {
            StringBuffer buffer = new StringBuffer();
            String parameters = getDocumentationParameters();
            if (parameters.length() == 0) {
                parameters = getDocumentationBlockParameters();
            }

            if (parameters.length() != 0) {
                buffer.append("<hr><pre class=\"param\">"+ parameters + "</pre><hr><br>");
            }
            buffer.append(super.getDocumentation());
            fullDocumentation = buffer.toString();
        }

        return fullDocumentation;
    }

    public boolean hasParameters() {
        return hasParameters;
    }

    public void setDocumentationParams(String parameters) {
        if (parameters.indexOf("(") == -1) {
            hasParameters = false;
            
        } else if (parameters.startsWith("(") && parameters.endsWith(")")) {
            if(parameters.length() == 2) {
                hasParameters = false;
            } else {
                String parameterList = parameters.substring(1, parameters.length() - 1).trim();
                hasParameters = parameterList.length() > 0;
            }
           parameters = getName() + parameters;
        }

        this.parameters = format(parameters);
    }

    public void setDocumentationBlockParams(String blockParameters) {
        this.blockParameters = format(blockParameters);
    }

    public String getDocumentationBlockParameters() {
        return blockParameters;
    }

    public String getDocumentationParameters() {
        return parameters;
    }
}
