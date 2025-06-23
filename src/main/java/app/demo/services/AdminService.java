package app.demo.services;

import app.demo.dto.NewsSourceDTO;
import app.demo.dto.UserDTO;
import app.demo.entities.NewsSource;
import app.demo.entities.Topic;
import app.demo.entities.User;
import app.demo.events.NewsSourceRepoChangeEvent;
import app.demo.events.TopicRepositoryChangeEvent;
import app.demo.exceptions.ExistingRssSource;
import app.demo.exceptions.TopicAlreadyExistsException;
import app.demo.exceptions.TopicDoesNotExistException;
import app.demo.mappers.NewsSourceMapper;
import app.demo.mappers.UserMapper;
import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import app.demo.repositories.TopicRepository;
import app.demo.repositories.UserRepository;
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
        var users = userRepository.findAll();
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

    public void deleteUser(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            userRepository.delete(user.get());
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    public void createTopic(String topicName) throws TopicAlreadyExistsException {
        // normalize topic name and remove special characters
        String normalizedTopicName = normalizeTopicName(topicName);

        if (topicRepository.existsByName(normalizedTopicName)) {
            throw new IllegalArgumentException("Topic with this name already exists");
        } else {
            Topic topic = new Topic();
            topic.setName(normalizedTopicName);
            topicRepository.save(topic);
            eventPublisher.publishEvent(new TopicRepositoryChangeEvent(topic, false));
        }
    }

    public void deleteTopic(String topicName) throws TopicDoesNotExistException {
        String normalizedTopicName = normalizeTopicName(topicName);
        Optional<Topic> topic = topicRepository.findByName(normalizedTopicName);
        if (topic.isPresent()) {
            topicRepository.delete(topic.get());
            eventPublisher.publishEvent(new TopicRepositoryChangeEvent(topic.get(), true));
        } else {
            throw new TopicDoesNotExistException(topicName);
        }
    }

    private String normalizeTopicName(String topicName) {
        return topicName.trim().toLowerCase()
                .replaceAll("\\d", "")                  // remove all digits
                .replaceAll("[^a-z\\s]", "")            // remove anything that's not a-z or whitespace
                .replaceAll("\\s+", "_");               // convert spaces to underscores
    }

    public NewsSourceDTO createNewsSource(NewsSourceDTO newsSourceDTO) throws ExistingRssSource {
        if (newsSourceRepository.existsByRssUrl(newsSourceDTO.getRssUrl())) {
            throw new ExistingRssSource();
        }

        NewsSource newsSource = newsSourceMapper.toEntity(newsSourceDTO);
        newsSourceRepository.save(newsSource);
        eventPublisher.publishEvent(new NewsSourceRepoChangeEvent());
        return newsSourceMapper.toDTO(newsSource);
    }

    public String deleteNewsSource(Long id) throws IllegalArgumentException {
        NewsSource newsSource = newsSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("News source not found with id: " + id));

        newsSourceRepository.delete(newsSource);
        eventPublisher.publishEvent(new NewsSourceRepoChangeEvent());
        return "News source deleted successfully";
    }

    public List<NewsSourceDTO> getAllNewsSources() {
        List<NewsSource> newsSources = newsSourceRepository.findAll();
        return newsSources.stream()
                .map(newsSourceMapper::toDTO)
                .collect(Collectors.toList());
    }

    public String deleteAllArticles() {
        try {
            articleRepository.deleteAll();
            return "All articles deleted successfully";
        } catch (Exception e) {
            return "Error deleting articles: " + e.getMessage();
        }
    }

}
