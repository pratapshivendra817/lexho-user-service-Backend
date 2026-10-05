package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.chat.dto.ChatMessagePageResponse;
import com.lexho.user_service.chat.service.ChatService;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.lexhopartner.dto.OrderResponseDto;
import com.lexho.user_service.lexhopartner.dto.PageResponseDto;
import com.lexho.user_service.lexhopartner.service.PartnerOrderService;
import com.lexho.user_service.partnerrequest.dto.PagedResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerIncomingRequestResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerRequestActionResponse;
import com.lexho.user_service.partnerrequest.service.PartnerRequestService;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/partner", "/api/v1/partner"})
@PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
public class PartnerDispatchCompatibilityController {

    private final ChatService chatService;
    private final PartnerOrderService partnerOrderService;
    private final PartnerRequestService partnerRequestService;

    public PartnerDispatchCompatibilityController(ChatService chatService,
                                                  PartnerOrderService partnerOrderService,
                                                  PartnerRequestService partnerRequestService) {
        this.chatService = chatService;
        this.partnerOrderService = partnerOrderService;
        this.partnerRequestService = partnerRequestService;
    }

    @GetMapping({"/orders/new", "/dispatch/new"})
    public ApiResponse<PageResponseDto<OrderResponseDto>> newRequests(
            @RequestAttribute(PartnerControllerText.ATTR_USER_ID) Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.success(PartnerControllerText.MSG_ORDERS_FETCHED,
                partnerOrderService.getNewOrders(partnerId, page, size, sortBy, direction));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderResponseDto> orderDetails(@RequestAttribute(PartnerControllerText.ATTR_USER_ID) Long partnerId,
                                                      @PathVariable Long id) {
        return ApiResponse.success(PartnerControllerText.MSG_ORDER_FETCHED, partnerOrderService.getOrder(partnerId, id));
    }

    @GetMapping("/orders/{id}/chat/messages")
    public ApiResponse<ChatMessagePageResponse> orderChatMessages(@PathVariable Long id,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PartnerControllerText.MSG_CHAT_MESSAGES_FETCHED, chatService.listMessagesByOrderId(id, page, size));
    }

    @GetMapping("/dispatch/requests/new")
    public ApiResponse<PagedResponse<PartnerIncomingRequestResponse>> newDispatchRequests(
            @RequestAttribute(PartnerControllerText.ATTR_USER_ID) Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                PartnerControllerText.MSG_REQUESTS_FETCHED,
                partnerRequestService.getIncomingRequests(partnerId, PartnerServiceRequestStatus.PENDING, page, size)
        );
    }

    @PostMapping("/dispatch/{requestId}/accept")
    public ApiResponse<PartnerRequestActionResponse> accept(@RequestAttribute(PartnerControllerText.ATTR_USER_ID) Long partnerId,
                                                            @PathVariable Long requestId) {
        return ApiResponse.success(PartnerControllerText.MSG_REQUEST_ACCEPTED,
                partnerRequestService.accept(partnerId, requestId));
    }

    @PostMapping("/dispatch/{requestId}/reject")
    public ApiResponse<PartnerRequestActionResponse> reject(@RequestAttribute(PartnerControllerText.ATTR_USER_ID) Long partnerId,
                                                            @PathVariable Long requestId) {
        return ApiResponse.success(PartnerControllerText.MSG_REQUEST_REJECTED,
                partnerRequestService.reject(partnerId, requestId, null));
    }
}
