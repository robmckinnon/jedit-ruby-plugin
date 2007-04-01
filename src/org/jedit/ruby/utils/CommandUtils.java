/*
 * CommandUtils.java - 
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
package org.jedit.ruby.utils;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextArea;
import org.jedit.ruby.RubyPlugin;

import java.io.*;
import java.util.TimerTask;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class CommandUtils {

    private static final String RUBY_DIR = "ruby";

    public static boolean isRubyInstalled() {
        try {
            String result;
            if (CommandUtils.isWindows()) {
                String text = "exec ruby.bat -v\n";
                File commandFile = CommandUtils.getCommandFile("ruby_version.bat", false, text);
                String command = '"' + commandFile.getPath() + '"';
                result = CommandUtils.getOutput(command, false);
            } else {
                result = CommandUtils.getOutput("ruby -v", false);
            }
            RubyPlugin.log("Ruby installed: " + result, CommandUtils.class);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            RubyPlugin.error(jEdit.getProperty("ruby.ruby-not-found.message"), CommandUtils.class);
            return false;
        }
    }

    public static File getStoragePath(String fileName) {
        File storageDirectory = new File(jEdit.getSettingsDirectory() + File.separatorChar + RUBY_DIR);
        if (!storageDirectory.exists()) {
            storageDirectory.mkdir();
        }
        return new File(storageDirectory.getPath() + File.separatorChar + fileName);
    }

    /**
     * @return string output of execution of the supplied system command
     */
    public static String getOutput(String command, boolean retryOnFail) throws IOException, InterruptedException {
        return getOutput(command, retryOnFail, 1500);
    }

    public static String getOutput(String command, boolean retryOnFail, int timeout) throws IOException, InterruptedException {
        StringBuffer buffer = new StringBuffer();
        Process process = run(command, timeout);
        readStream(process.getInputStream(), buffer);
        readStream(process.getErrorStream(), buffer);

        if (buffer.length() == 0 && retryOnFail) {
            return getOutput(command, false);
        } else {
            return buffer.toString();
        }
    }

    private static void readStream(InputStream stream, StringBuffer buffer) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(stream));
            if (reader.ready()) {
                buffer.append(reader.readLine());
                while (reader.ready()) {
                    buffer.append('\n').append(reader.readLine());
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * Runs supplied system command and returns process object.
     */
    private static Process run(String command, int timeout) throws IOException, InterruptedException {
        final Process process = Runtime.getRuntime().exec(command);
        TimerTask task = null;
        if (timeout != -1) {
            task = new TimerTask() {
                public void run() {
                    synchronized (process) {
                        // kills blocked subprocess
                        process.destroy();
                    }
                }

            };
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(task, timeout);
        }
        process.waitFor();

        if (task != null) {
            synchronized (process) {
                task.cancel();
            }
        }

        return process;
    }

    public static File getCommandFile(String fileName, boolean forceCreation, String text) throws IOException, InterruptedException {
        File commandFile = getStoragePath(fileName);
        if (forceCreation || !commandFile.exists()) {
            PrintWriter writer = new PrintWriter(new FileWriter(commandFile));
            writer.print(text);
            writer.close();

            if (!isWindows()) {
                RubyPlugin.log(getOutput("chmod +x " + commandFile.getPath(), false), RubyPlugin.class);
            }
        }
        return commandFile;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;
    }

    public static Object invoke(Object buffer, String methodName, Class[] paramTypes, Object... args) throws IllegalAccessException, InvocationTargetException {
        Method method = null;
        try {
            method = buffer.getClass().getMethod(methodName, paramTypes);
        } catch (Exception e) {
            Method[] methods = buffer.getClass().getMethods();
            for (Method m : methods) {
                if (m.getName().equals(methodName)) {
                    method = m;
                }
            }
        }

        return method.invoke(buffer, args);
    }

    public static Object getBuffer(TextArea textArea) {
        Object buffer = null;
        try {
            buffer = invoke(textArea, "getBuffer", null, (Object[])null);
        } catch (Exception e) {
            RubyPlugin.log("error getting buffer " + e.getMessage(), CommandUtils.class);
        }
        return buffer;
    }
}
