package com.redhat.devtools.intellij.qute.run.client;

public class JavaFileInfoResponse {

    private String javaFileUri;

    private int startLine;

    public String getJavaFileUri() {
        return javaFileUri;
    }

    public void setJavaFileUri(String javaFileUri) {
        this.javaFileUri = javaFileUri;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

}
