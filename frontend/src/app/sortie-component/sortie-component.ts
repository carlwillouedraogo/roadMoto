import { Component, inject, input} from '@angular/core';
import { SortieService } from '../sortie-service/sortie-service';
import { Sortie } from '../sortie-data/sortie-data';
import { MatCardModule } from '@angular/material/card';
import { MatToolbarModule } from '@angular/material/toolbar';
import { DatePipe } from '@angular/common';

@Component({
  imports: [MatCardModule, MatToolbarModule, DatePipe],
  selector: 'app-sortie-component',
  template: `
   <div class="sortie-component">
    <mat-card>

      <mat-card-header>
        <mat-card-title>{{ sortie().titre }}</mat-card-title>
        <mat-card-subtitle>{{ sortie().nomLieuDepart }} - {{ sortie().dateDepart | date:'dd/MM/yyyy' }}</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        @if(sortie().description) {
          <p>{{ sortie().description }}</p>
        }@else{
          <p>Aucune description disponible.</p>
        }
        <p>Niveau : {{ sortie().niveau }}</p>
        <p>Statut : {{ sortie().statut }}</p>
        <p>Places restantes : {{ sortie().placesRestantes }} / {{ sortie().placesMax }}</p>
        <p>Organisateur : {{ sortie().organisateur.pseudo }}</p>
      </mat-card-content>
    </mat-card>
   </div>
  `,
  styleUrl: './sortie-component.css',
})
export class SortieComponent {

  sortieService = inject(SortieService);

  sortie = input.required<Sortie>();
}
