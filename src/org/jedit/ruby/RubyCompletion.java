/*
 * RubyCompletion.java -
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

import sidekick.SideKickCompletion;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.jedit.ruby.ast.Method;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RubyCompletion extends SideKickCompletion {

    private List<Method> methods;
    private String partialMethod;
    private JWindow frame;
    private JTextPane textPane;

    public RubyCompletion(View view, String partialMethod, List<Method> methods) {
        super(view, partialMethod == null ? "." : "." + partialMethod, methods);
        this.methods = methods;
        this.partialMethod = partialMethod;
        frame = new JWindow((Frame)null);
        frame.setFocusable(false);
        textPane = new JTextPane();
        textPane.setEditorKit(new HTMLEditorKit());
		JScrollPane scroller = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        frame.getContentPane().add(scroller, BorderLayout.CENTER);
        frame.setSize(400,400);
    }

    public boolean handleKeystroke(int selectedIndex, char keyChar) {
        boolean emptyPopup = selectedIndex == -1;
        boolean space = keyChar == ' ';
        boolean stillTyping = !space && keyChar != '\t' && keyChar != '\n';

        if (stillTyping || emptyPopup) {
            textArea.userInput(keyChar);
        } else {
            insert(selectedIndex);
            if(space) {
                textArea.userInput(' ');
            }
        }

        return stillTyping;
    }

    public void insert(int index) {
        insert(methods.get(index));
    }

    public String getCompletionDescription(int index) {
        String documentation = methods.get(index).getDocumentation();
        RubyPlugin.log(documentation, getClass());
        RubyPlugin.log(documentation.indexOf("&lt;")+"", getClass());
        try {
            InputStream stream = new StringBufferInputStream(documentation);
            textPane.read(stream, new HTMLDocument());
        } catch (IOException e) {
            RubyPlugin.error(e, getClass());
        }

        frame.setLocation(getPoint(frame));
        if(!frame.isVisible()) {
            frame.setVisible(true);
        }
        return null;
    }

    private Point getPoint(JWindow window) {
        JEditTextArea textArea = view.getTextArea();
        int caret = textArea.getCaretPosition();
        Point location = textArea.offsetToXY(caret - getTokenLength());
        location.x += 150;
        location.y += textArea.getPainter().getFontMetrics().getHeight();

        SwingUtilities.convertPointToScreen(location, textArea.getPainter());

        Rectangle screenSize = window.getGraphicsConfiguration().getBounds();
        if(location.y + window.getHeight() >= screenSize.height) {
            location.y = location.y - window.getHeight() - textArea.getPainter().getFontMetrics().getHeight();
}
        Point point = location;
        return point;
    }

    private void insert(Method method) {
        Buffer buffer = view.getBuffer();
        RubyPlugin.log("method: " + method.getName(), getClass());
        int caretPosition = textArea.getCaretPosition();
        int offset = caretPosition;

        if (partialMethod != null) {
            offset -= partialMethod.length();
            buffer.remove(offset, partialMethod.length());
        }

        buffer.insert(offset, method.getName());
        frame.setVisible(false);
        frame.dispose();
        frame = null;
    }

}