package app.demo.services;

import app.demo.dto.TopicDTO;
import app.demo.dto.UserDTO;
import app.demo.entities.Topic;
import app.demo.entities.User;
import app.demo.exceptions.AlreadySubscribed;
import app.demo.exceptions.TopicNotFoundException;
import app.demo.mappers.TopicMapper;
import app.demo.mappers.UserMapper;
import app.demo.repositories.TopicRepository;
import app.demo.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAccountService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;

    public UserDTO getUserProfile(String username) {
        return userRepository.findByUsernameWithTopics(username)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional
    public TopicDTO subscribeToTopic(Long topicId, String username) throws TopicNotFoundException, AlreadySubscribed {
        User user = userRepository.findByUsernameWithTopics(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException("Topic does not exist"));

        Set<Topic> topics = user.getSubscribedTopics();

        if (topics.contains(topic)) {
            throw new AlreadySubscribed(topic.getName());
        }

        topics.add(topic);
        userRepository.save(user);
        return topicMapper.toDTO(topic);
    }

    @Transactional
    public void unsubscribeFromTopic(Long topicId, String username) throws TopicNotFoundException {
        User user = userRepository.findByUsernameWithTopics(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException("Topic does not exist"));

        Set<Topic> topics = user.getSubscribedTopics();
        if (!topics.remove(topic)) {
            throw new TopicNotFoundException("Topic not found in user's subscriptions");
        }

        userRepository.save(user);
    }
}
