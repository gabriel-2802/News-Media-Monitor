package rssfetcher.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import rssfetcher.demo.services.WorkerService;

@Component
public class WorkerRunner implements ApplicationRunner {

    private final WorkerService worker;

    public WorkerRunner(WorkerService worker) {
        this.worker = worker;
    }

    @Override
    public void run(ApplicationArguments args) {
        worker.start();
    }
}
