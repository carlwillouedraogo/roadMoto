package com.projet.projet_RoadMoto.controller;

import com.projet.projet_RoadMoto.dto.ParticipationResponse;
import com.projet.projet_RoadMoto.dto.SortieRequest;
import com.projet.projet_RoadMoto.dto.SortieResponse;
import com.projet.projet_RoadMoto.dto.SortieResume;
import com.projet.projet_RoadMoto.services.ParticipationService;
import com.projet.projet_RoadMoto.services.SortieService;
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
