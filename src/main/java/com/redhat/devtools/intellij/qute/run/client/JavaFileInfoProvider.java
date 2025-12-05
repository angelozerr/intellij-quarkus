package com.redhat.devtools.intellij.qute.run.client;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

public interface JavaFileInfoProvider {

    @JsonRequest("qute/javaFileInfo")
    CompletableFuture<JavaFileInfoResponse> getJavaFileInfo(JavaFileInfoRequestArguments params);
}
