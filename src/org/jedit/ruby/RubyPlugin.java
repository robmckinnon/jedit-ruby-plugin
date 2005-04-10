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

import javax.swing.SwingUtilities;
import java.io.*;
import java.awt.Point;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public class RubyPlugin extends EditPlugin {

    private static boolean debug = System.getProperty("user.home").equals("/home/a");
    private static final String RUBY_DIR = "ruby";

    public void start() {
        super.start();
        JRubyParser.setExpectedLabel(jEdit.getProperty("ruby.syntax-error.expected.label"));
        JRubyParser.setFoundLabel(jEdit.getProperty("ruby.syntax-error.found.label"));
        JRubyParser.setNothingLabel(jEdit.getProperty("ruby.syntax-error.nothing.label"));
    }

    public void stop() {
        super.stop();
    }

    public static void log(String message) {
        if(debug) {
            try {
                Log.log(Log.MESSAGE, jEdit.getPlugin("RubyPlugin"), message);
            } catch (Exception e) {
                System.out.println(message);
            }
        }
    }

    public static void error(String message) {
        try {
            EditPlugin plugin = jEdit.getPlugin("RubyPlugin");
            Log.log(Log.ERROR, plugin, message);
            View view = jEdit.getActiveView();
            if (view != null) {
                Macros.message(view, message);
            }
        } catch (Exception e) {
            System.err.println(message);
        }
    }

    public static int getNonSpaceStartOffset(int line) {
        int offset = 0;
        View view = jEdit.getActiveView();
        if (view != null) {
            Buffer buffer = view.getBuffer();
            if (buffer != null) {
                offset = buffer.getLineStartOffset(line);
                int end = buffer.getLineEndOffset(line);
                String text = buffer.getLineText(line);

                if(text.length() > 0) {
                    int index = 0;
                    while ((text.charAt(index) == ' ' || text.charAt(index) == '\t')
                            && (offset - index) < end) {
                        index++;
                    }
                    offset += index;
                }
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
                error(e.getMessage());
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            error(e.getMessage());
            e.printStackTrace();
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
}
