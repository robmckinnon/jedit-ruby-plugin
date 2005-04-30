/*
 * RDocViewer.java -
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
import org.gjt.sp.jedit.gui.DefaultFocusComponent;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.jEdit;
import org.jedit.ruby.RubyCache;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Method;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RDocViewer extends JPanel implements DefaultFocusComponent {

    private final static Map<RDocViewer, RDocViewer> viewers = new WeakHashMap<RDocViewer, RDocViewer>();

    private View view;
    private JTextField searchField;
    private JList resultList;
    private JTextPane documentationPane;
    private final JSplitPane splitPane;

    public RDocViewer(View view, String position) {
        super(new BorderLayout());

        this.view = view;
        searchField = new JTextField();
        resultList = initResultList();
        documentationPane = initDocumentationPane();

        configure(searchField);
        configure(resultList);
        configure(documentationPane);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.NORTH);
        searchPanel.add(wrapInScrollPane(resultList));
        Dimension size = searchPanel.getPreferredSize();
        size.width = 200;
        searchPanel.setPreferredSize(size);
        searchPanel.setMinimumSize(size);

        splitPane = getPanel(position, searchPanel, wrapInScrollPane(documentationPane));
        add(splitPane);
        viewers.put(this, null);
    }

    public static void setMethod(Method method) {
        for (RDocViewer viewer : viewers.keySet()) {
            viewer.setMember(method);
        }
    }

    private void setMember(Member member) {
        resultList.setSelectedValue(member, true);
        handleSelection();
    }

    private JTextPane initDocumentationPane() {
        HTMLEditorKit kit = new HTMLEditorKit();
        kit.setStyleSheet(new RDocStyleSheet(view));
        JTextPane documentationPane = new JTextPane();
        documentationPane.setEditorKit(kit);
        return documentationPane;
    }

    private void configure(JComponent component) {
        component.setFont(jEdit.getFontProperty("view.font"));
        component.setBackground(jEdit.getColorProperty("view.bgColor"));
        component.setForeground(jEdit.getColorProperty("view.fgColor"));
    }

    public void focusOnDefaultComponent() {
        searchField.requestFocus();
    }

    private JScrollPane wrapInScrollPane(JComponent component) {
        return new JScrollPane(component, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    private JList initResultList() {
        List<Member> members = RubyCache.getAllMembers();
        JList list = new JList(members.toArray());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if(!e.getValueIsAdjusting()) {
                    handleSelection();
                }
            }
        });
        return list;
    }

    private void handleSelection() {
        Member member = (Member)resultList.getSelectedValue();
        if(member != null) {
            String documentation = member.getDocumentation();
            documentationPane.setText(documentation);
            documentationPane.setCaretPosition(0);
        }
    }

    private static JSplitPane getPanel(String position, JPanel searchPanel, JScrollPane documentationPanel) {
        boolean useHorizontalLayout = position.equals(DockableWindowManager.TOP)
                || position.equals(DockableWindowManager.BOTTOM)
                || position.equals(DockableWindowManager.FLOATING);

        JSplitPane splitPane;
        if (useHorizontalLayout) {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, searchPanel, documentationPanel);
        } else {
            splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchPanel, documentationPanel);
        }
        return splitPane;
    }

}
