import {Component, Input} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {CohortsService} from '../../../generated';
import {WorkspaceData} from '../../resolvers/workspace';

@Component ({
  selector : 'app-card',
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/template.css',
    './component.css'],
  templateUrl: './component.html'
})

export class CardComponent implements OnInit {
  actionList = [];
  type: string;
  @Input('header')
  header: string;

  constructor(
      private cohortsService: CohortsService,
      private workspacesService: WorkspacesService,
      private route: ActivatedRoute,
  ) {
   this.actionList = [{
     notebook: {
       class: 'copy',
       link: 'query'}
   }];
   this.type = 'notebook';

  }
  ngOnInit() {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.type = this.card && this.card.notebook == null ? 'cohort' : 'notebook';
    this.actions = this.actionList.filter(elem =>  elem.type === this.type);

  }

  confirmDelete(notebook: FileDetail): void {
    this.notebookInFocus = notebook;
    this.deleteModal.open();
  }

  receiveNotebookDelete($event: FileDetail): void {
    this.workspacesService.deleteNotebook(this.wsNamespace, this.wsId, $event.name)
        .subscribe(() => {
         // this.notebooksLoading = true;
         // this.loadNotebookList();
          this.deleteModal.close();
        });
  }

  cloneThis(notebook: string): void {
    this.workspacesService.cloneNotebook(this.wsNamespace, this.wsId, notebook)
        .subscribe(() => {
         // this.loadNotebookList();
        });
  }

  receiveDelete($event): void {
    this.deleteCohort($event);
  }

  public deleteCohort(cohort: Cohort): void {
    /*this.cohortsService.deleteCohort(this.wsNamespace, this.wsId, cohort.id).subscribe(() => {
      this.cohortList.splice(
          this.cohortList.indexOf(cohort), 1);
      this.deleteModal.close();
    });*/
  }

  updateFinished(): void {
    // this.editModal.close();
    // this.reloadCohorts();
  }
}
