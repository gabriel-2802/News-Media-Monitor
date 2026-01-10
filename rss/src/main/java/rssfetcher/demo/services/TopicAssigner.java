package rssfetcher.demo.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rssfetcher.demo.entities.Article;
import rssfetcher.demo.entities.Topic;
import rssfetcher.demo.events.TopicRepositoryChangeEvent;
import rssfetcher.demo.repositories.TopicRepository;
import rssfetcher.demo.services.engines.ClassificationEngine;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Component responsible for assigning topics to articles using a classification engine.
 */
@Component
@Slf4j
public class TopicAssigner {
    private final ClassificationEngine classifierEngine;
    private final TopicRepository topicRepository;
    private Map<String, Topic> topicMap;
    private List<String> topicNames;

    public TopicAssigner(ClassificationEngine classifierEngine, TopicRepository topicRepository) {
        this.classifierEngine = classifierEngine;
        this.topicRepository = topicRepository;
        setTopics();
    }

    @EventListener
    public void handleTopicRepositoryChangeEvent(TopicRepositoryChangeEvent event) {
        log.info("Received TopicRepositoryChangeEvent, updating topic map.");
        if (event.isDeleted()) {
            topicMap.remove(event.topic().getName());
            topicNames.remove(event.topic().getName());
        } else {
            topicMap.put(event.topic().getName(), event.topic());
            topicNames.add(event.topic().getName());
        }
    }

    public void assignTopic(Article article) {

        String predictedTopic = classifierEngine.classify(article, topicNames);

        if (predictedTopic == null || predictedTopic.isEmpty()) {
            article.setTopic(topicRepository.getDefaultTopic());
        }

        article.setTopic(topicMap.get(predictedTopic));
    }

    private void setTopics() {
        var topics = topicRepository.findAllOrdered();
        topicNames = topics.stream().map(Topic::getName).toList();
        topicMap = topics.stream().collect(Collectors.toMap(Topic::getName, topic -> topic));
    }

}
