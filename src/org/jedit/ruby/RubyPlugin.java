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
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;
import org.jedit.ruby.parser.JRubyParser;
import org.jedit.ruby.ri.RiParser;
import org.jedit.ruby.completion.RubyKeyBindings;
import org.jedit.ruby.structure.RubyStructureMatcher;
import org.jedit.ruby.structure.BufferChangeHandler;
import org.jedit.ruby.utils.CharCaretListener;
import org.jedit.ruby.utils.EditorView;
import org.jedit.ruby.utils.ViewWrapper;
import org.jedit.ruby.utils.CommandUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Map;
import java.util.HashMap;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public final class RubyPlugin extends EBPlugin {

    private static final boolean debug = System.getProperty("user.home").equals("/Users/x");
    private static final CharCaretListener CHAR_CARET_LISTENER = new CharCaretListener();
    private static final Map<View, EditorView> views = new HashMap<View, EditorView>();

    public final void start() {
        super.start();
        JRubyParser.setExpectedLabel(jEdit.getProperty("ruby.syntax-error.expected.label"));
        JRubyParser.setFoundLabel(jEdit.getProperty("ruby.syntax-error.found.label"));
        JRubyParser.setNothingLabel(jEdit.getProperty("ruby.syntax-error.nothing.label"));

        RiParser.parseRdoc();

        View view = jEdit.getFirstView();
        while (view != null) {
            EditPane[] panes = view.getEditPanes();
            for (EditPane pane : panes) {
                addKeyListener(pane.getTextArea());
            }
            view = view.getNext();
        }
    }

    public void handleMessage(EBMessage message) {
        if (message instanceof ViewUpdate) {
            handleViewUpdate((ViewUpdate)message);
        } else if (message instanceof EditPaneUpdate) {
            handleEditUpdate((EditPaneUpdate)message);
        } else if (message instanceof BufferUpdate) {
            handleBufferUpdate((BufferUpdate)message);
        } else if (message instanceof PropertiesChanged) {
//            SideKickActions.propertiesChanged();
        }
    }

    private static void handleViewUpdate(ViewUpdate update) {
        if (ViewUpdate.CREATED == update.getWhat()) {
            //
        } else if (ViewUpdate.CLOSED == update.getWhat()) {
            views.remove(update.getView());
        }
    }

    private static void handleBufferUpdate(BufferUpdate update) {
        if (BufferUpdate.LOADED == update.getWhat()) {
            update.getBuffer().addBufferListener(BufferChangeHandler.instance());

        } if (BufferUpdate.CLOSED == update.getWhat()) {
            update.getBuffer().addBufferListener(BufferChangeHandler.instance());
        }
    }

    private void handleEditUpdate(EditPaneUpdate update) {
        JEditTextArea textArea = update.getEditPane().getTextArea();

        if (EditPaneUpdate.CREATED == update.getWhat()) {
            addKeyListener(textArea);
        } else if (EditPaneUpdate.DESTROYED == update.getWhat()) {
            removeKeyListener(textArea);
        }
    }

    private static void addKeyListener(JEditTextArea textArea) {
        RubyKeyBindings bindings = new RubyKeyBindings(textArea);
        RubyStructureMatcher structureMatcher = new RubyStructureMatcher();

        textArea.putClientProperty(RubyKeyBindings.class, bindings);
        textArea.putClientProperty(RubyStructureMatcher.class, structureMatcher);

        textArea.addKeyListener(bindings);
        textArea.addStructureMatcher(structureMatcher);
        textArea.addCaretListener(CHAR_CARET_LISTENER);
    }

    private static void removeKeyListener(JEditTextArea textArea) {
        RubyKeyBindings bindings = (RubyKeyBindings)textArea.getClientProperty(RubyKeyBindings.class);
        RubyStructureMatcher structureMatcher = (RubyStructureMatcher)textArea.getClientProperty(RubyStructureMatcher.class);

        textArea.putClientProperty(RubyKeyBindings.class, null);
        textArea.putClientProperty(RubyStructureMatcher.class, null);

        textArea.removeKeyListener(bindings);
        textArea.removeStructureMatcher(structureMatcher);
        textArea.removeCaretListener(CHAR_CARET_LISTENER);
    }

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

    public static EditorView getActiveView() {
        final View view = jEdit.getActiveView();

        if (!views.containsKey(view)) {
            EditorView editorView = view != null ? new ViewWrapper(view) : EditorView.NULL;
            views.put(view, editorView);
        }

        return views.get(view);
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
        return getActiveView().getNonSpaceStartOffset(line);
    }

    public static int getEndOffset(int line) {
        return getActiveView().getEndOffset(line);
    }

    public static int getStartOffset(int line) {
        return getActiveView().getStartOffset(line);
    }

    public static int getEndOfFileOffset() {
        return getActiveView().getEndOfFileOffset();
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

    public static boolean isRuby(org.gjt.sp.jedit.textarea.TextArea textArea) {
        return isRuby(CommandUtils.getBuffer(textArea));
    }

    private static boolean isRuby(Object buffer) {
        if (buffer instanceof Buffer) {
            return isRuby((Buffer)buffer);
        } else {
            throw new IllegalArgumentException("expecting buffer to be of type Buffer");
        }
    }

    public static boolean isRuby(Buffer buffer) {
        boolean isRubyBuffer = buffer.getMode() == rubyMode();
        return isRubyBuffer || isRuby(new File(buffer.getPath()));
    }

    public static boolean isRuby(File file) {
        return file.isFile() && rubyMode().accept(file.getPath(), "");
    }

    private static Mode rubyMode() {
        return jEdit.getMode("ruby");
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
