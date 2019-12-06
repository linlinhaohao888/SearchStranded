import java.util.ArrayList;

class Result {
    private  String filename;
    private ArrayList<String> contents;


    /**
     * initialization
     */
    public Result()
    {
        filename = "";
        contents = new ArrayList<>();
    }

    public Result(String filename, ArrayList<String> contents)
    {
        this.filename = filename;
        this.contents = contents;
    }

}
