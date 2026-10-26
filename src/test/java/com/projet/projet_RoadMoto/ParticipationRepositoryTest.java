package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Role;
import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.entity.Participation;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.repository.ParticipationRepository;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
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
import org.springframework.dao.DataIntegrityViolationException;

import org.testcontainers.junit.jupiter.Testcontainers;


import java.time.OffsetDateTime;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(TestcontainersConfig.class)
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
