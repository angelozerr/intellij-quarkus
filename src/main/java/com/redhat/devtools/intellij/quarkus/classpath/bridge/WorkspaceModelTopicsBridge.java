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
package com.redhat.devtools.intellij.quarkus.classpath.bridge;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Bridge to support WorkspaceModelTopics.CHANGED for any IJ versions:
 *
 * <ul>
 *     <li>com.intellij.workspaceModel.ide.WorkspaceModelTopics</li>
 *     <li>com.intellij.platform.backend.workspace.WorkspaceModelTopics</li>
 * </ul>
 *
 * @author Angelo ZERR
 */
public class WorkspaceModelTopicsBridge {

    private final static String[] WORKSPACE_MODEL_TOPICS_CLASS_NAMES = {"com.intellij.workspaceModel.ide.WorkspaceModelTopics", "com.intellij.platform.backend.workspace.WorkspaceModelTopics"};

    public final Topic<Object> CHANGED = getWorkspaceModelChangeTopic();


    private static @Nullable Topic<Object> getWorkspaceModelChangeTopic() {
        Class workspaceModelTopicsClass = getWorkspaceModelTopicsClass();
        if (workspaceModelTopicsClass == null) {
            return null;
        }
        try {
            Field field = workspaceModelTopicsClass.getDeclaredField("CHANGED");
            field.setAccessible(true);
            return (Topic<Object>) field.get(null);
        } catch (Exception e) {

        }
        return null;
    }

    private static @Nullable Class getWorkspaceModelTopicsClass() {
        for (String workspaceModelChangeListenerClassName : WORKSPACE_MODEL_TOPICS_CLASS_NAMES) {
            try {
                return Class.forName(workspaceModelChangeListenerClassName);
            } catch (Exception e) {

            }
        }
        return null;
    }
}
