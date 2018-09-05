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
  @ViewChild(CohortEditModalComponent)
  editModal: CohortEditModalComponent;

  constructor(
      private cohortsService: CohortsService,
      private workspacesService: WorkspacesService,
      private recentWork: RecentWorkComponent,
      private route: Router
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


  cloneNotebook(resource: RecentResource): void {
    this.workspacesService.cloneNotebook(this.wsNamespace, this.wsId, resource.notebook.name)
        .subscribe(() => {
          this.recentWork.updateList();
        });
  }

  deleteNotebook(resource: RecentResource): void {
    this.resource = resource.notebook;
    this.callbackFun = 'receiveNotebookDelete';
    this.notebookInFocus = resource.notebook;
    this.deleteModal.open();
  }

  receiveNotebookDelete($event: FileDetail): void {
    this.workspacesService.deleteNotebook(this.wsNamespace, this.wsId, $event.name)
        .subscribe(() => {
          this.recentWork.updateList();
          this.deleteModal.close();
        });
  }

  editCohort(resource: RecentResource): void {
    this.cohortInFocus = resource.cohort;

    // This ensures the cohort binding is picked up before the open resolves.
    setTimeout(_ => this.editModal.open(), 0);
  }

  cloneCohort(resource: RecentResource): void {
    const url =
        '/workspaces/' + this.wsNamespace + '/' + this.wsId + '/cohorts/build?criteria=';
    this.route.navigateByUrl(url
        + resource.cohort.criteria);
  }

  reviewCohort(resource: RecentResource): void {
    const url =
        '/workspaces/' + this.wsNamespace + '/' + this.wsId + '/cohorts/' + resource.cohort.id + '/review';
    this.route.navigateByUrl(url);
  }

  deleteCohort(resource: RecentResource): void {
    this.resource = resource.cohort;
this.callbackFun = 'receiveCohortDelete';
    this.cohortInFocus = resource.cohort;
    this.deleteModal.open();
  }

  receiveCohortDelete($event: Cohort): void {
    this.cohortsService.deleteCohort(this.wsNamespace, this.wsId, $event.id).subscribe(() => {
      this.recentWork.updateList();
      this.deleteModal.close();
    });
  }


  updateFinished(): void {
    // this.editModal.close();
    // this.reloadCohorts();
  }
}
