/*
 * CodeCompletor.java - 
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

import javax.swing.JMenuItem;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import java.awt.event.ActionEvent;
import java.awt.Point;
import java.util.*;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.search.RESearchMatcher;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import gnu.regexp.REMatch;
import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatchEnumeration;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class CodeCompletor {

    private JEditTextArea textArea;
    private Buffer buffer;
    private CodeAnalyzer analyzer;

    public CodeCompletor(View view) {
        textArea = view.getTextArea();
        buffer = view.getBuffer();
        analyzer = new CodeAnalyzer(textArea, buffer);
    }

    public List<Member.Method> getMethods() {
        List<Member.Method> methods = getMethods(buffer.getText(0, buffer.getLength()), textArea.getCaretPosition());
        return methods;
    }

    public String getRestOfLine() {
        return analyzer.getRestOfLine();
    }

    public String getPartialMethod() {
        return analyzer.getPartialMethod();
    }

    public boolean isInsertionPoint() {
        return analyzer.isInsertionPoint();
    }

    public void completeRubyMethod() {
        List<Member.Method> methods = getMethods();
        List<JMenuItem> menuItems = createMenuItems(methods);
        showPopupMenu(menuItems);
    }

    /**
     * Prints list of methods possibly appropriate at the given location.
     */
    public List<Member.Method> getMethods(String text, int location) {
        if (analyzer.getName() != null) {
            String className = analyzer.getClassName();

            if (className != null) {
                log(className);
                return RubyCache.getMethodsOfMember(className);
            } else {
                return completeUsingMethods(analyzer.getMethods());
            }
        } else {
            return new ArrayList<Member.Method>();
        }
    }

    private List<Member.Method> completeUsingMethods(List<String> methods) {
        List<Member> members = null;

        for (String method : methods) {
            List<Member> classes = RubyCache.getMembersWithMethod(method);
            if(members != null) {
                intersection(members, classes);
            } else {
                members = classes;
            }
        }

        List<Member.Method> results = new ArrayList<Member.Method>();
        for (Member member : members) {
            results.addAll(RubyCache.getMethodsOfMember(member.getFullName()));
        }
//        boolean is_class = analyzer.isClass();
//        String partial = analyzer.getPartialMethod();
//        filter_results(results.to_a, partial_method, isClass);
        return results;
    }

                        /**
     * Creates a new JMenuItem.
     */
    private JMenuItem createMenuItem(String text, Action action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(action);
        return item;
    }

    /**
     * Shows popup menu.
     */
    private void showPopupMenu(List<JMenuItem> menuItems) {
        if(menuItems.size() > 0) {
            JPopupMenu menu = new JPopupMenu();
            for (JMenuItem menuItem : menuItems) {
                menu.add(menuItem);
            }
            Point point = textArea.offsetToXY(textArea.getCaretPosition());
            GUIUtilities.showPopupMenu(menu, textArea, point.x, point.y);

            if(OperatingSystem.isWindows()) {
                menu.requestFocus();
            }
        }
    }

    private int getLineStartIndex() {
        int lineIndex = textArea.getCaretLine();
        int start = textArea.getLineStartOffset(lineIndex);
        return start;
    }

    private String getLineUpToCaret() {
        int start = getLineStartIndex();
        int end = textArea.getCaretPosition();
        String line = textArea.getText(start, end - start);
        return line;
    }

    /**
     * Returns array of regular expression matches.
     */
    private REMatch[] getAllMatches(String expression, String text) {
        try {
            RE re = new RE(expression, 0, RESearchMatcher.RE_SYNTAX_JEDIT);
            return re.getAllMatches(text);
        } catch (REException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns enumeration of regular expression matches.
     */
    private REMatchEnumeration getMatchesEnumeration(String expression, String text) {
        try {
            RE re = new RE(expression, 0, RESearchMatcher.RE_SYNTAX_JEDIT);
            return re.getMatchEnumeration(text);
        } catch (REException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getVariableName() {
        REMatch[] matches = getAllMatches("(\\S+)\\.", getLineUpToCaret());

        if (matches.length > 0) {
            REMatch match = matches[matches.length - 1];
            String variable = match.toString(1);
//    Macros.message(textArea, variable);
            return variable;
        } else {
            return null;
        }
    }

    private Set<String> getVariableMethodIterator() {
        String variable = getVariableName();
        Set<String> methods = new HashSet<String>();
        if (variable != null) {
            String text = textArea.getText(0, getLineStartIndex());
            REMatchEnumeration matches = getMatchesEnumeration("(.*)(" + variable + ")(\\.)(\\w+)", text);

            while (matches.hasMoreMatches()) {
                REMatch match = matches.nextMatch();
                String openingText = match.toString(1);
                boolean comment = (openingText.trim().startsWith("#"));

                if (!comment) {
                    String method = match.toString(4);
                    methods.add(method);
//                Macros.message(textArea, method);
                }
            }
        }

        return methods;
    }

/* runRi() {
  return getCommandOutput("ri " + searchTerm + " -sf Tagged");
    ri Array -sf Tagged
}
 */

    List<Member> getClassesFor(String methodName) {
        return RubyCache.getMembersWithMethod(methodName);
    }

    List<JMenuItem> createMenuItems(List<Member.Method> methods) {
        List<JMenuItem> items = new ArrayList<JMenuItem>();

        for (Member.Method method : methods) {
            // Macros.message(view, method.toString());

//            final JEditTextArea area = textArea;
//            final String name = method;
            // Macros.message(view, method.toString() + "2");

            JMenuItem item = new JMenuItem(method.getName());
            // Macros.message(view, method.toString() + "3");
            Action action = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    // String value;
                    // if(name.charAt(0) == ':') {
                    // value = name.substring(2);
                    // } else {
                    // value = name.substring(1);
                    // }
                    // area.setSelectedText(value);
                }
            };

            // Macros.message(view, method.toString() + "4");
            item.addActionListener(action);
            // Macros.message(view, method.toString() + "5");

            items.add(item);
        }

        Collections.sort(items, new Comparator<JMenuItem>() {
            public int compare(JMenuItem anItem, JMenuItem otherItem) {
                return anItem.getText().compareTo(otherItem.getText());
            }
        });

        return items;
    }

    Set intersection(Set set, Set otherSet) {
        Set intersection = new TreeSet();

        if (!set.isEmpty()) {
            intersection.addAll(set);
        }

        if (!intersection.isEmpty()) {
            intersection.retainAll(otherSet);
        }

        return intersection;
    }

    List<Member> intersection(List<Member> list, List<Member> otherList) {
        List<Member> intersection = new ArrayList<Member>();

        if (!list.isEmpty()) {
            intersection.addAll(list);
        }

        if (!intersection.isEmpty()) {
            intersection.retainAll(otherList);
        }

        return intersection;
    }

    private void log(String message) {
//        Macros.message(textArea, message);
    }

// try {
    // view.showWaitCursor();
    //completeRubyMethod();
// } catch(Exception e) {
    // Macros.message(view, e.getMessage());
// } finally {
    // view.hideWaitCursor();
// }

//values = getClassesFor("to_s");
//for(it = values.iterator(); it.hasNext(); Macros.message(textArea, it.next()));}
}