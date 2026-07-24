package news.media.monitor.manager.config;

import news.media.monitor.manager.exceptions.exceptions.DatabaseValidationException;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class FlywayConfig {

    private static final String SCHEMA                   = "users";
    private static final String MIGRATION_LOCATION       = "classpath:db/migration";
    private static final String BASELINE_VERSION         = "0";
    private static final String LOG_MIGRATION_FAILED     = "Flyway migration failed: {}";
    private static final String ERR_MIGRATION_FAILED     = "Database migration failed: ";

    @Bean
    public Flyway flyway(DataSource dataSource) {
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(SCHEMA)
                    .defaultSchema(SCHEMA)
                    .locations(MIGRATION_LOCATION)
                    .baselineOnMigrate(true)
                    .baselineVersion(BASELINE_VERSION)
                    .validateOnMigrate(true)
                    .outOfOrder(false)
                    .failOnMissingLocations(false)
                    .cleanDisabled(true)
                    .connectRetries(10)
                    .connectRetriesInterval(3)
                    .load();

            flyway.migrate();
            return flyway;
        } catch (FlywayException e) {
            log.error(LOG_MIGRATION_FAILED, e.getMessage(), e);
            throw new DatabaseValidationException(ERR_MIGRATION_FAILED + e.getMessage());
        }
    }
}