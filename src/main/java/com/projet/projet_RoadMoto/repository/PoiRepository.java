package com.projet.projet_RoadMoto.repository;

import com.projet.projet_RoadMoto.entity.Poi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PoiRepository extends JpaRepository<Poi,Long> {
    @Query(value = """
    select * from poi
    where ST_DWithin(localisation, ST_MakePoint(:lng, :lat)::geography, :rayonMetres)
      and (cast(:type as varchar) is null or type = cast(:type as varchar))
    order by ST_Distance(localisation, ST_MakePoint(:lng, :lat)::geography)
    """, nativeQuery = true)
    List<Poi> aProximite(@Param("lng") double lng,
                         @Param("lat") double lat,
                         @Param("rayonMetres") int rayonMetres,
                         @Param("type") String type);

    Optional<Poi> findByOsmId(Long osmId);
}
