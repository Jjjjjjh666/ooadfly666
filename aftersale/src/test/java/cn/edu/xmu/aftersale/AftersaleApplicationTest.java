package cn.edu.xmu.aftersale;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class AftersaleApplicationTest {

    @Test
    void mainShouldDelegateToSpringApplication() {
        String[] args = {"--spring.main.web-application-type=none"};

        try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(AftersaleApplication.class, args))
                    .thenReturn(null);

            AftersaleApplication.main(args);

            mocked.verify(() -> SpringApplication.run(AftersaleApplication.class, args));
        }
    }
}
