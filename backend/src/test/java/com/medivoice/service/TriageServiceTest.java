package com.medivoice.service;

import com.medivoice.config.DroolsConfig;
import com.medivoice.model.TriageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TriageService using actual Drools rules.
 */
class TriageServiceTest {

    private TriageService triageService;

    @BeforeEach
    void setUp() {
        // Build a real KieContainer from the DRL file
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(
                "src/main/resources/rules/triage-rules.drl",
                kieServices.getResources()
                        .newClassPathResource("rules/triage-rules.drl")
        );
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem).buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        KieContainer kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());

        triageService = new TriageService(kieContainer);
    }

    // ─── HIGH SEVERITY ──────────────────────────────

    @Test
    @DisplayName("Chest pain triggers HIGH severity")
    void testChestPainTriggersHigh() {
        TriageResult result = triageService.evaluate("I have severe chest pain right now");
        assertEquals("HIGH", result.getSeverity());
        assertNotNull(result.getReason());
    }

    @Test
    @DisplayName("Heart attack mention triggers HIGH severity")
    void testHeartAttackTriggersHigh() {
        TriageResult result = triageService.evaluate("I think I'm having a heart attack");
        assertEquals("HIGH", result.getSeverity());
    }

    @Test
    @DisplayName("Difficulty breathing triggers HIGH severity")
    void testBreathingEmergencyTriggersHigh() {
        TriageResult result = triageService.evaluate("I can't breathe properly");
        assertEquals("HIGH", result.getSeverity());
    }

    @Test
    @DisplayName("Choking triggers HIGH severity")
    void testChokingTriggersHigh() {
        TriageResult result = triageService.evaluate("Someone is choking on food");
        assertEquals("HIGH", result.getSeverity());
    }

    @Test
    @DisplayName("Stroke symptoms trigger HIGH severity")
    void testStrokeTriggersHigh() {
        TriageResult result = triageService.evaluate("My face is drooping and I have slurred speech");
        assertEquals("HIGH", result.getSeverity());
    }

    @Test
    @DisplayName("Severe bleeding triggers HIGH severity")
    void testSevereBleedingTriggersHigh() {
        TriageResult result = triageService.evaluate("I have severe bleeding from a wound");
        assertEquals("HIGH", result.getSeverity());
    }

    @Test
    @DisplayName("Poisoning triggers HIGH severity")
    void testPoisoningTriggersHigh() {
        TriageResult result = triageService.evaluate("I think I took an overdose of pills");
        assertEquals("HIGH", result.getSeverity());
    }

    @Test
    @DisplayName("Self-harm triggers HIGH severity")
    void testSelfHarmTriggersHigh() {
        TriageResult result = triageService.evaluate("I am thinking about self-harm");
        assertEquals("HIGH", result.getSeverity());
    }

    @Test
    @DisplayName("Anaphylaxis triggers HIGH severity")
    void testAnaphylaxisTriggersHigh() {
        TriageResult result = triageService.evaluate("My throat is closing and I can't swallow");
        assertEquals("HIGH", result.getSeverity());
    }

    // ─── MEDIUM SEVERITY ────────────────────────────

    @Test
    @DisplayName("High fever triggers MEDIUM severity")
    void testHighFeverTriggersMedium() {
        TriageResult result = triageService.evaluate("I have a fever over 103 degrees");
        assertEquals("MEDIUM", result.getSeverity());
    }

    @Test
    @DisplayName("Severe pain triggers MEDIUM severity")
    void testSeverePainTriggersMedium() {
        TriageResult result = triageService.evaluate("My pain is 10 out of 10, it's unbearable");
        assertEquals("MEDIUM", result.getSeverity());
    }

    @Test
    @DisplayName("Blood in vomit triggers MEDIUM severity")
    void testBloodInVomitTriggersMedium() {
        TriageResult result = triageService.evaluate("I'm vomiting blood since this morning");
        assertEquals("MEDIUM", result.getSeverity());
    }

    @Test
    @DisplayName("Head injury triggers MEDIUM severity")
    void testHeadInjuryTriggersMedium() {
        TriageResult result = triageService.evaluate("I hit my head and now I feel dizzy with concussion signs");
        assertEquals("MEDIUM", result.getSeverity());
    }

    // ─── LOW SEVERITY / NO TRIGGER ──────────────────

    @Test
    @DisplayName("Normal symptoms return LOW severity")
    void testNormalTextReturnsLow() {
        TriageResult result = triageService.evaluate("I have a mild headache and some sneezing");
        assertEquals("LOW", result.getSeverity());
    }

    @Test
    @DisplayName("Empty text returns LOW severity")
    void testEmptyTextReturnsLow() {
        TriageResult result = triageService.evaluate("");
        assertEquals("LOW", result.getSeverity());
    }

    @Test
    @DisplayName("Null text returns LOW severity")
    void testNullTextReturnsLow() {
        TriageResult result = triageService.evaluate(null);
        assertEquals("LOW", result.getSeverity());
    }

    // ─── TENSE CLASSIFICATION ───────────────────────

    @Test
    @DisplayName("Historical mention does NOT trigger HIGH severity")
    void testHistoricalMentionReturnsLow() {
        TriageResult result = triageService.evaluate("My grandfather died of a heart attack years ago");
        assertEquals("LOW", result.getSeverity());
    }

    @Test
    @DisplayName("Hypothetical question does NOT trigger HIGH severity")
    void testHypotheticalReturnsLow() {
        TriageResult result = triageService.evaluate("What if I had chest pain, what should I do");
        assertEquals("LOW", result.getSeverity());
    }

    @Test
    @DisplayName("Past tense with 'used to' does NOT trigger emergency")
    void testPastTenseReturnsLow() {
        TriageResult result = triageService.evaluate("I used to have difficulty breathing as a child");
        assertEquals("LOW", result.getSeverity());
    }
}
