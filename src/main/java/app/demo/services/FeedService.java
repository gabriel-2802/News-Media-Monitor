package app.demo.services;

import app.demo.dto.ArticleDTO;
import app.demo.dto.TopicDTO;
import app.demo.entities.Article;
import app.demo.entities.Topic;
import app.demo.mappers.ArticleMapper;
import app.demo.mappers.TopicMapper;
import app.demo.repositories.ArticleRepository;
import app.demo.repositories.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {
    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;
    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;

    public List<TopicDTO> getAllTopics() {
        return topicRepository.findAll().stream().map(topicMapper::toDTO).collect(Collectors.toList());
    }

    public List<ArticleDTO> getAllArticles() {
        return articleRepository.findAll().stream().sorted(Comparator.comparing(Article::getPublished)).map(articleMapper::toDTO).collect(Collectors.toList());
    }

    public List<ArticleDTO> getArticlesByTopic(String topicName) {
        Topic topic = topicRepository.findByName(topicName)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicName));

        return articleRepository.findByTopic(topic).stream()
                .sorted(Comparator.comparing(Article::getPublished))
                .map(articleMapper::toDTO)
                .collect(Collectors.toList());
    }
}
