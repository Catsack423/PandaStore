package project.project;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import project.project.Service.api.NotificationService;
import project.project.Service.api.PaymentService;

@SpringBootTest
@Import(ProjectApplicationTests.TestConfig.class)
class ProjectApplicationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public NotificationService notificationService() {
            return Mockito.mock(NotificationService.class);
        }

        @Bean
        public PaymentService paymentService() {
            return Mockito.mock(PaymentService.class);
        }
    }

    @Test
    void contextLoads() {
    }

}
