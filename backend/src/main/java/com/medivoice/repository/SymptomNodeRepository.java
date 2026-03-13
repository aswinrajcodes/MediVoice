package com.medivoice.repository;

import com.medivoice.model.SymptomNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SymptomNodeRepository extends Neo4jRepository<SymptomNode, Long> {

    Optional<SymptomNode> findByName(String name);

    @Query("MATCH (s1:Symptom)-[:COMBINED_WITH]->(s2:Symptom) " +
           "WHERE s1.name IN $symptomList AND s2.name IN $symptomList " +
           "RETURN count(*) as coOccurrenceCount")
    int countCoOccurrences(@Param("symptomList") List<String> symptomList);

    @Query("MATCH (s:Symptom)-[:ASSOCIATED_WITH]->(c:Condition) " +
           "WHERE s.name IN $symptomList " +
           "RETURN c.name as conditionName, c.severity as severity, count(s) as symptomMatchCount " +
           "ORDER BY symptomMatchCount DESC")
    List<ConditionMatch> findConditionsForSymptoms(@Param("symptomList") List<String> symptomList);

    interface ConditionMatch {
        String getConditionName();
        String getSeverity();
        int getSymptomMatchCount();
    }
}
