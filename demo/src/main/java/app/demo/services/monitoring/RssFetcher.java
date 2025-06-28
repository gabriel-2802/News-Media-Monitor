package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.services.HashService;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * component responsible for fetching and parsing RSS feeds from external news sources.
 * <p>
 * it uses the morss.it service to enrich feed content and converts entries into {@link Article} entities.
 */
@Component
@Slf4j
public class RssFetcher {
    private static final String MORSS_FEED_URL = "https://morss.it/"; // used to extract content from RSS feeds

    public List<Article> fetchFrom(NewsSource newsSource) {
        List<Article> articles = new ArrayList<>();

        try (XmlReader reader = new XmlReader(URI.create(MORSS_FEED_URL + newsSource.getRssUrl()).toURL())) {
            SyndFeed feed = new SyndFeedInput().build(reader);

            for (SyndEntry entry : feed.getEntries()) {
                articles.add(RssMapper.toEntity(entry, newsSource));
            }

        } catch (MalformedURLException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching RSS feed from {}: {}", newsSource.getRssUrl(), e.getMessage(), e);
        }

        return articles;
    }

    /*
     * inner class to encapsulate the mapping logic.
     * converts a SyndEntry object from the RSS feed into an Article entity.
    */
    private static class RssMapper {
        public static Article toEntity(SyndEntry syndEntry, NewsSource newsSource) {
            Article article = new Article();
            article.setTitle(syndEntry.getTitle());
            article.setContent(Jsoup.parse(!CollectionUtils.isEmpty(syndEntry.getContents()) ? syndEntry.getContents().getFirst().getValue() : " ").text());
            article.setSource(newsSource.getName());
            article.setUrl(syndEntry.getLink());
            article.setPublished(syndEntry.getPublishedDate() == null ? Date.from(Instant.now()) : syndEntry.getPublishedDate());
            article.setSummary(syndEntry.getDescription() != null ? syndEntry.getDescription().getValue() : "No description");
            article.setSha256Hash(HashService.sha256(article.getContent()));
            article.setSimHash(HashService.simHash(article.getContent()));
            return article;
        }
    }
}
