package checkout.config.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableR2dbcRepositories(basePackages = "checkout.domain")
@EnableTransactionManagement
public class R2dbcConfig {
    // For now we will depend on springboot and docker to perform the connections to the database.
}