package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Role;
import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;

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


import java.time.OffsetDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(TestcontainersConfig.class)
public class SortieRepositoryTest {

    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private SortieRepository sortieRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void createSortieTest(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("carl@example.com"));
        Sortie sortie = sortieRepository.save(creerSortie("Tour du Léman", organisateur));

        assertThat(sortie.getId()).isNotNull();          // l'INSERT a eu lieu
        assertThat(sortie.getCreeLe()).isNotNull();      // @CreationTimestamp a joué

        em.flush();
        em.clear();                                      // vide le cache de premier niveau

        Sortie relue = sortieRepository.findById(sortie.getId()).orElseThrow();

        assertThat(relue.getTitre()).isEqualTo("Tour du Léman");
        assertThat(relue.getStatut()).isEqualTo(Statut.BROUILLON);
        assertThat(relue.getOrganisateur().getId()).isEqualTo(organisateur.getId());
        assertThat(relue.getLieuDepart().getX()).isEqualTo(6.1432, within(0.0001));
        assertThat(relue.getLieuDepart().getY()).isEqualTo(46.2044, within(0.0001));
    }

    private Sortie creerSortie(String titre, Utilisateur utilisateur){
        Sortie sortie = new Sortie();
        sortie.setTitre(titre);
        sortie.setDescription("null");
        sortie.setDateDepart(OffsetDateTime.now());
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

    private Utilisateur creerUtilisateur(String mail){
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(mail);
        utilisateur.setMotDePasseHash("motdepasse123");
        utilisateur.setPseudo("dupuis");
        utilisateur.setRole(Role.USER);
        utilisateur.setNiveau(Niveau.TOURING);
        return utilisateur;
    }
}
