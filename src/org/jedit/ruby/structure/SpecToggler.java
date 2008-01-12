/*
 * RubyActions.java - Actions for Ruby plugin
 *
 * Copyright 2008 Robert McKinnon
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

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

public class SpecToggler {

    public static final Map<File, File> SPEC_TO_CODE = new HashMap<File, File>();
    public static final Map<File, File> CODE_TO_SPEC = new HashMap<File, File>();

    private File currentFile;
    private View view;
    private String bufferDirectory;
    private String bufferName;

    public SpecToggler(View theView) {
        view = theView;
        Buffer buffer = view.getBuffer();
        bufferDirectory = buffer.getDirectory();
        bufferName = buffer.getName();
        currentFile = new File(bufferDirectory, buffer.getName());
    }

    public void toggleSpec() {
        if (isBufferSpecFile()) {
            toggleToCode();
        } else {
            toggleToSpec();
        }
    }

    private void toggleToCode() {
        if (SPEC_TO_CODE.containsKey(currentFile)) {
            openFile(SPEC_TO_CODE.get(currentFile));
        } else {
            String[] parts = bufferDirectory.split(File.separatorChar + "spec" + File.separatorChar);
            File baseDir = new File(parts[0]);
            String subPath = parts[1];
            String codeFileName = bufferName.replaceFirst("_spec\\.rb", ".rb");

            File code = tryToOpenFile(baseDir, subPath, codeFileName);
            if (code == null) {
                subPath = "app" + File.separatorChar + subPath;
                code = tryToOpenFile(baseDir, subPath, codeFileName);
                if (code == null) {
                    code = tryToOpenFile(baseDir, subPath, bufferName.replaceFirst("_spec\\.rb", ""));
                }
            }
            if (code != null) {
                SPEC_TO_CODE.put(currentFile, code);
                CODE_TO_SPEC.put(code, currentFile);
            }
        }
    }

    private void toggleToSpec() {
        if (CODE_TO_SPEC.containsKey(currentFile)) {
            openFile(CODE_TO_SPEC.get(currentFile));
        } else {
            File specDirectory = getSpecDirectory();
            if (specDirectory != null) {
                String subPath = bufferDirectory.replaceFirst(specDirectory.getParent(), "");
                String specFileName = bufferName.replaceFirst("\\.rb$","_spec.rb");

                File spec = tryToOpenFile(specDirectory, subPath, specFileName);
                if (spec == null) {
                    subPath = subPath.replaceFirst("^/app","");
                    spec = tryToOpenFile(specDirectory, subPath, specFileName);
                    if (spec == null) {
                        spec = tryToOpenFile(specDirectory, subPath, bufferName + "_spec.rb");
                    }
                }
                if (spec != null) {
                    SPEC_TO_CODE.put(spec, currentFile);
                    CODE_TO_SPEC.put(currentFile, spec);
                }
            }
        }
    }

    private File tryToOpenFile(File directory, String subPath, String fileName) {
        File file = new File(new File(directory.getPath(), subPath), fileName);

        if (file.exists() && file.isFile()) {
            openFile(file);
            return file;
        } else {
            return null;
        }
    }

    private void openFile(File file) {
        jEdit.openFile(view, file.getAbsolutePath());
    }

    private File getSpecDirectory() {
        File parent = new File(bufferDirectory);
        File specDir = null;
        while ((parent = parent.getParentFile()) != null) {
            String[] files = parent.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.equals("spec");
                }
            });
            if (files != null && files.length > 0) {
                specDir = new File(parent.getPath(), files[0]);
                break;
            }
        }
        return specDir;
    }

    private boolean isBufferSpecFile() {
        return bufferName.contains("_spec");
    }
}