package app.demo.mappers;

import app.demo.dto.TopicDTO;
import app.demo.entities.Topic;
import app.demo.entities.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface TopicMapper {
    @Mapping(target = "subscribers", expression = "java(mapSubscribers(topic))")
    @BeanMapping(ignoreByDefault = false)
    TopicDTO toDTO(Topic topic);

    default Set<String> mapSubscribers(Topic topic) {
        return topic.getSubscribers().stream()
                .map(User::getUsername)
                .collect(java.util.stream.Collectors.toSet());
    }
}
