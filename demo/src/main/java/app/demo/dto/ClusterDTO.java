package app.demo.dto;

import lombok.Data;
import app.demo.dto.ArticleDTO;

import java.util.HashSet;
import java.util.Set;

@Data
public class ClusterDTO {
    private Long id;
    private ArticleDTO originalSource;
    private String topic;
    private Set<ArticleDTO> articles = new HashSet<>();
}
