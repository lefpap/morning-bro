package io.github.lefpap.morningbro.messaging.discord;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A message as Discord's webhook API takes it: the text and the identity it's posted under.
 *
 * @param avatarUrl left out when Morning Bro has no avatar, so the webhook's own is used
 * @param content   the message text, which takes Discord's markdown
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebhookPayload(
    String username,
    @JsonProperty("avatar_url") String avatarUrl,
    String content) {
}
