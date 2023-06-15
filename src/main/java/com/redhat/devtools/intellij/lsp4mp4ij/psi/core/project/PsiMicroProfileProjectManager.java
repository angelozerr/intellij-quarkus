/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project;

import com.intellij.ProjectTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.quarkus.classpath.ClasspathResourceChangedManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@link PsiMicroProfileProject} manager.
 *
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/project/JDTMicroProfileProjectManager.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/project/JDTMicroProfileProjectManager.java</a>
 */
@Service
public final class PsiMicroProfileProjectManager implements Disposable {

    private static final String JAVA_FILE_EXTENSION = "java";

    private MessageBusConnection connection;

    public static PsiMicroProfileProjectManager getInstance(Project project) {
        return ServiceManager.getService(project, PsiMicroProfileProjectManager.class);
    }

    private Project project;

    private final Map<Module, PsiMicroProfileProject> projects;
    private MicroProfileProjectListener microprofileProjectListener;

    private class MicroProfileProjectListener implements ModuleListener, ClasspathResourceChangedManager.Listener {

        @Override
        public void librariesChanged() {
            // Do nothing
        }

        @Override
        public void sourceFilesChanged(Set<Pair<VirtualFile, Module>> sources) {
            for (var pair : sources) {
                VirtualFile file = pair.getFirst();
                if (isConfigSource(file)) {
                    // A microprofile config file properties file source has been updated, evict the cache of the properties
                    Module javaProject = pair.getSecond();
                    PsiMicroProfileProject mpProject = getJDTMicroProfileProject(javaProject);
                    if (mpProject != null) {
                        mpProject.evictConfigSourcesCache();
                    }
                }
            }
        }

        @Override
        public void beforeModuleRemoved(@NotNull Project project, @NotNull Module module) {
            evict(module);
        }

        private void evict(Module javaProject) {
            if (javaProject != null) {
                // Remove the JDTMicroProfile project instance from the cache.
                projects.remove(javaProject);
            }
        }
    }

    private PsiMicroProfileProjectManager(Project project) {
        this.project = project;
        this.projects = new HashMap<>();
        initialize();
    }

    public PsiMicroProfileProject getJDTMicroProfileProject(Module project) {
        return getJDTMicroProfileProject(project, true);
    }

    private PsiMicroProfileProject getJDTMicroProfileProject(Module project, boolean create) {
        Module javaProject = project;
        PsiMicroProfileProject info = projects.get(javaProject);
        if (info == null) {
            if (!create) {
                return null;
            }
            info = new PsiMicroProfileProject(javaProject);
            projects.put(javaProject, info);
        }
        return info;
    }

    /**
     * Returns true if the given file is a MicroProfile config properties file (microprofile-config.properties, application.properties, application.yaml, etc) and false otherwise.
     *
     * @param file the file to check.
     * @return true if the given file is a MicroProfile config properties file (microprofile-config.properties, application.properties, application.yaml, etc) and false otherwise.
     */
    public boolean isConfigSource(VirtualFile file) {
        if (file == null) {
            return false;
        }
        String fileName = file.getName();
        for (IConfigSourceProvider provider : IConfigSourceProvider.EP_NAME.getExtensions()) {
            if (provider.isConfigSource(fileName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the given file is a Java file and false otherwise.
     *
     * @param file the file to check.
     * @return true if the given file is a Java file and false otherwise.
     */
    public boolean isJavaFile(VirtualFile file) {
        return file != null && JAVA_FILE_EXTENSION.equals(file.getExtension());
    }

    public void initialize() {
        if (microprofileProjectListener != null) {
            return;
        }
        connection = project.getMessageBus().connect(project);
        microprofileProjectListener = new MicroProfileProjectListener();
        connection.subscribe(ClasspathResourceChangedManager.TOPIC, microprofileProjectListener);
        connection.subscribe(ProjectTopics.MODULES, microprofileProjectListener);
    }

    @Override
    public void dispose() {
        if (connection != null) {
            connection.disconnect();
        }
        microprofileProjectListener = null;
    }
}
