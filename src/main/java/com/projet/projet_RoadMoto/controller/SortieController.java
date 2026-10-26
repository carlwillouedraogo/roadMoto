package com.projet.projet_RoadMoto.controller;

import com.projet.projet_RoadMoto.dto.ParticipationResponse;
import com.projet.projet_RoadMoto.dto.SortieRequest;
import com.projet.projet_RoadMoto.dto.SortieResponse;
import com.projet.projet_RoadMoto.services.ParticipationService;
import com.projet.projet_RoadMoto.services.SortieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sorties")
public class SortieController {
    private final SortieService sortieService;
    private final ParticipationService participationService;

    public SortieController(SortieService sortieService, ParticipationService participationService) {
        this.sortieService = sortieService;
        this.participationService = participationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SortieResponse creer(@Valid @RequestBody SortieRequest request,
                                Authentication authentication) {
        return sortieService.createSortie(request, authentication.getName());
    }
    @Operation(
            summary = "S'inscrire à une sortie",
            description = "L'utilisateur authentifié s'inscrit à une sortie publiée. "
                    + "La dernière place prise fait passer la sortie en COMPLETE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscription enregistrée"),
            @ApiResponse(responseCode = "404", description = "Sortie introuvable", content = @Content),
            @ApiResponse(responseCode = "409", description = "Sortie non publiée, déjà partie, complète, ou utilisateur déjà inscrit", content = @Content)
    })
    @PostMapping("/{id}/participation")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationResponse sInscrire(@PathVariable Long id, Authentication authentication){
        return participationService.sInscrire(id, authentication.getName());
    }

    @PostMapping("/{id}/publiee")
    @ResponseStatus(HttpStatus.CREATED)
    public SortieResponse publier(@PathVariable Long id,
                                Authentication authentication) {
        return sortieService.publierSortie(id, authentication.getName());
    }

}
