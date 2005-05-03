/*
 * RiParser.java - 
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

import org.jedit.ruby.ast.*;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.cache.RubyCache;
import org.gjt.sp.jedit.jEdit;

import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RiParser {

    public static void parseRdoc() {
        log("parsing RDoc from jar");
        List<JarEntry> entries = getEntries();
        for (JarEntry entry : entries) {
            loadClassDesciption(entry);
        }
        RubyCache.instance().instance().populateSuperclassMethods();
    }

    public static void loadClassDesciption(JarEntry entry) {
        String name = entry.getName();
        log(name);
        InputStream inputStream = RubyPlugin.class.getClassLoader().getResourceAsStream(name);
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(inputStream);
            ClassDescription result = (ClassDescription)input.readObject();
            cache(result);
        } catch (Exception e) {
            RubyPlugin.error(e, RiParser.class);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                RubyPlugin.error(e, RiParser.class);
            }
        }
    }

    public static List<JarEntry> getEntries() {
        List<JarEntry> entries = new ArrayList<JarEntry>();
        try {
            File file = getJarFile();
            log(file.getName());
            JarInputStream jar = new JarInputStream(new FileInputStream(file));
            JarEntry entry = jar.getNextJarEntry();
            while(entry != null) {
                if(!entry.isDirectory() && entry.getName().endsWith(".dat")) {
                    log(entry.getName());
                    entries.add(entry);
                }
                entry = jar.getNextJarEntry();
            }
        } catch (IOException e) {
            RubyPlugin.error(e, RiParser.class);
        }
        return entries;
    }

    public static File getJarFile() {
        File file = getJarFile(jEdit.getSettingsDirectory());
        if(!file.exists()) {
            file = getJarFile(jEdit.getJEditHome());
        }
        return file;
    }

    public static File getJarFile(String directory) {
        File dir = new File(directory, "jars");
        File file = new File(dir, "RubyPlugin.jar");
        return file;
    }

    public static void cache(ClassDescription description) {
        ClassMember parent = new ClassMember(description.getName(), 0, 0);
        parent.setParentMemberName(description.getSuperclass());
        parent.setEndOffset(0);
        parent.setNamespace(description.getNamespace());
        parent.setDocumentationComment(description.getComment());

        addMethods(description.getInstanceMethods(), parent);
        addMethods(description.getClassMethods(), parent);
        Member[] members = new Member[1];
        members[0] = parent;
        RubyMembers rubyMembers = new RubyMembers(members, new ArrayList<Problem>());
        RubyCache.instance().add(rubyMembers, "1.8/system");
    }

    public static void addMethods(List<MethodDescription> methods, ClassMember parent) {
        for (MethodDescription methodDescription : methods) {
            String name = methodDescription.getName();
            name = name.startsWith(".") ? name.substring(1) : name;
            Method method = new Method(name, "", "", 0, 0, methodDescription.isClassMethod());
            method.setNamespace(methodDescription.getNamespace());
            method.setDocumentationBlockParams(methodDescription.getBlockParameters());
            method.setDocumentationParams(methodDescription.getParameters());
            method.setDocumentationComment(methodDescription.getComment());
            method.setParentMember(null);
            method.setReceiver("");
            method.setEndOffset(0);
            parent.addChildMember(method);
        }
    }

    private static void log(String message) {
        RubyPlugin.log(message, RiParser.class);
    }

    public static void main(String[] args) {
        RiParser parser = new RiParser();
        parser.convertXmlToBinary();
    }

    private void convertXmlToBinary() {
        File directory = new File("/home/a/apps/versions/jedit/plugins/RubyPlugin/ri/java-xml");
        List<File> classDescriptions = findClassDescriptions(directory);
        for (File file : classDescriptions) {
            loadClassDescription(file);
        }
    }

    private void loadClassDescription(File file) {
        try {
            XMLDecoder d = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)));
            ClassDescription result = (ClassDescription)d.readObject();
            d.close();
            encode(result, file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void encode(ClassDescription result, File file) {
        String name = file.getName();
        int end = name.indexOf(".xml");
        name = name.substring(0, end) + ".dat";
        System.out.println(name);
        file = new File("/home/a/apps/versions/jedit/plugins/RubyPlugin/ri/java", name);

        try {
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file));
            output.writeObject(result);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void toXml(ClassDescription description) {
        try {
            XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream("/home/a/tmp/Test.xml")));
            e.writeObject(description);
            e.close();
            FileOutputStream fos = new FileOutputStream("/home/a/tmp/Test.txt");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(description);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<File> findClassDescriptions(File directory) {
        List<File> list = new ArrayList<File>();
        File[] entries = directory.listFiles();

        for (File entry : entries) {
            if(entry.isDirectory()) {
                list.addAll(findClassDescriptions(entry));
            } else if(entry.getName().endsWith("xml")) {
                list.add(entry);
            }
        }

        return list;
    }

    private static final String classes = "RI, RI::AliasName, RI::AnsiFormatter, RI::Attribute,\n" +
            "     RI::AttributeFormatter, RI::AttributeFormatter::AttrChar,\n" +
            "     RI::AttributeFormatter::AttributeString, RI::ClassDescription,\n" +
            "     RI::ClassEntry, RI::Constant, RI::Description, RI::HtmlFormatter,\n" +
            "     RI::IncludedModule, RI::MethodDescription, RI::MethodEntry,\n" +
            "     RI::MethodDescription, RI::ModuleDescription, RI::NamedThing,\n" +
            "     RI::Options, RI::Options::OptionList, RI::OverstrikeFormatter,\n" +
            "     RI::Paths, RI::RiCache, RI::RiReader, RI::RiWriter,\n" +
            "     RI::SimpleFormatter, RI::TextFormatter, RI::TopLevelEntry,";

    public static RubyMembers parse(String objectclass) {
        ClassDescription description = new ClassDescription();
        description.setAttributes(new ArrayList<Attribute>());
        List<MethodDescription> methods = new ArrayList<MethodDescription>();
        MethodDescription methodDescription = new MethodDescription();
        methodDescription.setName("new");
        methods.add(methodDescription);
        description.setClassMethods(methods);

        StringBuffer buffer = new StringBuffer();
        buffer.append("<tt>Object</tt> is the parent class of all classes in Ruby. Its methods are therefore available to all objects unless explicitly overridden.");
        buffer.append("<tt>Object</tt> mixes in the <tt>Kernel</tt> module, making the built-in kernel\n" +
                "      functions globally accessible. Although the instance methods of <tt>Object</tt>\n" +
                "      are defined by the <tt>Kernel</tt> module, we have chosen to document them here\n" +
                "      for clarity.");
        buffer.append("In the descriptions of Object's methods, the parameter <em>symbol</em> refers to\n" +
                "      a symbol, which is either a quoted string or a <tt>Symbol</tt> (such as\n" +
                "      <tt>:name</tt>).");

        description.setComment(buffer.toString());
        description.setConstants(new ArrayList<Constant>());
        description.setFullName("Object");
        description.setName("Object");
        description.setSuperclass("");
        ArrayList<IncludedModule> includes = new ArrayList<IncludedModule>();
        IncludedModule includedModule = new IncludedModule();
        includedModule.setName("Kernel");
        includes.add(includedModule);
        description.setIncludes(includes);

        ArrayList<MethodDescription> instanceMethods = new ArrayList<MethodDescription>();
        MethodDescription method = new MethodDescription();
        method.setName("==");
        method.setAliases(new ArrayList<String>());
        method.setBlockParameters("e1, e2");
        method.setParameters("     obj == other        => true or false\n" +
                "     obj.equal?(other)   => true or false\n" +
                "     obj.eql?(other)     => true or false");
        method.setComment(buffer.toString());
        method.setFullName("Object#==");
        method.setIsSingleton(true);
        method.setVisibility("public");
        method.setIsClassMethod(true);

        instanceMethods.add(method);
        method = new MethodDescription();
        method.setName("===");
        instanceMethods.add(method);

        description.setInstanceMethods(instanceMethods);
//        toXml(description);
        return null;
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
