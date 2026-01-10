package rssfetcher.demo.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rssfetcher.demo.services.ClusterService;
import rssfetcher.demo.services.MonitorService;

@Component
@Slf4j
@RequiredArgsConstructor
public class ArticleScheduler {
    private final ClusterService clusterService;
    private final MonitorService monitorService;

//    @Scheduled(fixedDelayString = "${scheduling.timeframe}")
    public void fetchAndClusterArticles() {
        monitorService.startMonitoring();
        clusterService.cluster();
    }
}
