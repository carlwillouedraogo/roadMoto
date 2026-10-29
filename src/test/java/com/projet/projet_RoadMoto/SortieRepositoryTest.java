package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Role;
import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.dto.SortieResponse;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.exception.InscriptionImpossibleException;
import com.projet.projet_RoadMoto.exception.MauvaisOrganisateurException;
import com.projet.projet_RoadMoto.exception.MauvaiseSortieException;
import com.projet.projet_RoadMoto.exception.SortieDejaPartieException;
import com.projet.projet_RoadMoto.exception.SortieNotFoundException;
import com.projet.projet_RoadMoto.exception.SuppressionImpossibleException;
import com.projet.projet_RoadMoto.exception.TransitionInvalideException;
import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.repository.ParticipationRepository;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;

import com.projet.projet_RoadMoto.services.ParticipationService;
import com.projet.projet_RoadMoto.services.SortieService;
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
import org.testcontainers.junit.jupiter.Testcontainers;


import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({TestcontainersConfig.class, SortieService.class, ParticipationService.class})
public class SortieRepositoryTest {

    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private SortieRepository sortieRepository;

    @Autowired
    private SortieService sortieService;

    @Autowired
    private ParticipationService participationService;

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private TestEntityManager em;

    /**
    Test créant un sortie
     */

    @Test
    void createSortieTest(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("carl@example.com","carl"));
        Sortie sortie = sortieRepository.save(creerSortie("Tour du Léman", organisateur, Statut.BROUILLON));

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

    /**
     * Test permettant de vérifier qu'un organisateur peut modifier le statut de sa sortie en publiée
     */

    @Test
    void testSortiePublieeParOrganisateur(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Sortie brouillon = sortieRepository.save(creerSortie("balade moto", organisateur, Statut.BROUILLON));

        assertThat(brouillon.getId()).isNotNull();
        assertThat(brouillon.getStatut()).isEqualTo(Statut.BROUILLON);

        em.flush();
        em.clear();

        SortieResponse publiee = sortieService.publierSortie(brouillon.getId(),organisateur.getEmail());

        em.flush();
        em.clear();                                      // relit la base, pas le cache

        Sortie publieeR = sortieRepository.findById(publiee.id()).orElseThrow();

        assertThat(publieeR.getStatut()).isEqualTo(Statut.PUBLIEE);
    }

    /**
     * Test permettant de vérifier si un utilisateur authentifié ne peut modifier le statut d'une sortie
     * qu'il n'a pas créer
     */

    @Test
    void testSortiePublieeParAutreUtilisateur(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Utilisateur utilisateur = utilisateurRepository.save(creerUtilisateur("blic@gmail.com","blic"));
        Sortie brouillon = sortieRepository.save(creerSortie("balade moto", organisateur, Statut.BROUILLON));

        assertThat(brouillon.getId()).isNotNull();
        assertThat(brouillon.getStatut()).isEqualTo(Statut.BROUILLON);

        em.flush();
        em.clear();

        assertThatThrownBy(()->
                sortieService.publierSortie(brouillon.getId(),utilisateur.getEmail())
        ).isInstanceOf(MauvaisOrganisateurException.class);
    }

    /**
     * Test vérifiant qu'on ne peut pas publier une sortie inexistante
     */

    @Test
    void testPublierSortieInexistante(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));

        assertThatThrownBy(()->
                sortieService.publierSortie(-1L, organisateur.getEmail())
        ).isInstanceOf(MauvaiseSortieException.class);
    }

    /**
     * Test vérifiant qu'une sortie déjà publiée ne peut pas être publiée une seconde fois
     */

    @Test
    void testPublierSortieDejaPubliee(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Sortie publiee = sortieRepository.save(creerSortie("balade moto", organisateur, Statut.PUBLIEE));

        em.flush();
        em.clear();

        assertThatThrownBy(()->
                sortieService.publierSortie(publiee.getId(), organisateur.getEmail())
        ).isInstanceOf(TransitionInvalideException.class);
    }

    /**
     * Test vérifiant que l'organisateur peut annuler une sortie avec un statut complet
     */

    @Test
    void testSortieAnnuleeParOrganisateur(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Sortie complete = sortieRepository.save(creerSortie("balade moto", organisateur, Statut.COMPLETE));

        em.flush();
        em.clear();

        SortieResponse reponse = sortieService.annulerSortie(complete.getId(),organisateur.getEmail());

        em.flush();
        em.clear();                                      // relit la base, pas le cache

        Sortie annulee = sortieRepository.findById(reponse.id()).orElseThrow();

        assertThat(reponse.statut()).isEqualTo(Statut.ANNULEE);
        assertThat(annulee.getStatut()).isEqualTo(Statut.ANNULEE);
    }

    /**
     * Test vérifiant que l'organisateur peut annuler une sortie publiée
     */

    @Test
    void testSortiePublieeAnnuleeParOrganisateur(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Sortie publiee = sortieRepository.save(creerSortie("balade moto", organisateur, Statut.PUBLIEE));

        em.flush();
        em.clear();

        sortieService.annulerSortie(publiee.getId(), organisateur.getEmail());

        em.flush();
        em.clear();

        assertThat(sortieRepository.findById(publiee.getId()).orElseThrow().getStatut()).isEqualTo(Statut.ANNULEE);
    }

    /**
     * Test vérifiant qu'un brouillon ne peut pas être annulé (il se supprime)
     */

    @Test
    void testAnnulerBrouillonRefuse(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Sortie brouillon = sortieRepository.save(creerSortie("balade moto", organisateur, Statut.BROUILLON));

        em.flush();
        em.clear();

        assertThatThrownBy(()->
                sortieService.annulerSortie(brouillon.getId(), organisateur.getEmail())
        ).isInstanceOf(TransitionInvalideException.class);
    }

    /**
     * Test vérifiant qu'on ne peut pas s'inscrire à une sortie annulée
     */

    @Test
    void testInscriptionSortieAnnuleeRefusee(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Utilisateur motard = utilisateurRepository.save(creerUtilisateur("motard@gmail.com","motard"));
        Sortie annulee = sortieRepository.save(creerSortie("balade moto", organisateur, Statut.ANNULEE));

        assertThatThrownBy(()->
                participationService.sInscrire(annulee.getId(), motard)
        ).isInstanceOf(InscriptionImpossibleException.class);
    }

    /**
     * Test vérifiant que les autres utilisateurs ne puissent annuler une sortie publiée
     */

    @Test
    void testSortieAnnuleeParAutreUtilisateur(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Utilisateur utilisateur = utilisateurRepository.save(creerUtilisateur("blic@gmail.com","blic"));
        Sortie publiee = sortieRepository.save(creerSortie("balade moto", organisateur, Statut.PUBLIEE));

        em.flush();
        em.clear();

        assertThatThrownBy(()->
                sortieService.annulerSortie(publiee.getId(),utilisateur.getEmail())
        ).isInstanceOf(MauvaisOrganisateurException.class);
    }

    /**
     * Test permettant de vérifier qu'un utilisateur ne puisse pas annuler une sortie inexistante
     */

    @Test
    void testAnnulerSortieInexistante(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));

        assertThatThrownBy(()->
                sortieService.annulerSortie(-1L, organisateur.getEmail())
        ).isInstanceOf(MauvaiseSortieException.class);
    }

    /**
     * Test permettant de vérifier qu'un utilisateur ne puisse annuler une sortie déjà annuler
     */

    @Test
    void testAnnulerSortieDejaAnnuler(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Sortie annulee = sortieRepository.save(creerSortie("balade moto", organisateur, Statut.ANNULEE));

        em.flush();
        em.clear();

        assertThatThrownBy(()->
                sortieService.annulerSortie(annulee.getId(), organisateur.getEmail())
        ).isInstanceOf(TransitionInvalideException.class);
    }

    /**
     * Test vérifiant qu'une sortie terminée ne peut pas être annulée
     */

    @Test
    void testAnnulerSortieTermineeRefuse(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Sortie terminee = creerSortie("balade moto", organisateur, Statut.TERMINEE);
        terminee.setDateDepart(OffsetDateTime.now().minusDays(1));
        sortieRepository.save(terminee);

        em.flush();
        em.clear();

        assertThatThrownBy(()->
                sortieService.annulerSortie(terminee.getId(), organisateur.getEmail())
        ).isInstanceOf(TransitionInvalideException.class);
    }

    /**
     * Test vérifiant qu'une sortie déjà partie (pas encore passée en TERMINEE) ne peut pas être annulée
     */

    @Test
    void testAnnulerSortieDejaPartieRefuse(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Sortie partie = creerSortie("balade moto", organisateur, Statut.PUBLIEE);
        partie.setDateDepart(OffsetDateTime.now().minusMinutes(5));
        sortieRepository.save(partie);

        em.flush();
        em.clear();

        assertThatThrownBy(()->
                sortieService.annulerSortie(partie.getId(), organisateur.getEmail())
        ).isInstanceOf(SortieDejaPartieException.class);
    }

    /**
     * Test vérifiant que seules les sorties publiées ou complètes dont la balade est finie passent en TERMINEE
     */

    @Test
    void testTerminerSortiesPassees(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        OffsetDateTime maintenant = OffsetDateTime.now();

        // départ il y a 2h, 23 min de balade : finie
        Sortie finie = creerSortie("finie", organisateur, Statut.PUBLIEE);
        finie.setDateDepart(maintenant.minusHours(2));
        sortieRepository.save(finie);

        Sortie completeFinie = creerSortie("complète finie", organisateur, Statut.COMPLETE);
        completeFinie.setDateDepart(maintenant.minusHours(2));
        sortieRepository.save(completeFinie);

        // départ il y a 10 min, 23 min de balade : encore en cours
        Sortie enCours = creerSortie("en cours", organisateur, Statut.PUBLIEE);
        enCours.setDateDepart(maintenant.minusMinutes(10));
        sortieRepository.save(enCours);

        // durée inconnue : finie dès le départ
        Sortie sansDuree = creerSortie("sans durée", organisateur, Statut.PUBLIEE);
        sansDuree.setDateDepart(maintenant.minusMinutes(1));
        sansDuree.setDureeEstimeeMin(null);
        sortieRepository.save(sansDuree);

        Sortie aVenir = sortieRepository.save(creerSortie("à venir", organisateur, Statut.PUBLIEE));

        Sortie brouillonPasse = creerSortie("brouillon passé", organisateur, Statut.BROUILLON);
        brouillonPasse.setDateDepart(maintenant.minusDays(1));
        sortieRepository.save(brouillonPasse);

        Sortie annuleePassee = creerSortie("annulée passée", organisateur, Statut.ANNULEE);
        annuleePassee.setDateDepart(maintenant.minusDays(1));
        sortieRepository.save(annuleePassee);

        int terminees = sortieService.terminerSortiesPassees(maintenant);

        assertThat(terminees).isEqualTo(3);
        assertThat(statutDe(finie)).isEqualTo(Statut.TERMINEE);
        assertThat(statutDe(completeFinie)).isEqualTo(Statut.TERMINEE);
        assertThat(statutDe(sansDuree)).isEqualTo(Statut.TERMINEE);
        assertThat(statutDe(enCours)).isEqualTo(Statut.PUBLIEE);
        assertThat(statutDe(aVenir)).isEqualTo(Statut.PUBLIEE);
        assertThat(statutDe(brouillonPasse)).isEqualTo(Statut.BROUILLON);
        assertThat(statutDe(annuleePassee)).isEqualTo(Statut.ANNULEE);
    }

    private Statut statutDe(Sortie sortie){
        return sortieRepository.findById(sortie.getId()).orElseThrow().getStatut();
    }

    /**
     * Test vérifiant que le détail d'une sortie inexistante renvoie SortieNotFoundException
     */

    @Test
    void testDetailSortieInexistante(){
        assertThatThrownBy(()->
                sortieService.detailSortie(-1L, null)
        ).isInstanceOf(SortieNotFoundException.class);
    }

    /**
     Test ne retournant que les sorties publiées à venir
     */

    @Test
    void testlisteSortiesRetourneSortiesPublieesAVenir(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Sortie publiee = sortieRepository.save(creerSortie("balade moto", organisateur, Statut.PUBLIEE));
        sortieRepository.save(creerSortie("brouillon", organisateur, Statut.BROUILLON));
        Sortie passee = creerSortie("sortie passée", organisateur, Statut.PUBLIEE);
        passee.setDateDepart(OffsetDateTime.now().minusDays(1));
        sortieRepository.save(passee);

        Page<Sortie> page = sortieRepository.rechercher(OffsetDateTime.now(), null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Sortie::getId).containsExactly(publiee.getId());
    }

    /**
    Test retournant les sorties par filtres
     */

    @Test
    void listeSortiesFiltreParNiveauDateMaxEtPlacesMin(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("org@gmail.com","org"));
        Sortie touring = sortieRepository.save(creerSortie("touring", organisateur, Statut.PUBLIEE));

        Sortie sport = creerSortie("sport", organisateur, Statut.PUBLIEE);
        sport.setNiveau(Niveau.SPORT);
        sortieRepository.save(sport);

        Sortie lointaine = creerSortie("lointaine", organisateur, Statut.PUBLIEE);
        lointaine.setDateDepart(OffsetDateTime.now().plusMonths(2));
        sortieRepository.save(lointaine);

        Sortie petite = creerSortie("petite", organisateur, Statut.PUBLIEE);
        petite.setPlacesMax(3);
        sortieRepository.save(petite);

        Page<Sortie> page = sortieRepository.rechercher(OffsetDateTime.now(), Niveau.TOURING,
                OffsetDateTime.now().plusMonths(1), 5, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Sortie::getId).containsExactly(touring.getId());
    }

    /**
    Test qui vérifie qu'un utilisateur authentifié ne voit aucune sortie non publiée d'un autre utilisateur
     */

    @Test
    void testUtilisateurVoitPasBrouillon(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@gmail.com","orga"));
        Utilisateur curieux = utilisateurRepository.save(creerUtilisateur("curieux@gmail.com","curieux"));
        Sortie sortie = sortieRepository.save(creerSortie("touring", organisateur, Statut.BROUILLON));

        assertThatThrownBy(()->
                sortieService.detailSortie(sortie.getId(),curieux))
                .isInstanceOf(SortieNotFoundException.class);
    }

    /**
    Test vérifiant si un utilisateur (organisateur) créant une sortie puisse consulter ses sorties non publiée (brouillon)
     */

    @Test
    void testOrganisateurVoitSonBrouillon(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@gmail.com","orga"));
        Sortie sortie = sortieRepository.save(creerSortie("touring", organisateur, Statut.BROUILLON));

        em.flush();
        em.clear();                                      // organisateur détaché, comme le principal en prod

        assertThat(sortieService.detailSortie(sortie.getId(), organisateur).id()).isEqualTo(sortie.getId());
    }

    /**
    Test vérifiant qu'un visiter (utilisateur non authentifié) puisse consulter les détails d'une sortie publiée
     */

    @Test
    void testVisiteurVoitSortiePublierDetail(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@gmail.com","orga"));
        Sortie sortie = sortieRepository.save(creerSortie("touring", organisateur, Statut.PUBLIEE));

        assertThat(sortieService.detailSortie(sortie.getId(),null).id()).isEqualTo(sortie.getId());
    }

    /**
    Test vérifiant qu'un visiteur (utilisateur non authentifié) puisse pas consulter les détails d'une sortie non publiée
     */

    @Test
    void testVisiteurVoitPasSortieBrouillonDetail(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@gmail.com","orga"));
        Sortie sortie = sortieRepository.save(creerSortie("touring", organisateur, Statut.BROUILLON));

        assertThatThrownBy(()->
                sortieService.detailSortie(sortie.getId(),null))
                .isInstanceOf(SortieNotFoundException.class);
    }

    /**
     * Test vérifiant que l'organisateur peut supprimer sa sortie, participations comprises
     */

    @Test
    void testOrganisateurSupprimeSaSortie(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@gmail.com","orga"));
        Sortie sortie = sortieRepository.save(creerSortie("touring", organisateur, Statut.BROUILLON));
        participationService.inscrireOrganisateur(sortie, organisateur);

        em.flush();
        em.clear();

        sortieService.deleteSortie(sortie.getId(), organisateur);

        em.flush();
        em.clear();

        assertThat(sortieRepository.findById(sortie.getId())).isEmpty();
        assertThat(participationRepository.countBySortieIdAndStatut(sortie.getId(), StatutParticipation.INSCRIT)).isZero();
    }

    /**
     * Test vérifiant qu'une sortie publiée ne peut pas être supprimée (elle doit être annulée)
     */

    @Test
    void testSupprimerSortiePublieeRefuse(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@gmail.com","orga"));
        Sortie publiee = sortieRepository.save(creerSortie("touring", organisateur, Statut.PUBLIEE));

        em.flush();
        em.clear();

        assertThatThrownBy(()->
                sortieService.deleteSortie(publiee.getId(), organisateur))
                .isInstanceOf(SuppressionImpossibleException.class);
        assertThat(sortieRepository.findById(publiee.getId())).isPresent();
    }

    /**
     * Test vérifiant qu'un autre utilisateur ne peut pas supprimer une sortie qu'il n'a pas créée
     */

    @Test
    void testAutreUtilisateurNePeutPasSupprimerSortie(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@gmail.com","orga"));
        Utilisateur curieux = utilisateurRepository.save(creerUtilisateur("curieux@gmail.com","curieux"));
        Sortie sortie = sortieRepository.save(creerSortie("touring", organisateur, Statut.PUBLIEE));

        em.flush();
        em.clear();

        assertThatThrownBy(()->
                sortieService.deleteSortie(sortie.getId(), curieux))
                .isInstanceOf(MauvaisOrganisateurException.class);
        assertThat(sortieRepository.findById(sortie.getId())).isPresent();
    }

    /**
     * Test vérifiant que la suppression d'une sortie inexistante lève SortieNotFoundException
     */

    @Test
    void testSupprimerSortieInexistante(){
        Utilisateur organisateur = utilisateurRepository.save(creerUtilisateur("orga@gmail.com","orga"));

        assertThatThrownBy(()->
                sortieService.deleteSortie(-1L, organisateur))
                .isInstanceOf(SortieNotFoundException.class);
    }

    /**
     * Méthode permettant de créér une sortie
     * @param titre
     * @param utilisateur
     * @param statut
     * @return Sortie
     */

    private Sortie creerSortie(String titre, Utilisateur utilisateur, Statut statut){
        Sortie sortie = new Sortie();
        sortie.setTitre(titre);
        sortie.setDescription("null");
        sortie.setDateDepart(OffsetDateTime.now().plusDays(7));
        sortie.setNomLieuDepart("genève");
        sortie.setLieuDepart(GEO.createPoint(new Coordinate(6.1432,46.2044)));
        sortie.setDistanceKm(12);
        sortie.setDureeEstimeeMin(23);
        sortie.setNiveau(Niveau.TOURING);
        sortie.setPlacesMax(10);
        sortie.setStatut(statut);
        sortie.setOrganisateur(utilisateur);
        sortie.setCreeLe(OffsetDateTime.now());
        return sortie;
    }

    /**
     * Méthode permettant de créér un Utilisateur
     * @param mail
     * @param pseudo
     * @return Utilisateur
     */

    private Utilisateur creerUtilisateur(String mail,String pseudo){
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(mail);
        utilisateur.setMotDePasseHash("motdepasse123");
        utilisateur.setPseudo(pseudo);
        utilisateur.setRole(Role.USER);
        utilisateur.setNiveau(Niveau.TOURING);
        return utilisateur;
    }
}
