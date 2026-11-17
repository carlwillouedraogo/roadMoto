package com.projet.projet_RoadMoto.dto.orsdto;

// Double et non double : ORS omet distance et duration quand elles valent 0
public record OrsSegment(Double distance, Double duration) {
}
