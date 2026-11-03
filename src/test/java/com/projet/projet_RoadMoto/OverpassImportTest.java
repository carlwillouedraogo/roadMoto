package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.domain.ResultatImport;
import com.projet.projet_RoadMoto.domain.Source;
import com.projet.projet_RoadMoto.domain.TypePoi;
import com.projet.projet_RoadMoto.entity.Poi;
import com.projet.projet_RoadMoto.repository.PoiRepository;
import com.projet.projet_RoadMoto.services.OverpassImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Teste l'import Overpass de bout en bout sans appeler la vraie API :
 * MockRestServiceServer renvoie une réponse JSON préparée, et les POI
 * sont réellement écrits dans le PostgreSQL/PostGIS de Testcontainers.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(TestcontainersConfig.class)
public class OverpassImportTest {

    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);

    // URL factice : seul MockRestServiceServer y répond, aucune requête ne sort de la machine
    private static final String URL = "http://overpass.test/api/interpreter";

    // Réponse au format Overpass réel (champ "type" inclus, absent du record OverpassElement) :
    //  - id 1 : col valide
    //  - id 2 : pas de tags          -> ignoré
    //  - id 3 : tags sans "name"     -> ignoré
    //  - id 4 : type non reconnu     -> ignoré
    //  - id 5 : point de vue valide, altitude avec espace et unité ("1 950 m")
    private static final String REPONSE_OVERPASS = """
            {
              "elements": [
                { "type": "node", "id": 1, "lat": 45.8689, "lon": 7.1706,
                  "tags": { "name": "Col du Grand-Saint-Bernard", "mountain_pass": "yes", "ele": "2469" } },
                { "type": "node", "id": 2, "lat": 46.1, "lon": 7.2 },
                { "type": "node", "id": 3, "lat": 46.2, "lon": 7.3,
                  "tags": { "mountain_pass": "yes" } },
                { "type": "node", "id": 4, "lat": 46.3, "lon": 7.4,
                  "tags": { "name": "Banque du Valais", "amenity": "bank" } },
                { "type": "node", "id": 5, "lat": 46.0, "lon": 7.5,
                  "tags": { "name": "Belvédère", "tourism": "viewpoint", "ele": "1 950 m" } }
              ]
            }
            """;

    @Autowired
    private PoiRepository poiRepository;

    @Autowired
    private TestEntityManager em;

    private MockRestServiceServer serveur;
    private OverpassImportService service;

    // Le service est construit à la main (pas de bean) pour lui injecter un RestClient branché sur le mock
    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        // bindTo doit être appelé avant que le service ne fasse builder.build()
        serveur = MockRestServiceServer.bindTo(builder).build();
        service = new OverpassImportService(poiRepository, builder, URL);
    }

    @Test
    void importCreePoiEtIgnoreElementsInvalidesTest() {
        repondAvec(REPONSE_OVERPASS);

        ResultatImport resultat = service.importerColsValais();
        // flush + clear : force l'écriture en base et vide le cache JPA,
        // pour que les lectures suivantes relisent vraiment la base
        em.flush();
        em.clear();

        // vérifie que l'appel HTTP attendu a bien eu lieu
        serveur.verify();
        assertThat(resultat).isEqualTo(new ResultatImport(2, 0, 3));

        Poi col = poiRepository.findByOsmId(1L).orElseThrow();
        assertThat(col.getNom()).isEqualTo("Col du Grand-Saint-Bernard");
        assertThat(col.getType()).isEqualTo(TypePoi.COL);
        assertThat(col.getSource()).isEqualTo(Source.OSM);
        assertThat(col.getAltitudeMetres()).isEqualTo(2469);
        // JTS : X = longitude, Y = latitude
        assertThat(col.getLocalisation().getX()).isEqualTo(7.1706);
        assertThat(col.getLocalisation().getY()).isEqualTo(45.8689);

        Poi pointDeVue = poiRepository.findByOsmId(5L).orElseThrow();
        assertThat(pointDeVue.getType()).isEqualTo(TypePoi.POINT_DE_VUE);
        // "1 950 m" doit être nettoyé en 1950
        assertThat(pointDeVue.getAltitudeMetres()).isEqualTo(1950);

        // les éléments invalides ne doivent pas être enregistrés
        assertThat(poiRepository.findByOsmId(2L)).isEmpty();
        assertThat(poiRepository.findByOsmId(3L)).isEmpty();
        assertThat(poiRepository.findByOsmId(4L)).isEmpty();
    }

    @Test
    void importMetAJourPoiExistantSansDupliquerTest() {
        // POI déjà importé avec le même osmId que le col de la réponse, mais des données obsolètes
        Poi existant = new Poi();
        existant.setOsmId(1L);
        existant.setNom("Ancien nom");
        existant.setType(TypePoi.COL);
        existant.setSource(Source.OSM);
        existant.setLocalisation(GEO.createPoint(new Coordinate(7.0, 45.0)));
        existant.setAltitudeMetres(2000);
        Long idExistant = poiRepository.save(existant).getId();
        em.flush();
        em.clear();
        long totalAvant = poiRepository.count();

        repondAvec(REPONSE_OVERPASS);

        ResultatImport resultat = service.importerColsValais();
        em.flush();
        em.clear();

        // seul le point de vue (id 5) est nouveau : le col est mis à jour, pas dupliqué
        assertThat(resultat).isEqualTo(new ResultatImport(1, 1, 3));
        assertThat(poiRepository.count()).isEqualTo(totalAvant + 1);

        Poi misAJour = poiRepository.findByOsmId(1L).orElseThrow();
        // même ligne en base (même id), mais champs rafraîchis depuis OSM
        assertThat(misAJour.getId()).isEqualTo(idExistant);
        assertThat(misAJour.getNom()).isEqualTo("Col du Grand-Saint-Bernard");
        assertThat(misAJour.getAltitudeMetres()).isEqualTo(2469);
        assertThat(misAJour.getLocalisation().getX()).isEqualTo(7.1706);
    }

    @Test
    void importSansElementsNeFaitRienTest() {
        long totalAvant = poiRepository.count();
        // réponse sans "elements" : elements() vaut null côté Java
        repondAvec("{}");

        ResultatImport resultat = service.importerColsValais();

        assertThat(resultat).isEqualTo(new ResultatImport(0, 0, 0));
        assertThat(poiRepository.count()).isEqualTo(totalAvant);
    }

    // Prépare le mock : attend un POST sur l'URL Overpass et renvoie le JSON donné
    private void repondAvec(String json) {
        serveur.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
}
