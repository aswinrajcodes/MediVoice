package com.medivoice.service;

import com.medivoice.model.SymptomNode;
import com.medivoice.repository.ConditionNodeRepository;
import com.medivoice.repository.SymptomNodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SymptomGraphServiceTest {

    @Mock
    private SymptomNodeRepository symptomNodeRepository;

    @Mock
    private ConditionNodeRepository conditionNodeRepository;

    @InjectMocks
    private SymptomGraphService symptomGraphService;

    @Test
    @DisplayName("Co-occurrence boost returns 0.25 for two related symptoms")
    void testCoOccurrenceBoostCalculation() {
        when(symptomNodeRepository.countCoOccurrences("chest pain", "shortness of breath"))
                .thenReturn(1L);

        double boost = symptomGraphService.getCoOccurrenceBoost(
                List.of("chest pain", "shortness of breath"));

        assertEquals(0.25, boost, 0.01);
    }

    @Test
    @DisplayName("Co-occurrence boost caps at 1.0")
    void testCoOccurrenceBoostCapsAtOne() {
        // 5 pairs, each with co-occurrence → 5 * 0.25 = 1.25 → capped at 1.0
        when(symptomNodeRepository.countCoOccurrences(anyString(), anyString()))
                .thenReturn(1L);

        double boost = symptomGraphService.getCoOccurrenceBoost(
                List.of("s1", "s2", "s3", "s4", "s5"));

        assertTrue(boost <= 1.0, "Boost should cap at 1.0");
    }

    @Test
    @DisplayName("Single symptom returns zero boost")
    void testSingleSymptomReturnsZero() {
        double boost = symptomGraphService.getCoOccurrenceBoost(List.of("headache"));

        assertEquals(0.0, boost);
        verify(symptomNodeRepository, never()).countCoOccurrences(anyString(), anyString());
    }

    @Test
    @DisplayName("Empty symptom list returns zero boost")
    void testEmptyListReturnsZero() {
        double boost = symptomGraphService.getCoOccurrenceBoost(List.of());

        assertEquals(0.0, boost);
    }
}
