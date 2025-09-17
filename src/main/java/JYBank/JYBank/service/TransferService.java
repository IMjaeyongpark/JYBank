package JYBank.JYBank.service;

import JYBank.JYBank.aop.annotation.Auditable;
import JYBank.JYBank.aop.annotation.Idempotent;
import JYBank.JYBank.aop.annotation.RateLimited;
import JYBank.JYBank.domain.ledger.LedgerEntry;
import JYBank.JYBank.domain.wallet.Wallet;
import JYBank.JYBank.dto.TransferDtos.*;
import JYBank.JYBank.repository.LedgerEntryRepository;
import JYBank.JYBank.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransferService {
    private final WalletRepository walletRepo;
    private final LedgerEntryRepository ledgerRepo;

    public TransferService(WalletRepository walletRepo, LedgerEntryRepository ledgerRepo) {
        this.walletRepo = walletRepo; this.ledgerRepo = ledgerRepo; }

    @Auditable(action = "TRANSFER_CREATE")
    @Idempotent(key = "#req.idemKey", ttlSeconds = 300)
    @RateLimited(key = "'transfer:' + #req.sourceWalletId", permitsPerMinute = 60)
    @Transactional
    public CreateRes create(CreateReq req) {
        Wallet src = walletRepo.findByIdForUpdate(req.sourceWalletId()).orElseThrow();
        Wallet dst = walletRepo.findByIdForUpdate(req.destWalletId()).orElseThrow();
        if (src.getBalance().compareTo(req.amount()) < 0) throw new IllegalArgumentException("INSUFFICIENT_BALANCE");

        // 차변: 출금, 대변: 입금 (원장 2건)
        src.debit(req.amount());
        dst.credit(req.amount());

        LedgerEntry d = new LedgerEntry();
        d.setWalletId(src.getId()); d.setDirection("debit");
        d.setAmount(req.amount()); d.setBalanceAfter(src.getBalance());
        d.setRefType("TRANSFER"); d.setRefId("TBD");

        LedgerEntry c = new LedgerEntry();
        c.setWalletId(dst.getId()); c.setDirection("credit");
        c.setAmount(req.amount()); c.setBalanceAfter(dst.getBalance());
        c.setRefType("TRANSFER"); c.setRefId("TBD");

        ledgerRepo.save(d); ledgerRepo.save(c);
        return new CreateRes(/*transferId*/ 0L, "COMPLETED");
    }
}
