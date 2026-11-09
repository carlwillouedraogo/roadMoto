package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.domain.ResultatImport;
import com.projet.projet_RoadMoto.domain.Source;
import com.projet.projet_RoadMoto.domain.TypePoi;
import com.projet.projet_RoadMoto.entity.FermetureSaisonniere;
import com.projet.projet_RoadMoto.entity.Poi;
import com.projet.projet_RoadMoto.repository.FermetureSaisonniereRepository;
import com.projet.projet_RoadMoto.repository.PoiRepository;
import com.projet.projet_RoadMoto.services.FermetureImportService;
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
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste l'application des fermetures saisonnières à partir du vrai fichier
 * fermetures-cols.json, sur le PostgreSQL/PostGIS de Testcontainers
 * (les contraintes SQL de cohérence des mois sont donc réellement vérifiées).
 *
 * Les compteurs attendus dépendent du contenu de fermetures-cols.json (7 entrées) :
 * à ajuster si le fichier change.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(TestcontainersConfig.class)
public class FermetureImportTest {

    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);

    private static final int NB_ENTREES_FICHIER = 7;
    // osmId présents dans fermetures-cols.json
    private static final long PLANCHES = 423639577L;          // ouvert toute l'année
    private static final long GRAND_ST_BERNARD = 2001818936L; // ouvert de juin à octobre

    @Autowired
    private PoiRepository poiRepository;

    @Autowired
    private FermetureSaisonniereRepository fermetureRepository;

    @Autowired
    private TestEntityManager em;

    private FermetureImportService service;

    // @DataJpaTest ne fournit pas d'ObjectMapper : le service est construit à la main
    @BeforeEach
    void setUp() {
        service = new FermetureImportService(poiRepository, fermetureRepository, new ObjectMapper());
    }

    @Test
    void appliqueFermeturesAuxPoiExistantsTest() {
        Long idPlanches = creerCol(PLANCHES, "Col des Planches");
        Long idGsb = creerCol(GRAND_ST_BERNARD, "Col du Grand Saint-Bernard");

        ResultatImport resultat = service.appliquerFermetures();
        em.flush();
        em.clear();

        // 2 POI trouvés, les 5 autres osmId du fichier n'existent pas en base
        assertThat(resultat).isEqualTo(new ResultatImport(2, 0, NB_ENTREES_FICHIER - 2));
        assertThat(fermetureRepository.count()).isEqualTo(2);

        FermetureSaisonniere planches = fermetureRepository.findById(idPlanches).orElseThrow();
        assertThat(planches.isOuvertTouteAnnee()).isTrue();
        assertThat(planches.getMoisOuvertureDebut()).isNull();
        assertThat(planches.getMoisOuvertureFin()).isNull();

        FermetureSaisonniere gsb = fermetureRepository.findById(idGsb).orElseThrow();
        assertThat(gsb.isOuvertTouteAnnee()).isFalse();
        assertThat(gsb.getMoisOuvertureDebut()).isEqualTo(6);
        assertThat(gsb.getMoisOuvertureFin()).isEqualTo(10);
    }

    @Test
    void metAJourFermetureExistanteSansDupliquerTest() {
        Long idPlanches = creerCol(PLANCHES, "Col des Planches");

        // fermeture obsolète : le fichier dit désormais "ouvert toute l'année"
        FermetureSaisonniere ancienne = new FermetureSaisonniere();
        ancienne.setPoi(em.find(Poi.class, idPlanches));
        ancienne.setOuvertTouteAnnee(false);
        ancienne.setMoisOuvertureDebut(3);
        ancienne.setMoisOuvertureFin(5);
        fermetureRepository.save(ancienne);
        em.flush();
        em.clear();

        ResultatImport resultat = service.appliquerFermetures();
        em.flush();
        em.clear();

        assertThat(resultat).isEqualTo(new ResultatImport(1, 0, NB_ENTREES_FICHIER - 1));
        // même ligne mise à jour, pas de doublon
        assertThat(fermetureRepository.count()).isEqualTo(1);

        FermetureSaisonniere maj = fermetureRepository.findById(idPlanches).orElseThrow();
        assertThat(maj.isOuvertTouteAnnee()).isTrue();
        // les mois doivent être remis à null (contrainte ck_fermeture_coherence)
        assertThat(maj.getMoisOuvertureDebut()).isNull();
        assertThat(maj.getMoisOuvertureFin()).isNull();
    }

    @Test
    void sansPoiCorrespondantNeCreeRienTest() {
        ResultatImport resultat = service.appliquerFermetures();
        em.flush();

        assertThat(resultat).isEqualTo(new ResultatImport(0, 0, NB_ENTREES_FICHIER));
        assertThat(fermetureRepository.count()).isZero();
    }

    // Persiste un col minimal et renvoie son id
    private Long creerCol(long osmId, String nom) {
        Poi poi = new Poi();
        poi.setOsmId(osmId);
        poi.setNom(nom);
        poi.setType(TypePoi.COL);
        poi.setSource(Source.OSM);
        poi.setLocalisation(GEO.createPoint(new Coordinate(7.0, 46.0)));
        Long id = poiRepository.save(poi).getId();
        em.flush();
        return id;
    }
}
