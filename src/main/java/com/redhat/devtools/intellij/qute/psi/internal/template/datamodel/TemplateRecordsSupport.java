/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.template.datamodel;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiRecordComponent;
import com.redhat.devtools.intellij.qute.psi.internal.template.TemplateDataSupport;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractInterfaceImplementationDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.qute.psi.utils.TemplateNameStrategy;
import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_INSTANCE_INTERFACE;
import static com.redhat.devtools.intellij.qute.psi.internal.template.datamodel.CheckedTemplateSupport.*;
import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.getTemplatePath;

/**
 * Template Records support for template files:
 *
 * <code>
 * public class HelloResource {
 * <p>
 * record Hello(String name) implements TemplateInstance {}
 * <p>
 * <p>
 * &#64;CheckedTemplate(basePath="Foo", defaultName=CheckedTemplate.HYPHENATED_ELEMENT_NAME)
 * record HelloWorld(String name) implements TemplateInstance {}
 * ...
 * <p>
 * <p>
 * &#64;GET
 * &#64;Produces(MediaType.TEXT_PLAIN)
 * public TemplateInstance get(@QueryParam("name") String name) {
 * return new Hello(name).data("bar", 100);
 * }
 * </code>
 *
 * @author Angelo ZERR
 * @see <a href=
 * "https://quarkus.io/guides/qute-reference#template-records">Template
 * Records</a>
 */
public class TemplateRecordsSupport extends AbstractInterfaceImplementationDataModelProvider {

    private static final String[] INTERFACE_NAMES = {TEMPLATE_INSTANCE_INTERFACE};

    @Override
    protected String[] getInterfaceNames() {
        return INTERFACE_NAMES;
    }

    @Override
    protected void processType(PsiClass type, SearchContext context, ProgressIndicator monitor) {
        if (!type.isRecord()) {
            return;
        }
        collectDataModelTemplateForTemplateRecord(type, context.getRelativeTemplateBaseDir(), context.getDataModelProject().getTemplates(),
                monitor);
    }

    private static void collectDataModelTemplateForTemplateRecord(PsiClass type,
                                                                  String relativeTemplateBaseDir,
                                                                  List<DataModelTemplate<DataModelParameter>> templates, ProgressIndicator monitor) {
        DataModelTemplate<DataModelParameter> template = createTemplateDataModel(type, relativeTemplateBaseDir, monitor);
        templates.add(template);
    }

    private static DataModelTemplate<DataModelParameter> createTemplateDataModel(PsiClass recordType,
                                                                                 String relativeTemplateBaseDir,
                                                                                 ProgressIndicator monitor) {

        PsiAnnotation checkedTemplateAnnotation = getCheckedAnnotation(recordType);
        boolean ignoreFragments = isIgnoreFragments(checkedTemplateAnnotation);
        String basePath = getBasePath(checkedTemplateAnnotation);
        TemplateNameStrategy templateNameStrategy = getDefaultName(checkedTemplateAnnotation);
        String className = getParentClassName(recordType);
        String recordName = recordType.getName();
        // src/main/resources/templates/${recordName}.qute.html
        String templateUri = getTemplatePath(relativeTemplateBaseDir, basePath, className, recordName, ignoreFragments, templateNameStrategy).getTemplateUri();

        // Create template data model with:
        // - template uri : Qute template file which must be bind with data model.
        // - source type : the record class which defines Templates
        // -
        DataModelTemplate<DataModelParameter> template = new DataModelTemplate<DataModelParameter>();
        template.setParameters(new ArrayList<>());
        template.setTemplateUri(templateUri);
        template.setSourceType(recordType.getQualifiedName());

        // Collect data parameters from the record fields
        for (PsiRecordComponent field : recordType.getRecordComponents()) {
            DataModelParameter parameter = new DataModelParameter();
            parameter.setKey(field.getName());
            parameter.setSourceType(PsiTypeUtils.resolveSignature(field.getType(), field.isVarArgs()));
            if (template.getParameter(parameter.getKey()) == null) {
                // Add parameter if it doesn't exist
                // to avoid parameters duplication
                template.addParameter(parameter);
            }
        }

        // Collect data parameters for the given template
        TemplateDataSupport.collectParametersFromDataMethodInvocation(recordType, template, monitor);
        return template;
    }

}
