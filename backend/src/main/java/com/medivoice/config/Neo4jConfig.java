package com.medivoice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;

@Configuration
@EnableNeo4jAuditing
public class Neo4jConfig {
    // Neo4j connection is auto-configured via application.yml properties:
    //   spring.neo4j.uri
    //   spring.neo4j.authentication.username
    //   spring.neo4j.authentication.password
    // This config class enables auditing and can be extended for
    // custom transaction management if needed.
}
