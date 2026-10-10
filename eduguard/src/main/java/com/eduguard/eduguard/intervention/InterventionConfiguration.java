package com.eduguard.eduguard.intervention;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
class InterventionConfiguration {
    @Bean
    Clock interventionClock() {
        return Clock.systemDefaultZone();
    }
}
