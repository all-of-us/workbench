import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {
  Cohort, CohortsService, FileDetail, NotebookRename,
  RecentResource, WorkspacesService
} from '../../../generated';
import {CohortEditModalComponent} from '../cohort-edit-modal/component';
import {ConfirmDeleteModalComponent} from '../confirm-delete-modal/component';
import {RecentWorkComponent} from '../recent-work/component';
import {RenameModalComponent} from '../rename-modal/component';

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
  @Input('card')
  card: RecentResource;
  actions = [];
  notebookInFocus: FileDetail;
  notebookRenameError = false;
  wsNamespace: string;
  wsId: string;
  cohortInFocus: Cohort;
  resource: any;
  callbackFun: any;
  router: Router;

  @ViewChild(RenameModalComponent)
  renameModal: RenameModalComponent;

  @ViewChild(ConfirmDeleteModalComponent)
  deleteModal: ConfirmDeleteModalComponent;

  @ViewChild(CohortEditModalComponent)
  editModal: CohortEditModalComponent;

  constructor(
      private cohortsService: CohortsService,
      private workspacesService: WorkspacesService,
      private recentWork: RecentWorkComponent,
      private route: Router
  ) {
    this.actionList = [
      {
        type: 'notebook',
        class: 'pencil',
        link: 'renameNotebook',
        text: 'Rename'
      }, {
        type: 'notebook',
        class: 'copy',
        link: 'cloneNotebook',
        text: 'Clone'
      }, {
        type: 'notebook',
        class: 'trash',
        text: 'Delete',
        link: 'deleteNotebook'
      }, {
        type: 'cohort',
        class: 'copy',
        text: 'Clone',
        link: 'cloneCohort'
      }, {
        type: 'cohort',
        class: 'pencil',
        text: 'Edit',
        link: 'editCohort'
      },  {
        type: 'cohort',
        class: 'grid-view',
        text: 'Review',
        link: 'reviewCohort'
      }, {
        type: 'cohort',
        class: 'trash',
        text: 'Delete',
        link: 'deleteCohort'
      }
    ];
  }

  ngOnInit() {
    this.wsNamespace = this.card.workspaceNamespace;
    this.wsId = this.card.workspaceFirecloudName;
    this.type = this.card && this.card.notebook == null ? 'cohort' : 'notebook';
    this.actions = this.actionList.filter(elem =>  elem.type === this.type);

  }

  renameNotebook(recentResource: RecentResource): void {
    this.notebookInFocus = this.card.notebook;
    this.renameModal.open();
  }

  receiveNotebookRename(rename: NotebookRename): void {
    let newName = rename.newName;
    if (!(new RegExp('^.+\.ipynb$').test(newName))) {
      newName = rename.newName + '.ipynb';
      rename.newName = newName;
    }
    if (new RegExp('.*\/.*').test(newName)) {
      this.renameModal.close();
      this.notebookRenameError = true;
      return;
    }
    this.workspacesService
        .renameNotebook(this.wsNamespace, this.wsId, rename)
        .subscribe((newNb) => {
          this.recentWork.updateList();
          this.renameModal.close();
    });
  }

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
        '/workspaces/' + this.wsNamespace
        + '/' + this.wsId + '/cohorts/' + resource.cohort.id + '/review';
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
