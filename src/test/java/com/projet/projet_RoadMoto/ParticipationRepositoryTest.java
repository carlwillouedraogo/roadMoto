package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Role;
import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.dto.HistoriqueParticipationResponse;
import com.projet.projet_RoadMoto.dto.ParticipationResponse;
import com.projet.projet_RoadMoto.entity.Participation;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.exception.DejaInscritException;
import com.projet.projet_RoadMoto.exception.InscriptionImpossibleException;
import com.projet.projet_RoadMoto.exception.NonInscritException;
import com.projet.projet_RoadMoto.exception.SortieDejaPartieException;
import com.projet.projet_RoadMoto.repository.ParticipationRepository;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import com.projet.projet_RoadMoto.services.ParticipationService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;

import org.testcontainers.junit.jupiter.Testcontainers;


import java.time.OffsetDateTime;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({TestcontainersConfig.class, ParticipationService.class})
public class ParticipationRepositoryTest {

    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private ParticipationRepository participationRepository;
    @Autowired
    private SortieRepository sortieRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private ParticipationService participationService;
    @Autowired
    private TestEntityManager em;

    @Test
    void testInsertionParticipant(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("test@gmail.com","alpha","123457"));
        Utilisateur utilisateur = utilisateurRepository.save(creerUtilisateur("test2@emil.com","beta", "123908"));
        Sortie sortie = sortieRepository.save(creerSortie("balade moto",organisateur));
        Participation participation = participationRepository.save(creerParticipation(utilisateur,sortie));

        Assertions.assertThat(participation.getId()).isNotNull();
        Assertions.assertThat(participation.getInscritLe()).isNotNull();
        Assertions.assertThat(participation.getSortie()).isNotNull();
        Assertions.assertThat(participation.getUtilisateur()).isNotNull();
        em.flush();
        em.clear();
        Participation relue = participationRepository.findById(participation.getId()).orElseThrow();

        Assertions.assertThat(relue.getSortie().getId()).isEqualTo(sortie.getId());
        Assertions.assertThat(relue.getUtilisateur().getId()).isEqualTo(utilisateur.getId());
        Assertions.assertThat(relue.getStatut()).isEqualTo(StatutParticipation.INSCRIT);
    }

    @Test
    void testDoubleInscriptionRefuse(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@example.com","orga","12345"));
        Utilisateur motard = utilisateurRepository.save(creerUtilisateur("motard@example.com","motard","12345"));
        Sortie sortie = sortieRepository.save(creerSortie("Tour du Léman", organisateur));

        participationRepository.saveAndFlush(creerParticipation(motard, sortie));

        Assertions.assertThatThrownBy(() ->
                participationRepository.saveAndFlush(creerParticipation(motard, sortie)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testDesinscriptionPasseParticipationEnDesist(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@example.com","orga","12345"));
        Utilisateur motard = utilisateurRepository.save(creerUtilisateur("motard@example.com","motard","12345"));
        Sortie sortie = sortieRepository.save(creerSortie("Tour du Léman", organisateur));
        Participation participation = participationRepository.saveAndFlush(creerParticipation(motard, sortie));

        ParticipationResponse reponse = participationService.desInscription(sortie.getId(), motard);

        em.flush();
        em.clear();

        Participation relue = participationRepository.findById(participation.getId()).orElseThrow();
        Assertions.assertThat(relue.getStatut()).isEqualTo(StatutParticipation.DESIST);
        Assertions.assertThat(reponse.statut()).isEqualTo(StatutParticipation.DESIST);
        Assertions.assertThat(reponse.placesRestantes()).isEqualTo(10);
        Assertions.assertThat(participationRepository.countBySortieIdAndStatut(sortie.getId(), StatutParticipation.INSCRIT)).isZero();
    }

    @Test
    void testDesinscriptionNonInscritRefusee(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@example.com","orga","12345"));
        Utilisateur motard = utilisateurRepository.save(creerUtilisateur("motard@example.com","motard","12345"));
        Sortie sortie = sortieRepository.save(creerSortie("Tour du Léman", organisateur));

        Assertions.assertThatThrownBy(() ->
                participationService.desInscription(sortie.getId(), motard))
                .isInstanceOf(NonInscritException.class);
    }

    @Test
    void testDesinscriptionDejaDesisteRefusee(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@example.com","orga","12345"));
        Utilisateur motard = utilisateurRepository.save(creerUtilisateur("motard@example.com","motard","12345"));
        Sortie sortie = sortieRepository.save(creerSortie("Tour du Léman", organisateur));
        participationRepository.saveAndFlush(creerParticipation(motard, sortie));

        participationService.desInscription(sortie.getId(), motard);

        Assertions.assertThatThrownBy(() ->
                participationService.desInscription(sortie.getId(), motard))
                .isInstanceOf(NonInscritException.class);
    }

    @Test
    void testOrganisateurNePeutPasSeDesinscrire(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@example.com","orga","12345"));
        Sortie sortie = creerSortie("Tour du Léman", organisateur);
        sortie.setStatut(Statut.PUBLIEE);
        Sortie publiee = sortieRepository.save(sortie);
        participationService.inscrireOrganisateur(publiee, organisateur);

        Assertions.assertThatThrownBy(() ->
                participationService.desInscription(publiee.getId(), organisateur))
                .isInstanceOf(InscriptionImpossibleException.class);
        Assertions.assertThat(participationRepository.countBySortieIdAndStatut(publiee.getId(), StatutParticipation.INSCRIT)).isEqualTo(1);
    }

    @Test
    void testReinscriptionApresDesistement(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@example.com","orga","12345"));
        Utilisateur motard = utilisateurRepository.save(creerUtilisateur("motard@example.com","motard","12345"));
        Sortie sortie = creerSortie("Tour du Léman", organisateur);
        sortie.setStatut(Statut.PUBLIEE);
        sortie = sortieRepository.save(sortie);

        ParticipationResponse inscription = participationService.sInscrire(sortie.getId(), motard);
        participationService.desInscription(sortie.getId(), motard);
        ParticipationResponse reinscription = participationService.sInscrire(sortie.getId(), motard);

        em.flush();
        em.clear();

        Assertions.assertThat(reinscription.id()).isEqualTo(inscription.id());   // même ligne réactivée
        Assertions.assertThat(reinscription.statut()).isEqualTo(StatutParticipation.INSCRIT);
        Assertions.assertThat(reinscription.placesRestantes()).isEqualTo(9);
        Assertions.assertThat(participationRepository.findById(inscription.id()).orElseThrow().getStatut())
                .isEqualTo(StatutParticipation.INSCRIT);
    }

    @Test
    void testInscriptionDejaInscritRefusee(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@example.com","orga","12345"));
        Utilisateur motard = utilisateurRepository.save(creerUtilisateur("motard@example.com","motard","12345"));
        Sortie sortie = creerSortie("Tour du Léman", organisateur);
        sortie.setStatut(Statut.PUBLIEE);
        Sortie publiee = sortieRepository.save(sortie);

        participationService.sInscrire(publiee.getId(), motard);

        Assertions.assertThatThrownBy(() ->
                participationService.sInscrire(publiee.getId(), motard))
                .isInstanceOf(DejaInscritException.class);
    }

    @Test
    void testDesinscriptionSortieDejaPartieRefusee(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@example.com","orga","12345"));
        Utilisateur motard = utilisateurRepository.save(creerUtilisateur("motard@example.com","motard","12345"));
        Sortie sortie = creerSortie("Tour du Léman", organisateur);
        sortie.setStatut(Statut.PUBLIEE);
        sortie.setDateDepart(OffsetDateTime.now().minusMinutes(5));
        Sortie partie = sortieRepository.save(sortie);
        participationRepository.saveAndFlush(creerParticipation(motard, partie));

        Assertions.assertThatThrownBy(() ->
                participationService.desInscription(partie.getId(), motard))
                .isInstanceOf(SortieDejaPartieException.class);
    }

    @Test
    void testDesinscriptionRouvreSortieComplete(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@example.com","orga","12345"));
        Utilisateur motard = utilisateurRepository.save(creerUtilisateur("motard@example.com","motard","12345"));
        Sortie sortie = creerSortie("Tour du Léman", organisateur);
        sortie.setPlacesMax(2);                          // minimum imposé par ck_sortie_places_max
        sortie.setStatut(Statut.COMPLETE);
        sortie = sortieRepository.save(sortie);
        participationRepository.save(creerParticipation(organisateur, sortie));
        participationRepository.saveAndFlush(creerParticipation(motard, sortie));

        participationService.desInscription(sortie.getId(), motard);

        em.flush();
        em.clear();

        Assertions.assertThat(sortieRepository.findById(sortie.getId()).orElseThrow().getStatut()).isEqualTo(Statut.PUBLIEE);
    }

    @Test
    void testHistoriqueParticipant(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@example.com","orga","12345"));
        Utilisateur motard = utilisateurRepository.save(creerUtilisateur("motard@example.com","motard","12345"));
        Sortie inscrite = sortieRepository.save(creerSortie("Tour du Léman", organisateur));
        Sortie quittee = sortieRepository.save(creerSortie("Col de la Faucille", organisateur));
        Sortie organisee = sortieRepository.save(creerSortie("Ma balade", motard));
        participationRepository.save(creerParticipation(motard, inscrite));
        Participation desistement = creerParticipation(motard, quittee);
        desistement.setStatut(StatutParticipation.DESIST);
        participationRepository.save(desistement);
        participationService.inscrireOrganisateur(organisee, motard);

        em.flush();
        em.clear();

        Page<HistoriqueParticipationResponse> historique = participationService.historique(motard, null, PageRequest.of(0, 10));
        Page<HistoriqueParticipationResponse> desistements = participationService.historique(motard, StatutParticipation.DESIST, PageRequest.of(0, 10));

        // la sortie qu'il organise relève de l'historique organisateur
        Assertions.assertThat(historique.getContent()).extracting(h -> h.sortie().id())
                .containsExactlyInAnyOrder(inscrite.getId(), quittee.getId());
        Assertions.assertThat(desistements.getContent()).extracting(h -> h.sortie().id())
                .containsExactly(quittee.getId());
        Assertions.assertThat(desistements.getContent().get(0).sortie().organisateur().pseudo()).isEqualTo("orga");
    }

    private Participation creerParticipation(Utilisateur utilisateur, Sortie sortie){
        Participation participation = new Participation();
        participation.setSortie(sortie);
        participation.setUtilisateur(utilisateur);
        participation.setStatut(StatutParticipation.INSCRIT);
        return participation;
    }

    private Sortie creerSortie(String titre, Utilisateur utilisateur){
        Sortie sortie = new Sortie();
        sortie.setTitre(titre);
        sortie.setDescription("null");
        sortie.setDateDepart(OffsetDateTime.now().plusDays(30));
        sortie.setNomLieuDepart("genève");
        sortie.setLieuDepart(GEO.createPoint(new Coordinate(6.1432,46.2044)));
        sortie.setDistanceKm(12);
        sortie.setDureeEstimeeMin(23);
        sortie.setNiveau(Niveau.TOURING);
        sortie.setPlacesMax(10);
        sortie.setStatut(Statut.BROUILLON);
        sortie.setOrganisateur(utilisateur);
        sortie.setCreeLe(OffsetDateTime.now());
        return sortie;
    }

    private Utilisateur creerUtilisateur(String mail,String pseudo, String mdp){
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(mail);
        utilisateur.setMotDePasseHash(mdp);
        utilisateur.setPseudo(pseudo);
        utilisateur.setRole(Role.USER);
        utilisateur.setNiveau(Niveau.TOURING);
        return utilisateur;
    }
}
