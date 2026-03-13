package com.medivoice.service;

import com.medivoice.model.EmergencyLevel;
import com.medivoice.model.TranscriptFact;
import com.medivoice.model.TriageResult;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TriageService {

    private final KieContainer kieContainer;

    public TriageService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    /**
     * Evaluate a transcript text against Drools triage rules.
     * First classifies tense to avoid triggering on historical/hypothetical mentions.
     */
    public TriageResult evaluate(String transcriptText) {
        if (transcriptText == null || transcriptText.isBlank()) {
            return new TriageResult(EmergencyLevel.LOW, "No text to evaluate");
        }

        // Pre-process: classify tense before Drools evaluation
        String tense = classifyTense(transcriptText);
        if (!tense.equals("ACTIVE")) {
            log.debug("Transcript classified as {} — skipping triage rules", tense);
            return new TriageResult(EmergencyLevel.LOW, "Historical/hypothetical mention — " + tense);
        }

        KieSession kieSession = kieContainer.newKieSession();
        try {
            TranscriptFact fact = new TranscriptFact();
            fact.setText(transcriptText.toLowerCase());
            fact.setSeverity(EmergencyLevel.LOW);
            fact.setTense(tense);

            kieSession.insert(fact);
            int rulesFired = kieSession.fireAllRules();

            log.debug("Triage evaluation: {} rules fired, severity={}, reason={}",
                    rulesFired, fact.getSeverity(), fact.getReason());

            return new TriageResult(fact.getSeverity(), fact.getReason());
        } catch (Exception e) {
            log.error("Error during triage evaluation: {}", e.getMessage());
            return new TriageResult(EmergencyLevel.LOW, "Triage evaluation error");
        } finally {
            kieSession.dispose();
        }
    }

    /**
     * Classifies transcript text as ACTIVE, HISTORICAL, or HYPOTHETICAL
     * to avoid false emergency triggers for past events or hypothetical questions.
     */
    private String classifyTense(String text) {
        String lower = text.toLowerCase();

        // Check for historical/past tense indicators
        String[] historicalMarkers = {
            "died of", "had a", "years ago", "when i was",
            "my grandfather", "my grandmother", "my father", "my mother",
            "history of", "used to", "previously had", "in the past",
            "when he was", "when she was", "long time ago",
            "my uncle", "my aunt", "family history"
        };
        for (String marker : historicalMarkers) {
            if (lower.contains(marker)) {
                return "HISTORICAL";
            }
        }

        // Check for hypothetical indicators
        String[] hypotheticalMarkers = {
            "what if", "could it be", "maybe", "just wondering",
            "hypothetically", "in theory", "would it be", "is it possible",
            "just curious", "asking for a friend", "what would happen"
        };
        for (String marker : hypotheticalMarkers) {
            if (lower.contains(marker)) {
                return "HYPOTHETICAL";
            }
        }

        return "ACTIVE";
    }
}
