package app.demo.services.monitoring.engines;

import app.demo.entities.Article;

import java.util.Set;

public class CustomApiClassifierEngine implements ClassifierEngine {
    private static final String API_URL = "";
    @Override
    public String classify(Article article, Set<String> topics) {
        return "";
    }
}
