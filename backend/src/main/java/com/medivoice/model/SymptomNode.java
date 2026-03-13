package com.medivoice.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.util.HashSet;
import java.util.Set;

@Node("Symptom")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SymptomNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Property("severity_weight")
    private double severityWeight;

    @Property("body_region")
    private String bodyRegion;

    @Relationship(type = "COMBINED_WITH", direction = Relationship.Direction.OUTGOING)
    private Set<SymptomNode> combinedWith = new HashSet<>();

    @Relationship(type = "ASSOCIATED_WITH", direction = Relationship.Direction.OUTGOING)
    private Set<ConditionNode> associatedConditions = new HashSet<>();

    public SymptomNode(String name, double severityWeight, String bodyRegion) {
        this.name = name;
        this.severityWeight = severityWeight;
        this.bodyRegion = bodyRegion;
    }
}
