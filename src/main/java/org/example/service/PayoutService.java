package org.example.service;

import org.example.config.PayoutProperties;
import org.example.domain.Payout;
import org.example.domain.PayoutRejectionReason;
import org.example.domain.PayoutRepository;
import org.example.domain.PayoutStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class PayoutService {

    private final PayoutRepository payouts;
    private final PayoutProperties props;
    private final CallbackDispatcher callbacks;
    private final Pattern validAccount;
    private final Pattern failingAccount;

    public PayoutService(PayoutRepository payouts, PayoutProperties props, CallbackDispatcher callbacks) {
        this.payouts = payouts;
        this.props = props;
        this.callbacks = callbacks;
        this.validAccount = Pattern.compile(props.validAccountPattern());
        this.failingAccount = Pattern.compile(props.failingAccountPattern());
    }

    // idempotent on payout_id
    @Transactional
    public Payout process(String payoutId, String bankAccount, BigDecimal amount) {
        Optional<Payout> existing = payouts.findByPayoutId(payoutId);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (amount == null || amount.signum() <= 0) {
            return payouts.save(Payout.rejected(payoutId, bankAccount, amount, PayoutRejectionReason.INVALID_AMOUNT));
        }
        if (!validAccount.matcher(bankAccount).matches()) {
            return payouts.save(Payout.rejected(payoutId, bankAccount, amount, PayoutRejectionReason.BENEFICIARY_ACCOUNT_INVALID));
        }
        Payout payout = payouts.save(Payout.accepted(payoutId, bankAccount, amount,
                newBankRef(), decideOutcome(bankAccount), UUID.randomUUID().toString()));
        callbacks.enqueue(payout);
        return payout;
    }

    @Transactional(readOnly = true)
    public PayoutStatus status(String payoutId) {
        sleep(CallbackDispatcher.randomBetween(props.statusLatencyMin(), props.statusLatencyMax()).toMillis());
        return payouts.findByPayoutId(payoutId).map(Payout::getStatus).orElse(PayoutStatus.NOT_FOUND);
    }

    private PayoutStatus decideOutcome(String bankAccount) {
        if (failingAccount.matcher(bankAccount).matches()) {
            return PayoutStatus.FAILED;
        }
        return ThreadLocalRandom.current().nextDouble() < props.failureRate() ? PayoutStatus.FAILED : PayoutStatus.SUCCEEDED;
    }

    private String newBankRef() {
        byte[] bytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(bytes);
        return "BR" + HexFormat.of().withUpperCase().formatHex(bytes);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
