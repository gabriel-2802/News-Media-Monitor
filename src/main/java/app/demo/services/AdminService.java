package app.demo.services;

import app.demo.dto.NewsSourceDTO;
import app.demo.dto.UserDTO;
import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.entities.Topic;
import app.demo.entities.User;
import app.demo.events.NewsSourceRepoChangeEvent;
import app.demo.events.TopicRepositoryChangeEvent;
import app.demo.exceptions.ExistingRssSource;
import app.demo.exceptions.SourceNotExisting;
import app.demo.exceptions.TopicAlreadyExistsException;
import app.demo.exceptions.TopicNotFoundException;
import app.demo.mappers.NewsSourceMapper;
import app.demo.mappers.UserMapper;
import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import app.demo.repositories.TopicRepository;
import app.demo.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TopicRepository topicRepository;
    private final NewsSourceMapper newsSourceMapper;
    private final NewsSourceRepository newsSourceRepository;
    private final ArticleRepository articleRepository;
    private final ApplicationEventPublisher eventPublisher;


    public List<UserDTO> getAllUsers() {
        var users = userRepository.findAllWithTopics();
        return userMapper.toDTO(users);
    }

    public UserDTO getUser(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return userMapper.toDTO(user.get());
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    @Transactional
    public void deleteUser(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            userRepository.delete(user.get());
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    @Transactional
    public void createTopic(String topicName) throws TopicAlreadyExistsException {
        // normalize topic name and remove special characters
        String normalizedTopicName = normalizeTopicName(topicName);

        if (topicRepository.existsByName(normalizedTopicName)) {
            throw new TopicAlreadyExistsException(normalizedTopicName);
        } else {
            Topic topic = new Topic();
            topic.setName(normalizedTopicName);
            topicRepository.save(topic);
            eventPublisher.publishEvent(new TopicRepositoryChangeEvent(topic, false));
        }
    }

    @Transactional
    public void deleteTopic(String topicName) throws TopicNotFoundException {
        String normalizedTopicName = normalizeTopicName(topicName);
        Optional<Topic> topic = topicRepository.findByName(normalizedTopicName);

        if (topic.isEmpty()) {
            throw new TopicNotFoundException(normalizedTopicName);
        }

        Topic topicToDelete = topic.get();
        Topic defaultTopic = topicRepository.getDefaultTopic();

        // reassign articles
        List<Article> articles = articleRepository.findByTopic(topicToDelete).stream()
                .peek(article -> article.setTopic(defaultTopic))
                .toList();
        articleRepository.saveAll(articles);

        // unlink users
        List<User> users = userRepository.findAllSubcribedToTopic(topicToDelete);
        users.forEach(user -> {
            user.getSubscribedTopics().remove(topicToDelete);
            userRepository.save(user);
        });

        topicRepository.delete(topicToDelete);
        eventPublisher.publishEvent(new TopicRepositoryChangeEvent(topicToDelete, true));
    }


    private String normalizeTopicName(String topicName) {
        return topicName.trim().toLowerCase()
                .replaceAll("\\d", "")
                .replaceAll("[^a-z\\s]", " ")
                .replaceAll("\\s+", "_");
    }

    @Transactional
    public NewsSourceDTO createNewsSource(NewsSourceDTO newsSourceDTO) throws ExistingRssSource {
        if (newsSourceRepository.existsByRssUrl(newsSourceDTO.getRssUrl())) {
            throw new ExistingRssSource();
        }

        NewsSource newsSource = newsSourceMapper.toEntity(newsSourceDTO);
        newsSourceRepository.save(newsSource);
        eventPublisher.publishEvent(new NewsSourceRepoChangeEvent());
        return newsSourceMapper.toDTO(newsSource);
    }

    @Transactional
    public NewsSourceDTO deleteNewsSource(Long id) throws IllegalArgumentException, SourceNotExisting {
        NewsSource newsSource = newsSourceRepository.findById(id)
                .orElseThrow(() -> new SourceNotExisting("News source with id " + id + " does not exist"));

        newsSourceRepository.delete(newsSource);
        eventPublisher.publishEvent(new NewsSourceRepoChangeEvent());
        return newsSourceMapper.toDTO(newsSource);
    }

    public List<NewsSourceDTO> getAllNewsSources() {
        List<NewsSource> newsSources = newsSourceRepository.findAll();
        return newsSources.stream()
                .map(newsSourceMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAllArticles() {
        articleRepository.deleteAll();
    }

}
