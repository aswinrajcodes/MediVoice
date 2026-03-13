package com.medivoice.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

@Node("Condition")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConditionNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Property("severity")
    private EmergencyLevel severity;

    @Property("description")
    private String description;

    @Property("recommendation")
    private String recommendation;

    public ConditionNode(String name, EmergencyLevel severity, String description, String recommendation) {
        this.name = name;
        this.severity = severity;
        this.description = description;
        this.recommendation = recommendation;
    }
}
