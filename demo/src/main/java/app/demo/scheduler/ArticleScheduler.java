package app.demo.scheduler;

import app.demo.services.monitoring.ClusterService;
import app.demo.services.monitoring.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ArticleScheduler {
    private final ClusterService clusterService;
    private final MonitorService monitorService;

    @Scheduled(fixedDelayString = "${scheduling.monitor-cluster}")
    public void fetchAndClusterArticles() {
        monitorService.startMonitoring();
        clusterService.cluster();
    }
}
