package com.projet.projet_RoadMoto.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Tâche planifiée passant en TERMINEE les sorties dont la balade est finie
 */
@Component
public class TerminaisonSortieTache {
    private final SortieService sortieService;

    public TerminaisonSortieTache(SortieService sortieService) {
        this.sortieService = sortieService;
    }

    // toutes les 15 minutes
    @Scheduled(cron = "0 */15 * * * *")
    public void terminerSortiesPassees() {
        sortieService.terminerSortiesPassees(OffsetDateTime.now());
    }
}
