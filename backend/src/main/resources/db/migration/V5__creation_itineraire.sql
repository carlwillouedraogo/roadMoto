CREATE TABLE itineraire (
    sortie_id   BIGINT                     PRIMARY KEY,
    trace       GEOGRAPHY(LineString,4326) NOT NULL,
    distance_m  INT                        NOT NULL,
    duree_s     INT                        NOT NULL,
    source      VARCHAR(20)                NOT NULL,
    calcule_le  TIMESTAMPTZ                NOT NULL,

    CONSTRAINT fk_itineraire_sortie
        FOREIGN KEY (sortie_id) REFERENCES sortie (id) ON DELETE CASCADE,
    CONSTRAINT ck_itineraire_distance_positive CHECK (distance_m > 0),
    CONSTRAINT ck_itineraire_duree_positive    CHECK (duree_s > 0)
);

CREATE INDEX idx_itineraire_trace ON itineraire USING GIST (trace);

CREATE TABLE etape (
   id                           BIGSERIAL             PRIMARY KEY,
   itineraire_id                BIGINT                NOT NULL,
   ordre                        INT                   NOT NULL,
   position                     GEOGRAPHY(Point,4326) NOT NULL,
   poi_id                       BIGINT,
   type_etape                   VARCHAR(20)           NOT NULL,
   distance_depuis_precedente_m INT,
   duree_depuis_precedente_s    INT,
   duree_arret_min              INT,

   CONSTRAINT fk_etape_itineraire
       FOREIGN KEY (itineraire_id) REFERENCES itineraire (sortie_id) ON DELETE CASCADE,
   CONSTRAINT fk_etape_poi
       FOREIGN KEY (poi_id) REFERENCES poi (id) ON DELETE SET NULL,

   CONSTRAINT uk_etape_ordre UNIQUE (itineraire_id, ordre),
   CONSTRAINT ck_etape_ordre_positif CHECK (ordre >= 0),
   CONSTRAINT ck_etape_arret_positif CHECK (duree_arret_min IS NULL OR duree_arret_min >= 0)
);

CREATE INDEX idx_etape_position ON etape USING GIST (position);
CREATE INDEX idx_etape_poi      ON etape (poi_id);
