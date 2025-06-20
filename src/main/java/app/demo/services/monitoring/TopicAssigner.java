package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.entities.Topic;
import app.demo.repositories.TopicRepository;
import app.demo.services.monitoring.engines.ClassifierEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TopicAssigner {
    private final ClassifierEngine classifierEngine;
    private final TopicRepository topicRepository;

    public void assignTopic(Article article) {
        String predictedTopic = classifierEngine.classify(article, getAvailableTopics());
        if (predictedTopic == null || predictedTopic.isEmpty()) {
            article.setTopic(topicRepository.getDefaultTopic());
        }

        Topic topic = topicRepository.findByName(predictedTopic)
                .orElseThrow(() -> new IllegalStateException("Topic not found: " + predictedTopic));
        article.setTopic(topic);
    }

    private Set<String> getAvailableTopics() {
        return topicRepository.findAll().stream()
                .map(Topic::getName)
                .collect(Collectors.toSet());
    }


}
