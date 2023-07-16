/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij.operations.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LanguageServerWrapper;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.intellij.lsp4ij.futures.FutureUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LSContentAssistProcessor extends CompletionContributor implements DumbAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSContentAssistProcessor.class);

    private static final @NotNull Key<CompletableFuture<Void>> key = Key.create(LSContentAssistProcessor.class.getName());

    private static final @NotNull Key<? super Long> key2 = Key.create(LSContentAssistProcessor.class.getName()+ "@");

    private static class ProposalResult {

        public final Either<List<CompletionItem>, CompletionList> completion;

        public final LanguageServer languageServer;

        public final CancelChecker cancelChecker;

        private ProposalResult(Either<List<CompletionItem>, CompletionList> completion, LanguageServer languageServer, CancelChecker cancelChecker) {
            this.completion = completion;
            this.languageServer = languageServer;
            this.cancelChecker = cancelChecker;
        }
    }
    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        if (result.isStopped()) {
            return;
        }
        Document document = parameters.getEditor().getDocument();
        Editor editor = parameters.getEditor();
        PsiFile file = parameters.getOriginalFile();
        Project project = file.getProject();
        int offset = parameters.getOffset();

        ProgressManager.checkCanceled();

        Long ID= System.currentTimeMillis();

        file.putUserData(key2, ID);
        CompletableFuture<Void> newFuture = null;
        synchronized (key) {
            CompletableFuture<Void> future = file.getUserData(key);
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }

        CompletableFuture<List<Pair<LanguageServerWrapper, LanguageServer>>> completionLanguageServersFuture = initiateLanguageServers(project, document);
        try {
            /*
             process the responses out of the completable loop as it may cause deadlock if user is typing
             more characters as toProposals will require as read lock that this thread already have and
             async processing is occuring on a separate thread.
             */
            CompletionParams params = LSPIJUtils.toCompletionParams(LSPIJUtils.toUri(document), offset, document);
            BlockingDeque<ProposalResult> proposals = new LinkedBlockingDeque<>();
            newFuture = FutureUtils.computeAsyncCompose(cancelChecker ->
                    completionLanguageServersFuture
                            .thenComposeAsync(languageServers -> cancelChecker.trackAndExecute(() ->
                                    CompletableFuture.allOf(languageServers.stream()
                                            .map(languageServer ->
                                                    cancelChecker.trackAndExecute(() -> languageServer.getSecond().getTextDocumentService().completion(params))
                                                            .thenAcceptAsync(completion -> proposals.add(new ProposalResult(completion, languageServer.getSecond(), cancelChecker))))
                                            .toArray(CompletableFuture[]::new)))));
            file.putUserData(key, newFuture);
            while (!newFuture.isDone() || !proposals.isEmpty()) {
                if(ID != file.getUserData(key2)) {
                    throw new ProcessCanceledException();
                }

                ProgressManager.checkCanceled();
                ProposalResult pair = proposals.poll(25, TimeUnit.MILLISECONDS);
                if (pair != null) {
                    pair.cancelChecker.checkCanceled();

                    result.addAllElements(toProposals(file, editor, document, offset, pair.completion,
                            pair.languageServer));
                }
            }
        } catch (ProcessCanceledException cancellation) {
            throw cancellation;
        } catch (RuntimeException | InterruptedException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
            result.addElement(createErrorProposal(offset, e));
        } finally {
            synchronized (key) {
                if (newFuture != null && !newFuture.isDone()) {
                    newFuture.cancel(true);
                }
            }
        }
    }

    private Collection<? extends LookupElement> toProposals(PsiFile file, Editor editor, Document document,
                                                            int offset, Either<List<CompletionItem>,
            CompletionList> completion, LanguageServer languageServer) {
        if (completion != null) {
            List<CompletionItem> items = completion.isLeft() ? completion.getLeft() : completion.getRight().getItems();
            boolean isIncomplete = completion.isRight() && completion.getRight().isIncomplete();
            return items.stream()
                    .map(item -> createLookupItem(file, editor, offset, item, isIncomplete, languageServer))
                    .filter(item -> item.validate(document, offset, null))
                    .map(item -> PrioritizedLookupElement.withGrouping(item, item.getItem().getKind().getValue()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private <T> CompletableFuture<T> addFuture(CompletableFuture<T> f, List<CompletableFuture> futures) {
        futures.add(f);
        return f;
    }

    private void cancel(List<CompletableFuture> futures, PsiFile file) {
        if (futures != null) {
            futures.forEach(f -> {
                if (!f.isDone()) {
                    f.cancel(true);
                }
            });
        }
        //file.putUserData(key,null);
    }

    private LSIncompleteCompletionProposal createLookupItem(PsiFile file, Editor editor, int offset,
                                                            CompletionItem item, boolean isIncomplete,
                                                            LanguageServer languageServer) {
        return isIncomplete ? new LSIncompleteCompletionProposal(file, editor, offset, item, languageServer) :
                new LSCompletionProposal(file, editor, offset, item, languageServer);
    }


    private LookupElement createErrorProposal(int offset, Exception ex) {
        return LookupElementBuilder.create("Error while computing completion", "");
    }

    private CompletableFuture<List<Pair<LanguageServerWrapper, LanguageServer>>> initiateLanguageServers(Project project, Document document) {
        return LanguageServiceAccessor.getInstance(project).getLanguageServers(document,
                capabilities -> {
                    CompletionOptions provider = capabilities.getCompletionProvider();
                    if (provider != null) {
                        return true;
                    }
                    return false;
                });
    }
}
