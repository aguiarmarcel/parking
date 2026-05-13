package com.marcel.parking.controller;

import com.marcel.parking.dto.RevenueRequest;
import com.marcel.parking.dto.RevenueResponse;
import com.marcel.parking.service.RevenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/revenue")
@RestController
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    @GetMapping
    public RevenueResponse getRevenue(@Valid @RequestBody RevenueRequest request) {
        return revenueService.getRevenue(request);
    }
}
