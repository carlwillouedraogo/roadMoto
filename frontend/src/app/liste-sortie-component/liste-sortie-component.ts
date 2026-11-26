import { Component, inject, OnInit } from '@angular/core';
import { SortieService } from '../sortie-service/sortie-service';
import { SortieComponent } from '../sortie-component/sortie-component';
import { Page } from '../sortie-data/page';

@Component({
  imports: [SortieComponent],
  template: `
    <div class="liste-sortie-component">
      <h1>Liste des sorties</h1>

      @if (sortieService.chargement()) {
        <p>Chargement des sorties...</p>
      } @else if (sortieService.erreur()) {
        <p class="erreur">{{ sortieService.erreur() }}</p>
      } @else {
        @for (sortie of sortieService.allSorties(); track sortie.id) {
          <app-sortie-component [sortie]="sortie" />
        } @empty {
          <p>Aucune sortie publiée pour le moment.</p>
        }
      }
</div>
  `,
  selector: 'app-liste-sortie-component',
  styleUrl: './liste-sortie-component.css',
})
export class ListeSortieComponent implements OnInit {
  protected sortieService = inject(SortieService);

  ngOnInit() {
    this.sortieService.chargerSorties();
  }
}
