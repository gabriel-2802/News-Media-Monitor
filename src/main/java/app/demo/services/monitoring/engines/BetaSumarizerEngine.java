package app.demo.services.monitoring.engines;

import app.demo.entities.Article;
import app.demo.entities.ArticleCluster;
import org.springframework.stereotype.Component;

@Component("BetaSumarizerEngine")
public class BetaSumarizerEngine implements SumarizerEngine {
    @Override
    public String summarize(Article article) {
        return "";
    }

    @Override
    public String summarize(ArticleCluster articleCluster) {
        return "";
    }

    private String getFirstSentence(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String[] sentences = text.split("(?<=[.!?])\\s+");
        return sentences.length > 0 ? sentences[0] : "";
    }
}
