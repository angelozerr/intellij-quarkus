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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Bridge to support WorkspaceModelChangeListener API interface for any IJ versions:
 *
 * <ul>
 *     <li>com.intellij.workspaceModel.ide.WorkspaceModelChangeListener</li>
 *     <li>com.intellij.platform.backend.workspace.WorkspaceModelChangeListener</li>
 * </ul>
 *
 * @author Angelo ZERR
 */
public abstract class WorkspaceModelChangeListenerBridge {

    private final static String[] WORKSPACE_MODEL_CHANGE_LISTENER_CLASS_NAMES = {"com.intellij.workspaceModel.ide.WorkspaceModelChangeListener", "com.intellij.platform.backend.workspace.WorkspaceModelChangeListener"};

    private final Class WORKSPACE_MODEL_CHANGE_LISTENER_CLASS = getWorkspaceModelChangeListenerClass();

    private final Object proxyInstance;

    public WorkspaceModelChangeListenerBridge() {
        if (WORKSPACE_MODEL_CHANGE_LISTENER_CLASS != null) {
            // Here the WorkspaceModelChangeListener API interface exists, create an instance which implements it with a Proxy.
            proxyInstance = Proxy.newProxyInstance(WorkspaceModelChangeListenerBridge.this.getClass().getClassLoader(),
                    new Class[]{WORKSPACE_MODEL_CHANGE_LISTENER_CLASS},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if ("beforeChanged".equals(method.getName())) {
                                // WorkspaceModelChangeListener#beforeChanged(VersionedStorageChange event) has been called
                                VersionedStorageChangeBridge event = createEvent(args);
                                beforeChanged(event);
                            } else if ("changed".equals(method.getName())) {
                                // WorkspaceModelChangeListener#changed(VersionedStorageChange event) has been called
                                VersionedStorageChangeBridge event = createEvent(args);
                                changed(event);
                            }
                            return null;
                        }
                    });
        } else {
            proxyInstance = null;
        }
    }

    public Object getProxyInstance() {
        return proxyInstance;
    }

    private VersionedStorageChangeBridge createEvent(Object[] args) {
        return new VersionedStorageChangeBridge(args[0]);
    }

    public abstract void beforeChanged(@NotNull VersionedStorageChangeBridge event);

    public abstract void changed(@NotNull VersionedStorageChangeBridge event);

    private static @Nullable Class getWorkspaceModelChangeListenerClass() {
        for (String workspaceModelChangeListenerClassName : WORKSPACE_MODEL_CHANGE_LISTENER_CLASS_NAMES) {
            try {
                return Class.forName(workspaceModelChangeListenerClassName);
            } catch (Exception e) {

            }
        }
        return null;
    }

}
