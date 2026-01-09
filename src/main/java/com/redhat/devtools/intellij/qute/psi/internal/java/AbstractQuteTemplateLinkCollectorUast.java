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
package com.redhat.devtools.intellij.qute.psi.internal.java;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.uast.*;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;
import com.redhat.devtools.intellij.qute.psi.utils.TemplateNameStrategy;
import com.redhat.devtools.intellij.qute.psi.utils.TemplatePathInfo;
import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.concurrent.CancellationException;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.*;
import static com.redhat.devtools.intellij.qute.psi.internal.template.datamodel.CheckedTemplateSupport.*;

/**
 * Abstract class which collects Java and Kotlin elements defining a Qute
 * template link.
 *
 * <ul>
 *   <li>Fields / properties typed as {@code Template}</li>
 *   <li>Methods declared in {@code @CheckedTemplate} types</li>
 *   <li>Java records or Kotlin classes implementing {@code TemplateInstance}</li>
 * </ul>
 *
 * <p>
 * This implementation relies on <b>UAST</b> to support both Java and Kotlin
 * source files with a single visitor.
 * </p>
 *
 * @author Angelo ZERR
 */
public abstract class AbstractQuteTemplateLinkCollectorUast extends AbstractUastVisitor {

    protected final PsiFile typeRoot;
    protected final IPsiUtils utils;
    protected final ProgressIndicator monitor;

    protected AbstractQuteTemplateLinkCollectorUast(
            @NotNull PsiFile typeRoot,
            @NotNull IPsiUtils utils,
            @NotNull ProgressIndicator monitor) {
        this.typeRoot = typeRoot;
        this.utils = utils;
        this.monitor = monitor;
    }

    /**
     * Entry point.
     *
     * Converts the {@link PsiFile} to {@link UFile} and starts traversal.
     */
    public final void collect() {
        try {
            UFile uFile = UastContextKt.toUElement(typeRoot, UFile.class);
            if (uFile != null) {
                uFile.accept(this);
            }
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        }
    }

    /**
     * Support for <b>Template Fields</b>.
     *
     * <pre>
     * {@code
     * private Template items;
     *
     * // Kotlin
     * lateinit var items: Template
     * }
     * </pre>
     *
     * @see <a href=
     * "https://quarkus.io/guides/qute-reference#quarkus_integration">
     * Quarkus Integration</a>
     */
    @Override
    public boolean visitField(@NotNull UField node) {
        PsiField psiField = (PsiField) node.getJavaPsi();
        if (psiField == null) {
            return false;
        }

        if (isTemplateType(node.getType())) {

            // Try to get the @Location annotation on the field / property
            PsiLiteralValue locationExpression = getLocationLiteral(node);

            PsiClass type = psiField.getContainingClass();
            String fieldName = node.getName();

            collectTemplateLink(
                    null,
                    psiField,
                    locationExpression,
                    type,
                    null,
                    fieldName,
                    false,
                    TemplateNameStrategy.ELEMENT_NAME
            );
        }
        return super.visitField(node);
    }

    /**
     * Support for:
     *
     * <ul>
     *   <li><b>TypeSafe Templates</b> (@CheckedTemplate)</li>
     *   <li><b>Template Records</b> (Java records)</li>
     *   <li><b>Kotlin equivalents</b> (class / data class / object implementing TemplateInstance)</li>
     * </ul>
     *
     * @see <a href=
     * "https://quarkus.io/guides/qute-reference#typesafe_templates">
     * TypeSafe Templates</a>
     */
    @Override
    public boolean visitClass(@NotNull UClass node) {

        PsiClass psiClass = node.getJavaPsi();

        // --------------------------------------------------
        // Template Records (Java record + Kotlin equivalent)
        // --------------------------------------------------
        if (psiClass != null &&
            isTemplateRecordLike(psiClass) &&
            !isTemplateContents(psiClass)) {

            String typeName = psiClass.getName();

            UAnnotation checkedAnnotation = node.findAnnotation(CHECKED_TEMPLATE_ANNOTATION);
            boolean ignoreFragments = isIgnoreFragments(checkedAnnotation);
            String basePath = getBasePath(checkedAnnotation);
            TemplateNameStrategy templateNameStrategy = getDefaultName(checkedAnnotation);

            collectTemplateLinkForMethodOrRecord(
                    basePath,
                    psiClass,
                    typeName,
                    psiClass,
                    ignoreFragments,
                    templateNameStrategy
            );
        }

        // --------------------------------------------------
        // TypeSafe Templates (@CheckedTemplate)
        // --------------------------------------------------
        UAnnotation checkedAnnotation = node.findAnnotation(CHECKED_TEMPLATE_ANNOTATION);
        if (checkedAnnotation != null) {

            boolean ignoreFragments = isIgnoreFragments(checkedAnnotation);
            String basePath = getBasePath(checkedAnnotation);
            TemplateNameStrategy templateNameStrategy = getDefaultName(checkedAnnotation);

            for (UMethod method : node.getMethods()) {
                PsiMethod psiMethod = method.getJavaPsi();
                if (psiMethod == null) {
                    continue;
                }

                collectTemplateLinkForMethodOrRecord(
                        basePath,
                        psiMethod,
                        method.getName(),
                        psiMethod.getContainingClass(),
                        ignoreFragments,
                        templateNameStrategy
                );
            }
        }

        return super.visitClass(node);
    }

    /**
     * Collects template links for a method or a record / data class.
     */
    protected void collectTemplateLinkForMethodOrRecord(
            String basePath,
            PsiElement methodOrRecord,
            String methodOrRecordName,
            PsiClass type,
            boolean ignoreFragments,
            TemplateNameStrategy templateNameStrategy) {

        collectTemplateLink(
                basePath,
                methodOrRecord,
                null,
                type,
                null,
                methodOrRecordName,
                ignoreFragments,
                templateNameStrategy
        );
    }

    /**
     * Returns true if the given type represents {@code io.quarkus.qute.Template}.
     */
    private static boolean isTemplateType(PsiType type) {
        if (type instanceof PsiClassType) {
            PsiClass clazz = ((PsiClassType) type).resolve();
            return clazz != null &&
                    TEMPLATE_CLASS.equals(clazz.getQualifiedName());
        }
        return false;
    }

    /**
     * Returns true if the class (Java or Kotlin) implements {@code TemplateInstance}.
     *
     * This method covers:
     * <ul>
     *   <li>Java records</li>
     *   <li>Kotlin classes</li>
     *   <li>Kotlin data classes</li>
     *   <li>Kotlin objects</li>
     * </ul>
     */
    private static boolean isTemplateRecordLike(PsiClass node) {
        for (PsiClassType current : node.getImplementsListTypes()) {
            if (current instanceof PsiClassReferenceType type &&
                type.getReference() != null &&
                TEMPLATE_INSTANCE_INTERFACE.equals(type.getReference().getQualifiedName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the class is annotated with {@code @TemplateContents}.
     */
    private static boolean isTemplateContents(PsiClass node) {
        return AnnotationUtils.hasAnnotation(node, TEMPLATE_CONTENTS_ANNOTATION);
    }

    /**
     * Returns the {@code @Location("...")} literal expression if present.
     */
    private static @Nullable PsiLiteralValue getLocationLiteral(UAnnotated owner) {
        UAnnotation ann = owner.findAnnotation(LOCATION_ANNOTATION);
        if (ann == null) {
            return null;
        }
        UExpression value = ann.findAttributeValue("value");
        PsiElement source = value != null ? value.getSourcePsi() : null;
        return source instanceof PsiLiteralValue ? (PsiLiteralValue) source : null;
    }

    /**
     * Creates an LSP {@link Range} from a UAST element.
     */
    protected Range createRange(@NotNull UElement element) {
        PsiElement psi = element.getSourcePsi();
        if (psi == null) {
            psi = element.getJavaPsi();
        }
        if (psi == null) {
            return null;
        }

        TextRange tr = psi.getTextRange();
        return utils.toRange(typeRoot, tr.getStartOffset(), tr.getLength());
    }

    /**
     * Collects a Qute template link without resolved template file.
     */
    protected abstract void collectTemplateLink(
            String basePath,
            PsiElement node,
            PsiLiteralValue locationAnnotation,
            PsiClass type,
            String className,
            String fieldOrMethodName,
            boolean ignoreFragment,
            TemplateNameStrategy templateNameStrategy
    );

    /**
     * Collects a fully resolved Qute template link.
     */
    protected abstract void collectTemplateLink(
            String basePath,
            PsiElement node,
            PsiLiteralValue locationAnnotation,
            PsiClass type,
            String className,
            String fieldOrMethodName,
            String location,
            VirtualFile templateFile,
            TemplatePathInfo templatePathInfo
    );
}
