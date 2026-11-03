package com.projet.projet_RoadMoto;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Démarre un conteneur PostgreSQL avec PostGIS pour les tests (Docker requis).
 * @ServiceConnection branche automatiquement la datasource Spring sur ce conteneur,
 * sans avoir à configurer l'URL, l'utilisateur ou le mot de passe.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        // image PostGIS (requise pour les colonnes géographiques), déclarée compatible avec "postgres"
        // pour que Testcontainers l'accepte comme une base PostgreSQL classique
        return new PostgreSQLContainer(
                DockerImageName.parse("postgis/postgis:17-3.5")
                        .asCompatibleSubstituteFor("postgres"));
    }
}
