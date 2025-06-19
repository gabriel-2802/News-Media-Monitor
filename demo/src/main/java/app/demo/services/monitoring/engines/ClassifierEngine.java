package app.demo.services.monitoring.engines;

import app.demo.entities.Article;

public interface ClassifierEngine {
    String classify(Article article);
}
