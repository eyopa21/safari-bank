package org.example.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.service.PayoutService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bank/payouts")
@Tag(name = "Payouts")
public class PayoutController {

    private final PayoutService service;

    @PostMapping
    @Operation(summary = "Submit a payout")
    public PayoutResponse submit(@Valid @RequestBody PayoutRequest request) {
        return PayoutResponse.from(service.process(request.payoutId(), request.bankAccount(), request.amount()));
    }

    @GetMapping("/{payout_id}")
    @Operation(summary = "Bank's view of a payout")
    public PayoutStatusResponse status(@PathVariable("payout_id") String payoutId) {
        return new PayoutStatusResponse(service.status(payoutId));
    }
}
