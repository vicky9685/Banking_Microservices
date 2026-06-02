package com.bank.transaction.api;

import com.bank.common.dto.ApiResponse;
import com.bank.transaction.dto.TransferDtos.*;
import com.bank.transaction.service.TransferService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ApiResponse<TransferResponse> initiate(@Valid @RequestBody TransferRequest req,
                                                  @RequestHeader(value = "Idempotency-Key", required = false) String idemHeader) {
        if (req.idempotencyKey() == null && idemHeader != null) {
            req = new TransferRequest(req.fromAccountId(), req.toAccountId(),
                    req.amount(), req.currency(), idemHeader);
        }
        return ApiResponse.ok(transferService.initiate(req), "Transfer accepted");
    }

    @GetMapping("/{id}")
    public ApiResponse<TransferResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(transferService.get(id));
    }

    /**
     * Decision callback from workflow-service after a human reviews a held transfer.
     * Auth: in production this would require a service-to-service mTLS identity or
     * a signed token, since the callback can release funds.
     */
    @PostMapping("/{id}/decision")
    public ApiResponse<TransferResponse> decision(@PathVariable UUID id,
                                                  @RequestParam boolean approved,
                                                  @RequestParam(required = false) String comment) {
        return ApiResponse.ok(transferService.applyDecision(id, approved, comment));
    }
}
