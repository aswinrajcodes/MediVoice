package com.medivoice.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Drools fact representing a transcript segment for triage evaluation.
 * Inserted into the KieSession and modified by rules.
 */
@Data
@NoArgsConstructor
public class TranscriptFact {

    private String text;
    private EmergencyLevel severity = EmergencyLevel.LOW;
    private String reason = "";
    private String tense = "ACTIVE"; // ACTIVE, HISTORICAL, HYPOTHETICAL

    public TranscriptFact(String text) {
        this.text = text;
    }
}
