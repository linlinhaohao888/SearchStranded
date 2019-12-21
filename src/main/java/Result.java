class Result {
    private String filename;
    String contents;

    /**
     * initialization
     */
    public Result(String filename, String contents) {
        this.filename = filename;
        this.contents = contents;
    }

    public String getFilename() {
        return filename;
    }

    public String getContent() {
        return contents;
    }
}
