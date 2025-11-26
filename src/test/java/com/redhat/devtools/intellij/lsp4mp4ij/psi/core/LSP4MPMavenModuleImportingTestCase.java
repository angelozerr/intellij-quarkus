/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;


import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.IndexingTestUtil;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.quarkus.QuarkusDeploymentSupport;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class to import lsp4mp maven project.
 */
public abstract class LSP4MPMavenModuleImportingTestCase extends MavenModuleImportingTestCase {

    private static final Map<String, Module> loadedProjects = new HashMap<>();

    protected Module loadMavenProject(String projectName) throws Exception {
        return loadMavenProject(projectName, false);
    }

    protected Module loadMavenProject(String projectName, boolean collectAndAddQuarkusDeploymentDependencies) throws Exception {
        String projectPath = "projects/lsp4mp/projects/maven/" + projectName;
        Module module = loadedProjects.get(projectPath);
        if (module != null) {
            return module;
        }

        module = createMavenModule(new File(projectPath));
        if(collectAndAddQuarkusDeploymentDependencies) {
            QuarkusDeploymentSupport.getInstance(getTestFixture().getProject()).updateClasspathWithQuarkusDeployment(module, new EmptyProgressIndicator());
        }
        IndexingTestUtil.waitUntilIndexesAreReady(getTestFixture().getProject());
        loadedProjects.put(projectPath, module);
        return module;
    }
}

