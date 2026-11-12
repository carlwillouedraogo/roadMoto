package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.client.OrsClient;
import com.projet.projet_RoadMoto.domain.Source;
import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.domain.TypeEtape;
import com.projet.projet_RoadMoto.dto.itinerairedto.CalculItineraireRequest;
import com.projet.projet_RoadMoto.dto.etapedto.EtapeRequest;
import com.projet.projet_RoadMoto.dto.etapedto.EtapeResponse;
import com.projet.projet_RoadMoto.dto.itinerairedto.ItineraireCalcule;
import com.projet.projet_RoadMoto.dto.itinerairedto.ItineraireResponse;
import com.projet.projet_RoadMoto.dto.etapedto.TronconCalcule;
import com.projet.projet_RoadMoto.entity.Etape;
import com.projet.projet_RoadMoto.entity.Itineraire;
import com.projet.projet_RoadMoto.entity.Poi;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.exception.EtapesInvalidesException;
import com.projet.projet_RoadMoto.exception.ItineraireIncalculableException;
import com.projet.projet_RoadMoto.exception.ItineraireIntrouvableException;
import com.projet.projet_RoadMoto.exception.ItineraireNonModifiableException;
import com.projet.projet_RoadMoto.exception.MauvaisOrganisateurException;
import com.projet.projet_RoadMoto.exception.PoiIntrouvableException;
import com.projet.projet_RoadMoto.exception.SortieIntrouvableException;
import com.projet.projet_RoadMoto.repository.ItineraireRepository;
import com.projet.projet_RoadMoto.repository.PoiRepository;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.services.ItineraireService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitaire pur (pas de contexte Spring, pas de base) : les repositories, le client ORS
 * et le TransactionTemplate sont mockés, seule la logique du service est vérifiée.
 */
@ExtendWith(MockitoExtension.class)
class ItineraireServiceTest {

    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);
    private static final String ORGANISATEUR = "orga@moto.ch";
    private static final OffsetDateTime DEPART = OffsetDateTime.parse("2026-12-01T09:00:00+01:00");

    @Mock
    private SortieRepository sortieRepository;
    @Mock
    private ItineraireRepository itineraireRepository;
    @Mock
    private PoiRepository poiRepository;
    @Mock
    private OrsClient orsClient;
    @Mock
    private TransactionTemplate transactionTemplate;

    private ItineraireService service;

    @BeforeEach
    void setUp() {
        service = new ItineraireService(sortieRepository, itineraireRepository,
                poiRepository, orsClient, transactionTemplate);
    }

    // ---------- cas nominal ----------

    @Test
    void calculeEnregistreItineraireTest() {
        Poi col = poi(12L, "Col du Sanetsch", 7.29, 46.33);
        when(sortieRepository.findWithOrganisateurById(1L)).thenReturn(Optional.of(sortie(Statut.PUBLIEE)));
        when(poiRepository.findAllById(any())).thenReturn(List.of(col));
        when(orsClient.calculer(any())).thenReturn(calcule(new TronconCalcule(12_340, 1_800),
                new TronconCalcule(20_000, 2_700)));
        stubEcriture();

        ItineraireResponse reponse = service.calculer(1L, ORGANISATEUR, requete(
                etapeLibre(7.36, 46.23, null),
                etapePoi(12L, 15),
                etapeLibre(7.10, 46.10, null)));

        // coordonnées envoyées à ORS : ordre conservé, format [lon, lat], le POI remplace le point libre
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<List<Double>>> coords = ArgumentCaptor.forClass(List.class);
        verify(orsClient).calculer(coords.capture());
        assertThat(coords.getValue()).containsExactly(
                List.of(7.36, 46.23), List.of(7.29, 46.33), List.of(7.10, 46.10));

        // entité enregistrée
        ArgumentCaptor<Itineraire> captor = ArgumentCaptor.forClass(Itineraire.class);
        verify(itineraireRepository).save(captor.capture());
        Itineraire enregistre = captor.getValue();
        assertThat(enregistre.getSource()).isEqualTo(Source.OPENROUTESERVICE);
        assertThat(enregistre.getDistanceM()).isEqualTo(32_340);
        assertThat(enregistre.getDureeS()).isEqualTo(4_500);

        List<Etape> etapes = enregistre.getEtapes();
        assertThat(etapes).hasSize(3);
        assertThat(etapes).extracting(Etape::getOrdre).containsExactly(0, 1, 2);
        assertThat(etapes).extracting(Etape::getTypeEtape)
                .containsExactly(TypeEtape.DEPART, TypeEtape.INTERMEDIAIRE, TypeEtape.ARRIVEE);
        assertThat(etapes.get(0).getDistanceDepuisPrecedenteM()).isNull();
        assertThat(etapes.get(0).getDureeDepuisPrecedenteS()).isNull();
        assertThat(etapes.get(1).getDistanceDepuisPrecedenteM()).isEqualTo(12_340);
        assertThat(etapes.get(2).getDureeDepuisPrecedenteS()).isEqualTo(2_700);
        assertThat(etapes.get(1).getPoi()).isSameAs(col);
        assertThat(etapes.get(1).getDureeArretMin()).isEqualTo(15);

        // réponse
        assertThat(reponse.sortieId()).isEqualTo(1L);
        assertThat(reponse.distanceKm()).isEqualTo(32.3);
        assertThat(reponse.dureeMin()).isEqualTo(75);
        assertThat(reponse.trace()).hasSize(3);
        assertThat(reponse.etapes().get(1).poiNom()).isEqualTo("Col du Sanetsch");
        assertThat(reponse.etapes().get(1).distanceDepuisPrecedenteKm()).isEqualTo(12.3);
        assertThat(reponse.etapes().get(0).poiId()).isNull();
    }

    @Test
    void heuresPassageCumulentTrajetsArretsTest() {
        when(sortieRepository.findWithOrganisateurById(1L)).thenReturn(Optional.of(sortie(Statut.PUBLIEE)));
        when(orsClient.calculer(any())).thenReturn(calcule(new TronconCalcule(30_000, 1_800),
                new TronconCalcule(40_000, 2_700)));
        stubEcriture();

        ItineraireResponse reponse = service.calculer(1L, ORGANISATEUR, requete(
                etapeLibre(7.36, 46.23, null),
                etapeLibre(7.29, 46.33, 15),
                etapeLibre(7.10, 46.10, null)));

        // 09:00 départ, +30 min de trajet, 15 min d'arrêt, +45 min de trajet
        assertThat(reponse.etapes()).extracting(EtapeResponse::heurePassage).containsExactly(
                DEPART, DEPART.plusMinutes(30), DEPART.plusMinutes(90));
    }

    @Test
    void supprimeAncienItineraireAvantEnregistrerTest() {
        when(sortieRepository.findWithOrganisateurById(1L)).thenReturn(Optional.of(sortie(Statut.BROUILLON)));
        when(orsClient.calculer(any())).thenReturn(calcule(new TronconCalcule(10_000, 600)));
        stubEcriture();

        service.calculer(1L, ORGANISATEUR, requete(etapeLibre(7.0, 46.0, null), etapeLibre(7.1, 46.1, null)));

        InOrder ordre = inOrder(itineraireRepository);
        ordre.verify(itineraireRepository).supprimerParSortieId(1L);
        ordre.verify(itineraireRepository).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = Statut.class, names = {"BROUILLON", "PUBLIEE", "COMPLETE"})
    void accepteSortiesEncoreActivesTest(Statut statut) {
        when(sortieRepository.findWithOrganisateurById(1L)).thenReturn(Optional.of(sortie(statut)));
        when(orsClient.calculer(any())).thenReturn(calcule(new TronconCalcule(10_000, 600)));
        stubEcriture();

        assertThatCode(() -> service.calculer(1L, ORGANISATEUR,
                requete(etapeLibre(7.0, 46.0, null), etapeLibre(7.1, 46.1, null))))
                .doesNotThrowAnyException();
    }

    @Test
    void sansPoiConsultePasRepositoryPoiTest() {
        when(sortieRepository.findWithOrganisateurById(1L)).thenReturn(Optional.of(sortie(Statut.PUBLIEE)));
        when(orsClient.calculer(any())).thenReturn(calcule(new TronconCalcule(10_000, 600)));
        stubEcriture();

        service.calculer(1L, ORGANISATEUR, requete(etapeLibre(7.0, 46.0, null), etapeLibre(7.1, 46.1, null)));

        verifyNoInteractions(poiRepository);
    }

    // ---------- refus ----------

    @Test
    void refuseSortieIntrouvableTest() {
        when(sortieRepository.findWithOrganisateurById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculer(99L, ORGANISATEUR,
                requete(etapeLibre(7.0, 46.0, null), etapeLibre(7.1, 46.1, null))))
                .isInstanceOf(SortieIntrouvableException.class);
        verifyNoInteractions(orsClient, itineraireRepository);
    }

    @Test
    void refusePasOrganisateurTest() {
        when(sortieRepository.findWithOrganisateurById(1L)).thenReturn(Optional.of(sortie(Statut.PUBLIEE)));

        assertThatThrownBy(() -> service.calculer(1L, "intrus@moto.ch",
                requete(etapeLibre(7.0, 46.0, null), etapeLibre(7.1, 46.1, null))))
                .isInstanceOf(MauvaisOrganisateurException.class);
        verifyNoInteractions(orsClient, itineraireRepository);
    }

    @ParameterizedTest
    @EnumSource(value = Statut.class, names = {"ANNULEE", "TERMINEE"})
    void refuseSortieAnnuleeTermineeTest(Statut statut) {
        when(sortieRepository.findWithOrganisateurById(1L)).thenReturn(Optional.of(sortie(statut)));

        assertThatThrownBy(() -> service.calculer(1L, ORGANISATEUR,
                requete(etapeLibre(7.0, 46.0, null), etapeLibre(7.1, 46.1, null))))
                .isInstanceOf(ItineraireNonModifiableException.class);
        verifyNoInteractions(orsClient, itineraireRepository);
    }

    @Test
    void refusePoiIntrouvableTest() {
        when(sortieRepository.findWithOrganisateurById(1L)).thenReturn(Optional.of(sortie(Statut.PUBLIEE)));
        when(poiRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.calculer(1L, ORGANISATEUR,
                requete(etapeLibre(7.0, 46.0, null), etapePoi(404L, null))))
                .isInstanceOf(PoiIntrouvableException.class)
                .hasMessageContaining("404");
        verifyNoInteractions(orsClient, itineraireRepository);
    }

    @Test
    void refuseOrsRenvoieNombreTronconsIncoherentTest() {
        when(sortieRepository.findWithOrganisateurById(1L)).thenReturn(Optional.of(sortie(Statut.PUBLIEE)));
        // 3 étapes demandées mais un seul tronçon renvoyé
        when(orsClient.calculer(any())).thenReturn(calcule(new TronconCalcule(10_000, 600)));

        assertThatThrownBy(() -> service.calculer(1L, ORGANISATEUR, requete(
                etapeLibre(7.0, 46.0, null), etapeLibre(7.1, 46.1, null), etapeLibre(7.2, 46.2, null))))
                .isInstanceOf(ItineraireIncalculableException.class);
        verifyNoInteractions(itineraireRepository);
    }

    @Test
    void refuseDeuxEtapesConsecutivesAuMemeEndroitTest() {
        when(sortieRepository.findWithOrganisateurById(1L)).thenReturn(Optional.of(sortie(Statut.PUBLIEE)));

        assertThatThrownBy(() -> service.calculer(1L, ORGANISATEUR, requete(
                etapeLibre(7.36, 46.23, null), etapeLibre(7.36, 46.23, null))))
                .isInstanceOf(EtapesInvalidesException.class)
                .hasMessageContaining("0 et 1");
        verifyNoInteractions(orsClient, itineraireRepository);
    }

    @Test
    void accepteBoucleRetourDepartTest() {
        when(sortieRepository.findWithOrganisateurById(1L)).thenReturn(Optional.of(sortie(Statut.PUBLIEE)));
        when(orsClient.calculer(any())).thenReturn(calcule(new TronconCalcule(10_000, 600),
                new TronconCalcule(10_000, 600)));
        stubEcriture();

        assertThatCode(() -> service.calculer(1L, ORGANISATEUR, requete(
                etapeLibre(7.36, 46.23, null), etapeLibre(7.29, 46.33, null), etapeLibre(7.36, 46.23, null))))
                .doesNotThrowAnyException();
    }

    // ---------- getItineraire ----------

    @Test
    void getItineraireRenvoieItineraireEnregistreTest() {
        Itineraire itineraire = itineraireExistant(sortie(Statut.PUBLIEE));
        when(itineraireRepository.findWithEtapesById(1L)).thenReturn(Optional.of(itineraire));

        ItineraireResponse reponse = service.getItineraire(1L, null);

        assertThat(reponse.sortieId()).isEqualTo(1L);
        assertThat(reponse.distanceKm()).isEqualTo(25.0);
        assertThat(reponse.dureeMin()).isEqualTo(40);
        assertThat(reponse.source()).isEqualTo(Source.OPENROUTESERVICE);
        assertThat(reponse.calculeLe()).isEqualTo(itineraire.getCalculeLe());
        assertThat(reponse.trace()).containsExactly(List.of(7.36, 46.23), List.of(7.29, 46.33));

        assertThat(reponse.etapes()).hasSize(2);
        EtapeResponse depart = reponse.etapes().get(0);
        EtapeResponse arrivee = reponse.etapes().get(1);
        assertThat(depart.type()).isEqualTo(TypeEtape.DEPART);
        assertThat(depart.poiId()).isNull();
        assertThat(depart.distanceDepuisPrecedenteKm()).isNull();
        assertThat(depart.heurePassage()).isEqualTo(DEPART);
        assertThat(arrivee.type()).isEqualTo(TypeEtape.ARRIVEE);
        assertThat(arrivee.poiId()).isEqualTo(12L);
        assertThat(arrivee.poiNom()).isEqualTo("Col du Sanetsch");
        assertThat(arrivee.longitude()).isEqualTo(7.29);
        assertThat(arrivee.latitude()).isEqualTo(46.33);
        assertThat(arrivee.distanceDepuisPrecedenteKm()).isEqualTo(25.0);
        assertThat(arrivee.dureeDepuisPrecedenteMin()).isEqualTo(40);
        assertThat(arrivee.heurePassage()).isEqualTo(DEPART.plusMinutes(40));
    }

    @Test
    void getItineraireRefuseSiAucunItineraireTest() {
        when(itineraireRepository.findWithEtapesById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getItineraire(1L, null))
                .isInstanceOf(ItineraireIntrouvableException.class)
                .hasMessageContaining("1");
    }

    @Test
    void getItineraireBrouillonVisibleParOrganisateurTest() {
        Sortie sortie = sortie(Statut.BROUILLON);
        when(itineraireRepository.findWithEtapesById(1L)).thenReturn(Optional.of(itineraireExistant(sortie)));

        assertThat(service.getItineraire(1L, sortie.getOrganisateur()).sortieId()).isEqualTo(1L);
    }

    @Test
    void getItineraireBrouillonCacheAuxAnonymesTest() {
        when(itineraireRepository.findWithEtapesById(1L))
                .thenReturn(Optional.of(itineraireExistant(sortie(Statut.BROUILLON))));

        assertThatThrownBy(() -> service.getItineraire(1L, null))
                .isInstanceOf(ItineraireIntrouvableException.class);
    }

    @Test
    void getItineraireBrouillonCacheAuxAutresUtilisateursTest() {
        when(itineraireRepository.findWithEtapesById(1L))
                .thenReturn(Optional.of(itineraireExistant(sortie(Statut.BROUILLON))));

        assertThatThrownBy(() -> service.getItineraire(1L, intrus()))
                .isInstanceOf(ItineraireIntrouvableException.class);
    }

    // ---------- deleteItineraire ----------

    @Test
    void deleteItineraireSiOrganisateurTest() {
        Sortie sortie = sortie(Statut.PUBLIEE);
        Itineraire itineraire = itineraireExistant(sortie);
        when(itineraireRepository.findById(1L)).thenReturn(Optional.of(itineraire));

        service.deleteItineraire(1L, sortie.getOrganisateur());

        verify(itineraireRepository).delete(itineraire);
    }

    @Test
    void deleteItineraireSiPasOrganisateurTest() {
        when(itineraireRepository.findById(1L)).thenReturn(Optional.of(itineraireExistant(sortie(Statut.PUBLIEE))));

        assertThatThrownBy(() -> service.deleteItineraire(1L, intrus()))
                .isInstanceOf(MauvaisOrganisateurException.class);
        verify(itineraireRepository, never()).delete(any(Itineraire.class));
    }

    @Test
    void deleteItineraireSiAucunItineraireTest() {
        when(itineraireRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteItineraire(1L, sortie(Statut.PUBLIEE).getOrganisateur()))
                .isInstanceOf(ItineraireIntrouvableException.class);
        verify(itineraireRepository, never()).delete(any(Itineraire.class));
    }

    @ParameterizedTest
    @EnumSource(value = Statut.class, names = {"ANNULEE", "TERMINEE"})
    void deleteItineraireRefuseSiSortieAnnuleeOuTermineeTest(Statut statut) {
        Sortie sortie = sortie(statut);
        when(itineraireRepository.findById(1L)).thenReturn(Optional.of(itineraireExistant(sortie)));

        assertThatThrownBy(() -> service.deleteItineraire(1L, sortie.getOrganisateur()))
                .isInstanceOf(ItineraireNonModifiableException.class);
        verify(itineraireRepository, never()).delete(any(Itineraire.class));
    }

    // ---------- helpers ----------
    private Utilisateur intrus() {
        Utilisateur intrus = new Utilisateur();
        intrus.setId(8L);
        intrus.setEmail("intrus@moto.ch");
        return intrus;
    }


    /** Itinéraire déjà en base : départ libre, arrivée sur un POI, 25 km en 40 min. */
    private Itineraire itineraireExistant(Sortie sortie) {
        Itineraire itineraire = new Itineraire();
        itineraire.setId(sortie.getId());
        itineraire.setSortie(sortie);
        itineraire.setTrace(GEO.createLineString(new Coordinate[]{
                new Coordinate(7.36, 46.23), new Coordinate(7.29, 46.33)}));
        itineraire.setDistanceM(25_000);
        itineraire.setDureeS(2_400);
        itineraire.setSource(Source.OPENROUTESERVICE);
        itineraire.setCalculeLe(OffsetDateTime.parse("2026-11-10T18:00:00+01:00"));

        Etape depart = new Etape();
        depart.setPosition(GEO.createPoint(new Coordinate(7.36, 46.23)));
        depart.setTypeEtape(TypeEtape.DEPART);
        itineraire.ajouterEtape(depart);

        Poi col = poi(12L, "Col du Sanetsch", 7.29, 46.33);
        Etape arrivee = new Etape();
        arrivee.setPosition(col.getLocalisation());
        arrivee.setPoi(col);
        arrivee.setTypeEtape(TypeEtape.ARRIVEE);
        arrivee.setDistanceDepuisPrecedenteM(25_000);
        arrivee.setDureeDepuisPrecedenteS(2_400);
        itineraire.ajouterEtape(arrivee);
        return itineraire;
    }

    /** Le TransactionTemplate exécute directement le callback ; save() renvoie l'entité reçue. */
    private void stubEcriture() {
        when(transactionTemplate.execute(any())).thenAnswer(inv ->
                inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        when(itineraireRepository.save(any(Itineraire.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Sortie sortie(Statut statut) {
        Utilisateur organisateur = new Utilisateur();
        organisateur.setId(7L);
        organisateur.setEmail(ORGANISATEUR);

        Sortie sortie = new Sortie();
        sortie.setId(1L);
        sortie.setStatut(statut);
        sortie.setDateDepart(DEPART);
        sortie.setOrganisateur(organisateur);
        return sortie;
    }

    private Poi poi(Long id, String nom, double lon, double lat) {
        Poi poi = new Poi();
        poi.setId(id);
        poi.setNom(nom);
        poi.setLocalisation(GEO.createPoint(new Coordinate(lon, lat)));
        return poi;
    }

    private EtapeRequest etapeLibre(double lon, double lat, Integer arretMin) {
        return new EtapeRequest(null, lon, lat, TypeEtape.INTERMEDIAIRE, arretMin);
    }

    private EtapeRequest etapePoi(Long poiId, Integer arretMin) {
        return new EtapeRequest(poiId, null, null, TypeEtape.INTERMEDIAIRE, arretMin);
    }

    private CalculItineraireRequest requete(EtapeRequest... etapes) {
        return new CalculItineraireRequest(List.of(etapes));
    }

    /** Résultat ORS factice : distance et durée totales = somme des tronçons. */
    private ItineraireCalcule calcule(TronconCalcule... troncons) {
        LineString trace = GEO.createLineString(new Coordinate[]{
                new Coordinate(7.36, 46.23), new Coordinate(7.29, 46.33), new Coordinate(7.10, 46.10)});
        int distance = 0, duree = 0;
        for (TronconCalcule t : troncons) {
            distance += t.distanceM();
            duree += t.dureeS();
        }
        return new ItineraireCalcule(trace, distance, duree, List.of(troncons));
    }
}
