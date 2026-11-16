package com.projet.projet_RoadMoto.controller;

import com.projet.projet_RoadMoto.dto.AvertissementResponse;
import com.projet.projet_RoadMoto.security.UtilisateurPrincipal;
import com.projet.projet_RoadMoto.services.FermetureSaisonniereService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sorties/{sortieId}/avertissements")
@Tag(name = "Fermetures saisonnières")
public class FermetureSaisonniereController {

    private final FermetureSaisonniereService fermetureSaisonniereService;

    public FermetureSaisonniereController(FermetureSaisonniereService fermetureSaisonniereService) {
        this.fermetureSaisonniereService = fermetureSaisonniereService;
    }

    @Operation(
            summary = "Cols fermés sur l'itinéraire d'une sortie",
            description = "Liste les cols traversés par l'itinéraire qui sont généralement fermés au mois de départ. "
                    + "Le paramètre date permet de tester une autre date que la date de départ prévue. "
                    + "Accessible sans connexion ; un brouillon n'est visible que par son organisateur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avertissements (liste vide si aucun col fermé)"),
            @ApiResponse(responseCode = "400", description = "Date invalide (format attendu : AAAA-MM-JJ)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sortie introuvable, sans itinéraire, ou brouillon d'un autre organisateur", content = @Content)
    })
    @GetMapping
    public List<AvertissementResponse> avertissements(
            @PathVariable Long sortieId,
            @Parameter(description = "Date à tester (AAAA-MM-JJ), par défaut la date de départ de la sortie")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return fermetureSaisonniereService.avertissementsPourSortie(
                sortieId, date, principal != null ? principal.getEntite() : null);
    }
}
