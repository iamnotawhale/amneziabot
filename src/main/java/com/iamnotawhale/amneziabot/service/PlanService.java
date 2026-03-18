package com.iamnotawhale.amneziabot.service;

import com.iamnotawhale.amneziabot.api.dto.PlanResponse;
import com.iamnotawhale.amneziabot.domain.Plan;
import com.iamnotawhale.amneziabot.repository.PlanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<PlanResponse> listPlans() {
        return planRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Plan getByCode(String code) {
        return planRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Plan not found: " + code));
    }

    private PlanResponse toResponse(Plan plan) {
        return new PlanResponse(
                plan.getCode(),
                plan.getName(),
                plan.getDurationDays(),
                plan.getTrafficLimitBytes(),
                plan.getDeviceLimit(),
                plan.isTrial()
        );
    }
}
