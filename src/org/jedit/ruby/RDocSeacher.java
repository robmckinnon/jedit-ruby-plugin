/*
 * RDocSeacher.java - 
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

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Macros;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.TimerTask;

/**
 * Allows user to search Ruby documentation using ri - Ruby interactive reference.
 *  - Brings up dialog for user to enter search term.
 *  - Macro runs ri on term, and reports ri results in another dialog.
 *  - Remembers last term searched, and places it in search entry field.
 *  - If user has text selected, then that is placed in search entry field instead.
 *
 * @author robmckinnon at users.sourceforge.net
 */
public class RDocSeacher {

    /**
     * Performs Ruby documentation search.
     */
    public static void doSearch(View view) throws IOException, InterruptedException {
        String term = getSearchTerm(view, view.getTextArea().getSelectedText());
        if(term != null) {
            jEdit.setProperty("ruby-ri-search-term", term);
    //    Macros.message(view, ri(term));
            showDialog(view, "", ri(term));
        }
    }

    /**
     * Runs supplied system command and returns process object.
     */
    private static Process run(String[] command) throws IOException,InterruptedException {
        System.out.println(System.getProperty("user.dir"));
        final Process process = Runtime.getRuntime().exec(command);

        java.util.Timer timer = new java.util.Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                synchronized(process) {
                    // kills blocked subprocess
                    process.destroy();
                }
            }

        };
        timer.schedule(task, 1500);

        process.waitFor();

        synchronized(process) {
            task.cancel();
        }

        return process;
    }

    /**
     * Returns string output of execution of the supplied system command.
     */
    private static String getOutput(String[] command) throws IOException, InterruptedException {
        Process process = run(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuffer buffer = new StringBuffer();

        if(reader.ready()) {
            buffer.append(reader.readLine());
            while(reader.ready()) {
                buffer.append('\n' + reader.readLine());
            }
        }
        reader.close();

        reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        if(reader.ready()) {
            buffer.append(reader.readLine());
            while(reader.ready()) {
                buffer.append('\n' + reader.readLine());
            }
        }
        reader.close();

        if(buffer.length() == 0)
            return jEdit.getProperty("ruby.search-documentation.error");
        else
            return buffer.toString();
    }

    /**
     * Runs ri on supplied string search term and returns result string.
     */
    private static String ri(String searchTerm) throws IOException, InterruptedException {
        if(searchTerm.length() == 0)
            searchTerm = "-c";
//        boolean windows = System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;

//        if (windows) {
//            return getOutput(new String[] {"ri.bat", searchTerm});
//        } else {
            return getOutput(new String[] {"ri","-T", searchTerm});
//        }
    }

    private static JScrollPane getScrollPane(JTextArea label, Action closeAction) {
        JScrollPane scrollPane = new JScrollPane(label);
        final String CLOSE = "close";
        scrollPane.getActionMap().put(CLOSE, closeAction);

        InputMap inputMap = scrollPane.getInputMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLOSE);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), CLOSE);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), CLOSE);
        return scrollPane;
    }

    private static void showDialog(View frame, String title, String text) {
        final JDialog dialog = new JDialog(frame, title, false);
        JTextArea label = new JTextArea(text);
        label.setEditable(true);
        label.setBackground(dialog.getContentPane().getBackground());
        JScrollPane pane = getScrollPane(label, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });

        dialog.setContentPane(pane);
        dialog.pack();

        int height = dialog.getHeight();
        if(dialog.getHeight() > frame.getHeight()*.8) {
            height = (int)(frame.getHeight()*.8);
        }

        dialog.setSize((int)(dialog.getWidth() *1.05), height);
        dialog.setLocationRelativeTo(frame);

        dialog.show();
    }

    /**
     * Displays dialog for user to enter search term.
     */
    private static String getSearchTerm(View view, String term) {

        if(term == null) {
            term = jEdit.getProperty("ruby-ri-search-term", "");
        }

        String label = jEdit.getProperty("ruby.search-documentation.dialog.label");
        term = Macros.input(view, label, term);
        return term;
    }

}
