package teste.magalu;

import checkout.PaymentServiceApplication;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("Requires Docker for Testcontainers (MySQL) and external Redis instance. Run with Docker daemon available.")
@SpringBootTest(classes = PaymentServiceApplication.class)
@ActiveProfiles("test")
class MagaluApplicationTests {

	@Test
	void contextLoads() {
	}

}
