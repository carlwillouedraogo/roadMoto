package com.projet.projet_RoadMoto.demo;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.dto.InscriptionRequest;
import com.projet.projet_RoadMoto.dto.SortieRequest;
import com.projet.projet_RoadMoto.dto.SortieResponse;
import com.projet.projet_RoadMoto.dto.UtilisateurResponse;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import com.projet.projet_RoadMoto.services.ParticipationService;
import com.projet.projet_RoadMoto.services.SortieService;
import com.projet.projet_RoadMoto.services.UtilisateurService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@Profile("demo")
public class DonneesDemo implements CommandLineRunner {


    private final UtilisateurRepository utilisateurs;
    private final SortieService sortieService;
    private final ParticipationService participationService;
    private final UtilisateurService utilisateurService;
    private final UtilisateurRepository utilisateurRepository;

    public DonneesDemo(UtilisateurRepository utilisateurs, SortieService sortieService, ParticipationService participationService, UtilisateurService utilisateurService, UtilisateurRepository utilisateurRepository) {
        this.utilisateurs = utilisateurs;
        this.sortieService = sortieService;
        this.participationService = participationService;
        this.utilisateurService = utilisateurService;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (utilisateurs.count() > 0) return;   // idempotent

       Utilisateur marc = creerCompte("marc@demo.ch", "marc", "Yamaha Tracer 9", Niveau.TOURING);
        Utilisateur lea = creerCompte("lea@demo.ch",  "lea",  "BMW R 1250 GS",   Niveau.SPORTIF);
        Utilisateur jon = creerCompte("jon@demo.ch",  "jon",  "Honda CB500X",    Niveau.DECOUVERTE);


        SortieResponse boucle = sortieService.createSortie(new SortieRequest(
                        "Boucle des quatre cols",
                        "Furka, Grimsel, Susten et Nufenen depuis Andermatt.",
                        OffsetDateTime.now().plusDays(20),
                        "Andermatt", 46.6355, 8.5939, Niveau.TOURING, 12),
                marc);
        sortieService.publierSortie(boucle.id(), marc.getEmail());

        SortieResponse matinale = sortieService.createSortie(new SortieRequest(
                        "Grand-Saint-Bernard au lever du jour",
                        "Départ matinal de Martigny, petit-déjeuner au col.",
                        OffsetDateTime.now().plusDays(9),
                        "Martigny", 46.1028, 7.0719, Niveau.SPORTIF, 2),
                lea);
        sortieService.publierSortie(matinale.id(), "lea@demo.ch");
        participationService.sInscrire(matinale.id(), jon);   // → COMPLETE

        sortieService.createSortie(new SortieRequest(
                        "Tour du Jura — à confirmer", "Itinéraire en cours de préparation.",
                        OffsetDateTime.now().plusDays(30),
                        "Lausanne", 46.5197, 6.6323, Niveau.DECOUVERTE, 6),
                jon);   // reste en BROUILLON
    }


    private Utilisateur creerCompte(String email, String pseudo, String moto, Niveau niveau) {
        utilisateurService.inscrire(new InscriptionRequest(email, "demo1234", pseudo, moto, niveau));
        return utilisateurRepository.findByEmail(email).orElseThrow();
    }
}
