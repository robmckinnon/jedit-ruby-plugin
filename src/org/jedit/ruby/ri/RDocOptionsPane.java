/*
* RDocOptionsPane.java -
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

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.jedit.ruby.utils.CommandUtils;
import org.jedit.ruby.RubyPlugin;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class RDocOptionsPane extends AbstractOptionPane {

    private static final String RDOC_PATH_PREFIX = "rubyplugin.rdoc-path.";
    private JList pathList;
    private DefaultListModel pathListModel;
    private final List<String> deletedPaths;
    private JButton add;
    private JButton remove;

    public RDocOptionsPane() {
        super("rdoc");
        deletedPaths = new ArrayList<String>();
    }

    protected final void _init() {
        setLayout(new BorderLayout());

        pathListModel = new DefaultListModel();
        int i = 0;
        String element;
        while ((element = jEdit.getProperty(RDOC_PATH_PREFIX + i)) != null) {
            pathListModel.addElement(element);
            i++;
        }
        pathList = new JList(pathListModel);
        pathList.addListSelectionListener(new ListHandler());

        ActionHandler handler = new ActionHandler();
        add = initButton("Plus.png", "options.rdoc.add", handler);
        remove = initButton("Minus.png", "options.rdoc.remove", handler);

        add(BorderLayout.CENTER, new JScrollPane(pathList));
        add(BorderLayout.NORTH, initButtonPanel(add, remove));
        updateEnabled();
    }

    private static JPanel initButtonPanel(JButton add, JButton remove) {
        JLabel label = new JLabel(jEdit.getProperty("options.rdoc.caption"));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        panel.add(label);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(add);
        panel.add(remove);
        panel.add(Box.createGlue());
        return panel;
    }

    private static JButton initButton(String iconName, String tooltipProperty, ActionHandler handler) {
        JButton button = new RolloverButton(GUIUtilities.loadIcon(iconName));
        button.setToolTipText(jEdit.getProperty(tooltipProperty));
        button.addActionListener(handler);
        return button;
    }

    protected final void _save() {
        int i;
        StringBuffer buffer = new StringBuffer("require '" + CommandUtils.getStoragePath("rdoc_to_java.rb") + "'\n");
        File resultPath = CommandUtils.getStoragePath("java-xml");
        buffer.append("result_dir = '").append(resultPath).append("'\n");
        buffer.append("template_dir = '").append(resultPath.getParent()).append("'\n");
        buffer.append("serializer = JavaXmlSerializer.new(template_dir, result_dir)\n");

        for (i = 0; i < pathListModel.getSize(); i++) {
            String path = (String)pathListModel.getElementAt(i);
            jEdit.setProperty(RDOC_PATH_PREFIX + i, path);
            File directory = new File(path);
            String baseDirectory = directory.getParent() + File.separatorChar;
            buffer.append("serializer.convert_dir('").append(baseDirectory).append("', '").append(directory.getName()).append("')\n");
        }

        try {
            File convertScript = CommandUtils.getCommandFile("convert_rdoc.rb", true, buffer.toString());
            String log;
            if(CommandUtils.isWindows()) {
                String text = "ruby.bat \"" + convertScript.getPath() + "\"";
                File commandFile = CommandUtils.getCommandFile("convert_rdoc.bat", false, text);
                log = CommandUtils.getOutput(commandFile.getPath(), false);
            } else {
                String command = "ruby " + convertScript.getPath();
                RubyPlugin.log("Running: " + command, getClass());
                log = CommandUtils.getOutput(command, false, -1);
            }

            RubyPlugin.log(log, getClass());
        } catch (Exception e) {
            e.printStackTrace();
            RubyPlugin.error(e, getClass());
        }
        jEdit.unsetProperty(RDOC_PATH_PREFIX + i);
    }

    private void updateEnabled() {
        boolean selected = (pathList.getSelectedValue() != null);
        remove.setEnabled(selected);
    }

    private final class ActionHandler implements ActionListener {
        public final void actionPerformed(ActionEvent event) {
            Object source = event.getSource();
            if (source == add) {
                handleAdd();
            } else if (source == remove) {
                String deleted = (String)pathListModel.remove(pathList.getSelectedIndex());
                deletedPaths.add(deleted);
                updateEnabled();
            }
        }

        private void handleAdd() {
            String userHome = System.getProperty("user.home");
            File rdocPath = new File(userHome, ".rdoc");

            if(!rdocPath.isDirectory()) {
                rdocPath = rdocPath.getParentFile();
            }

            String path = rdocPath.getPath() + File.separatorChar;
            String[] files = GUIUtilities.showVFSFileDialog(null, path, VFSBrowser.CHOOSE_DIRECTORY_DIALOG, true);
            if (files != null) {
                for (String file : files) {
                    pathListModel.addElement(file);
                }
            }
        }
    }

    private final class ListHandler implements ListSelectionListener {
        public final void valueChanged(ListSelectionEvent evt) {
            updateEnabled();
        }
    }
}