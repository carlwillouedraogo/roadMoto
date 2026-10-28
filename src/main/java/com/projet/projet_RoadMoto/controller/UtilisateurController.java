package com.projet.projet_RoadMoto.controller;

import com.projet.projet_RoadMoto.dto.UtilisateurResponse;
import com.projet.projet_RoadMoto.security.UtilisateurPrincipal;
import com.projet.projet_RoadMoto.services.UtilisateurService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping("/me")
    public UtilisateurResponse me(@AuthenticationPrincipal UtilisateurPrincipal principal){
        return utilisateurService.toResponse(principal.getEntite());
    }
}
