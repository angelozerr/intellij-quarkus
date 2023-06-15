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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.*;
import java.util.function.Consumer;

/**
 * Source file change notifier with a debounce mode.
 */
public class SourceFileChangeNotifier implements Disposable  {

    private static final long DEBOUNCE_DELAY = 1000;
    private Timer debounceTimer;
    private TimerTask debounceTask;

    private final Set<Pair<VirtualFile, Module>> sourceFiles;

    private final Consumer<Set<Pair<VirtualFile, Module>>> notifier;

    public SourceFileChangeNotifier(Consumer<Set<Pair<VirtualFile, Module>>> notifier) {
        this.notifier = notifier;
        sourceFiles = new HashSet<>();
    }
    public synchronized void addSourceFile(Pair<VirtualFile, Module> pair) {
        if (debounceTask != null) {
            debounceTask.cancel();
        }
        synchronized (sourceFiles) {
            sourceFiles.add(pair);
        }

        debounceTask = new TimerTask() {
            @Override
            public void run() {
                synchronized (sourceFiles) {
                    notifier.accept(sourceFiles);
                    sourceFiles.clear();
                }
            }
        };

        if (debounceTimer == null) {
            debounceTimer = new Timer();
        }

        debounceTimer.schedule(debounceTask, DEBOUNCE_DELAY);
    }

    @Override
    public void dispose() {
        if (debounceTask != null) {
            debounceTask.cancel();
        }
        if(debounceTimer != null) {
            debounceTimer.cancel();
        }
    }
}