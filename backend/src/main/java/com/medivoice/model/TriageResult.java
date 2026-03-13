package com.medivoice.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TriageResult {

    private EmergencyLevel severity;
    private String reason;
}
