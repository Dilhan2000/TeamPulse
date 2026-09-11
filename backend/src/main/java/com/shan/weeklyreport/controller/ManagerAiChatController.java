package com.shan.weeklyreport.controller;

import com.shan.weeklyreport.dto.ai.*;
import com.shan.weeklyreport.exception.AiServiceDisabledException;
import com.shan.weeklyreport.security.UserPrincipal;
import com.shan.weeklyreport.service.ai.AiChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Manager AI Chat Assistant endpoints (SRS08 C8-T06).
 */
@RestController
@RequestMapping("/api/manager/ai-chat")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerAiChatController {

    private final AiChatService aiChatService;

    public ManagerAiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @GetMapping("/availability")
    public ResponseEntity<AiAvailabilityResponse> checkAvailability() {
        boolean available = aiChatService.isAvailable();
        return ResponseEntity.ok(new AiAvailabilityResponse(available));
    }

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChatMessageRequest request) {

        if (!aiChatService.isAvailable()) {
            throw new AiServiceDisabledException("AI assistant is not configured");
        }

        String reply = aiChatService.chat(principal.id(), request.history(), request.message());
        return ResponseEntity.ok(new ChatResponse(reply));
    }

    @PostMapping("/summary")
    public ResponseEntity<SummaryResponse> generateSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerateSummaryRequest request) {

        if (!aiChatService.isAvailable()) {
            throw new AiServiceDisabledException("AI assistant is not configured");
        }

        String summary = aiChatService.generateSummary(
                principal.id(),
                request.weekStartDate(),
                request.projectId()
        );
        return ResponseEntity.ok(new SummaryResponse(summary));
    }
}
