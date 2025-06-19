package app.demo.mappers;

import app.demo.dto.ClusterDTO;
import app.demo.entities.ArticleCluster;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ArticleMapper.class})
public interface ClusterMapper {
    ClusterDTO clusterToClusterDTO(ArticleCluster cluster);
}
