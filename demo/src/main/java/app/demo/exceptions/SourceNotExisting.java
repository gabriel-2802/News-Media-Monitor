package app.demo.exceptions;

public class SourceNotExisting extends Exception {
    public SourceNotExisting(String srcName) {
        super("Source with name '" + srcName + "' does not exist.");
    }
}
