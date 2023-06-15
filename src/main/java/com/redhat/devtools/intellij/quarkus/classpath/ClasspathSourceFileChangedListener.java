/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.quarkus.classpath;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Classpath resource changed listener used to track update of Java source and microprofile-config.properties files.
 */
class ClasspathSourceFileChangedListener extends PsiTreeChangeAdapter implements BulkFileListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathSourceFileChangedListener.class);
    private final Project project;

    private final SourceFileChangeNotifier sourcesChanged;

    ClasspathSourceFileChangedListener(Project project, SourceFileChangeNotifier sourcesChanged) {
        this.project = project;
        this.sourcesChanged = sourcesChanged;
    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
        handleChangedPsiTree(event);
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        handleChangedPsiTree(event);
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        handleChangedPsiTree(event);
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
        handleChangedPsiTree(event);
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        handleChangedPsiTree(event);
    }

    @Override
    public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
        handleChangedPsiTree(event);
    }

    private void handleChangedPsiTree(PsiTreeChangeEvent event) {
        PsiFile psiFile = event.getFile();
        if (psiFile == null) {
            return;
        }
        tryToAddSourceFile(psiFile.getVirtualFile(), true);
    }

    private void tryToAddSourceFile(VirtualFile file, boolean checkExistingFile) {
        if (checkExistingFile && (file == null || !file.exists())) {
            // The file doesn't exist
            return;
        }
        if (!isJavaFile(file, project) && !isConfigSource(file, project)) {
            return;
        }
        // The file is a Java file or microprofile-config.properties
        Module fileProject = LSPIJUtils.getProject(file);
        if (fileProject == null) {
            return;
        }
        // Debounce the notification that the file has changed
        sourcesChanged.addSourceFile(Pair.pair(file, fileProject));
    }

    @Override
    public void before(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            boolean expectedEvent = (event instanceof VFileDeleteEvent);
            if (expectedEvent) {
                // A file has been deleted
                // We need to track delete event in 'before' method because we need the project of the file (in after we loose this information).
                tryToAddSourceFile(event.getFile(), false);
            }
        }
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            boolean expectedEvent = (event instanceof VFileCreateEvent || event instanceof VFileContentChangeEvent);
            if (expectedEvent) {
                // A file has been created, updated
                tryToAddSourceFile(event.getFile(), false);
            }
        }
    }

    private static boolean isJavaFile(VirtualFile file, Project project) {
        return PsiMicroProfileProjectManager.getInstance(project).isJavaFile(file);
    }

    private static boolean isConfigSource(VirtualFile file, Project project) {
        return PsiMicroProfileProjectManager.getInstance(project).isConfigSource(file);
    }
}
