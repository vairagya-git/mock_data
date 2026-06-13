package com.example.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Component
public class WireMockRunner implements CommandLineRunner {

    @Value("${wiremock.port:9000}")
    private int port;

    @Value("${wiremock.stubs-dir:}")
    private String stubsDir;

    @Override
    public void run(String... args) throws Exception {
        WireMockConfiguration config = WireMockConfiguration.options().port(port);

        if (stubsDir != null && !stubsDir.isBlank()) {
            config = config.usingFilesUnderDirectory(stubsDir);
            System.out.println("Loading stubs from: " + stubsDir);
        }

        WireMockServer wireMockServer = new WireMockServer(config);
        wireMockServer.start();

        // helloworld stub — body loaded from helloworld/response.json relative to CWD
        Path helloWorldFile = Paths.get("helloworld", "response.json");
        String responseBody = Files.readString(helloWorldFile);
        wireMockServer.stubFor(get(urlEqualTo("/helloworld"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        System.out.println("WireMock server started on port: " + port);
        System.out.println("Endpoint: GET http://localhost:" + port + "/helloworld");

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            wireMockServer.stop();
            System.out.println("WireMock server stopped.");
            latch.countDown();
        }));
        latch.await();
    }
}
