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
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Method;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.Point;
import java.io.*;
import java.util.*;

/**
 * Allows user to search Ruby documentation using ri - Ruby interactive reference.
 * - Brings up dialog for user to enter search term.
 * - Macro runs ri on term, and reports ri results in another dialog.
 * - Remembers last term searched, and places it in search entry field.
 * - If user has text selected, then that is placed in search entry field instead.
 *
 * @author robmckinnon at users.sourceforge.net
 */
public class RDocSeacher {

    private static final RDocSeacher instance = new RDocSeacher();

    private Map<String, String> termToResult;

    /**
     * singleton private constructor
     */
    private RDocSeacher() {
        termToResult = new HashMap<String, String>();
    }

    public static void doSearch(View view) throws IOException, InterruptedException {
        instance.performSearch(view);
    }

    public static void doSearch(View view, String searchTerm) {
        try {
            instance.searchFor(searchTerm, view);
        } catch (IOException e) {
            e.printStackTrace();
            RubyPlugin.error(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            RubyPlugin.error(e.getMessage());
        }
    }

    /**
     * Performs Ruby documentation search.
     */
    public void performSearch(View view) throws IOException, InterruptedException {
        String term = getSearchTerm(view, view.getTextArea().getSelectedText());
        if (term != null) {
            searchFor(term, view);
        }
    }

    private void searchFor(String term, View view) throws IOException, InterruptedException {
        jEdit.setProperty("ruby-ri-search-term", term);
        //    Macros.message(view, ri(term));
        String result = ri(term);

        if(result.startsWith("More than one method matched your request.")) {
            List<Member> methods = parseMultipleResults(result);
            Member[] members = methods.toArray(new Member[methods.size()]);
            Point location = RubyPlugin.getCenteredPopupLocation(view);
            new TypeAheadPopup(view, members, location);
        } else {
            showDialog(view, "", result);
        }
    }

    public static List<Member> parseMultipleResults(String result) {
        StringTokenizer lines = new StringTokenizer(result, "\n");
        List<Member> methods = new ArrayList<Member>();

        while(lines.hasMoreTokens()) {
            String line = lines.nextToken();
            if(line.startsWith(" ")) {
                StringTokenizer tokenizer = new StringTokenizer(line.trim(), ", ");
                while(tokenizer.hasMoreTokens()) {
                    String method = tokenizer.nextToken();
                    methods.add(new Method(method, "none", "none", 0));
                }
            }
        }

        Collections.sort(methods);
        return methods;
    }

    /**
     * Runs supplied system command and returns process object.
     */
    private Process run(String command) throws IOException, InterruptedException {
        final Process process = Runtime.getRuntime().exec(command);

        java.util.Timer timer = new java.util.Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                synchronized (process) {
                    // kills blocked subprocess
                    process.destroy();
                }
            }

        };
        timer.schedule(task, 1500);

        process.waitFor();

        synchronized (process) {
            task.cancel();
        }

        return process;
    }

    /**
     * Returns string output of execution of the supplied system command.
     */
    private String getOutput(String command, boolean retryOnFail) throws IOException, InterruptedException {
        Process process = run(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuffer buffer = new StringBuffer();

        if (reader.ready()) {
            buffer.append(reader.readLine());
            while (reader.ready()) {
                buffer.append('\n' + reader.readLine());
            }
        }
        reader.close();

        reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        if (reader.ready()) {
            buffer.append(reader.readLine());
            while (reader.ready()) {
                buffer.append('\n' + reader.readLine());
            }
        }
        reader.close();

        if (buffer.length() == 0) {
            if (retryOnFail) {
                return getOutput(command, false);
            } else {
                return null;
            }
        } else {
            String result = buffer.toString();
            termToResult.put(command, result);
            return result;
        }
    }

    /**
     * Runs ri on supplied string search term and returns result string.
     */
    private String ri(String searchTerm) throws IOException, InterruptedException {
        if (searchTerm.length() == 0) {
            searchTerm = "-c";
        }

        if (termToResult.containsKey(searchTerm)) {
            return termToResult.get(searchTerm);

        } else {
            boolean windows = System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;
            String result;
            if (windows) {
                result = getOutput("ri.bat -T " + '"' + searchTerm + '"', true);
            } else {
                result = getOutput("ri -T " + searchTerm, true);
            }

            if (result == null) {
                result = rri(windows, searchTerm);
            }

            if (result == null) {
                result = jEdit.getProperty("ruby.search-documentation.error");
            }

            return result;
        }
    }

    private String rri(boolean windows, String searchTerm) throws IOException, InterruptedException {
        File commandFile = windows ? RubyPlugin.getStoragePath("rri.bat") : RubyPlugin.getStoragePath("rri.sh");
        File resultFile = RubyPlugin.getStoragePath("ri_result.txt");

        if (!commandFile.exists()) {
            createCommandFile(windows, resultFile, commandFile);
        }

        String result = null;

        if (windows) {
            getOutput('"' + commandFile.getPath() + '"' + ' ' + '"' + searchTerm + '"', false);
        } else {
            try {
                getOutput(commandFile.getPath() + ' ' + searchTerm, false);
            } catch (IOException e) {
                if (e.getMessage().indexOf("rri.sh: cannot execute") != -1) {
                    result = jEdit.getProperty("ruby.search-documentation.permission", new String[] {commandFile.getPath()});
                } else {
                    throw e;
                }
            }
        }

        if(result == null) {
            result = RubyPlugin.readFile(resultFile);
        }

        return result;
    }

    private void createCommandFile(boolean windows, File resultFile, File commandFile) throws IOException {
        String rri;
        if (windows) {
            rri = "exec ri.bat -T %1 > " + '"' + resultFile + '"' + '\n';
        } else {
            rri = "#!/bin/sh\n"
                    + "ri -T $1 > " + resultFile + '\n';
        }
        PrintWriter writer = new PrintWriter(new FileWriter(commandFile));
        writer.print(rri);
        writer.close();
    }

    private JScrollPane getScrollPane(JTextArea label, Action closeAction) {
        JScrollPane scrollPane = new JScrollPane(label);
        final String CLOSE = "close";
        scrollPane.getActionMap().put(CLOSE, closeAction);

        InputMap inputMap = scrollPane.getInputMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLOSE);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), CLOSE);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), CLOSE);
        return scrollPane;
    }

    private void showDialog(View frame, String title, String text) {
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
        if (dialog.getHeight() > frame.getHeight() * .8) {
            height = (int) (frame.getHeight() * .8);
        }

        dialog.setSize((int) (dialog.getWidth() * 1.05), height);
        dialog.setLocationRelativeTo(frame);

        dialog.show();
    }

    /**
     * Displays dialog for user to enter search term.
     */
    private String getSearchTerm(View view, String term) {
        if (term == null) {
            term = jEdit.getProperty("ruby-ri-search-term", "");
        }

        String label = jEdit.getProperty("ruby.search-documentation.dialog.label");
        term = Macros.input(view, label, term);
        return term;
    }

}
