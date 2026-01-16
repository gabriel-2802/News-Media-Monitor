package app.demo.services;

import app.demo.dto.NewsSourceDTO;
import app.demo.dto.UserDTO;
import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.entities.User;
import app.demo.events.NewsSourceRepoChangeEvent;
import app.demo.exceptions.ExistingRssSource;
import app.demo.exceptions.SourceNotExisting;
import app.demo.mappers.NewsSourceMapper;
import app.demo.mappers.UserMapper;
import app.demo.repositories.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final NewsSourceMapper newsSourceMapper;
    private final NewsSourceRepository newsSourceRepository;
    private final ArticleRepository articleRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ArticleClusterRepository articleClusterRepository;


    public List<UserDTO> getAllUsers() {
        var users = userRepository.findAllWithTopics();
        return userMapper.toDTO(users);
    }

    public UserDTO getUser(String username) {
        Optional<User> user = userRepository.findByUsernameWithTopics(username);
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
    public NewsSourceDTO createNewsSource(NewsSourceDTO newsSourceDTO) throws ExistingRssSource {
        if (newsSourceRepository.existsByRssUrl(newsSourceDTO.rssUrl())) {
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

    @Transactional
    public void purgeAll() {
        List<Article> articles = articleRepository.findAll().stream().peek(a -> a.setCluster(null)).toList();
        articleRepository.saveAll(articles);
        notificationRepository.deleteAll();
        articleClusterRepository.deleteAll();
        articleRepository.deleteAll();
    }
}
