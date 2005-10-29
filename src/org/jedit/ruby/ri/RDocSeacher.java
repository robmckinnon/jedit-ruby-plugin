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
package org.jedit.ruby.ri;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Macros;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Method;
import org.jedit.ruby.structure.TypeAheadPopup;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.utils.CommandUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;

/**
 * Allows user to search Ruby documentation using ri - Ruby interactive reference.<ul><li>
 *   Brings up dialog for user to enter search term.</li><li>
 *   Macro runs ri on term, and reports ri results in another dialog.</li><li>
 *   Remembers last term searched, and places it in search entry field.</li><li>
 *   If user has text selected, then that is placed in search entry field instead.</li></ul>
 *
 * @author robmckinnon at users.sourceforge.net
 */
public final class RDocSeacher {

    private static final RDocSeacher instance = new RDocSeacher();

    private final Map<String, String> termToResult;

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
            RubyPlugin.error(e, RDocSeacher.class);
        } catch (InterruptedException e) {
            e.printStackTrace();
            RubyPlugin.error(e, RDocSeacher.class);
        }
    }

    /**
     * Performs Ruby documentation search.
     */
    private void performSearch(View view) throws IOException, InterruptedException {
        String term = getSearchTerm(view, view.getTextArea().getSelectedText());
        if (term != null) {
            jEdit.setProperty("ruby-ri-search-term", term);
            searchFor(term, view);
        }
    }

    private void searchFor(String term, View view) throws IOException, InterruptedException {
        String result = ri(term);

        if(result.startsWith("More than one method matched your request.")) {
            List<Member> methods = parseMultipleResults(result);
            Member[] members = methods.toArray(new Member[methods.size()]);
            new TypeAheadPopup(view, members, members[0], org.jedit.ruby.structure.TypeAheadPopup.SEARCH_POPUP);
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
                    String namespace = null;
                    String methodName = tokenizer.nextToken();
                    int index = methodName.lastIndexOf("#");
                    int otherIndex = methodName.lastIndexOf("::");
                    index = Math.max(index, otherIndex);
                    boolean isClassMethod = index == otherIndex;
                    int adj = isClassMethod ? 2 : 1;

                    if (index != -1) {
                        namespace = methodName.substring(0, index);
                        methodName = methodName.substring(index + adj);
                    }

                    Method method = new Method(methodName, null, "none", "none", isClassMethod);
                    method.setNamespace(namespace);
                    methods.add(method);
                }
            }
        }

        Collections.sort(methods);
        return methods;
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
            String result;
            if (CommandUtils.isWindows()) {
                result = CommandUtils.getOutput("ri.bat -T " + '"' + searchTerm + '"', true);
            } else {
                result = CommandUtils.getOutput("ri -T " + searchTerm, true);
            }

            if (result.length() == 0) {
                result = rri(searchTerm);
            }

            if (result == null) {
                result = jEdit.getProperty("ruby.search-documentation.error");
            } else {
                termToResult.put(searchTerm, result);
            }

            return result;
        }
    }

    private static String rri(String searchTerm) throws IOException, InterruptedException {
        File resultFile = CommandUtils.getStoragePath("ri_result.txt");
        String command = getRriCommand(resultFile, searchTerm);
        CommandUtils.getOutput(command, false);
        return RubyPlugin.readFile(resultFile);
    }

    private static String getRriCommand(File resultFile, String searchTerm) throws IOException, InterruptedException {
        if (CommandUtils.isWindows()) {
            String text = "exec ri.bat -T %1 > " + '"' + resultFile + '"' + '\n';
            File commandFile = CommandUtils.getCommandFile("rri.bat", false, text);
            return '"' + commandFile.getPath() + '"' + ' ' + '"' + searchTerm + '"';
        } else {
            String text = "#!/bin/sh\n"
                                + "ri -T $1 > " + resultFile + '\n';
            File commandFile = CommandUtils.getCommandFile("rri.sh", false, text);
            return commandFile.getPath() + ' ' + searchTerm;
        }
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

        dialog.setVisible(true);
    }

    /**
     * Displays dialog for user to enter search term.
     */
    private static String getSearchTerm(View view, String term) {
        if (term == null) {
            term = jEdit.getProperty("ruby-ri-search-term", "");
        }

        String label = jEdit.getProperty("ruby.search-documentation.dialog.label");
        term = Macros.input(view, label, term);
        return term;
    }

}
