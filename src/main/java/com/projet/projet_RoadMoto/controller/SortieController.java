package com.projet.projet_RoadMoto.controller;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.dto.ParticipationResponse;
import com.projet.projet_RoadMoto.dto.SortieRequest;
import com.projet.projet_RoadMoto.dto.SortieResponse;
import com.projet.projet_RoadMoto.security.UtilisateurPrincipal;
import com.projet.projet_RoadMoto.services.ParticipationService;
import com.projet.projet_RoadMoto.services.SortieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

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
                                @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return sortieService.createSortie(request, principal.getEntite());
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
    public ParticipationResponse sInscrire(@PathVariable Long id, @AuthenticationPrincipal UtilisateurPrincipal principal){
        return participationService.sInscrire(id, principal.getEntite());
    }

    @PostMapping("/{id}/publier")
    public SortieResponse publier(@PathVariable Long id,
                                  @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return sortieService.publierSortie(id, principal.getUsername());
    }

    @PostMapping("/{id}/annuler")
    public SortieResponse annuler(@PathVariable Long id,
                                  @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return sortieService.annulerSortie(id, principal.getUsername());
    }
    @GetMapping
    public Page<SortieResponse> lister(@RequestParam(required = false) Niveau niveau,
                                       @RequestParam(required = false) OffsetDateTime dateMax,
                                       @RequestParam(required = false) Integer placesMin,
                                       @ParameterObject Pageable pageable){
        return sortieService.listeSorties(niveau,dateMax,placesMin,pageable);
    }

    @GetMapping("/{id}")
    public SortieResponse detailSortie(@PathVariable Long id,
                                       @AuthenticationPrincipal UtilisateurPrincipal principal){
        return sortieService.detailSortie(id, principal != null ? principal.getEntite() : null);
    }
    @DeleteMapping("/{id}/participation")
    public ParticipationResponse desincrire(@PathVariable Long id, @AuthenticationPrincipal UtilisateurPrincipal principal){
        return participationService.desInscription(id,principal.getEntite());
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id, @AuthenticationPrincipal UtilisateurPrincipal principal){
        sortieService.deleteSortie(id, principal.getEntite());
    }

}
