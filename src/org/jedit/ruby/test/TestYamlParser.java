/*
 * TestYamlParser.java -
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

import junit.framework.TestCase;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.JarInputStream;
import java.util.jar.JarEntry;
import java.net.URL;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class TestYamlParser extends TestCase {

    public final void testClassParsing() {
        assertTrue(true);
    }

    private static final String objectClass = "--- !ruby/object:RI::ClassDescription \n" +
            "attributes: []\n" +
            "class_methods: \n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: new\n" +
            "comment: \n" +
            "  - !ruby/struct:SM::Flow::P \n" +
            "    body: \"<tt>Object</tt> is the parent class of all classes in Ruby. Its methods are\n" +
            "      therefore available to all objects unless explicitly overridden.\"\n" +
            "  - !ruby/struct:SM::Flow::P \n" +
            "    body: \"<tt>Object</tt> mixes in the <tt>Kernel</tt> module, making the built-in kernel\n" +
            "      functions globally accessible. Although the instance methods of <tt>Object</tt>\n" +
            "      are defined by the <tt>Kernel</tt> module, we have chosen to document them here\n" +
            "      for clarity.\"\n" +
            "  - !ruby/struct:SM::Flow::P \n" +
            "    body: \"In the descriptions of Object's methods, the parameter <em>symbol</em> refers to\n" +
            "      a symbol, which is either a quoted string or a <tt>Symbol</tt> (such as\n" +
            "      <tt>:name</tt>).\"\n" +
            "constants: []\n" +
            "full_name: Object\n" +
            "includes: \n" +
            "  - !ruby/object:RI::IncludedModule \n" +
            "    name: Kernel\n" +
            "instance_methods: \n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"==\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"===\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"=~\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"__id__\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"__send__\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: class\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: clone\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: display\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: dup\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"eql?\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"equal?\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: extend\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: freeze\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"frozen?\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: hash\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: id\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: initialize_copy\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: inspect\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: instance_eval\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"instance_of?\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: instance_variable_get\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: instance_variable_set\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: instance_variables\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"is_a?\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"kind_of?\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: method\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: methods\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"nil?\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: object_id\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: private_methods\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: protected_methods\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: public_methods\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: remove_instance_variable\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"respond_to?\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: send\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: singleton_method_added\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: singleton_method_removed\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: singleton_method_undefined\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: singleton_methods\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: taint\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: \"tainted?\"\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: to_a\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: to_s\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: type\n" +
            "  - !ruby/object:RI::MethodDescription \n" +
            "    name: untaint\n" +
            "name: Object\n" +
            "superclass: ";
}
