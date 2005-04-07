/*
 * RubyProjectViewerListener.java - 
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

import projectviewer.event.ProjectViewerListener;
import projectviewer.event.ProjectViewerEvent;
import projectviewer.vpt.VPTProject;
import projectviewer.vpt.VPTGroup;
import projectviewer.vpt.VPTNode;
import org.gjt.sp.util.Log;
import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;

import java.util.Collection;
import java.util.Iterator;
import java.io.*;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RubyProjectViewerListener implements ProjectViewerListener {

    public void projectLoaded(ProjectViewerEvent event) {
        VPTProject project = event.getProject();
        RubyPlugin.log("project loaded: " + project.getName());
//            project.addProjectListener(new RubyProjectListener());
        reparse(project);
    }

    public void projectAdded(ProjectViewerEvent event) {
        RubyPlugin.log("project added: " + event.getProject().getName());
        reparse(event.getProject());
    }

    public void projectRemoved(ProjectViewerEvent event) {
        RubyPlugin.log("project removed: " + event.getProject().getName());
        RubyCache.clear();
        reparse(event.getProject());
    }

    public void groupAdded(ProjectViewerEvent event) {
        VPTGroup group = (VPTGroup) event.getSource();
        RubyPlugin.log("group added: " + group);
        reparse(event.getProject());
    }

    public void groupRemoved(ProjectViewerEvent event) {
        VPTGroup group = (VPTGroup) event.getSource();
        RubyPlugin.log("group removed: " + group);
        RubyCache.clear();
        reparse(event.getProject());
    }

    public void groupActivated(ProjectViewerEvent event) {
        VPTGroup group = (VPTGroup) event.getSource();
        RubyPlugin.log("group activated: " + group);
    }

    public void nodeMoved(ProjectViewerEvent event) {
        VPTNode node = (VPTNode) event.getSource();
        RubyPlugin.log("node moved: " + node);
    }

    public void reparse(VPTProject project) {
        if (project != null) {
            Collection openableNodes = project.getOpenableNodes();
            RubyPlugin.log("parsing " + openableNodes.size() + " project files: " + project.getName());
            for (Iterator iterator = openableNodes.iterator(); iterator.hasNext();) {
                VPTNode node = (VPTNode) iterator.next();
                RubyPlugin.log("node: " + node.getNodePath());
            }

            for (Iterator iterator = openableNodes.iterator(); iterator.hasNext();) {
                VPTNode node = (VPTNode) iterator.next();
                String path = node.getNodePath();
                RubyPlugin.log("parsing: " + path);
                try {
                    addFile(path);
                } catch (Exception e) {
                    RubyPlugin.error(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void addFile(String path) {
        Mode rubyMode = jEdit.getMode("ruby");
        File file = new File(path);
        if (file.isFile() && rubyMode.accept(path, "")) {
            String text = getText(path, file);
            if (text != null) {
                RubyCache.add(text, path);
            }
        }
    }

    private String getText(String path, File file) {
        String text = null;
//        Buffer buffer = jEdit.getBuffer(path);
//        if (buffer == null) {
            text = readFile(file);
//        } else {
//            text = buffer.getText(0, buffer.getLength());
//        }
        return text;
    }

    private String readFile(File file) {
        StringBuffer buffer = new StringBuffer();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            try {
                char[] chars = new char[1024];
                int length;
                while (-1 != (length = bufferedReader.read(chars))) {
                    buffer.append(chars, 0, length);
                }
            } catch (IOException e) {
                RubyPlugin.error(e.getMessage());
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            RubyPlugin.error(e.getMessage());
            e.printStackTrace();
        }

        return buffer.toString();
    }
}
