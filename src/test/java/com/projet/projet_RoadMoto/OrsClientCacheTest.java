package com.projet.projet_RoadMoto;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.projet.projet_RoadMoto.client.OrsClient;
import com.projet.projet_RoadMoto.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Vérifie le cache des calculs ORS : WireMock remplace le vrai ORS et compte les requêtes reçues.
 * Contexte Spring réduit au client ORS, au cache et au RestClient : le @Cacheable ne fonctionne
 * qu'à travers le proxy Spring, mais aucune base (ni Docker) n'est nécessaire.
 */
@SpringBootTest(classes = OrsClientCacheTest.Contexte.class)
class OrsClientCacheTest {

    private static final String CHEMIN_ORS = "/v2/directions/driving-car/geojson";

    private static final List<List<Double>> COORDONNEES_MARTIGNY_GSB =
            List.of(List.of(7.0717, 46.1028), List.of(7.1706, 45.8689));
    private static final List<List<Double>> COORDONNEES_SION_SANETSCH =
            List.of(List.of(7.3606, 46.2331), List.of(7.29, 46.33));

    @RegisterExtension
    static WireMockExtension ors = WireMockExtension.newInstance()
            // sans http2PlainDisabled, le client HTTP du JDK tente un upgrade HTTP/2 (h2c)
            // que Jetty coupe sur un POST avec corps : "EOF reached while reading"
            .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
            .configureStaticDsl(true)   // verify(...) statique vise ce serveur
            .build();

    // le client ORS appelle WireMock au lieu du vrai service
    @DynamicPropertySource
    static void urlOrs(DynamicPropertyRegistry registry) {
        registry.add("app.ors.url", ors::baseUrl);
    }

    @Autowired private OrsClient orsClient;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void preparer() {
        // le contexte Spring (et donc le cache) est partagé entre les tests
        cacheManager.getCache("itineraires").clear();

        ors.stubFor(post(urlPathEqualTo(CHEMIN_ORS))
                .willReturn(okJson("""
                        {"features":[{"geometry":{"coordinates":[[7.07,46.10],[7.12,46.00],[7.17,45.87]]},
                          "properties":{"summary":{"distance":42000.0,"duration":3000.0},
                                        "segments":[{"distance":42000.0,"duration":3000.0}]}}]}
                        """)));
    }

    @Test
    void deuxCalculsIdentiquesAppellentOrsUneFoisTest() {
        orsClient.calculer(COORDONNEES_MARTIGNY_GSB);
        orsClient.calculer(COORDONNEES_MARTIGNY_GSB);

        verify(1, postRequestedFor(urlPathEqualTo(CHEMIN_ORS)));
    }

    @Test
    void deuxCalculsDifferentsAppellentOrsDeuxFoisTest() {
        orsClient.calculer(COORDONNEES_MARTIGNY_GSB);
        orsClient.calculer(COORDONNEES_SION_SANETSCH);

        verify(2, postRequestedFor(urlPathEqualTo(CHEMIN_ORS)));
    }

    @Configuration
    @Import({OrsClient.class, CacheConfig.class})
    @ImportAutoConfiguration({
            CacheAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            HttpClientAutoConfiguration.class,
            RestClientAutoConfiguration.class})
    static class Contexte {
    }
}
