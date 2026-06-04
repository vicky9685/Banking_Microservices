package com.bank.transaction.api;

import com.bank.common.dto.ApiResponse;
import com.bank.transaction.dto.TransferDtos.*;
import com.bank.transaction.service.TransferService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Transfers")
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BACKOFFICE')")
    public ApiResponse<TransferResponse> initiate(@Valid @RequestBody TransferRequest req,
                                                  @RequestHeader(value = "Idempotency-Key", required = false) String idemHeader) {
        if (req.idempotencyKey() == null && idemHeader != null) {
            req = new TransferRequest(req.fromAccountId(), req.toAccountId(),
                    req.amount(), req.currency(), idemHeader);
        }
        return ApiResponse.ok(transferService.initiate(req), "Transfer accepted");
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TransferResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(transferService.get(id));
    }

    /**
     * Decision callback from workflow-service after a human reviews a held transfer.
     * Restricted to RISK_OFFICER / SENIOR_RISK_OFFICER / BACKOFFICE because the
     * call releases funds. The HMAC service-signature filter blocks direct hits
     * regardless; this is the user-identity layer.
     */
    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('RISK_OFFICER','SENIOR_RISK_OFFICER','BACKOFFICE')")
    public ApiResponse<TransferResponse> decision(@PathVariable UUID id,
                                                  @RequestParam boolean approved,
                                                  @RequestParam(required = false) String comment) {
        return ApiResponse.ok(transferService.applyDecision(id, approved, comment));
    }
}
