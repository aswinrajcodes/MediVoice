package com.medivoice.repository;

import com.medivoice.model.SymptomDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SymptomSearchRepository extends ElasticsearchRepository<SymptomDocument, String> {

    List<SymptomDocument> findByNameContaining(String name);

    List<SymptomDocument> findByKeywordsContaining(String keyword);

    List<SymptomDocument> findByBodyRegion(String bodyRegion);

    List<SymptomDocument> findBySeverity(String severity);
}
