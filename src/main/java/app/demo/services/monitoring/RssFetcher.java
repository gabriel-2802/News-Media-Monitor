package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.services.HashService;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/*
    * RssFetcher is responsible for fetching RSS feeds from a given URL.
    * It uses the ROME library to parse the RSS feed and convert it into a list of Article entities.
    * It handles exceptions related to malformed URLs and general errors during the fetching process.
 */
@Component
@Slf4j
public class RssFetcher {
    private static final String MORSS_FEED_URL = "https://morss.it/"; // used to extract content from RSS feeds


    /**
     * Fetches articles from a given news source's RSS feed.
     *
     * @param newsSource the NewsSource containing the RSS URL
     * @return a list of Article entities fetched from the RSS feed
     */
    List<Article> fetchFrom(NewsSource newsSource) {
        List<Article> articles = new ArrayList<>();

        try (XmlReader reader = new XmlReader(URI.create(MORSS_FEED_URL + newsSource.getRssUrl()).toURL())) {
            SyndFeed feed = new SyndFeedInput().build(reader);

            for (SyndEntry entry : feed.getEntries()) {
                articles.add(RssMapper.toEntity(entry, newsSource));
            }

        } catch (MalformedURLException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching RSS feed from {}: {}", newsSource.getRssUrl(), e.getMessage());
        }

        return articles;
    }

    /*
     * Inner class to encapsulate the mapping logic.
     * This class is used to convert a SyndEntry object from the RSS feed into an Article entity.
    */
    private static class RssMapper {
        /**
         * Converts a SyndEntry to an Article entity.
         *
         * @param syndEntry  the SyndEntry to convert
         * @param newsSource the NewsSource associated with the article
         * @return an Article entity populated with data from the SyndEntry
         */
        public static Article toEntity(SyndEntry syndEntry, NewsSource newsSource) {
            Article article = new Article();
            article.setTitle(syndEntry.getTitle());
            article.setTitle(syndEntry.getTitle());
            article.setContent(Jsoup.parse(syndEntry.getContents() != null && !syndEntry.getContents().isEmpty() ? syndEntry.getContents().getFirst().getValue() : "No content").text());
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
