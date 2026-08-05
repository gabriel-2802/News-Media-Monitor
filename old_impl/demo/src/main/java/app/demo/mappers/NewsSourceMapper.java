package app.demo.mappers;

import app.demo.dto.NewsSourceDTO;
import app.demo.entities.NewsSource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NewsSourceMapper {
    NewsSourceDTO toDTO(NewsSource newsSource);

    @Mapping(target = "lastFetchedAt", ignore = true)
    @Mapping(target = "fetchedThisCycle", constant = "false")
    @Mapping(target = "lockedBy", ignore = true)
    @Mapping(target = "lockedAt", ignore = true)
    @Mapping(target = "consecutiveFailures", constant = "0")
    @Mapping(target = "lastError", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    NewsSource toEntity(NewsSourceDTO newsSourceDTO);
}
