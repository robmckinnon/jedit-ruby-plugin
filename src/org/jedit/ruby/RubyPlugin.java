/*
 * RubyPlugin.java - Ruby plugin for jEdit
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
package org.jedit.ruby;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;
import org.jedit.ruby.parser.JRubyParser;
import org.jedit.ruby.ri.ClassDescription;
import org.jedit.ruby.ri.MethodDescription;
import org.jedit.ruby.ast.*;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.io.*;
import java.awt.Point;
import java.util.List;
import java.util.ArrayList;
import java.util.jar.JarInputStream;
import java.util.jar.JarEntry;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public class RubyPlugin extends EditPlugin {

    private static boolean debug = System.getProperty("user.home").equals("/home/a");
    private static final String RUBY_DIR = "ruby";

    public void stop() {
        super.stop();
    }

    public void start() {
        super.start();
        JRubyParser.setExpectedLabel(jEdit.getProperty("ruby.syntax-error.expected.label"));
        JRubyParser.setFoundLabel(jEdit.getProperty("ruby.syntax-error.found.label"));
        JRubyParser.setNothingLabel(jEdit.getProperty("ruby.syntax-error.nothing.label"));
        parseRdoc();
    }

    private void parseRdoc() {
        log("parsing RDoc from jar");
        List<JarEntry> entries = getEntries();
        for (JarEntry entry : entries) {
            loadClassDesciption(entry);
        }
    }

    private void loadClassDesciption(JarEntry entry) {
        String name = entry.getName();
        log(name);
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(name);
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(inputStream);
            ClassDescription result = (ClassDescription)input.readObject();
            cache(result);
        } catch (Exception e) {
            error(e, getClass());
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                error(e, getClass());
            }
        }
    }

    private List<JarEntry> getEntries() {
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
            error(e, getClass());
        }
        return entries;
    }

    private File getJarFile() {
        File file = getJarFile(jEdit.getSettingsDirectory());
        if(!file.exists()) {
            file = getJarFile(jEdit.getJEditHome());
        }
        return file;
    }

    private File getJarFile(String directory) {
        File dir = new File(directory, "jars");
        File file = new File(dir, "RubyPlugin.jar");
        return file;
    }

    private void cache(ClassDescription result) {
        ClassMember parent = new ClassMember(result.getName(), 0, 0);
        parent.setNamespace(null);
        parent.setParentMember(null);
        parent.setReceiver("");
        parent.setEndOffset(0);

        addMethods(result.getInstanceMethods(), parent);
        addMethods(result.getClassMethods(), parent);
        Member[] members = new Member[1];
        members[0] = parent;
        RubyMembers rubyMembers = new RubyMembers(members, new ArrayList<Problem>());
        RubyCache.add(rubyMembers, "1.8/system");
    }

    private void addMethods(List<MethodDescription> methods, ClassMember parent) {
        for (MethodDescription methodDescription : methods) {
            String name = methodDescription.getName();
            name = name.startsWith(".") ? name.substring(1) : name;
            Method method = new Method(name, "", "", 0, 0, methodDescription.isClassMethod());
            method.setNamespace(null);
            method.setParentMember(null);
            method.setReceiver("");
            method.setEndOffset(0);
            parent.addChildMember(method);
        }
    }

    private static void log(String message) {
        log(message, RubyPlugin.class);
    }

    public static void log(String message, Class clas) {
        if(debug) {
            try {
                Log.log(Log.MESSAGE, clas, message);
            } catch (Exception e) {
                System.out.println(message);
            }
        }
    }

    public static void error(Exception e, Class clas) {
        String message = e.getClass().getName();
        message = message.substring(message.lastIndexOf('.') + 1);
        if(e.getMessage() != null && e.getMessage().length() > 0) {
            message += ": " + e.getMessage();
        }
        e.printStackTrace();
        error(message, clas);
    }

    public static void error(String message, Class clas) {
        try {
            Log.log(Log.ERROR, clas, message);
            View view = jEdit.getActiveView();
            if (view != null) {
//                show("", message, view, JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            System.err.println(message);
        }
    }

    public static int getNonSpaceStartOffset(int line) {
        int offset = 0;
        View view = jEdit.getActiveView();

        if (view != null && view.getBuffer() != null) {
            Buffer buffer = view.getBuffer();
            offset = buffer.getLineStartOffset(line);
            int end = buffer.getLineEndOffset(line);
            String text = buffer.getLineText(line);
            int length = text.length();

            if (length > 0) {
                int index = 0;
                while (index < length
                        && (text.charAt(index) == ' ' || text.charAt(index) == '\t')
                        && (offset - index) < end) {
                    index++;
                }
                offset += index;
            }
        }

        return offset;
    }

    public static int getEndOffset(int line) {
        int offset = 0;
        View view = jEdit.getActiveView();
        if (view != null) {
            Buffer buffer = view.getBuffer();
            if (buffer != null) {
                offset = buffer.getLineEndOffset(line) - 1;
            }
        }

        return offset;
    }

    public static int getStartOffset(int line) {
        int startOffset = 0;
        View view = jEdit.getActiveView();
        if (view != null) {
            Buffer buffer = view.getBuffer();
            if (buffer != null) {
                startOffset = buffer.getLineStartOffset(line);
            }
        }
        return startOffset;
    }

    public static int getEndOfFileOffset() {
        View view = jEdit.getActiveView();
        int offset = 0;
        if (view != null) {
            Buffer buffer = view.getBuffer();
            offset = buffer.getLineEndOffset(buffer.getLineCount() - 1);
        }
        return offset;
    }

    public static String readFile(File file) {
        StringBuffer buffer = new StringBuffer();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            try {
                char[] chars = new char[1024];
                int length;
                while (-1 != (length = bufferedReader.read(chars))) {
                    buffer.append(chars, 0, length);
                }
            } catch (IOException e) {
                error(e, RubyPlugin.class);
            }
        } catch (FileNotFoundException e) {
            error(e, RubyPlugin.class);
        }

        return buffer.toString();
    }

    public static File getStoragePath(String fileName) {
        File storageDirectory = new File(jEdit.getSettingsDirectory() + File.separatorChar + RUBY_DIR);
        if (!storageDirectory.exists()) {
            storageDirectory.mkdir();
        }
        return new File(storageDirectory.getPath() + File.separatorChar + fileName);
    }

    public static Point getCaretPopupLocation(View view) {
        JEditTextArea textArea = view.getTextArea();
        textArea.scrollToCaret(false);
        Point location = textArea.offsetToXY(textArea.getCaretPosition());
        location.y += textArea.getPainter().getFontMetrics().getHeight();
        SwingUtilities.convertPointToScreen(location, textArea.getPainter());
        return location;
    }

    public static Point getCenteredPopupLocation(View view) {
        JEditTextArea textArea = view.getTextArea();
        textArea.scrollToCaret(false);
        Point location = new Point(textArea.getSize().width / 3, textArea.getSize().height / 5);
        return location;
    }

    public static boolean isRubyFile(Buffer buffer) {
        return isRubyFile(new File(buffer.getPath()));
    }

    public static boolean isRubyFile(File file) {
        Mode rubyMode = jEdit.getMode("ruby");
        return file.isFile() && rubyMode.accept(file.getPath(), "");
    }

    public static void showMessage(String titleKey, String messageKey, View view) {
        String title = titleKey == null ? null : jEdit.getProperty(titleKey);
        String message = jEdit.getProperty(messageKey);
        show(title, message, view, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showMessage(String messageKey, View view) {
        showMessage(null, messageKey, view);
    }

    private static void show(String title, String message, View view, int type) {
        GUIUtilities.hideSplashScreen();
        JOptionPane.showMessageDialog(view, message, title, type);
    }

}
