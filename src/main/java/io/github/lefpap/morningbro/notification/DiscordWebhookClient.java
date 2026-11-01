package io.github.lefpap.morningbro.notification;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
interface DiscordWebhookClient {

    @PostExchange
    void post(@RequestBody DiscordMessage message);
}
