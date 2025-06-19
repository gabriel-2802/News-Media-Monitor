package app.demo.scheduler;

import app.demo.services.monitoring.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonitoringScheduler {
    private final MonitorService monitorService;

}
