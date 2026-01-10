package app.demo.exceptions;

public class ExistingRssSource extends Exception {
    public ExistingRssSource() {
        super("This RSS source already exists.");
    }
}
