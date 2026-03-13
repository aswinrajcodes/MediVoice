package com.medivoice.service;

import com.medivoice.model.SymptomDocument;
import com.medivoice.repository.SymptomSearchRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-text symptom search using ElasticSearch.
 * Provides fuzzy search across symptom names, descriptions, and keywords.
 */
@Service
@Slf4j
public class SymptomSearchService {

    private final SymptomSearchRepository searchRepository;

    public SymptomSearchService(SymptomSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    /**
     * Search symptoms by query string — matches against name, description, and keywords.
     */
    public List<SymptomDocument> searchSymptoms(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            String lower = query.toLowerCase().trim();

            // Search by name first
            List<SymptomDocument> results = new ArrayList<>(searchRepository.findByNameContaining(lower));

            // Also search by keywords
            List<SymptomDocument> keywordResults = searchRepository.findByKeywordsContaining(lower);
            for (SymptomDocument doc : keywordResults) {
                if (results.stream().noneMatch(r -> r.getId().equals(doc.getId()))) {
                    results.add(doc);
                }
            }

            return results;
        } catch (Exception e) {
            log.warn("ElasticSearch query failed (ES may not be available): {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Search symptoms by body region.
     */
    public List<SymptomDocument> searchByBodyRegion(String bodyRegion) {
        try {
            return searchRepository.findByBodyRegion(bodyRegion);
        } catch (Exception e) {
            log.warn("ElasticSearch query by body region failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Index a single symptom document.
     */
    public void indexSymptom(SymptomDocument symptom) {
        try {
            searchRepository.save(symptom);
        } catch (Exception e) {
            log.warn("Failed to index symptom '{}': {}", symptom.getName(), e.getMessage());
        }
    }

    /**
     * Seed the ElasticSearch index with initial symptom data on startup.
     * Mirrors the Neo4j symptom graph data for full-text search.
     */
    @PostConstruct
    public void seedSearchIndex() {
        try {
            long count = searchRepository.count();
            if (count > 0) {
                log.info("ElasticSearch symptom index already seeded — {} documents exist", count);
                return;
            }

            log.info("Seeding ElasticSearch symptom index...");

            List<SymptomDocument> symptoms = List.of(
                new SymptomDocument("chest pain", "Pain, pressure, or tightness in the chest area",
                        "chest", "HIGH", List.of("chest", "heart", "cardiac", "pressure", "tightness", "angina"), 0.9),
                new SymptomDocument("shortness of breath", "Difficulty breathing or feeling breathless",
                        "chest", "HIGH", List.of("breathing", "breathless", "dyspnea", "respiratory", "lungs"), 0.85),
                new SymptomDocument("fever", "Elevated body temperature above normal range",
                        "systemic", "MEDIUM", List.of("temperature", "hot", "chills", "sweating", "pyrexia"), 0.5),
                new SymptomDocument("rash", "Visible change in skin color, texture, or appearance",
                        "skin", "LOW", List.of("skin", "itchy", "bumps", "red", "hives", "dermatitis"), 0.3),
                new SymptomDocument("headache", "Pain in the head or upper neck area",
                        "head", "LOW", List.of("head", "migraine", "throbbing", "tension", "scalp"), 0.4),
                new SymptomDocument("stiff neck", "Difficulty or pain when moving the neck",
                        "head", "MEDIUM", List.of("neck", "meningitis", "stiffness", "cervical"), 0.6),
                new SymptomDocument("nausea", "Feeling of unease and discomfort in the stomach with urge to vomit",
                        "abdomen", "LOW", List.of("queasy", "sick", "vomit", "stomach", "gastric"), 0.3),
                new SymptomDocument("dizziness", "Feeling faint, lightheaded, or unsteady",
                        "head", "MEDIUM", List.of("lightheaded", "vertigo", "spinning", "faint", "balance"), 0.5),
                new SymptomDocument("arm weakness", "Loss of strength or inability to move arm normally",
                        "extremities", "HIGH", List.of("weak", "arm", "numbness", "stroke", "paralysis"), 0.7),
                new SymptomDocument("cough", "Sudden expulsion of air from the lungs",
                        "chest", "LOW", List.of("coughing", "throat", "phlegm", "mucus", "bronchitis"), 0.2),
                new SymptomDocument("vomiting", "Forceful emptying of stomach contents through the mouth",
                        "abdomen", "MEDIUM", List.of("throwing up", "nausea", "stomach", "emesis"), 0.4),
                new SymptomDocument("blurred vision", "Lack of sharpness in vision making objects appear out of focus",
                        "head", "MEDIUM", List.of("vision", "eyes", "sight", "blurry", "focus"), 0.5),
                new SymptomDocument("severe pain", "Extremely intense pain rated 8-10 on pain scale",
                        "systemic", "HIGH", List.of("intense", "unbearable", "agony", "excruciating"), 0.7),
                new SymptomDocument("slurred speech", "Difficulty speaking clearly, words sound garbled",
                        "head", "HIGH", List.of("speech", "talking", "stroke", "garbled", "aphasia"), 0.8)
            );

            searchRepository.saveAll(symptoms);
            log.info("ElasticSearch symptom index seeded with {} documents", symptoms.size());

        } catch (Exception e) {
            log.warn("Failed to seed ElasticSearch index (ES may not be available): {}", e.getMessage());
        }
    }
}
