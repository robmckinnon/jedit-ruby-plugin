/*
 * TypeAheadPopup.java - Type ahead popup
 *
 * Copyright 2005 Robert McKinnon
 *           2000, 2001 Slava Pestov
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

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Method;
import org.jedit.ruby.ast.MemberVisitorAdapter;
import org.jedit.ruby.icons.MemberIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Majorly altered version of Slava Pestov's
 * org.gjt.sp.jedit.gui.CompleteWord.
 *
 * @author robmckinnon at users,sourceforge,net
 */
public class TypeAheadPopup extends JWindow {

    public static final PopupState FILE_STRUCTURE_POPUP = new FileStructureState();
    public static final PopupState SEARCH_POPUP = new SearchState();
    public static final PopupState FIND_DECLARATION_POPUP = new FindDeclarationState();

    private static final String NARROW_LIST_ON_TYPING = "rubyplugin.file-structure-popup.narrow-list-on-typing";
    private static final String SHOW_ALL = "rubyplugin.file-structure-popup.show-all";
    private static final String UP_TO_PARENT_TEXT = "..";
    private static final int VISIBLE_LIST_SIZE = 15;
    private static final int MAX_MISMATCHED_CHARACTERS = 3;
    private static final char BACKSPACE_KEY = (char)-1;
    private static final char ESCAPE_KEY = (char)-2;
    private static final int IGNORE = -2;
    private static final Border TOP_LINE_BORDER = new TopLineBorder(Color.GRAY);

    private Member toParentMember;
    private Member[] members;
    private Member[] displayMembers;
    private Member[] originalMembers;
    private LinkedList<Member[]> parentsList;
    private View view;
    private JEditTextArea textArea;
    private JList popupList;
    private String validCharacters;
    private String searchText;
    private String searchPrefix;
    private JLabel searchLabel;
    private Point position;
    private int mismatchCharacters;
    private boolean handleFocusOnDispose;
    private boolean narrowListOnTyping;
    private char narrowListMnemonic;
    private char showAllMnemonic;
    private JCheckBox showAllCheckBox;
    private JCheckBox narrowListCheckBox;
    private boolean allHaveSameName;
    private PopupState state;

    public TypeAheadPopup(View view, Member[] members, Member selectedMember, PopupState state) {
        this(view, members, members, null, selectedMember, null, state);
    }

    private TypeAheadPopup(View editorView, Member[] originalMembers, Member[] displayMembers, LinkedList<Member[]> parentMembers, Member selectedMember, Point location, PopupState state) {
        super(editorView);
        RubyPlugin.log("selected is: " + String.valueOf(selectedMember), getClass());

        this.state = state;
        view = editorView;
        textArea = editorView.getTextArea();

        if (parentMembers != null) {
            parentsList = parentMembers;
        } else {
            parentsList = new LinkedList<Member[]>();
        }

        position = location;
        searchPrefix = " " + jEdit.getProperty("ruby.file-structure-popup.search.label");
        mismatchCharacters = 0;
        searchText = "";
        validCharacters = "_(),.[]";
        handleFocusOnDispose = true;

        narrowListOnTyping = jEdit.getBooleanProperty(NARROW_LIST_ON_TYPING, false);

        this.originalMembers = originalMembers;
        if (state.showAllMembers()) {
            members = getExpandedMembers(displayMembers);
        } else {
            members = displayMembers;
        }
        popupList = initPopupList(selectedMember);

        if (popupList != null) {
            setVisible(true);
        }
    }

    private Member[] getExpandedMembers(Member[] members) {
        List<Member> memberList = new ArrayList<Member>();
        populateMemberList(members, memberList);
        return memberList.toArray(new Member[0]);
    }

    public void setVisible(boolean visible) {
        if (visible) {
            searchLabel = new JLabel("");
            searchLabel.setOpaque(true);
            searchLabel.setBackground(Color.white);
            searchLabel.setBorder(TOP_LINE_BORDER);

            JPanel topPanel;

            if (state.displayShowAllCheckBox()) {
                String key = "ruby.file-structure-popup.show-all";
                showAllMnemonic = jEdit.getProperty(key + ".mnemonic").charAt(0);
                showAllCheckBox = initCheckBox(key, state.showAllMembers(), showAllMnemonic, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setShowAllMembers(((JCheckBox)e.getSource()).isSelected());
                    }
                });
                topPanel = new JPanel(new GridLayout(2, 1));
                topPanel.add(showAllCheckBox);
            } else {
                topPanel = new JPanel(new GridLayout(1, 1));
            }

            String key = "ruby.file-structure-popup.narrow-search";
            narrowListMnemonic = jEdit.getProperty(key + ".mnemonic").charAt(0);
            narrowListCheckBox = initCheckBox(key, narrowListOnTyping, narrowListMnemonic, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setNarrowListOnTyping(((JCheckBox)e.getSource()).isSelected());
                }
            });
            topPanel.add(narrowListCheckBox);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(topPanel, BorderLayout.NORTH);
            panel.add(searchLabel, BorderLayout.SOUTH);
            JScrollPane scroller = new JScrollPane(popupList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            NonTraversablePanel contentPane = new NonTraversablePanel(new BorderLayout());
            contentPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            setContentPane(contentPane);
            getContentPane().add(panel, BorderLayout.NORTH);
            getContentPane().add(scroller);

            putFocusOnPopup();
            pack();

            if (position == null) {
                setLocationRelativeTo(view);
            } else {
                setLocation(position);
            }
            super.setVisible(true);
            Object selectedValue = popupList.getSelectedValue();
            popupList.setSelectedIndex(0);
            popupList.setSelectedValue(selectedValue, true);

            KeyHandler keyHandler = new KeyHandler();
            addKeyListener(keyHandler);
            popupList.addKeyListener(keyHandler);
            view.setKeyEventInterceptor(keyHandler);
        } else {
            super.setVisible(false);
        }
    }

    private JCheckBox initCheckBox(String resourceKey, boolean selected, char mnemonic, ActionListener listener) {
        String label = jEdit.getProperty(resourceKey + ".label");
        final JCheckBox checkBox = new JCheckBox(label, selected);
        checkBox.setMnemonic(mnemonic);
        checkBox.setFocusable(false);
        checkBox.addActionListener(listener);
        return checkBox;
    }

    private void setShowAllMembers(boolean selected) {
        jEdit.setProperty(SHOW_ALL, Boolean.toString(selected));
        dispose();
        new TypeAheadPopup(view, originalMembers, originalMembers, null, (Member)popupList.getSelectedValue(), position, state);
    }

    private void putFocusOnPopup() {
        GUIUtilities.requestFocus(this, popupList);
    }

    private JList initPopupList(Member selectedMember) {
        int selectedIndex = getSelectedIndex(selectedMember, members);

        if (selectedIndex >= 0) {
            boolean parentLinkAtTop = selectedMember != null
                    && selectedMember.hasParentMember()
                    && !state.showAllMembers()
                    && state == FILE_STRUCTURE_POPUP;
            if (parentLinkAtTop) {
                selectedIndex++;
            }
            return initPopupList(selectedIndex);
        } else if (selectedIndex == IGNORE) {
            return null;
        } else {
            RubyPlugin.error("couldn't find selected member " + selectedMember.getName(), getClass());
            return null;
        }
    }

    private int getSelectedIndex(Member selectedMember, Member[] members) {
        int selectedIndex = 0;

        if (selectedMember != null) {
            List<Member> memberList = Arrays.asList(members);

            if (memberList.contains(selectedMember)) {
                selectedIndex = memberList.indexOf(selectedMember);
            } else {
                List<Member> memberPath = selectedMember.getMemberPath();
                for (Member member : memberPath) {
                    if (memberList.contains(member)) {
                        Member[] childMembers = member.getChildMembers();
                        handleFocusOnDispose = false;
                        dispose();
                        parentsList.add(members);
                        new TypeAheadPopup(view, originalMembers, childMembers, parentsList, selectedMember, position, state);
                        selectedIndex = IGNORE;
                        break;
                    }
                }
            }
        }

        return selectedIndex;
    }

    private JList initPopupList(int index) {
        displayMembers = initDisplayMembers(members, parentsList);
        JList list = new JList(displayMembers);
        configureList(list);
        list.setSelectedValue(displayMembers[index], true);
        return list;
    }

    private Member[] initDisplayMembers(Member[] members, LinkedList<Member[]> parentsList) {
        if (!state.showAllMembers()) {
            if (parentsList.size() > 0) {
                Member parentMember = members[0].getParentMember();
                toParentMember = parentMember;
                Member[] memberArray = new Member[members.length + 1];
                memberArray[0] = toParentMember;
                int index = 1;
                for (Member member : members) {
                    memberArray[index++] = member;
                }
                members = memberArray;
            }
        }
        return members;
    }

    private void populateMemberList(Member[] members, List<Member> memberList) {
        for (Member member : members) {
            memberList.add(member);
            if (member.hasChildMembers()) {
                populateMemberList(member.getChildMembers(), memberList);
            }
        }
    }

    private void configureList(JList list) {
        list.setVisibleRowCount(VISIBLE_LIST_SIZE);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                handleSelection((Member)popupList.getSelectedValue(), true);
            }
        });

        list.setCellRenderer(new PopupListCellRenderer());
    }

    public void dispose() {
        view.setKeyEventInterceptor(null);
        super.dispose();
        if (handleFocusOnDispose) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    textArea.requestFocus();
                }
            });
        }
    }

    private void handleSelection(Member member, boolean showMenu) {
        state.handleSelection(member, showMenu, this, view);
    }

    private void goToMember(Member member, Buffer buffer) {
        dispose();
        int offset = member.getStartOffset();
        RubyPlugin.log(member + ": " + offset, getClass());
        view.goToBuffer(buffer);
        if(offset > buffer.getLength()) {
            offset = buffer.getLength();
        }
        JEditTextArea textArea = view.getTextArea();
        textArea.setCaretPosition(offset);
    }

    private void updateMatchedMembers(char typed) {
        if (typed == ESCAPE_KEY) {
            mismatchCharacters = 0;
        } else if (typed == BACKSPACE_KEY && mismatchCharacters > 0) {
            mismatchCharacters--;
        }

        String newSearchText = getNewSearchText(typed);
        setSearchText(newSearchText);
        List<Member> matches = getMatchingMembers(newSearchText);

        if (matches.size() == 0) {
            searchLabel.setForeground(Color.red);
            if (typed != BACKSPACE_KEY && mismatchCharacters < MAX_MISMATCHED_CHARACTERS) {
                mismatchCharacters++;
            }
        } else {
            searchLabel.setForeground(Color.black);
            if (narrowListOnTyping) {
                Member[] membersToShow = matches.toArray(new Member[matches.size()]);
                resetList(membersToShow, membersToShow[0]);
            } else {
                resetList(displayMembers, matches.get(0));
            }
        }
    }

    private void resetList(Member[] membersToShow, Member selectedMember) {
        popupList.setListData(membersToShow);
        if (searchText.length() > 0) {
            popupList.setVisibleRowCount(VISIBLE_LIST_SIZE + 1);
        } else {
            popupList.setVisibleRowCount(VISIBLE_LIST_SIZE);
        }
        popupList.setSelectedValue(selectedMember, true);
    }

    private void setSearchText(String text) {
        if (mismatchCharacters < MAX_MISMATCHED_CHARACTERS) {
            searchText = text;

            if (searchText.length() == 0) {
                searchLabel.setText(searchText);
            } else {
                searchLabel.setText(searchPrefix + searchText);
            }
        }
    }

    private String getNewSearchText(char typed) {
        switch (typed) {
            case BACKSPACE_KEY:
                return searchText.substring(0, searchText.length() - 1);

            case ESCAPE_KEY:
                return "";

            default:
                return searchText + typed;
        }
    }

    private void setNarrowListOnTyping(boolean narrow) {
        narrowListOnTyping = narrow;
        jEdit.setProperty(NARROW_LIST_ON_TYPING, Boolean.toString(narrow));
        if (searchText.length() > 0) {
            updateMatchedMembers(' '); // naive refresh
            updateMatchedMembers(BACKSPACE_KEY);
        }
    }

    private List<Member> getMatchingMembers(String text) {
        text = text.toLowerCase();
        List<Member> visibleMembers = new ArrayList<Member>();

        for (Member member : displayMembers) {
            if (member == toParentMember) {
                if (UP_TO_PARENT_TEXT.startsWith(text)) {
                    visibleMembers.add(member);
                }
            } else if (member.getFullName().toLowerCase().startsWith(text) ||
                    member.getName().toLowerCase().startsWith(text)) {
                visibleMembers.add(member);
            }
        }

        allHaveSameName = visibleMembers.size() > 1;
        String name = null;
        for (Member member : visibleMembers) {
            if (name != null) {
                allHaveSameName = allHaveSameName && name.equals(member.getFullName());
            }
            name = member.getFullName();
        }

        return visibleMembers;
    }

    private static class TopLineBorder extends LineBorder {
        public TopLineBorder(Color color) {
            super(color);
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Color oldColor = g.getColor();
            int i;
            g.setColor(lineColor);
            for (i = 0; i < thickness; i++) {
                int newWidth = width - i - i - 1;
                int x1 = x + i;
                int y1 = y + i;
                int x2 = x1 + newWidth;
                g.drawLine(x1, y1, x2, y1);
            }
            g.setColor(oldColor);
        }
    }

    private class KeyHandler extends KeyAdapter {

        public void keyPressed(KeyEvent event) {
            switch (event.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    handleEscapePressed(event);
                    break;

                case KeyEvent.VK_UP:
                    incrementSelection(event, -1);
                    break;

                case KeyEvent.VK_DOWN:
                    incrementSelection(event, 1);
                    break;

                case KeyEvent.VK_PAGE_UP:
                case KeyEvent.VK_LEFT:
                    incrementSelection(event, -1 * (VISIBLE_LIST_SIZE - 1));
                    break;

                case KeyEvent.VK_PAGE_DOWN:
                case KeyEvent.VK_RIGHT:
                    incrementSelection(event, (VISIBLE_LIST_SIZE - 1));
                    break;

                case KeyEvent.VK_HOME:
                    incrementSelection(event, -1 * getListSize());
                    break;

                case KeyEvent.VK_END:
                    incrementSelection(event, getListSize());
                    break;

                case KeyEvent.VK_BACK_SPACE:
                    handleBackSpacePressed(event);
                    break;

                case KeyEvent.VK_F4:
                    handleSelection(event, false);

                default:
                    if (event.isAltDown() || event.isMetaDown()) {
                        char keyChar = event.getKeyChar();
                        if (keyChar == narrowListMnemonic) {
                            narrowListCheckBox.setSelected(!narrowListOnTyping);
                            setNarrowListOnTyping(!narrowListOnTyping);
                            event.consume();
                        } else if (keyChar == showAllMnemonic) {
                            showAllCheckBox.setSelected(!state.showAllMembers());
                            setShowAllMembers(!state.showAllMembers());
                            event.consume();
                        }
                    }
                    break;
            }
        }

        public void keyTyped(KeyEvent event) {
            char character = event.getKeyChar();
            int keyCode = event.getKeyCode();
            int keyChar = event.getKeyChar();
            if (keyCode == KeyEvent.VK_TAB || keyCode == KeyEvent.VK_ENTER ||
                    keyChar == KeyEvent.VK_TAB || keyChar == KeyEvent.VK_ENTER) {
                handleSelection(event, true);

            } else {
                if (event != null && !ignoreKeyTyped(keyCode, character, event)) {
                    handleCharacterTyped(character, event);
                }
            }
        }

        private void handleBackSpacePressed(KeyEvent event) {
            if (searchText.length() != 0) {
                updateMatchedMembers(BACKSPACE_KEY);
            }
            event.consume();
        }

        private int getListSize() {
            return popupList.getModel().getSize();
        }

        private void incrementSelection(KeyEvent event, int increment) {
            int selected = popupList.getSelectedIndex();
            selected += increment;

            int size = getListSize();

            if (selected >= size) {
                selected = size - 1;
            } else if (selected < 0) {
                selected = 0;
            } else if (getFocusOwner() == popupList) {
                return;
            }

            popupList.setSelectedIndex(selected);
            popupList.ensureIndexIsVisible(selected);
            event.consume();
        }

        private boolean ignoreKeyTyped(int keyCode, char keyChar, KeyEvent event) {
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

        private void handleEscapePressed(KeyEvent event) {
            if (searchText.length() > 0) {
                updateMatchedMembers(ESCAPE_KEY);
            } else {
                dispose();
            }
            event.consume();
        }

        private void handleSelection(KeyEvent event, boolean showMenu) {
            event.consume();
            Member member = (Member)TypeAheadPopup.this.popupList.getSelectedValue();
            TypeAheadPopup.this.handleSelection(member, showMenu);
        }

        private void handleCharacterTyped(char character, KeyEvent event) {
            boolean valid = Character.isLetterOrDigit(character)
                    || validCharacters.indexOf(character) != -1;

            if (valid) {
                updateMatchedMembers(character);
                event.consume();
            }
        }
    }

    private class PopupListCellRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);

            Member member = (Member)value;
            StringBuffer buffer = new StringBuffer();

            if (member == toParentMember) {
                buffer.append("[ " + UP_TO_PARENT_TEXT + " ]");
            } else {
                if (state.showAllMembers()) {
                    for (int i = 0; i < member.getParentCount(); i++) {
                        buffer.append("  ");
                    }
                    buffer.append(member.getFullName());
                } else {
                    buffer.append(member.getFullName());
                }

                if (allHaveSameName && member.hasParentMember()) {
                    buffer.append(" (" + member.getParentMember().getFullName() + ")");
                }
            }

            setText(buffer.toString());
            MemberIcon memberIcon = new MemberIcon(member);
            setIcon(memberIcon.getIcon());
            return this;
        }
    }

    private interface PopupState {
        void handleSelection(Member member, boolean showMenu, TypeAheadPopup popup, View view);

        boolean showAllMembers();

        boolean displayShowAllCheckBox();
    }

    private static class FileStructureState implements PopupState {
        public void handleSelection(Member member, boolean showMenu, TypeAheadPopup popup, View view) {
            if (member == popup.toParentMember && showMenu) {
                popup.handleFocusOnDispose = false;
                popup.dispose();
                Member[] members = popup.parentsList.removeLast();
                new TypeAheadPopup(view, popup.originalMembers, members, popup.parentsList, member, popup.position, popup.state);

            } else if (member.hasChildMembers() && showMenu && !showAllMembers()) {
                Member[] childMembers = member.getChildMembers();
                popup.handleFocusOnDispose = false;
                popup.dispose();
                popup.parentsList.add(popup.members);
                new TypeAheadPopup(view, popup.originalMembers, childMembers, popup.parentsList, null, popup.position, popup.state);

            } else {
                Buffer buffer = view.getBuffer();
                popup.goToMember(member, buffer);
            }
        }

        public boolean showAllMembers() {
            return jEdit.getBooleanProperty(SHOW_ALL, false);
        }

        public boolean displayShowAllCheckBox() {
            return true;
        }
    }

    private static class FindDeclarationState implements PopupState {
        public void handleSelection(Member member, boolean showMenu, final TypeAheadPopup popup, final View view) {
            member.accept(new MemberVisitorAdapter() {
                public void handleMethod(Method method) {
                    String path = method.getFilePath();
                    Buffer buffer = jEdit.openFile(view, path);
                    popup.goToMember(method, buffer);
                }
            });
        }

        public boolean showAllMembers() {
            return false;
        }

        public boolean displayShowAllCheckBox() {
            return false;
        }
    }

    private static class SearchState implements PopupState {
        public void handleSelection(Member member, boolean showMenu, TypeAheadPopup popup, View view) {
            popup.dispose();
            RDocSeacher.doSearch(view, member.getName());
        }

        public boolean showAllMembers() {
            return false;
        }

        public boolean displayShowAllCheckBox() {
            return false;
        }
    }

}
