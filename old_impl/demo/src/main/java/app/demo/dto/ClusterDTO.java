package app.demo.dto;

import java.util.Set;

public record ClusterDTO(
        Long id,
        ArticleDTO originalSource,
        Set<ArticleDTO> articles
) {}
