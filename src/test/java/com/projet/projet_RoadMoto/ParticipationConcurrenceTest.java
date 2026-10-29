package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Role;
import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.dto.UtilisateurResponse;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.exception.SortieCompleteException;
import com.projet.projet_RoadMoto.repository.ParticipationRepository;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import com.projet.projet_RoadMoto.services.ParticipationService;
import com.projet.projet_RoadMoto.services.SortieService;
import org.junit.jupiter.api.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;


import java.time.OffsetDateTime;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfig.class)
class ParticipationConcurrenceTest {

    @Autowired private ParticipationService participationService;
    @Autowired private SortieService sortieService;
    @Autowired private SortieRepository sortieRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private ParticipationRepository participationRepository;

    private Sortie sortie;

    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);


    @BeforeEach
    void preparer() {
        Utilisateur organisateur = utilisateurRepository.save(
                creerUtilisateur("orga@example.com", "orga"));

        // 2 places : l'organisateur en occupe une, il en reste exactement une
        sortie = sortieRepository.save(creerSortie("Col du Susten", organisateur, 2));
        participationService.inscrireOrganisateur(sortie, organisateur);

        motardA = utilisateurRepository.save(creerUtilisateur("motard-a@example.com", "motard-a"));
        motardB = utilisateurRepository.save(creerUtilisateur("motard-b@example.com", "motard-b"));
    }

    @AfterEach
    void nettoyer() {
        participationRepository.deleteAll();
        sortieRepository.deleteAll();
        utilisateurRepository.deleteAll();
    }

    private Utilisateur motardA;
    private Utilisateur motardB;

    @Test
    void deuxInscriptionsSimultaneesDepassentPasCapaciteTest() throws Exception {
        CountDownLatch topDepart = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<Boolean> tentativeA = tentative(motardA, topDepart);
        Callable<Boolean> tentativeB = tentative(motardB, topDepart);

        Future<Boolean> resultatA = pool.submit(tentativeA);
        Future<Boolean> resultatB = pool.submit(tentativeB);

        topDepart.countDown();   // les deux threads partent ensemble

        int succes = (resultatA.get(10, TimeUnit.SECONDS) ? 1 : 0)
                + (resultatB.get(10, TimeUnit.SECONDS) ? 1 : 0);
        pool.shutdown();

        assertThat(succes)
                .as("une seule des deux inscriptions doit aboutir")
                .isEqualTo(1);

        assertThat(participationRepository.countBySortieIdAndStatut(
                sortie.getId(), StatutParticipation.INSCRIT))
                .as("la capacité ne doit jamais être dépassée")
                .isEqualTo(2);
    }

    private Callable<Boolean> tentative(Utilisateur utilisateur, CountDownLatch topDepart) {
        return () -> {
            topDepart.await();
            try {
                participationService.sInscrire(sortie.getId(), utilisateur);
                return true;
            } catch (SortieCompleteException e) {
                return false;   // SortieCompleteException attendue pour le perdant
            }
        };
    }

    private Utilisateur creerUtilisateur(String mail,String pseudo){
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(mail);
        utilisateur.setMotDePasseHash("1233457mdp");
        utilisateur.setPseudo(pseudo);
        utilisateur.setRole(Role.USER);
        utilisateur.setNiveau(Niveau.TOURING);
        return utilisateur;
    }

    private Sortie creerSortie(String titre, Utilisateur utilisateur, int nbPlaces){
        Sortie sortie = new Sortie();
        sortie.setTitre(titre);
        sortie.setDescription("null");
        sortie.setDateDepart(OffsetDateTime.now().plusDays(30));
        sortie.setNomLieuDepart("genève");
        sortie.setLieuDepart(GEO.createPoint(new Coordinate(6.1432,46.2044)));
        sortie.setDistanceKm(12);
        sortie.setDureeEstimeeMin(23);
        sortie.setNiveau(Niveau.TOURING);
        sortie.setPlacesMax(nbPlaces);
        sortie.setStatut(Statut.PUBLIEE);
        sortie.setOrganisateur(utilisateur);
        sortie.setCreeLe(OffsetDateTime.now());
        return sortie;
    }
}
