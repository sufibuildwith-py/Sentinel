package com.sentinel.revenue.api;

import com.sentinel.revenue.webhook.WebhookRequestHandler;
import com.sentinel.revenue.webhook.WebhookResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
public class RazorpayWebhookController {
    private final WebhookRequestHandler handler;
    public RazorpayWebhookController(WebhookRequestHandler handler) { this.handler = handler; }
    @PostMapping(value = "/razorpay", consumes = "application/json")
    public ResponseEntity<WebhookResult> receive(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
        return ResponseEntity.ok(handler.handle(rawBody, signature, eventId));
    }
}
