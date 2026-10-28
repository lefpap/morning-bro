package io.github.lefpap.morningbro;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "morning-bro.run-on-startup=false",
        "spring.http.serviceclient.discord.base-url=https://example.invalid/hook"
})
class MorningBroApplicationTests {

    @Test
    void contextLoads() {
    }

}
