package com.cupk.common;

public enum FileUsage {
    RESOURCE("resources"), SUBMISSION("submissions"), COURSE_COVER("course-covers");

    private final String directory;
    FileUsage(String directory) { this.directory = directory; }
    public String directory() { return directory; }
}
