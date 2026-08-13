package com.sentinel.dto;

public class RunbookEntry {

    private final String filename;
    private final String content;
    private final float[] vector;

    public RunbookEntry(String filename, String content, float[] vector) {
        this.filename = filename;
        this.content = content;
        this.vector = vector;
    }

    public String getFilename() {
        return filename;
    }

    public String getContent() {
        return content;
    }

    public float[] getVector() {
        return vector;
    }
}
