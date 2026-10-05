package com.lexho.user.controller;

import com.lexho.user.dto.ApiResponse;
import com.lexho.user.dto.CallActionRequest;
import com.lexho.user.dto.CallHistoryResponse;
import com.lexho.user.dto.CallSessionResponse;
import com.lexho.user.dto.PagedResponse;
import com.lexho.user.security.AuthenticatedUser;
import com.lexho.user.security.CurrentUserService;
import com.lexho.user.service.CallService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/calls")
public class CallController {

    private final CallService callService;
    private final CurrentUserService currentUserService;

    public CallController(CallService callService, CurrentUserService currentUserService) {
        this.callService = callService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/history")
    public ApiResponse<PagedResponse<CallHistoryResponse>> history(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        AuthenticatedUser user = currentUserService.requireCurrentUser();
        return ApiResponse.success(callService.getHistory(user, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CallSessionResponse> getCall(@PathVariable Long id) {
        AuthenticatedUser user = currentUserService.requireCurrentUser();
        return ApiResponse.success(callService.getCall(user, id));
    }

    @PostMapping("/end")
    public ApiResponse<CallSessionResponse> endCall(@Valid @RequestBody CallActionRequest request) {
        AuthenticatedUser user = currentUserService.requireCurrentUser();
        return ApiResponse.success(callService.endCall(user, request.sessionId()));
    }

    @GetMapping("/missed")
    public ApiResponse<List<CallHistoryResponse>> missedCalls() {
        AuthenticatedUser user = currentUserService.requireCurrentUser();
        return ApiResponse.success(callService.getMissedCalls(user));
    }
}
