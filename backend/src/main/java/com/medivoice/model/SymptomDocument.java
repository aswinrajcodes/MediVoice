package com.medivoice.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * ElasticSearch document for full-text symptom search.
 */
@Document(indexName = "symptoms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SymptomDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String bodyRegion;

    @Field(type = FieldType.Keyword)
    private String severity;

    @Field(type = FieldType.Text, analyzer = "standard")
    private List<String> keywords;

    @Field(type = FieldType.Double)
    private double severityWeight;

    public SymptomDocument(String name, String description, String bodyRegion,
                           String severity, List<String> keywords, double severityWeight) {
        this.name = name;
        this.description = description;
        this.bodyRegion = bodyRegion;
        this.severity = severity;
        this.keywords = keywords;
        this.severityWeight = severityWeight;
    }
}
