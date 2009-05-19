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
import org.jedit.ruby.utils.CommandUtils;
import org.jedit.ruby.cache.RubyCache;
import org.gjt.sp.jedit.jEdit;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class RiParser {

    public static void parseRdoc() {
        RubyCache.resetCache();
        copyOverRubyCode();
        log("parsing RDoc from jar");
        List<JarEntry> entries = getEntries();
        for (JarEntry entry : entries) {
            loadClassDesciption(entry);
        }
        RubyCache.instance().populateSuperClassMethods();
    }

    private static List<String> getRDocExcludePatterns() {
        List<String> excludePatterns = new ArrayList<String>();
        if (!jEdit.getBooleanProperty(RDocViewer.INCLUDE_RAILS, true)) {
            excludePatterns.add("rails_2_3_2/Action");
            excludePatterns.add("rails_2_3_2/Active");
            excludePatterns.add("rails_2_3_2/Hpricot");
        }
        if (!jEdit.getBooleanProperty(RDocViewer.INCLUDE_RAILS_2_0, false)) {
            excludePatterns.add("rails_2_0_2/Action");
            excludePatterns.add("rails_2_0_2/Active");
            excludePatterns.add("rails_2_0_2/Hpricot");
        }
        return excludePatterns;
    }

    private static void copyOverRubyCode() {
        try {
            copyOverFile("rdoc_to_java.rb");
            copyOverFile("cdesc.erb");
        } catch (Exception e) {
            e.printStackTrace();
            RubyPlugin.error(e, RiParser.class);
        }
    }

    private static void copyOverFile(String name) throws IOException, InterruptedException {
        InputStream inputStream = RubyPlugin.class.getClassLoader().getResourceAsStream("ri/" + name);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        File file = CommandUtils.getStoragePath(name);
        FileWriter writer = new FileWriter(file);
        String line;
        while((line = reader.readLine()) != null) {
            writer.write(line + '\n');
        }
        reader.close();
        writer.close();
        if(!CommandUtils.isWindows()) {
            CommandUtils.getOutput("chmod +x " + file.getPath(), false);
        }
    }

    private static void loadClassDesciption(JarEntry entry) {
        String name = entry.getName();
        InputStream inputStream = RubyPlugin.class.getClassLoader().getResourceAsStream(name);
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(inputStream);
            ClassDescription result = (ClassDescription)input.readObject();
            String path = name.substring(name.lastIndexOf("/") + 1);
            cache(result, path);
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

    private static List<JarEntry> getEntries() {
        List<JarEntry> entries = new ArrayList<JarEntry>();
        try {
            List<String> rdocExcludePatterns = getRDocExcludePatterns();
            JarInputStream jar = new JarInputStream(new FileInputStream(getJarFile()));
            JarEntry entry;
            while ((entry = jar.getNextJarEntry()) != null) {
                boolean includeEntry = !excludeEntry(rdocExcludePatterns, entry);
                if (includeEntry) {
                    entries.add(entry);
                }
            }
        } catch (IOException e) {
            RubyPlugin.error(e, RiParser.class);
        }
        return entries;
    }

    private static boolean excludeEntry(List<String> rdocExcludePatterns, JarEntry entry) {
        boolean exclude = entry.isDirectory() || !entry.getName().endsWith(".dat");

        if (!exclude) {
            for (String excludePattern : rdocExcludePatterns) {
                if (entry.getName().indexOf(excludePattern) != -1) {
                    exclude = true;
                    break;
                }
            }
        }
        return exclude;
    }

    private static File getJarFile() {
        File file = getJarFile(jEdit.getSettingsDirectory());
        if(!file.exists()) {
            file = getJarFile(jEdit.getJEditHome());
        }
        log(file.getName());
        return file;
    }

    private static File getJarFile(String directory) {
        File dir = new File(directory, "jars");
        return new File(dir, "RubyPlugin.jar");
    }

    private static void cache(ClassDescription description, String path) {
        ClassMember parent = new ClassMember(description.getName());
        parent.setSuperClassName(description.getSuperclass());
        parent.setEndOffset(0);
        String namespace = description.getNamespace();
        if(namespace != null && namespace.trim().length() > 0) {
            namespace += "::";
        }
        parent.setNamespace(namespace);
        parent.setDocumentationComment(description.getComment());

        addMethods(description.getInstanceMethods(), parent);
        addMethods(description.getClassMethods(), parent);
        RubyCache.instance().addClass(parent, path);
    }

    private static void addMethods(List<MethodDescription> methods, ClassMember parent) {
        for (MethodDescription methodDescription : methods) {
            String name = methodDescription.getName();
            name = name.startsWith(".") ? name.substring(1) : name;
            Method method = new Method(name, null, "", "", methodDescription.isClassMethod());
            method.setNamespace(methodDescription.getNamespace());
            method.setDocumentationBlockParams(methodDescription.getBlockParameters());
            method.setDocumentationParams(methodDescription.getParameters());
            method.setDocumentationComment(methodDescription.getComment());
            method.setParentMemberName(parent.getName());
            method.setParentMember(null);
            method.setReceiver("", null);
            method.setEndOffset(0);
            parent.addChildMember(method);
        }
    }

    private static void log(String message) {
        RubyPlugin.log(message, RiParser.class);
    }

    public static void parse() {
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
    }

}
