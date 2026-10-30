package com.projet.projet_RoadMoto.controller;

import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.dto.HistoriqueParticipationResponse;
import com.projet.projet_RoadMoto.dto.SortieResponse;
import com.projet.projet_RoadMoto.dto.UtilisateurResponse;
import com.projet.projet_RoadMoto.security.UtilisateurPrincipal;
import com.projet.projet_RoadMoto.services.ParticipationService;
import com.projet.projet_RoadMoto.services.SortieService;
import com.projet.projet_RoadMoto.services.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final SortieService sortieService;
    private final ParticipationService participationService;

    public UtilisateurController(UtilisateurService utilisateurService, SortieService sortieService, ParticipationService participationService) {
        this.utilisateurService = utilisateurService;
        this.sortieService = sortieService;
        this.participationService = participationService;
    }

    @GetMapping("/me")
    public UtilisateurResponse me(@AuthenticationPrincipal UtilisateurPrincipal principal){
        return utilisateurService.toResponse(principal.getEntite());
    }

    @Operation(
            summary = "Historique de l'organisateur",
            description = "Toutes les sorties créées par l'utilisateur, brouillons et annulées comprises, "
                    + "de la plus récente à la plus ancienne."
    )
    @GetMapping("/me/sorties-organisees")
    public Page<SortieResponse> sortiesOrganisees(@RequestParam(required = false) Statut statut,
                                                  @ParameterObject Pageable pageable,
                                                  @AuthenticationPrincipal UtilisateurPrincipal principal){
        return sortieService.historiqueOrganisateur(principal.getEntite(), statut, pageable);
    }

    @Operation(
            summary = "Historique du participant",
            description = "Les sorties d'autres organisateurs auxquelles l'utilisateur s'est inscrit, "
                    + "désistements compris, de la plus récente à la plus ancienne."
    )
    @GetMapping("/me/participations")
    public Page<HistoriqueParticipationResponse> participations(@RequestParam(required = false) StatutParticipation statut,
                                                                @ParameterObject Pageable pageable,
                                                                @AuthenticationPrincipal UtilisateurPrincipal principal){
        return participationService.historique(principal.getEntite(), statut, pageable);
    }
}
