package com.medivoice.config;

import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.io.ResourceType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class DroolsConfig {

    @Bean
    public KieContainer kieContainer() throws IOException {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        // Load the triage rules DRL file from classpath
        ClassPathResource rulesResource = new ClassPathResource("rules/triage-rules.drl");
        try (InputStream is = rulesResource.getInputStream()) {
            byte[] ruleBytes = is.readAllBytes();
            kieFileSystem.write("src/main/resources/rules/triage-rules.drl",
                    kieServices.getResources()
                            .newByteArrayResource(ruleBytes)
                            .setResourceType(ResourceType.DRL));
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        Results results = kieBuilder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            log.error("Drools compilation errors: {}", results.getMessages());
            throw new RuntimeException("Error building Drools rules: " + results.getMessages());
        }

        if (results.hasMessages(Message.Level.WARNING)) {
            log.warn("Drools compilation warnings: {}", results.getMessages());
        }

        log.info("Drools rules compiled successfully");
        return kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
    }
}
