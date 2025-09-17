package JYBank.JYBank.web;

import JYBank.JYBank.dto.PayoutDtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payouts")
public class PayoutController {
    @PostMapping
    public ResponseEntity<CreateRes> create(@RequestHeader("Idempotency-Key") String idemKey,
                                            @RequestBody CreateReq body) {
        // TODO: 서비스 호출 (멱등/레이트리밋은 AOP에서)
        return ResponseEntity.accepted().body(new CreateRes(1L, "PENDING"));
    }
}
