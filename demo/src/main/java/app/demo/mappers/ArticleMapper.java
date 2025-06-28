package app.demo.mappers;

import app.demo.dto.ArticleDTO;
import app.demo.entities.Article;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ArticleMapper {
    @Mapping(target = "clusterId", source = "cluster.id")
    @Mapping(target = "topic", source = "topic.name")
    @BeanMapping(ignoreByDefault = false)
    ArticleDTO toDTO(Article article);
}
