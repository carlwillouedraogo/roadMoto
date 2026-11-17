package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.dto.AvertissementResponse;
import com.projet.projet_RoadMoto.entity.FermetureSaisonniere;
import com.projet.projet_RoadMoto.entity.Poi;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.exception.ItineraireIntrouvableException;
import com.projet.projet_RoadMoto.exception.SortieIntrouvableException;
import com.projet.projet_RoadMoto.repository.FermetureSaisonniereRepository;
import com.projet.projet_RoadMoto.repository.ItineraireRepository;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.services.FermetureSaisonniereService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitaire pur : le repository est mocké (la requête spatiale ST_DWithin n'est pas testée ici),
 * on vérifie le filtrage par mois de départ et la construction des avertissements.
 */
@ExtendWith(MockitoExtension.class)
class FermetureSaisonniereServiceTest {

    private static final Long SORTIE_ID = 42L;
    private static final int TOLERANCE = 500;

    @Mock
    private FermetureSaisonniereRepository fermetureSaisonniereRepository;

    @Mock
    private SortieRepository sortieRepository;

    @Mock
    private ItineraireRepository itineraireRepository;

    private FermetureSaisonniereService service;

    @BeforeEach
    void setUp() {
        service = new FermetureSaisonniereService(fermetureSaisonniereRepository, sortieRepository,
                itineraireRepository, TOLERANCE);
    }

    @Test
    void colFermeAuMoisDeDepartProduitUnAvertissementCompletTest() {
        FermetureSaisonniere galibier = fermeture(1L, "Col du Galibier", 2642, 6, 10);
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, TOLERANCE)).thenReturn(List.of(galibier));

        List<AvertissementResponse> avertissements = service.avertissements(SORTIE_ID, depart(12));

        assertThat(avertissements).containsExactly(new AvertissementResponse(
                1L, "Col du Galibier", 2642, 6, 10,
                "Col du Galibier (2642 m) : généralement fermé en décembre, ouvert de juin à octobre."));
    }

    @Test
    void colOuvertAuMoisDeDepartAucunAvertissementTest() {
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, TOLERANCE))
                .thenReturn(List.of(fermeture(1L, "Col du Galibier", 2642, 6, 10)));

        assertThat(service.avertissements(SORTIE_ID, depart(7))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {6, 10})
    void bornesDeLaPeriodeDOuvertureInclusivesTest(int mois) {
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, TOLERANCE))
                .thenReturn(List.of(fermeture(1L, "Col du Galibier", 2642, 6, 10)));

        assertThat(service.avertissements(SORTIE_ID, depart(mois))).isEmpty();
    }

    @Test
    void colOuvertTouteLAnneeAucunAvertissementTest() {
        FermetureSaisonniere col = fermeture(1L, "Col de la Faucille", 1323, null, null);
        col.setOuvertTouteAnnee(true);
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, TOLERANCE)).thenReturn(List.of(col));

        assertThat(service.avertissements(SORTIE_ID, depart(1))).isEmpty();
    }

    @Test
    void moisDOuvertureInconnusColConsidereOuvertTest() {
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, TOLERANCE))
                .thenReturn(List.of(fermeture(1L, "Col inconnu", 1800, 6, null)));

        assertThat(service.avertissements(SORTIE_ID, depart(1))).isEmpty();
    }

    @Test
    void periodeDOuvertureAChevalSurLAnneeEstGereeTest() {
        // Ouvert de novembre à mars : fermé en juillet, ouvert en janvier
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, TOLERANCE))
                .thenReturn(List.of(fermeture(1L, "Col d'hiver", 1500, 11, 3)));

        assertThat(service.avertissements(SORTIE_ID, depart(1))).isEmpty();
        assertThat(service.avertissements(SORTIE_ID, depart(7)))
                .singleElement()
                .extracting(AvertissementResponse::message)
                .isEqualTo("Col d'hiver (1500 m) : généralement fermé en juillet, ouvert de novembre à mars.");
    }

    @Test
    void altitudeInconnueAbsenteDuMessageTest() {
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, TOLERANCE))
                .thenReturn(List.of(fermeture(1L, "Col du Grimsel", null, 6, 10)));

        assertThat(service.avertissements(SORTIE_ID, depart(1)))
                .singleElement()
                .satisfies(a -> {
                    assertThat(a.altitude()).isNull();
                    assertThat(a.message())
                            .isEqualTo("Col du Grimsel : généralement fermé en janvier, ouvert de juin à octobre.");
                });
    }

    @Test
    void neGardeQueLesColsFermesDansLOrdreDuRepositoryTest() {
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, TOLERANCE)).thenReturn(List.of(
                fermeture(1L, "Col du Galibier", 2642, 6, 10),
                fermeture(2L, "Col des Mosses", 1445, 1, 12),
                fermeture(3L, "Col du Grimsel", 2164, 6, 10)));

        assertThat(service.avertissements(SORTIE_ID, depart(12)))
                .extracting(AvertissementResponse::poiId)
                .containsExactly(1L, 3L);
    }

    @Test
    void aucunColTraverseListeVideTest() {
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, TOLERANCE)).thenReturn(List.of());

        assertThat(service.avertissements(SORTIE_ID, depart(12))).isEmpty();
    }

    @Test
    void transmetSortieEtLaToleranceConfigureeTest() {
        FermetureSaisonniereService avecTolerance = new FermetureSaisonniereService(fermetureSaisonniereRepository,
                sortieRepository, itineraireRepository, 1000);
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, 1000)).thenReturn(List.of());

        avecTolerance.avertissements(SORTIE_ID, depart(12));

        verify(fermetureSaisonniereRepository).colsTraverses(SORTIE_ID, 1000);
    }

    @Test
    void pourSortieSansDateUtiliseLaDateDeDepartTest() {
        when(sortieRepository.findWithOrganisateurById(SORTIE_ID)).thenReturn(Optional.of(sortie(Statut.PUBLIEE, 12)));
        when(itineraireRepository.existsById(SORTIE_ID)).thenReturn(true);
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, TOLERANCE))
                .thenReturn(List.of(fermeture(1L, "Col du Galibier", 2642, 6, 10)));

        assertThat(service.avertissementsPourSortie(SORTIE_ID, null, null)).hasSize(1);
    }

    @Test
    void pourSortieAvecDateSimuleeUtiliseCetteDateTest() {
        when(sortieRepository.findWithOrganisateurById(SORTIE_ID)).thenReturn(Optional.of(sortie(Statut.PUBLIEE, 12)));
        when(itineraireRepository.existsById(SORTIE_ID)).thenReturn(true);
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, TOLERANCE))
                .thenReturn(List.of(fermeture(1L, "Col du Galibier", 2642, 6, 10)));

        assertThat(service.avertissementsPourSortie(SORTIE_ID, LocalDate.of(2026, 7, 14), null)).isEmpty();
    }

    @Test
    void pourSortieIntrouvableTest() {
        when(sortieRepository.findWithOrganisateurById(SORTIE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.avertissementsPourSortie(SORTIE_ID, null, null))
                .isInstanceOf(SortieIntrouvableException.class);
    }

    @Test
    void pourBrouillonDUnAutreOrganisateurIntrouvableTest() {
        when(sortieRepository.findWithOrganisateurById(SORTIE_ID)).thenReturn(Optional.of(sortie(Statut.BROUILLON, 12)));
        Utilisateur autre = new Utilisateur();
        autre.setId(99L);

        assertThatThrownBy(() -> service.avertissementsPourSortie(SORTIE_ID, null, autre))
                .isInstanceOf(SortieIntrouvableException.class);
    }

    @Test
    void pourBrouillonVisibleParSonOrganisateurTest() {
        Sortie brouillon = sortie(Statut.BROUILLON, 12);
        when(sortieRepository.findWithOrganisateurById(SORTIE_ID)).thenReturn(Optional.of(brouillon));
        when(itineraireRepository.existsById(SORTIE_ID)).thenReturn(true);
        when(fermetureSaisonniereRepository.colsTraverses(SORTIE_ID, TOLERANCE)).thenReturn(List.of());

        assertThat(service.avertissementsPourSortie(SORTIE_ID, null, brouillon.getOrganisateur())).isEmpty();
    }

    @Test
    void pourSortieSansItineraireTest() {
        when(sortieRepository.findWithOrganisateurById(SORTIE_ID)).thenReturn(Optional.of(sortie(Statut.PUBLIEE, 12)));
        when(itineraireRepository.existsById(SORTIE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.avertissementsPourSortie(SORTIE_ID, null, null))
                .isInstanceOf(ItineraireIntrouvableException.class);
    }

    private static Sortie sortie(Statut statut, int moisDepart) {
        Utilisateur organisateur = new Utilisateur();
        organisateur.setId(1L);
        Sortie sortie = new Sortie();
        sortie.setId(SORTIE_ID);
        sortie.setStatut(statut);
        sortie.setOrganisateur(organisateur);
        sortie.setDateDepart(depart(moisDepart));
        return sortie;
    }

    private static OffsetDateTime depart(int mois) {
        return OffsetDateTime.parse("2026-%02d-15T09:00:00+01:00".formatted(mois));
    }

    private static FermetureSaisonniere fermeture(Long poiId, String nom, Integer altitude,
                                                  Integer debut, Integer fin) {
        Poi poi = new Poi();
        poi.setId(poiId);
        poi.setNom(nom);
        poi.setAltitudeMetres(altitude);

        FermetureSaisonniere f = new FermetureSaisonniere();
        f.setPoiId(poiId);
        f.setPoi(poi);
        f.setMoisOuvertureDebut(debut);
        f.setMoisOuvertureFin(fin);
        return f;
    }
}
