package app.demo.mappers;

import app.demo.dto.TopicDTO;
import app.demo.entities.Topic;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TopicMapper {
    @BeanMapping(ignoreByDefault = false)
    TopicDTO toDTO(Topic topic);
}
