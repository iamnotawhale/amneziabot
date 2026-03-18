package com.iamnotawhale.amneziabot.config;

import com.iamnotawhale.amneziabot.domain.Plan;
import com.iamnotawhale.amneziabot.repository.PlanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlanSeedConfig {

    @Bean
    CommandLineRunner seedPlans(PlanRepository planRepository) {
        return args -> {
            createIfMissing(planRepository, "TRIAL_1D", "Trial 1 day", 1, 2L * 1024 * 1024 * 1024, 1, true);
            createIfMissing(planRepository, "UNLIM_30D", "Unlimited 30 days", 30, null, 3, false);
            createIfMissing(planRepository, "GB200_30D", "200 GB 30 days", 30, 200L * 1024 * 1024 * 1024, 3, false);
            createIfMissing(planRepository, "FOREVER_UNLIM", "Forever unlimited", null, null, null, false);
        };
    }

    private void createIfMissing(
            PlanRepository planRepository,
            String code,
            String name,
                Integer durationDays,
            Long trafficLimitBytes,
                Integer deviceLimit,
            boolean isTrial
    ) {
        if (planRepository.findByCode(code).isPresent()) {
            return;
        }
        Plan plan = new Plan();
        plan.setCode(code);
        plan.setName(name);
        plan.setDurationDays(durationDays);
        plan.setTrafficLimitBytes(trafficLimitBytes);
        plan.setDeviceLimit(deviceLimit);
        plan.setTrial(isTrial);
        planRepository.save(plan);
    }
}
