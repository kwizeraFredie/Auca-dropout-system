package rw.ac.auca.dropout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AucaDropoutApplication {
    public static void main(String[] args) {
        SpringApplication.run(AucaDropoutApplication.class, args);
    }
}
