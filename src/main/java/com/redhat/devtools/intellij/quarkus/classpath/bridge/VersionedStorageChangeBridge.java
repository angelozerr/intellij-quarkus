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

import kotlin.sequences.Sequence;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VersionedStorageChangeBridge {

    private final Map<Class, Method> getChangesMethods = new HashMap<>();

    private final Object versionedStorageChange;

    public VersionedStorageChangeBridge(Object versionedStorageChange) {
        this.versionedStorageChange = versionedStorageChange;
    }

    public boolean hasChanges(Class entityClass) {
        Method method = getGetChangesMethod(versionedStorageChange);
        if (method != null) {
            try {
                Object result = method.invoke(versionedStorageChange, new Object[]{entityClass});
                if (result instanceof Sequence) {
                    return ((Sequence) result).iterator().hasNext();
                }
                if (result instanceof Sequence) {
                    return !((List) result).isEmpty();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private Method getGetChangesMethod(Object versionedStorageChange) {
        Class clazz = versionedStorageChange.getClass();
        Method getChangesMethod = getChangesMethods.get(clazz);
        if (getChangesMethod != null) {
            return getChangesMethod;
        }
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if ("getChanges".equals(method.getName())) {
                method.setAccessible(true);
                getChangesMethod = method;
                getChangesMethods.put(clazz, getChangesMethod);
            }
        }
        return getChangesMethod;
    }


}
