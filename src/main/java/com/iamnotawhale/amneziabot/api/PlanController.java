package com.iamnotawhale.amneziabot.api;

import com.iamnotawhale.amneziabot.api.dto.PlanResponse;
import com.iamnotawhale.amneziabot.service.PlanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public List<PlanResponse> listPlans() {
        return planService.listPlans();
    }
}
