package app.demo.services.monitoring.engines;

import app.demo.entities.Article;
import app.demo.entities.ArticleCluster;

/*
    * interface used to summarize articles and news
 */
public interface SumarizerEngine {
    String summarize(Article article);
    String summarize(ArticleCluster articleCluster);
}
