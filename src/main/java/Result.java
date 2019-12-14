import java.util.ArrayList;

class Result {
    private String filename;
//    private ArrayList<String> contents;
    String contents;

    /**
     * initialization
     */
    public Result() {
        filename = "";
//        contents = new ArrayList<>();
        contents = "";
    }

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
