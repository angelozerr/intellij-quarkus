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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiManager;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import com.intellij.workspaceModel.ide.WorkspaceModelTopics;
import com.redhat.devtools.intellij.quarkus.classpath.bridge.WorkspaceModelTopicsBridge;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Classpath resource change manager provides the capability to track update of libraries changed and Java, microprofile-config properties files
 * by any component by registering a listener {@link Listener}.
 *
 * <code>
 * ClasspathResourceChangeManager.Listener myListener = ...
 * project.getMessageBus().connect(project).subscribe(ClasspathResourceChangeManager.TOPIC, myListener);
 * </code>
 *
 *
 * <ul>
 *     <li>Track update of libraries is done with {@link WorkspaceModelTopics}.
 *     In other words {@link Listener#librariesChanged()}  are fired when all libraries are inserted, deleted, updated.</li>
 *     <li>Track update of Java, microprofile-config properties files are done when Java Psi file is updated, when Java file is created, deleted, saved.</li>
 * </ul>
 */
public class ClasspathResourceChangedManager implements Disposable {

    public static final Topic<ClasspathResourceChangedManager.Listener> TOPIC = Topic.create(ClasspathResourceChangedManager.class.getName(), ClasspathResourceChangedManager.Listener.class);

    private SourceFileChangeNotifier sourcesChanged;
    private MessageBusConnection connection;
    private ClasspathSourceFileChangedListener listener;

    public static ClasspathResourceChangedManager getInstance(Project project) {
        return ServiceManager.getService(project, ClasspathResourceChangedManager.class);
    }

    public interface Listener {

        void librariesChanged();

        void sourceFilesChanged(Set<Pair<VirtualFile, Module>> sources);
    }

    private final Project project;

    public ClasspathResourceChangedManager(Project project) {
        this.project = project;
    }

    private synchronized void initialize(Project project) {
        if (sourcesChanged != null) {
            return;
        }
        // Send source files changed in debounce mode
        this.sourcesChanged = new SourceFileChangeNotifier(files ->
                project.getMessageBus().syncPublisher(ClasspathResourceChangedManager.TOPIC).sourceFilesChanged(files)
        );
        listener = new ClasspathSourceFileChangedListener(project, sourcesChanged);
        connection = project.getMessageBus().connect();
        // Track end of Java libraries update
        WorkspaceModelTopicsBridge workspaceModelTopicsBridge = new WorkspaceModelTopicsBridge();
        Object libraryChangedListener = new ClasspathLibraryChangedListener(project).getProxyInstance();
        if (libraryChangedListener != null && workspaceModelTopicsBridge.CHANGED != null) {
            connection.subscribe(workspaceModelTopicsBridge.CHANGED, libraryChangedListener);
        }
        // Track delete, create, update of file
        connection.subscribe(VirtualFileManager.VFS_CHANGES, listener);
        // Track update of Psi Java, properties files
        PsiManager.getInstance(project).addPsiTreeChangeListener(listener, project);
    }

    public void attach(Listener listener, MessageBusConnection connection) {
        initialize(project);
        connection.subscribe(ClasspathResourceChangedManager.TOPIC, listener);
    }

    @Override
    public void dispose() {
        if (sourcesChanged != null) {
            this.sourcesChanged.dispose();
            this.connection.disconnect();
            PsiManager.getInstance(project).removePsiTreeChangeListener(listener);
            sourcesChanged = null;
        }
    }
}
