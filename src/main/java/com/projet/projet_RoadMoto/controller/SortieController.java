package com.projet.projet_RoadMoto.controller;

import com.projet.projet_RoadMoto.dto.SortieRequest;
import com.projet.projet_RoadMoto.dto.SortieResponse;
import com.projet.projet_RoadMoto.services.SortieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sorties")
public class SortieController {
    private final SortieService sortieService;

    public SortieController(SortieService sortieService) {
        this.sortieService = sortieService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SortieResponse creer(@Valid @RequestBody SortieRequest request,
                                Authentication authentication) {
        return sortieService.createSortie(request, authentication.getName());
    }
}
