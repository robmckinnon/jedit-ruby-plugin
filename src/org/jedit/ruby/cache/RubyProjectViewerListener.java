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
package org.jedit.ruby.cache;

import projectviewer.event.ProjectViewerListener;
import projectviewer.event.ProjectViewerEvent;
import projectviewer.vpt.VPTProject;
import projectviewer.vpt.VPTGroup;
import projectviewer.vpt.VPTNode;

import java.util.Collection;
import java.io.*;

import org.jedit.ruby.RubyPlugin;

/**
 * @author robmckinnon at users.sourceforge.net
 */
final class RubyProjectViewerListener implements ProjectViewerListener {

    public final void projectLoaded(ProjectViewerEvent event) {
        VPTProject project = event.getProject();
        RubyPlugin.log("project loaded: " + project.getName(), getClass());
        reparse(project);
    }

    public final void projectAdded(ProjectViewerEvent event) {
        VPTProject project = event.getProject();
        RubyPlugin.log("project added: " + project.getName(), getClass());
        reparse(event.getProject());
    }

    public final void projectRemoved(ProjectViewerEvent event) {
        VPTProject project = event.getProject();
        RubyPlugin.log("project removed: " + project.getName(), getClass());
    }

    public final void groupAdded(ProjectViewerEvent event) {
        VPTGroup group = (VPTGroup) event.getSource();
        RubyPlugin.log("group added: " + group, getClass());
        reparse(event.getProject());
    }

    public final void groupRemoved(ProjectViewerEvent event) {
        VPTGroup group = (VPTGroup) event.getSource();
        RubyPlugin.log("group removed: " + group, getClass());
        RubyCache.resetCache();
        reparse(event.getProject());
    }

    public final void groupActivated(ProjectViewerEvent event) {
        VPTGroup group = (VPTGroup) event.getSource();
        RubyPlugin.log("group activated: " + group, getClass());
    }

    public final void nodeMoved(ProjectViewerEvent event) {
        VPTNode node = (VPTNode) event.getSource();
        RubyPlugin.log("node moved: " + node, getClass());
    }

    private void reparse(VPTProject project) {
        if (project != null) {
            Collection openableNodes = project.getOpenableNodes();
            RubyPlugin.log("parsing " + openableNodes.size() + " project files: " + project.getName(), getClass());

            for (Object openableNode : openableNodes) {
                VPTNode node = (VPTNode) openableNode;
                String path = node.getNodePath();
                RubyPlugin.log("parsing: " + path, getClass());
                try {
                    addFile(path);
                } catch (Exception e) {
                    RubyPlugin.error(e.getMessage(), getClass());
                    e.printStackTrace();
                }
            }
        }
    }

    private static void addFile(String path) {
        File file = new File(path);

        if (RubyPlugin.isRubyFile(file)) {
            String text = RubyPlugin.readFile(file);
            if (text != null) {
                RubyCache.instance().addMembers(text, path);
            }
        }
    }

}
