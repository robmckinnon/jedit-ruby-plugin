/*
 * MethodDescription.java -
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
public final class MethodDescription extends Description {

    private List<String> aliases;
    private String blockParameters;
    private String parameters;
    private String visibility;
    private String aliasFor;
    private boolean isClassMethod;
    private boolean isSingleton;

    public final List getAliases() {
        return aliases;
    }

    public final void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public final String getBlockParameters() {
        return blockParameters;
    }

    public final void setBlockParameters(String blockParameters) {
        this.blockParameters = blockParameters;
    }

    public final String getParameters() {
        return parameters;
    }

    public final void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public final String getVisibility() {
        return visibility;
    }

    public final void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public final String getAliasFor() {
        return aliasFor;
    }

    public final void setAliasFor(String aliasFor) {
        this.aliasFor = aliasFor;
    }

    public final boolean getIsClassMethod() {
        return isClassMethod;
    }

    public final boolean isClassMethod() {
        return isClassMethod;
    }

    public final void setIsClassMethod(boolean classMethod) {
        isClassMethod = classMethod;
    }

    public final boolean getIsSingleton() {
        return isSingleton;
    }

    public final boolean isSingleton() {
        return isSingleton;
    }

    public final void setIsSingleton(boolean singleton) {
        isSingleton = singleton;
    }
}
