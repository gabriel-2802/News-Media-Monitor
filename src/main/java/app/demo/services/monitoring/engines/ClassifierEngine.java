package app.demo.services.monitoring.engines;

import app.demo.entities.Article;
import app.demo.entities.Topic;

import java.util.Set;

public interface ClassifierEngine {
    String classify(Article article,  Set<String> topics);
}
