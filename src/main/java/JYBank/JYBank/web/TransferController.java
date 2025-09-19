package JYBank.JYBank.web;

import JYBank.JYBank.dto.TransferDtos.*;
import JYBank.JYBank.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/transfers")
public class TransferController {
    private final TransferService service;
    public TransferController(TransferService service) { this.service = service; }

    //송금, 결제, 주문
    @PostMapping
    public ResponseEntity<CreateRes> create(@RequestHeader("Idempotency-Key") String idemKey,
                                            @RequestBody @Valid CreateReq body) {
        body = new CreateReq(body.sourceWalletId(), body.destWalletId(), body.amount(), body.memo(), idemKey);
        return ResponseEntity.ok(service.create(body));
    }
}
