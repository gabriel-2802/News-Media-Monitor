package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.entities.Topic;
import app.demo.events.TopicRepositoryChangeEvent;
import app.demo.repositories.TopicRepository;
import app.demo.services.monitoring.engines.ClassifierEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TopicAssigner {
    private final ClassifierEngine classifierEngine;
    private final TopicRepository topicRepository;
    private Map<String, Topic> topicMap;

    public TopicAssigner(ClassifierEngine classifierEngine, TopicRepository topicRepository) {
        this.classifierEngine = classifierEngine;
        this.topicRepository = topicRepository;
        this.topicMap = getAvailableTopics();
    }

    @EventListener
    public void handleTopicRepositoryChangeEvent(TopicRepositoryChangeEvent event) {
        log.info("Received TopicRepositoryChangeEvent, updating topic map.");
        if (event.isDeleted()) {
            topicMap.remove(event.getTopic().getName());
        } else {
            topicMap.put(event.getTopic().getName(), event.getTopic());
        }
    }

    public void assignTopic(Article article) {
        String predictedTopic = classifierEngine.classify(article, topicMap.keySet());

        if (predictedTopic == null || predictedTopic.isEmpty()) {
            article.setTopic(topicRepository.getDefaultTopic());
        }

        article.setTopic(topicMap.get(predictedTopic));
    }

    private Map<String, Topic> getAvailableTopics() {
        return topicRepository.findAll().stream()
                .collect(Collectors.toMap(Topic::getName, topic -> topic));
    }


}
