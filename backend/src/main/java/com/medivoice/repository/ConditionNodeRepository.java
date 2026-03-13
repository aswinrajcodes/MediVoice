package com.medivoice.repository;

import com.medivoice.model.ConditionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConditionNodeRepository extends Neo4jRepository<ConditionNode, Long> {

    Optional<ConditionNode> findByName(String name);
}
