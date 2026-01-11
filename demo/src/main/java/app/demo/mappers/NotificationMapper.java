package app.demo.mappers;

import app.demo.dto.NotificationDTO;
import app.demo.entities.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "article.id", target = "articleId")
    @Mapping(source = "article.title", target = "articleTitle")
    NotificationDTO toDto(Notification notification);
}
