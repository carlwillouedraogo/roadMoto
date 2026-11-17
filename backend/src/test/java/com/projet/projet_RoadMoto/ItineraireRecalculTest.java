package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.client.OrsClient;
import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Role;
import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.domain.TypeEtape;
import com.projet.projet_RoadMoto.dto.itinerairedto.CalculItineraireRequest;
import com.projet.projet_RoadMoto.dto.etapedto.EtapeRequest;
import com.projet.projet_RoadMoto.dto.itinerairedto.ItineraireCalcule;
import com.projet.projet_RoadMoto.dto.etapedto.TronconCalcule;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import com.projet.projet_RoadMoto.services.ItineraireService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Recalcul d'un itinéraire contre une vraie base PostGIS : le second calcul doit remplacer le premier
 * (une seule ligne dans itineraire, les étapes du premier calcul supprimées) sans violer uk_etape_ordre.
 * Seul le client ORS est mocké, ce qui permet de compter les appels réseau.
 * @SpringBootTest : le service ouvre lui-même ses transactions (TransactionTemplate),
 * d'où le nettoyage manuel dans @AfterEach (pas de rollback automatique).
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class ItineraireRecalculTest {

    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);
    private static final String ORGANISATEUR = "orga-itineraire@example.com";

    @Autowired private ItineraireService itineraireService;
    @Autowired private SortieRepository sortieRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private OrsClient orsClient;

    private Sortie sortie;

    @BeforeEach
    void preparer() {
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur());
        sortie = sortieRepository.save(creerSortie(organisateur));

        // 3 étapes demandées → ORS renvoie 2 tronçons
        when(orsClient.calculer(any())).thenReturn(new ItineraireCalcule(
                GEO.createLineString(new Coordinate[]{
                        new Coordinate(7.36, 46.23), new Coordinate(7.29, 46.33), new Coordinate(7.10, 46.10)}),
                32_340, 4_500,
                List.of(new TronconCalcule(12_340, 1_800), new TronconCalcule(20_000, 2_700))));
    }

    // l'itinéraire et ses étapes partent avec la sortie (ON DELETE CASCADE)
    @AfterEach
    void nettoyer() {
        sortieRepository.deleteAll();
        utilisateurRepository.deleteAll();
    }

    @Test
    void recalculRemplaceItineraireSansDoublonTest() {
        CalculItineraireRequest requete = new CalculItineraireRequest(List.of(
                etape(7.36, 46.23, TypeEtape.DEPART),
                etape(7.29, 46.33, TypeEtape.INTERMEDIAIRE),
                etape(7.10, 46.10, TypeEtape.ARRIVEE)));

        itineraireService.calculer(sortie.getId(), ORGANISATEUR, requete);
        itineraireService.calculer(sortie.getId(), ORGANISATEUR, requete);

        assertThat(jdbc.queryForObject(
                "select count(*) from itineraire where sortie_id = ?", Integer.class, sortie.getId()))
                .as("le recalcul remplace l'itinéraire au lieu d'en ajouter un second")
                .isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "select count(*) from etape where itineraire_id = ?", Integer.class, sortie.getId()))
                .as("seules les étapes du dernier calcul restent")
                .isEqualTo(3);

        assertThat(jdbc.queryForList(
                "select ordre from etape where itineraire_id = ? order by ordre", Integer.class, sortie.getId()))
                .containsExactly(0, 1, 2);

        // le service délègue chaque calcul au client ORS. Le mock remplace le bean et son cache :
        // le nombre de requêtes HTTP réellement envoyées à ORS est vérifié dans OrsClientCacheTest
        verify(orsClient, times(2)).calculer(any());
    }

    // ---------- helpers ----------

    private EtapeRequest etape(double lon, double lat, TypeEtape type) {
        return new EtapeRequest(null, lon, lat, type, null);
    }

    private Utilisateur creerUtilisateur() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(ORGANISATEUR);
        utilisateur.setMotDePasseHash("1233457mdp");
        utilisateur.setPseudo("orga-itineraire");
        utilisateur.setRole(Role.USER);
        utilisateur.setNiveau(Niveau.TOURING);
        return utilisateur;
    }

    private Sortie creerSortie(Utilisateur organisateur) {
        Sortie sortie = new Sortie();
        sortie.setTitre("Col du Sanetsch");
        sortie.setDescription("recalcul d'itinéraire");
        sortie.setDateDepart(OffsetDateTime.now().plusDays(30));
        sortie.setNomLieuDepart("Sion");
        sortie.setLieuDepart(GEO.createPoint(new Coordinate(7.36, 46.23)));
        sortie.setDistanceKm(32);
        sortie.setDureeEstimeeMin(75);
        sortie.setNiveau(Niveau.TOURING);
        sortie.setPlacesMax(5);
        sortie.setStatut(Statut.PUBLIEE);
        sortie.setOrganisateur(organisateur);
        sortie.setCreeLe(OffsetDateTime.now());
        return sortie;
    }
}
