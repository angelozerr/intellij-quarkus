package com.redhat.devtools.intellij.qute.lang.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.tree.IElementType;

public class QutePsiElement extends ASTWrapperPsiElement {

    public QutePsiElement(ASTNode node) {
        super(node);
    }
}
