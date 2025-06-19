package app.demo.services.monitoring.engines;

import app.demo.entities.Article;
import org.springframework.stereotype.Component;

@Component("betaClassifierEngine")
public class BetaClassifierEngine implements ClassifierEngine {
    @Override
    public String classify(Article article) {
        return "default";
    }
}
