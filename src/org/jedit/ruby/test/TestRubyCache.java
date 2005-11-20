/*
 * TestRubyCache.java - 
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
package org.jedit.ruby.test;

import org.jedit.ruby.cache.RubyCache;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Method;

import java.util.List;

import junit.framework.TestCase;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class TestRubyCache extends TestCase {

    private static void addClassesToCache() {
        RubyCache.instance().addMembers(TestRubyParser.CLASS, "CLASS");
        RubyCache.instance().addMembers(TestRubyParser.ARR_CLASS, "ARR_CLASS");
    }

    private static void addModuleToCache() {
        RubyCache.instance().addMembers(TestRubyParser.DEF_IN_MODULE, "DEF_IN_MODULE");
    }

    public final void testGetMethods() {
        addClassesToCache();
        List<Method> methods = RubyCache.instance().getMethods("red");

        assertEquals("Assert file name correct", "CLASS", methods.get(0).getFileName());
        assertEquals("Assert file name correct", "red", methods.get(0).getName());
    }

    public final void testGetClassByMethod() {
        addClassesToCache();
        assertFindByMethodCorrect("red", 0, "Green", 1);
        assertFindByMethodCorrect("[]", 0, "Green", 1);
    }

    public final void testGetMethodByClass() {
        addClassesToCache();
        assertFindByClassCorrect("Green", 0, "[]", 2, "ARR_CLASS");
        assertFindByClassCorrect("Green", 1, "red", 2, "CLASS");
    }

    public final void testGetClassByCombo() {
        addClassesToCache();
        addModuleToCache();
        assertFindByMethodCorrect("red", 0, "Blue", 2);
        assertFindByMethodCorrect("red", 1, "Green", 2);
        assertFindByMethodCorrect("[]", 0, "Green", 1);
    }

    public final void testGetMethodByCombo() {
        addClassesToCache();
        addModuleToCache();
        assertFindByClassCorrect("Green", 0, "[]", 2, "ARR_CLASS");
        assertFindByClassCorrect("Green", 1, "red", 2, "CLASS");
        assertFindByClassCorrect("Blue", 0, "red", 1, "DEF_IN_MODULE");
    }

    private static void assertFindByMethodCorrect(String method, int index, String parentName, int parentCount) {
        List<Member> members = RubyCache.instance().getMembersWithMethodAsList(method);
        assertEquals("Assert parent match correct for: " + method, parentCount, members.size());
        assertEquals("Assert name correct", parentName, members.get(index).getName());
    }

    private static void assertFindByClassCorrect(String parentName, int index, String methodName, int methodCount, String filePath) {
        List<Method> members = RubyCache.instance().getMethodsOfMemberAsList(parentName);
        assertEquals("Assert child match correct", methodCount, members.size());
        assertEquals("Assert name correct", methodName, members.get(index).getName());
        assertEquals("Assert path correct", filePath, members.get(index).getFilePath());
    }

}
