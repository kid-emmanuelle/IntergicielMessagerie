package org.insa.intergicielmessagerie;

import org.insa.intergicielmessagerie.service.FileService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IntergicielMessagerieApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntergicielMessagerieApplication.class, args);
    }

    @Bean
    CommandLineRunner init(FileService fileService) {
        return args -> {
            // Initialize file storage directory
            fileService.init();
        };
    }
}
