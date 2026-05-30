package checkout.config.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

@Configuration
@EnableR2dbcRepositories(basePackages = "checkout.domain")
@EnableTransactionManagement
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return new R2dbcCustomConversions(R2dbcCustomConversions.StoreConversions.NONE,
                List.of(new DocumentWriteConverter(), new DocumentReadConverter()));
    }
}