/*
 * RubyPlugin.java - Ruby editor plugin for jEdit
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
import org.jedit.ruby.ri.RiParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public final class RubyPlugin extends EditPlugin {

    private static final boolean debug = System.getProperty("user.home").equals("/home/b");

    public final void start() {
        super.start();
        JRubyParser.setExpectedLabel(jEdit.getProperty("ruby.syntax-error.expected.label"));
        JRubyParser.setFoundLabel(jEdit.getProperty("ruby.syntax-error.found.label"));
        JRubyParser.setNothingLabel(jEdit.getProperty("ruby.syntax-error.nothing.label"));

        RiParser.parseRdoc();
    }

//    public void handleMessage(EBMessage message) {
//        if (message instanceof ViewUpdate) {
//            handleViewUpdate((ViewUpdate)message);
//        } else if (message instanceof EditPaneUpdate) {
//            handleEditUpdate((EditPaneUpdate)message);
//        } else if (message instanceof BufferUpdate) {
//            handleBufferUpdate((BufferUpdate) message);
//        } else if (message instanceof PropertiesChanged) {
//            SideKickActions.propertiesChanged();
//        }
//    }
//
//    private void handleBufferUpdate(BufferUpdate update) {
//        if (update.getWhat() == BufferUpdate.CLOSED) {
////            finishParsingBuffer(update.getBuffer());
//        }
//    }
//
//    private void handleEditUpdate(EditPaneUpdate update) {
//        EditPane editPane = update.getEditPane();
//
//        if (update.getWhat() == EditPaneUpdate.CREATED) {
//            addKeyListener(editPane.getTextArea());
//        } else if (update.getWhat() == EditPaneUpdate.DESTROYED) {
//            removeKeyListener(editPane.getTextArea());
//        }
//    }
//
//    private void handleViewUpdate(ViewUpdate update) {
//        if (update.getWhat() == ViewUpdate.CREATED) {
////                initView(update.getView());
//        } else if (update.getWhat() == ViewUpdate.CLOSED) {
////                uninitView(update.getView());
//        }
//    }

    public static void log(String message, Class clas) {
        if (debug) {
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
        if (e.getMessage() != null && e.getMessage().length() > 0) {
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
                if (debug) {
                    show("", message, view, JOptionPane.ERROR_MESSAGE);
                }
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
        return new Point(textArea.getSize().width / 3, textArea.getSize().height / 5);
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

    public static boolean ignoreKeyTyped(int keyCode, char keyChar, KeyEvent event) {
        boolean ignore;

        switch (keyCode) {
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_PAGE_UP:
            case KeyEvent.VK_PAGE_DOWN:
            case KeyEvent.VK_HOME:
            case KeyEvent.VK_END:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
                ignore = true;
                break;
            default:
                // for some reason have to match backspace and tab using keyChar
                ignore = keyChar == KeyEvent.VK_BACK_SPACE
                        || keyChar == KeyEvent.VK_TAB
                        || event.isActionKey()
                        || event.isControlDown()
                        || event.isAltDown()
                        || event.isMetaDown();
        }
        return ignore;
    }

}
