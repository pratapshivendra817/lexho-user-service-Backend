package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.entity.SupportFaqCategory;
import com.lexho.user_service.admin.repository.SupportFaqCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqRepository;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.lexhopartner.dto.PageResponseDto;
import com.lexho.user_service.lexhopartner.dto.PartnerSupportCreateRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerSupportReplyRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerSupportStatusUpdateRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerSupportMessageDto;
import com.lexho.user_service.lexhopartner.dto.PartnerSupportTicketDetailDto;
import com.lexho.user_service.lexhopartner.dto.PartnerSupportTicketDto;
import com.lexho.user_service.lexhopartner.service.PartnerSupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

import static com.lexho.user_service.lexhopartner.controller.PartnerControllerText.ATTR_USER_ID;

@RestController
@RequestMapping({"/api/partner/support", "/api/v1/partner/support"})
@PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
@Tag(name = "Lexho Partner Support")
public class LexhoPartnerSupportController {

    private final PartnerSupportService partnerSupportService;
    private final SupportFaqCategoryRepository supportFaqCategoryRepository;
    private final SupportFaqRepository supportFaqRepository;

    public LexhoPartnerSupportController(PartnerSupportService partnerSupportService,
                                         SupportFaqCategoryRepository supportFaqCategoryRepository,
                                         SupportFaqRepository supportFaqRepository) {
        this.partnerSupportService = partnerSupportService;
        this.supportFaqCategoryRepository = supportFaqCategoryRepository;
        this.supportFaqRepository = supportFaqRepository;
    }

    @GetMapping("/faqs")
    @Operation(summary = "Get partner support FAQs")
    public ApiResponse<List<Map<String, Object>>> faqs() {
        List<Map<String, Object>> data = supportFaqCategoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(category -> Map.<String, Object>of(
                        "id", category.getCategoryKey(),
                        "label", category.getLabel(),
                        "displayOrder", category.getDisplayOrder(),
                        "faqs", supportFaqRepository.findByCategoryAndActiveTrueOrderByDisplayOrderAscIdAsc(category)
                                .stream()
                                .map(this::mapFaq)
                                .toList()
                ))
                .toList();
        return ApiResponse.success(PartnerControllerText.MSG_FAQS_FETCHED, data);
    }

    @PostMapping("/tickets")
    @Operation(summary = "Create partner support ticket")
    public ApiResponse<PartnerSupportTicketDto> createTicket(
            @RequestAttribute(ATTR_USER_ID) Long partnerId,
            @Valid @RequestBody PartnerSupportCreateRequest request) {
        return ApiResponse.success(PartnerControllerText.MSG_TICKET_CREATED,
                partnerSupportService.create(partnerId, request));
    }

    @GetMapping("/tickets")
    @Operation(summary = "Get partner support ticket history")
    public ApiResponse<PageResponseDto<PartnerSupportTicketDto>> tickets(
            @RequestAttribute(ATTR_USER_ID) Long partnerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PartnerControllerText.MSG_TICKET_HISTORY_FETCHED,
                partnerSupportService.history(partnerId, status, page, size));
    }

    @GetMapping("/tickets/{ticketId}")
    @Operation(summary = "Get partner support ticket details")
    public ApiResponse<PartnerSupportTicketDetailDto> ticket(
            @RequestAttribute(ATTR_USER_ID) Long partnerId,
            @PathVariable String ticketId) {
        return ApiResponse.success(PartnerControllerText.MSG_TICKET_FETCHED,
                partnerSupportService.detail(partnerId, ticketId));
    }

    @GetMapping("/tickets/{ticketId}/stream")
    @Operation(summary = "Stream partner support ticket updates")
    public SseEmitter stream(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                             @PathVariable String ticketId) {
        return partnerSupportService.stream(partnerId, ticketId);
    }

    @PostMapping("/tickets/{ticketId}/reply")
    @Operation(summary = "Reply to partner support ticket")
    public ApiResponse<PartnerSupportMessageDto> reply(
            @RequestAttribute(ATTR_USER_ID) Long partnerId,
            @PathVariable String ticketId,
            @Valid @RequestBody PartnerSupportReplyRequest request) {
        return ApiResponse.success(PartnerControllerText.MSG_REPLY_ADDED,
                partnerSupportService.reply(partnerId, ticketId, request));
    }

    @PatchMapping("/tickets/{ticketId}/status")
    @Operation(summary = "Update partner support ticket status")
    public ApiResponse<PartnerSupportTicketDto> updateStatus(
            @RequestAttribute(ATTR_USER_ID) Long partnerId,
            @PathVariable String ticketId,
            @Valid @RequestBody PartnerSupportStatusUpdateRequest request) {
        return ApiResponse.success(PartnerControllerText.MSG_TICKET_STATUS_UPDATED,
                partnerSupportService.updateStatus(partnerId, ticketId, request.status()));
    }

    private Map<String, Object> mapFaq(SupportFaq faq) {
        return Map.of(
                "id", faq.getFaqKey(),
                "question", faq.getQuestion(),
                "answer", faq.getAnswer(),
                "displayOrder", faq.getDisplayOrder()
        );
    }
}
