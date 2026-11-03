package com.projet.projet_RoadMoto.controller;

import com.projet.projet_RoadMoto.services.OverpassImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("import-poi")
public class ImportPoiRunner implements CommandLineRunner {
    private final OverpassImportService service;

    public ImportPoiRunner(OverpassImportService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {
        service.importerColsValais();
    }
}
