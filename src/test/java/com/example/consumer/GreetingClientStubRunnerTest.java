package com.example.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubFinder;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer verification against the provider's LOCAL stubs.
 *
 * <p>Stub coordinates and {@code StubsMode.LOCAL} are supplied via
 * {@code stubrunner.*} properties (see src/test/resources/application.yml),
 * which are filtered from the pom's {@code provider.stubs.*} properties. This is
 * what lets cross-repo CI override the version to the one it just installed into
 * the runner-local .m2, without editing any Java code.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner
class GreetingClientStubRunnerTest {
    @Autowired
    private StubFinder stubFinder;

    @Test
    void consumerGetsGreetingFromProviderStub() {
        String stubBaseUrl = stubFinder.findStubUrl("demo-contract-provider").toString();

        GreetingClient client = new GreetingClient(stubBaseUrl);
        assertThat(client.greetingFor("Team")).isEqualTo("Hello Team");
    }
}
