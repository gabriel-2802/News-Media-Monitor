package app.demo.services;

import app.demo.dto.TopicDTO;
import app.demo.mappers.TopicMapper;
import app.demo.repositories.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {
    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;

    public List<TopicDTO> getAllTopics() {
        return topicRepository.findAll().stream().map(topicMapper::toDTO).collect(Collectors.toList());
    }
}
