package demo.subscription.api;

import demo.subscription.app.WebhookService;
import demo.subscription.domain.GatewayWebhookEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/gateway-a")
    public String receive(
            @RequestHeader(value = "X-Demo-Signature", required = false) String signature,
            @RequestBody GatewayWebhookEvent event) {
        webhookService.handleGatewayA(signature, event);
        return "ok";
    }
}
