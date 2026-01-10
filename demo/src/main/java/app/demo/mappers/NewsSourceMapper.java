package app.demo.mappers;

import app.demo.dto.NewsSourceDTO;
import app.demo.entities.NewsSource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NewsSourceMapper {
    NewsSourceDTO toDTO(NewsSource newsSource);

    NewsSource toEntity(NewsSourceDTO newsSourceDTO);
}
