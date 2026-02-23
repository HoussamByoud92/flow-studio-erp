package com.flowstudio.api_gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class GatewayFallbackController {

    @GetMapping(value = "/", produces = "text/html")
    public Mono<String> gatewayStatus() {
        String html = """
                <html>
                <head>
                    <title>API Gateway</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f9; padding: 50px; text-align: center; }
                        .container { background: #fff; padding: 40px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); display: inline-block; max-width: 600px; }
                        h1 { color: #2c3e50; margin-bottom: 10px; }
                        .status { color: #27ae60; font-weight: bold; font-size: 1.3em; padding: 5px 10px; border: 2px solid #27ae60; border-radius: 5px; display: inline-block; margin: 20px 0; }
                        p { color: #555; line-height: 1.6; }
                        .footer { margin-top: 30px; font-size: 0.9em; color: #888; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Flow Studio ERP</h1>
                        <h2>API Gateway Core</h2>
                        <div class="status">● UP & RUNNING</div>
                        <p>The API Gateway is currently active and actively listening for incoming requests. It is successfully connected to the Eureka Discovery Server and Config Server.</p>
                        <p>All microservices traffic is properly secured and routed through this node.</p>
                        <div class="footer">Spring Cloud Gateway Engine</div>
                    </div>
                </body>
                </html>
                """;
        return Mono.just(html);
    }
}
