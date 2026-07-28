package data.provider.services;

import data.provider.dto.responses.TopicDto;
import data.provider.repositories.ArticleRepository;
import data.provider.repositories.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final ArticleRepository articleRepository;

    public boolean exists(final String name) {
        return topicRepository.existsByName(name);
    }

    public List<TopicDto> getAllTopics() {
        return topicRepository.findAll().stream()
                .map(topic -> new TopicDto(topic, articleRepository.countByTopic(topic.getName())))
                .toList();
    }
}
