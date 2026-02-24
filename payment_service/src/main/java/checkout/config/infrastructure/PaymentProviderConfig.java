package checkout.config.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class PaymentProviderConfig {

    @Value("${ABACATEPAY_API_URL}")
    private String payment_provider_url;

    @Value("${PSP_API_KEY}")
    private String apiKey;

    @Value("${ABACATEPAY_TIMEOUT_MS}")
    private Long timeOut;


    @Bean(name = "paymentProviderWebClient")
    public WebClient abacatePayWebClient() {

        ConnectionProvider connectionProvider = ConnectionProvider.builder("abacatePayWebClient")
                .maxConnections(100)
                .pendingAcquireMaxCount(1000)
                .pendingAcquireTimeout(Duration.ofSeconds(45))
                .maxIdleTime(Duration.ofSeconds(20))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(Duration.ofMillis(timeOut))
                .secure()
                .compress(true);

        return WebClient.builder()
                .baseUrl(payment_provider_url)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "MagaluPaymentService/1.0")
                .build();

    }
}
