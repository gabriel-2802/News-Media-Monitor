package app.demo.services;

import app.demo.dto.ArticleDTO;
import app.demo.dto.NewsSourceDTO;
import app.demo.dto.SearchRequestDTO;
import app.demo.dto.TopicDTO;
import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.entities.Topic;
import app.demo.mappers.ArticleMapper;
import app.demo.mappers.NewsSourceMapper;
import app.demo.mappers.TopicMapper;
import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import app.demo.repositories.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final NewsSourceRepository newsSourceRepository;
    private final NewsSourceMapper newsSourceMapper;

    public List<TopicDTO> getAllTopics() {
        return topicRepository.findAll().stream().map(topicMapper::toDTO).collect(Collectors.toList());
    }

    public List<NewsSourceDTO> getAllNewsSources() {
        List<NewsSource> newsSources = newsSourceRepository.findAll();
        return newsSources.stream()
                .map(newsSourceMapper::toDTO)
                .collect(Collectors.toList());
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

    public List<ArticleDTO> searchArticles(SearchRequestDTO searchRequest) {
        List<Article> articles = SearchByKeyword(searchRequest.keyword());
        addTopicConstraint(searchRequest.topicName(), articles);
        addSourceConstraint(searchRequest.sourceName(), articles);

        return articles.stream()
                .sorted(Comparator.comparing(Article::getPublished))
                .map(articleMapper::toDTO)
                .collect(Collectors.toList());
    }

    private List<Article> SearchByKeyword(String keyword) {
        if (keyword.isEmpty()) {
            return new ArrayList<> ();
        }
        return articleRepository.searchByKeyword(keyword);
    }

    private void addTopicConstraint(String topicName, List<Article> articles) {
        if (topicName.isEmpty()) {
            return;
        }

        if (articles.isEmpty()) {
            articles.addAll(articleRepository.findByTopicName(topicName));
        } else {
            articles.removeIf(article -> !article.getTopic().getName().equals(topicName));
        }
    }

    private void addSourceConstraint(String sourceName, List<Article> articles) {
        if (sourceName.isEmpty()) {
            return;
        }

        if (articles.isEmpty()) {
            articles.addAll(articleRepository.findBySourceName(sourceName));
        } else {
            articles.removeIf(article -> !article.getSource().equals(sourceName));
        }
    }

    public List<ArticleDTO> getArticlesByClusterId(Long clusterId) {
        if (clusterId == null) {
            return new ArrayList<>();
        }
        return articleRepository.findByClusterId(clusterId).stream()
                .sorted(Comparator.comparing(Article::getPublished))
                .map(articleMapper::toDTO)
                .collect(Collectors.toList());
    }
}


