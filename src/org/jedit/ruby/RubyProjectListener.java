/*
 * RubyProjectListener.java - 
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

import projectviewer.event.ProjectListener;
import projectviewer.event.ProjectEvent;
import projectviewer.vpt.VPTFile;

import java.util.List;
import java.util.ArrayList;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RubyProjectListener implements ProjectListener {

    public void fileAdded(ProjectEvent event) {
        VPTFile file = event.getAddedFile();
        String path = file.getNodePath();
        addFile(path);
    }

    public void filesAdded(ProjectEvent event) {
        List<VPTFile> files = new ArrayList<VPTFile>(event.getAddedFiles());
        for (VPTFile file : files) {
            String path = file.getNodePath();
            addFile(path);
        }
    }

    private void addFile(String path) {
        RubyPlugin.log("file added: " + path, getClass());
//        RubyProjectViewerListener.addFile(path);
    }

    public void fileRemoved(ProjectEvent event) {
        VPTFile file = event.getAddedFile();
        RubyPlugin.log("file removed: " + file.getNodePath(), getClass());
    }

    public void filesRemoved(ProjectEvent event) {
        List<VPTFile> files = new ArrayList<VPTFile>(event.getAddedFiles());
        for (VPTFile file : files) {
            RubyPlugin.log("file removed: " + file.getNodePath(), getClass());
        }
    }

    public void propertiesChanged(ProjectEvent event) {
        RubyPlugin.log("properties changed: " + event.getProject().getName(), getClass());
    }
}
