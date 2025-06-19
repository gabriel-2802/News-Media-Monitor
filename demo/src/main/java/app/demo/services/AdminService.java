package app.demo.services;

import app.demo.dto.TopicDTO;
import app.demo.dto.UserDTO;
import app.demo.entities.Topic;
import app.demo.entities.User;
import app.demo.exceptions.TopicAlreadyExistsException;
import app.demo.exceptions.TopicDoesNotExistException;
import app.demo.mappers.TopicMapper;
import app.demo.mappers.UserMapper;
import app.demo.repositories.TopicRepository;
import app.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
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
    private final TopicMapper topicMapper;

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

    public void createTopic(String topicName) throws TopicAlreadyExistsException {
        // normalize topic name and remove special characters
        String normalizedTopicName = normalizeTopicName(topicName);

        if (topicRepository.existsByName(normalizedTopicName)) {
            throw new IllegalArgumentException("Topic with this name already exists");
        } else {
            Topic topic = new Topic();
            topic.setName(normalizedTopicName);
            topicRepository.save(topic);
        }
    }

    public void deleteTopic(String topicName) throws TopicDoesNotExistException {
        String normalizedTopicName = normalizeTopicName(topicName);
        Optional<Topic> topic = topicRepository.findByName(normalizedTopicName);
        if (topic.isPresent()) {
            topicRepository.delete(topic.get());
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
}
