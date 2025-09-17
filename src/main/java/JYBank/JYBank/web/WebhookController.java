package JYBank.JYBank.web;

import JYBank.JYBank.dto.DepositDtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {
    @PostMapping("/deposit")
    public ResponseEntity<Res> deposit(@RequestHeader("X-Signature") String sig,
                                       @RequestBody WebhookReq body) {
        // TODO: HMAC(sig) 검증 + 멱등(pgTrxId)
        return ResponseEntity.ok(new Res(1L, "COMPLETED"));
    }
}
