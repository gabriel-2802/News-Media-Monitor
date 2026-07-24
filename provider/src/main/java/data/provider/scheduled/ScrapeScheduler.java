package data.provider.scheduled;

import data.provider.services.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScrapeScheduler {

    private final ArticleService articleService;

    @Scheduled(cron = "0 0 */6 * * *")
    public void triggerScheduledScrape() {
        articleService.triggerScrape();
    }
}
