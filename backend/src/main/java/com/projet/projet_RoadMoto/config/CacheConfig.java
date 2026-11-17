package com.projet.projet_RoadMoto.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Active les annotations de cache (@Cacheable).
 * Le cache lui-même (nom, taille, durée de vie) est configuré dans application.yml (spring.cache).
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
