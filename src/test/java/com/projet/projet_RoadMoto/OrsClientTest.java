package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.client.OrsClient;
import com.projet.projet_RoadMoto.dto.itinerairedto.ItineraireCalcule;
import com.projet.projet_RoadMoto.exception.EtapesInvalidesException;
import com.projet.projet_RoadMoto.exception.ItineraireIncalculableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Test du client ORS sans réseau : MockRestServiceServer intercepte les requêtes du RestClient
 * et vérifie l'URL, les en-têtes et le corps envoyés.
 */
class OrsClientTest {

    private static final String BASE = "https://api.heigit.org/openrouteservice";
    private static final String URL = BASE + "/v2/directions/driving-car/geojson";

    private MockRestServiceServer serveur;
    private OrsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        serveur = MockRestServiceServer.bindTo(builder).build();
        client = new OrsClient(builder, BASE, "cle-test");
    }

    @Test
    void appelleBonneUrlAvecCleEtLitLaReponseTest() {
        serveur.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "cle-test"))
                .andExpect(content().json("{\"coordinates\":[[7.36,46.23],[7.29,46.33]],\"instructions\":false,\"geometry_simplify\":true}", JsonCompareMode.STRICT))
                .andRespond(withSuccess("""
                        {"type":"FeatureCollection","bbox":[7.29,46.23,7.36,46.33],
                         "features":[{"type":"Feature",
                           "geometry":{"type":"LineString","coordinates":[[7.36,46.23],[7.30,46.30],[7.29,46.33]]},
                           "properties":{"summary":{"distance":12345.6,"duration":1800.4},
                                         "segments":[{"distance":12345.6,"duration":1800.4,"steps":[]}]}}],
                         "metadata":{"attribution":"openrouteservice.org"}}
                        """, MediaType.parseMediaType("application/geo+json;charset=UTF-8")));

        ItineraireCalcule calcule = client.calculer(List.of(List.of(7.36, 46.23), List.of(7.29, 46.33)));

        serveur.verify();
        assertThat(calcule.distanceM()).isEqualTo(12_346);
        assertThat(calcule.dureeS()).isEqualTo(1_800);
        assertThat(calcule.trace().getNumPoints()).isEqualTo(3);
        assertThat(calcule.troncons()).hasSize(1);
    }

    @Test
    void remonteMessageErreurOrs() {
        serveur.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"code":2010,"message":"Could not find routable point within a radius of 350.0 meters of specified coordinate 0: 46.2300000 7.3600000."}}
                                """));

        assertThatThrownBy(() -> client.calculer(List.of(List.of(46.23, 7.36), List.of(46.33, 7.29))))
                .isInstanceOf(ItineraireIncalculableException.class)
                .hasMessageContaining("404")
                .hasMessageContaining("Could not find routable point");
    }

    @Test
    void cleRefuseeTest() {
        serveur.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Access to this API has been disallowed\"}"));

        assertThatThrownBy(() -> client.calculer(List.of(List.of(7.36, 46.23), List.of(7.29, 46.33))))
                .isInstanceOf(ItineraireIncalculableException.class)
                .hasMessageContaining("403")
                .hasMessageContaining("disallowed");
    }

    @Test
    void refuseTrajetVideRenvoyeParOrsTest() {
        // étapes distinctes mais ramenées au même point de route : ORS renvoie un seul point et 0 m
        serveur.expect(requestTo(URL))
                .andRespond(withSuccess("""
                        {"features":[{"geometry":{"coordinates":[[7.36,46.23]]},
                          "properties":{"summary":{},"segments":[{"distance":0.0,"duration":0.0}]}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.calculer(List.of(List.of(7.36, 46.23), List.of(7.36001, 46.23001))))
                .isInstanceOf(EtapesInvalidesException.class)
                .hasMessageContaining("trop proches");
    }

    @Test
    void envoiePasGeometrySimplifyAvecEtapeIntermediaireTest() {
        // ORS refuse geometry_simplify sur une route de plus d'un segment
        serveur.expect(requestTo(URL))
                .andExpect(content().json(
                        "{\"coordinates\":[[7.07,46.10],[7.00,46.06],[7.36,46.23]],\"instructions\":false}", JsonCompareMode.STRICT))
                .andRespond(withSuccess("""
                        {"features":[{"geometry":{"coordinates":[[7.07,46.10],[7.00,46.06],[7.36,46.23]]},
                          "properties":{"summary":{"distance":50000.0,"duration":3600.0},
                                        "segments":[{"distance":20000.0,"duration":1500.0},
                                                    {"distance":30000.0,"duration":2100.0}]}}]}
                        """, MediaType.APPLICATION_JSON));

        ItineraireCalcule calcule = client.calculer(
                List.of(List.of(7.07, 46.10), List.of(7.00, 46.06), List.of(7.36, 46.23)));

        serveur.verify();
        assertThat(calcule.troncons()).hasSize(2);
    }
}
