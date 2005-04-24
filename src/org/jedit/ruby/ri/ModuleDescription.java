/*
 * ModuleDescription.java - 
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
package org.jedit.ruby.ri;

import java.util.List;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class ModuleDescription extends Description {

    private List<Attribute> attributes;
    private List<Constant> constants;
    private List<IncludedModule> includes;
    private List<MethodDescription> classMethods;
    private List<MethodDescription> instanceMethods;
    
    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public List<Constant> getConstants() {
        return constants;
    }

    public void setConstants(List<Constant> constants) {
        this.constants = constants;
    }

    public List<IncludedModule> getIncludes() {
        return includes;
    }

    public void setIncludes(List<IncludedModule> includes) {
        this.includes = includes;
    }

    public List<MethodDescription> getClassMethods() {
        return classMethods;
    }

    public void setClassMethods(List<MethodDescription> classMethods) {
        this.classMethods = classMethods;
    }

    public List<MethodDescription> getInstanceMethods() {
        return instanceMethods;
    }

    public void setInstanceMethods(List<MethodDescription> instanceMethods) {
        this.instanceMethods = instanceMethods;
    }
}
