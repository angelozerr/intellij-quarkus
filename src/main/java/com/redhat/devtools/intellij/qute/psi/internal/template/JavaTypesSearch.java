/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.template;

import com.intellij.codeInsight.completion.AllClassesGetter;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.completion.impl.BetterPrefixMatcher;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.AbstractTypeResolver;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import com.redhat.qute.commons.JavaTypeInfo;
import com.redhat.qute.commons.JavaTypeKind;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Java types search for a given pattern and project Uri.
 * 
 * @author Angelo ZERR
 *
 */
public class JavaTypesSearch {

	private static final Logger LOGGER = Logger.getLogger(JavaTypesSearch.class.getName());

	private final Module javaProject;

	private final String packageName;

	private final String typeName;

	private final SearchScope scope;

	public JavaTypesSearch(String pattern, Module javaProject) {
		this.javaProject = javaProject;

		String typeName = pattern;
		String packageName = "";
		SearchScope searchScope = javaProject.getModuleScope();
		if (StringUtils.isNotEmpty(typeName)) {
			searchScope = javaProject.getModuleWithLibrariesScope();
			int index = typeName.lastIndexOf('.');
			if (index != -1) {
				// ex : pattern = org.acme.qute.It
				// -> packageName = org.acme.qute
				// -> typeName = It
				packageName = typeName.substring(0, index);
				typeName = typeName.substring(index + 1, typeName.length());
				// support for inner class
				try {
					PsiClass innerClass = JavaPsiFacade.getInstance(javaProject.getProject()).findClass(packageName, (GlobalSearchScope) searchScope);
					if (innerClass != null) {
						packageName = null;
						searchScope = searchScope.intersectWith(new LocalSearchScope(innerClass));
					}
				} catch (RuntimeException e) {
					LOGGER.log(Level.WARNING, "Error while getting inner class for '" + packageName + "'.", e);
				}
			}
		}

		//typeName += ".*";
		this.typeName = typeName;
		this.packageName = packageName;
		this.scope = searchScope;
	}

	public List<JavaTypeInfo> search(ProgressIndicator monitor) {
		List<JavaTypeInfo> javaTypes = new ArrayList<>();
		collectPackages(javaTypes);

		PrefixMatcher matcher = new CamelHumpMatcher(typeName, true, false);
		matcher = new BetterPrefixMatcher(matcher, Integer.MIN_VALUE);

		//PrefixMatcher matcher = new PlainPrefixMatcher(typeName);
		Project project = javaProject.getProject();
		GlobalSearchScope s = GlobalSearchScope.allScope(project);
		final List<String> existing = new ArrayList<>();
		AllClassesGetter.processJavaClasses(matcher, project, s, psiClass ->{
			String qName = psiClass.getQualifiedName();
			if (qName != null && qName.startsWith(packageName) && existing.add(qName)) {
				collectClass(psiClass, javaTypes);
			}
			return true;
		});

		//collectClassesAndInterfaces(monitor, javaTypes);
		return javaTypes;
	}

	private void collectPackages(List<JavaTypeInfo> javaTypes) {
		if (packageName != null) {
			Set<String> subPackages = new HashSet<>();
			try {
				// Loop for package root
				PsiPackage pack = JavaPsiFacade.getInstance(javaProject.getProject()).findPackage(packageName);
				fillWithSubPackages(packageName, pack, subPackages);
				collectClassesAndInterfaces(pack,javaTypes);
			} catch (RuntimeException e) {
				LOGGER.log(Level.WARNING, "Error while collecting sub packages for '" + packageName + "'.", e);
			}

			for (String subPackageName : subPackages) {
				JavaTypeInfo packageInfo = new JavaTypeInfo();
				packageInfo.setJavaTypeKind(JavaTypeKind.Package);
				packageInfo.setSignature(subPackageName);
				javaTypes.add(packageInfo);
			}
		}
	}

	private void fillWithSubPackages(String packageName, PsiPackage packageFragmentRoot,
			Set<String> subPackages) {
		try {
			PsiPackage[] allPackages = packageFragmentRoot.getSubPackages();
			for (int i = 0; i < allPackages.length; i++) {
				PsiPackage psiPackage = allPackages[i];
				String subPackageName = psiPackage.getQualifiedName();
				if (subPackageName.startsWith(packageName)) {
					subPackages.add(subPackageName);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.log(Level.WARNING, "Error while collecting sub packages for '" + packageName + "' in '"
					+ packageFragmentRoot.getQualifiedName() + "'.", e);
		}
	}

	private void collectClassesAndInterfaces(PsiPackage psiPackage, List<JavaTypeInfo> javaTypes) {
		PsiClass[] types = psiPackage.getClasses();
		for (PsiClass type: types
			 ) {
			collectClass(type, javaTypes);
		}
	}

	private void collectClass(PsiClass type, List<JavaTypeInfo> javaTypes) {
		String typeSignature = AbstractTypeResolver.resolveJavaTypeSignature(type);
		if (typeSignature != null) {
			JavaTypeInfo classInfo = new JavaTypeInfo();
			classInfo.setSignature(typeSignature);
			javaTypes.add(classInfo);

			try {
				classInfo.setJavaTypeKind(PsiTypeUtils.getJavaTypeKind(type));
			} catch (RuntimeException e) {
				LOGGER.log(Level.WARNING, "Error while collecting Java Types for '" + packageName
						+ " package and Java type '" + typeName + "'.", e);
			}
		}
	}

}
