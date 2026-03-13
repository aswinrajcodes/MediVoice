package com.medivoice.service;

import com.medivoice.model.ConditionNode;
import com.medivoice.model.EmergencyLevel;
import com.medivoice.model.SymptomNode;
import com.medivoice.repository.ConditionNodeRepository;
import com.medivoice.repository.SymptomNodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SymptomGraphService {

    private final SymptomNodeRepository symptomRepo;
    private final ConditionNodeRepository conditionRepo;

    public SymptomGraphService(SymptomNodeRepository symptomRepo,
                                ConditionNodeRepository conditionRepo) {
        this.symptomRepo = symptomRepo;
        this.conditionRepo = conditionRepo;
    }

    /**
     * Compute co-occurrence boost based on how many related symptoms
     * are present simultaneously in the Neo4j graph.
     * Returns a value between 0.0 and 1.0.
     */
    public double getCoOccurrenceBoost(List<String> detectedSymptoms) {
        if (detectedSymptoms == null || detectedSymptoms.size() < 2) {
            return 0.0;
        }

        try {
            int coOccurrenceCount = symptomRepo.countCoOccurrences(detectedSymptoms);
            // Each co-occurrence pair adds 0.25, capped at 1.0
            return Math.min(1.0, coOccurrenceCount * 0.25);
        } catch (Exception e) {
            log.warn("Failed to query Neo4j for co-occurrences: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Seed the Neo4j graph with initial symptom/condition data on app startup.
     */
    @PostConstruct
    public void seedSymptomGraph() {
        try {
            // Check if data already exists
            if (symptomRepo.count() > 0) {
                log.info("Symptom graph already seeded — {} nodes exist", symptomRepo.count());
                return;
            }

            log.info("Seeding symptom graph with initial data...");

            // Create symptom nodes
            SymptomNode chestPain = new SymptomNode("chest pain", 0.9, "chest");
            SymptomNode shortnessOfBreath = new SymptomNode("shortness of breath", 0.85, "chest");
            SymptomNode fever = new SymptomNode("fever", 0.5, "systemic");
            SymptomNode rash = new SymptomNode("rash", 0.3, "skin");
            SymptomNode headache = new SymptomNode("headache", 0.4, "head");
            SymptomNode stiffNeck = new SymptomNode("stiff neck", 0.6, "head");
            SymptomNode nausea = new SymptomNode("nausea", 0.3, "abdomen");
            SymptomNode dizziness = new SymptomNode("dizziness", 0.5, "head");
            SymptomNode armWeakness = new SymptomNode("arm weakness", 0.7, "extremities");
            SymptomNode cough = new SymptomNode("cough", 0.2, "chest");
            SymptomNode vomiting = new SymptomNode("vomiting", 0.4, "abdomen");
            SymptomNode blurredVision = new SymptomNode("blurred vision", 0.5, "head");
            SymptomNode severePain = new SymptomNode("severe pain", 0.7, "systemic");
            SymptomNode slurredSpeech = new SymptomNode("slurred speech", 0.8, "head");

            // Create condition nodes
            ConditionNode heartAttack = new ConditionNode("Heart Attack", EmergencyLevel.HIGH,
                    "Acute myocardial infarction", "Call 911 immediately");
            ConditionNode stroke = new ConditionNode("Stroke", EmergencyLevel.HIGH,
                    "Cerebrovascular accident", "Call 911 immediately — time critical");
            ConditionNode meningitis = new ConditionNode("Meningitis", EmergencyLevel.HIGH,
                    "Inflammation of brain membranes", "Seek emergency care immediately");
            ConditionNode pneumonia = new ConditionNode("Pneumonia", EmergencyLevel.MEDIUM,
                    "Lung infection", "See a doctor within 24 hours");
            ConditionNode flu = new ConditionNode("Influenza", EmergencyLevel.LOW,
                    "Viral respiratory infection", "Rest, hydrate, see doctor if worsening");
            ConditionNode migraine = new ConditionNode("Migraine", EmergencyLevel.LOW,
                    "Severe headache disorder", "Rest in a dark room, take OTC pain relief");

            // Set up COMBINED_WITH relationships (symptom co-occurrences)
            chestPain.getCombinedWith().add(shortnessOfBreath);   // HIGH boost (0.8)
            chestPain.getCombinedWith().add(nausea);              // HIGH boost (0.7)
            fever.getCombinedWith().add(rash);                     // MEDIUM boost (0.4)
            headache.getCombinedWith().add(stiffNeck);             // HIGH boost (0.9)
            dizziness.getCombinedWith().add(armWeakness);          // HIGH boost (0.8)
            fever.getCombinedWith().add(cough);                    // LOW boost (0.2)
            headache.getCombinedWith().add(blurredVision);         // MEDIUM boost (0.5)
            nausea.getCombinedWith().add(dizziness);               // MEDIUM boost (0.4)
            slurredSpeech.getCombinedWith().add(armWeakness);      // HIGH boost (0.9)

            // Set up ASSOCIATED_WITH relationships (symptom → condition)
            chestPain.getAssociatedConditions().add(heartAttack);
            shortnessOfBreath.getAssociatedConditions().add(heartAttack);
            nausea.getAssociatedConditions().add(heartAttack);

            slurredSpeech.getAssociatedConditions().add(stroke);
            armWeakness.getAssociatedConditions().add(stroke);
            dizziness.getAssociatedConditions().add(stroke);

            headache.getAssociatedConditions().add(meningitis);
            stiffNeck.getAssociatedConditions().add(meningitis);
            fever.getAssociatedConditions().add(meningitis);

            fever.getAssociatedConditions().add(pneumonia);
            cough.getAssociatedConditions().add(pneumonia);
            shortnessOfBreath.getAssociatedConditions().add(pneumonia);

            fever.getAssociatedConditions().add(flu);
            cough.getAssociatedConditions().add(flu);
            headache.getAssociatedConditions().add(migraine);
            blurredVision.getAssociatedConditions().add(migraine);

            // Save all conditions first (they're referenced by symptoms)
            conditionRepo.save(heartAttack);
            conditionRepo.save(stroke);
            conditionRepo.save(meningitis);
            conditionRepo.save(pneumonia);
            conditionRepo.save(flu);
            conditionRepo.save(migraine);

            // Save all symptoms (with their relationships)
            symptomRepo.save(chestPain);
            symptomRepo.save(shortnessOfBreath);
            symptomRepo.save(fever);
            symptomRepo.save(rash);
            symptomRepo.save(headache);
            symptomRepo.save(stiffNeck);
            symptomRepo.save(nausea);
            symptomRepo.save(dizziness);
            symptomRepo.save(armWeakness);
            symptomRepo.save(cough);
            symptomRepo.save(vomiting);
            symptomRepo.save(blurredVision);
            symptomRepo.save(severePain);
            symptomRepo.save(slurredSpeech);

            log.info("Symptom graph seeded: {} symptom nodes, {} condition nodes",
                    symptomRepo.count(), conditionRepo.count());

        } catch (Exception e) {
            log.warn("Failed to seed symptom graph (Neo4j may not be available): {}", e.getMessage());
        }
    }
}
