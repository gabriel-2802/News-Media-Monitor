package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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

        try  {
            SyndFeed feed = fetchFrom(MORSS_FEED_URL + newsSource.getRssUrl());
            for (SyndEntry entry : feed.getEntries()) {
                articles.add(RssMapper.toEntity(entry, newsSource));
            }
        } catch (Exception e) {
            try {
                // if morss.it fails, try fetching directly from the RSS URL
                SyndFeed feed = fetchFrom(newsSource.getRssUrl());
                for (SyndEntry entry : feed.getEntries()) {
                    articles.add(RssMapper.toEntity(entry, newsSource));
                }
            } catch (Exception ex) {
                log.error("Failed to fetch articles from {}: {}", newsSource.getName(), ex.getMessage());
            }
        }

        return articles;
    }

    private SyndFeed fetchFrom(String rssUrl) throws MalformedURLException, Exception {
        XmlReader reader = new XmlReader(URI.create(rssUrl).toURL());
        return new SyndFeedInput().build(reader);
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
            article.setSummary(syndEntry.getDescription() != null ? syndEntry.getDescription().getValue() : " ");
            return article;
        }
    }
}
