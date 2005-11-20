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
package org.jedit.ruby.structure;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Method;
import org.jedit.ruby.ast.MemberVisitorAdapter;
import org.jedit.ruby.*;

import javax.swing.*;
import javax.swing.border.Border;
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
public final class TypeAheadPopup extends JWindow {

    public static final PopupState FILE_STRUCTURE_POPUP = new FileStructureState();
    public static final PopupState SEARCH_POPUP = new SearchState();
    public static final PopupState FIND_DECLARATION_POPUP = new FindDeclarationState();

    static final int VISIBLE_LIST_SIZE = 15;
    static final String UP_TO_PARENT_TEXT = "..";

    private static final String NARROW_LIST_ON_TYPING = "rubyplugin.file-structure-popup.narrow-list-on-typing";
    private static final String SHOW_ALL = "rubyplugin.file-structure-popup.show-all";
    private static final int MAX_MISMATCHED_CHARACTERS = 3;
    private static final char BACKSPACE_KEY = (char)-1;
    private static final char ESCAPE_KEY = (char)-2;
    private static final int IGNORE = -2;
    private static final Border TOP_LINE_BORDER = new TopLineBorder();

    private Member toParentMember;
    private final Member[] members;
    private Member[] displayMembers;
    private final Member[] originalMembers;
    private final LinkedList<Member[]> parentsList;
    private final View view;
    private final JEditTextArea textArea;
    private TypeAheadPopupListCellRenderer cellRenderer;
    private final JList popupList;
    private String searchText;
    private final String searchPrefix;
    private JLabel searchLabel;
    private final Point position;
    private JCheckBox showAllCheckBox;
    private JCheckBox narrowListCheckBox;
    private final PopupState state;
    private boolean handleFocusOnDispose;
    private boolean narrowListOnTyping;
    private char narrowListMnemonic;
    private char showAllMnemonic;
    private int mismatchCharacters;
    private FocusAdapter textAreaFocusListener;

    public TypeAheadPopup(View view, Member[] members, Member selectedMember, PopupState state) {
        this(view, members, members, null, selectedMember, null, state);
    }

    private TypeAheadPopup(View editorView, Member[] originalMembers, Member[] displayMembers, LinkedList<Member[]> parentMembers, Member selectedMember, Point location, PopupState state) {
        super(editorView);
        RubyPlugin.log("selected is: " + String.valueOf(selectedMember), getClass());

        this.state = state;
        view = editorView;
        textArea = editorView.getTextArea();
        position = location;
        searchPrefix = " " + jEdit.getProperty("ruby.file-structure-popup.search.label");
        mismatchCharacters = 0;
        searchText = "";
        handleFocusOnDispose = true;

        narrowListOnTyping = jEdit.getBooleanProperty(NARROW_LIST_ON_TYPING, false);

        parentsList = parentMembers != null ? parentMembers : new LinkedList<Member[]>();
        this.originalMembers = originalMembers;
        members = getMembers(state, displayMembers);
        popupList = initPopupList(selectedMember);

        if (popupList != null) {
            setVisible(true);
        }
    }

    private Member[] getMembers(PopupState state, Member[] displayMembers) {
        if (state.showAllMembers()) {
            List<Member> memberList = new ArrayList<Member>();
            populateMemberList(displayMembers, memberList);
            return memberList.toArray(new Member[0]);
        } else {
            return displayMembers;
        }
    }

    public final void setVisible(boolean visible) {
        if (visible) {
            initContentPane();

            textAreaFocusListener = new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    handleFocusOnDispose = false;
                    dispose();
                }
            };

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

            TypeAheadPopupKeyHandler keyHandler = new TypeAheadPopupKeyHandler(this);
            addKeyListener(keyHandler);
            popupList.addKeyListener(keyHandler);
            view.setKeyEventInterceptor(keyHandler);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    textArea.requestFocus(); // fix for focus problems under Windows
                    textArea.addFocusListener(textAreaFocusListener);
                }
            });
        } else {
            super.setVisible(false);
        }
    }

    private void initContentPane() {
        searchLabel = new JLabel("");
        searchLabel.setOpaque(true);
        searchLabel.setBackground(Color.white);
        searchLabel.setBorder(TOP_LINE_BORDER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(initTopPanel(), BorderLayout.NORTH);
        panel.add(searchLabel, BorderLayout.SOUTH);
        JScrollPane scroller = new JScrollPane(popupList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        NonTraversablePanel contentPane = new NonTraversablePanel(new BorderLayout());
        contentPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setContentPane(contentPane);
        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().add(scroller);
    }

    private JPanel initTopPanel() {
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
        return topPanel;
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
                toParentMember = members[0].getParentMember();
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
                handleSelection(true);
            }
        });

        cellRenderer = new TypeAheadPopupListCellRenderer(toParentMember, state.showAllMembers());
        list.setCellRenderer(cellRenderer);
    }

    public final void dispose() {
        view.setKeyEventInterceptor(null);
        textArea.removeFocusListener(textAreaFocusListener);
        super.dispose();
        if (handleFocusOnDispose) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    view.getTextArea().requestFocusInWindow();
                }
            });
        }
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

    final void updateMatchedMembers(char typed) {
        if (typed == ESCAPE_KEY) {
            mismatchCharacters = 0;
        } else if (typed == BACKSPACE_KEY && mismatchCharacters > 0) {
            mismatchCharacters--;
        }

        String newSearchText = getNewSearchText(typed);
        setSearchText(newSearchText);
        List<Member> matches = getMatchingMembers(newSearchText);
        cellRenderer.resetAllHaveSameName(matches);

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
            } else if (member.getFullName().toLowerCase().startsWith(text)
                    || member.getName().toLowerCase().startsWith(text)) {
                visibleMembers.add(member);
            }
        }

        return visibleMembers;
    }

    final void handleSelection(boolean showMenu) {
        Member member = (Member)popupList.getSelectedValue();
        state.handleSelection(member, showMenu, this, view);
    }

    final boolean handleAltPressedWith(char keyChar) {
        boolean handled = false;

        if (keyChar == narrowListMnemonic) {
            narrowListCheckBox.setSelected(!narrowListOnTyping);
            setNarrowListOnTyping(!narrowListOnTyping);
            handled = true;
        } else if (keyChar == showAllMnemonic) {
            showAllCheckBox.setSelected(!state.showAllMembers());
            setShowAllMembers(!state.showAllMembers());
            handled = true;
        }

        return handled;
    }

    final void handleBackSpacePressed() {
        if (searchText.length() != 0) {
            updateMatchedMembers(BACKSPACE_KEY);
        }
    }

    final void handleEscapePressed() {
        if (searchText.length() > 0) {
            updateMatchedMembers(ESCAPE_KEY);
        } else {
            dispose();
        }
    }

    final int getListSize() {
        return popupList.getModel().getSize();
    }

    final void incrementSelection(int increment) {
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
    }

    private interface PopupState {
        void handleSelection(Member member, boolean showMenu, TypeAheadPopup popup, View view);

        boolean showAllMembers();

        boolean displayShowAllCheckBox();
    }

    private static final class FileStructureState implements PopupState {
        public final void handleSelection(Member member, boolean showMenu, TypeAheadPopup popup, View view) {
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

        public final boolean showAllMembers() {
            return jEdit.getBooleanProperty(SHOW_ALL, false);
        }

        public final boolean displayShowAllCheckBox() {
            return true;
        }
    }

    private static final class FindDeclarationState implements PopupState {

        public final void handleSelection(Member member, boolean showMenu, final TypeAheadPopup popup, final View view) {
            member.accept(new MemberVisitorAdapter() {
                public void handleMethod(Method method) {
                    String path = method.getFilePath();
                    Buffer buffer = jEdit.openFile(view, path);
                    popup.goToMember(method, buffer);
                }
            });
        }

        public final boolean showAllMembers() {
            return false;
        }

        public final boolean displayShowAllCheckBox() {
            return false;
        }
    }

    private static final class SearchState implements PopupState {
        public final void handleSelection(Member member, boolean showMenu, TypeAheadPopup popup, View view) {
            popup.dispose();
            org.jedit.ruby.ri.RDocSeacher.doSearch(view, member.getFullName());
        }

        public final boolean showAllMembers() {
            return false;
        }

        public final boolean displayShowAllCheckBox() {
            return false;
        }
    }

}
