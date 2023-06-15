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

import com.intellij.openapi.project.Project;
import com.redhat.devtools.intellij.quarkus.classpath.bridge.VersionedStorageChangeBridge;
import com.redhat.devtools.intellij.quarkus.classpath.bridge.WorkspaceModelChangeListenerBridge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classpath resource changed listener used to track update of libraries.
 */
class ClasspathLibraryChangedListener extends WorkspaceModelChangeListenerBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathLibraryChangedListener.class);

    private static final String[] LIBRARY_ENTITY_CLASSES = {"com.intellij.workspaceModel.storage.bridgeEntities.LibraryEntity", "com.intellij.workspaceModel.storage.bridgeEntities.api.LibraryEntityImpl", "com.intellij.platform.workspace.jps.entities.LibraryEntity"};

    private final Class libraryClass;

    private final Project project;

    public ClasspathLibraryChangedListener(Project project) {
        super();
        this.project = project;
        libraryClass = getLibraryClass();
        // here Java reflection is used to try to support several IJ versions
        if (libraryClass == null) {
            LOGGER.warn("Cannot find LibraryEntity class '" + String.join(",", LIBRARY_ENTITY_CLASSES) + "' in classpath.");
        }
    }

    @Override
    public void beforeChanged(@NotNull VersionedStorageChangeBridge event) {

    }

    @Override
    public void changed(@NotNull VersionedStorageChangeBridge event) {
        if (hasLibraryChanges(event)) {
            project.getMessageBus().syncPublisher(ClasspathResourceChangedManager.TOPIC).librariesChanged();
        }
    }

    private boolean hasLibraryChanges(VersionedStorageChangeBridge event) {
        if (libraryClass == null) {
            return false;
        }
        return event.hasChanges(libraryClass);
    }

    private static @Nullable Class getLibraryClass() {
        for (String libraryClassName : LIBRARY_ENTITY_CLASSES) {
            try {
                return Class.forName(libraryClassName);
            } catch (Exception e) {

            }
        }
        return null;
    }
}
