package com.example.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubFinder;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Deliberately-breaking consumer scenario.
 *
 * <p>Disabled by default so {@code mvn test} stays green. Enable the demo with:
 * <pre>mvn test -Pbreaking-demo</pre>
 *
 * <p>It calls a path the provider contract does NOT define. The LOCAL stub has
 * no mapping for it, so Stub Runner returns 404 and the client throws. This
 * mirrors what a real breaking consumer change (new/renamed path or unsupported
 * response) would do in cross-repo verification: the changed consumer tests run
 * against the provider contract/stubs and fail.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner
class BreakingConsumerScenarioTest {

    @Autowired
    private StubFinder stubFinder;

    @Value("${breaking.demo.enabled:false}")
    private boolean breakingDemoEnabled;

    @Test
    void callingAnUncontractedPathFailsAgainstStubs() {
        assumeTrue(breakingDemoEnabled,
                "Breaking demo disabled. Run with -Pbreaking-demo to see it fail.");

        String stubBaseUrl = stubFinder.findStubUrl("demo-contract-provider").toString();
        UncontractedClient client = new UncontractedClient(stubBaseUrl);

        assertThatThrownBy(() -> client.callWrongPath("Team"))
                .isInstanceOf(RestClientResponseException.class);
    }
}
