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
import org.gjt.sp.jedit.gui.KeyEventWorkaround;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;

/**
 * Altered version of Slava Pestov's
 * org.gjt.sp.jedit.gui.CompleteWord.
 *
 * @author robmckinnon at users,sourceforge,net
 */
public class TypeAheadPopup extends JWindow {

    private static final int VISIBLE_LIST_SIZE = 9;
    private static final int MAX_MISMATCHED_CHARACTERS = 3;
    private static final char BACKSPACE_KEY = (char)-1;
    private static final char ESCAPE_KEY = (char)-2;

    private View view;
    private JEditTextArea textArea;
    private JList popupList;
    private String validCharacters;
    private Member[] members;
    private String searchText;
    private String searchPrefix;
    private JLabel searchLabel;
    private int mismatchCharacters;

    public TypeAheadPopup(View editorView, Member[] fileMembers, Point location, String validChars) {
        super(editorView);
        searchPrefix = jEdit.getProperty("file-structure-popup.search.label");
        mismatchCharacters = 0;
        searchText = "";
        validCharacters = validChars;
        members = fileMembers;
        setContentPane(new NonTraversablePanel(new BorderLayout()));

        view = editorView;
        textArea = editorView.getTextArea();
        searchLabel = new JLabel("");
        popupList = initPopupList(fileMembers);

        // stupid scrollbar policy is an attempt to work around
        // bugs people have been seeing with IBM's JDK -- 7 Sep 2000
        JScrollPane scroller = new JScrollPane(popupList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        getContentPane().add(searchLabel, BorderLayout.NORTH);
        getContentPane().add(scroller);

        GUIUtilities.requestFocus(this, popupList);
        pack();
        setLocation(location);
        setVisible(true);

        TypeAheadPopup.KeyHandler keyHandler = new TypeAheadPopup.KeyHandler();
        addKeyListener(keyHandler);
        popupList.addKeyListener(keyHandler);
        editorView.setKeyEventInterceptor(keyHandler);
    }

    private JList initPopupList(Member[] members) {
        JList list = new JList(members);
        list.setVisibleRowCount(VISIBLE_LIST_SIZE);
        list.setSelectedIndex(0);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                moveToSelected();
            }
        });

        list.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);

                if (index < 9)
                    setText((index + 1) + ": " + value);
                else if (index == 9)
                    setText("0: " + value);
                else
                    setText(value.toString());

                return this;
            }
        });

        return list;
    }

    public void dispose() {
        view.setKeyEventInterceptor(null);
        super.dispose();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                textArea.requestFocus();
            }
        });
    }

    private void moveToSelected() {
        Member nember = (Member) popupList.getSelectedValue();
        int offset = nember.getOffset();
        view.goToBuffer(view.getBuffer());
        view.getTextArea().setCaretPosition(offset);
        dispose();
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
            popupList.setListData(matches.toArray());
            popupList.setSelectedIndex(0);
            popupList.setVisibleRowCount(VISIBLE_LIST_SIZE);
        }
    }

    private void setSearchText(String text) {
        if (mismatchCharacters < MAX_MISMATCHED_CHARACTERS) {
            searchText = text;

            if(searchText.length() == 0) {
                searchLabel.setText(searchText);
            } else {
                searchLabel.setText(searchPrefix + searchText);
            }
        }
    }

    private String getNewSearchText(char typed) {
        switch(typed) {
            case BACKSPACE_KEY:
                return searchText.substring(0, searchText.length() - 1);

            case ESCAPE_KEY:
                return "";

            default:
                return searchText + typed;
        }
    }

    private List<Member> getMatchingMembers(String text) {
        text = text.toLowerCase();
        List<Member> visibleMembers = new ArrayList<Member>();

        for (Member member : members) {
            if (member.getLowerCaseName().startsWith(text)) {
                visibleMembers.add(member);
            }
        }

        return visibleMembers;
    }

    class KeyHandler extends KeyAdapter {

        public void keyPressed(KeyEvent event) {
            switch (event.getKeyCode()) {
                case KeyEvent.VK_TAB:
                case KeyEvent.VK_ENTER:
                    handleSelection(event); break;

                case KeyEvent.VK_ESCAPE:
                    handleEscapePressed(event); break;

                case KeyEvent.VK_UP:
                    incrementSelection(event, -1); break;

                case KeyEvent.VK_DOWN:
                    incrementSelection(event, 1); break;

                case KeyEvent.VK_PAGE_UP:
                case KeyEvent.VK_LEFT:
                    incrementSelection(event, -1 * (VISIBLE_LIST_SIZE - 1)); break;

                case KeyEvent.VK_PAGE_DOWN:
                case KeyEvent.VK_RIGHT:
                    incrementSelection(event, (VISIBLE_LIST_SIZE - 1)); break;

                case KeyEvent.VK_HOME:
                    incrementSelection(event, -1 * getListSize()); break;

                case KeyEvent.VK_END:
                    incrementSelection(event, getListSize()); break;

                case KeyEvent.VK_BACK_SPACE:
                    handleBackSpacePressed(event); break;

                default:
                    if (event.isActionKey()
                            || event.isControlDown()
                            || event.isAltDown()
                            || event.isMetaDown()) {
                        dispose();
                        view.processKeyEvent(event);
                    }
                    break;
            }
        }

        public void keyTyped(KeyEvent event) {
            char character = event.getKeyChar();
            int keyCode = event.getKeyCode();
            event = KeyEventWorkaround.processKeyEvent(event);

            if (event != null && !ignoreKeyTyped(keyCode, character)) {
                if (Character.isDigit(character)) {
                    handleDigitTyped(character, event);

                } else {
                    handleCharacterTyped(character, event);
                }
            }
        }

        private void handleBackSpacePressed(KeyEvent event) {
            log("handle backspace pressed");
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
            } else if(selected < 0) {
                selected = 0;
            } else if (getFocusOwner() == popupList) {
                return;
            }

            popupList.setSelectedIndex(selected);
            popupList.ensureIndexIsVisible(selected);
            event.consume();
        }

        private boolean ignoreKeyTyped(int keyCode, char keyChar) {
            switch(keyCode) {
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
                    return true;
                default:
                    // for some reason have to match backspace and tab using keyChar
                    return keyChar == KeyEvent.VK_BACK_SPACE || keyChar == KeyEvent.VK_TAB;
            }
        }

        private void handleEscapePressed(KeyEvent event) {
            if (searchText.length() > 0) {
                updateMatchedMembers(ESCAPE_KEY);
            } else {
                dispose();
            }
            event.consume();
        }

        private void handleSelection(KeyEvent event) {
            moveToSelected();
            event.consume();
        }

        private void handleCharacterTyped(char character, KeyEvent event) {
            boolean valid = Character.isLetterOrDigit(character)
                    || validCharacters.indexOf(character) != -1;

            if (valid) {
                updateMatchedMembers(character);
            } else {
                handleSelection(event);
            }
            event.consume();
        }

        private void handleDigitTyped(char digit, KeyEvent event) {
            int index = digit - '0';
            if (index == 0) {
                index = 9;
            } else {
                index--;
            }
            if (index < getListSize()) {
                popupList.setSelectedIndex(index);
                handleSelection(event);
            }
        }

    }

    class NonTraversablePanel extends JPanel {
        public NonTraversablePanel(LayoutManager layout) {
            super(layout);
        }

        /**
         * Returns false to indicate this component can't
         * be traversed by pressing the Tab key.
         */
        public boolean isManagingFocus() {
            return false;
        }

        /**
         * Makes the tab key work in Java 1.4.
         */
        public boolean getFocusTraversalKeysEnabled() {
            return false;
        }
    }

    private void log(String msg) {
        Log.log(Log.DEBUG, this, msg);
    }

}
