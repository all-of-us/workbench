import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {
  Cohort, CohortsService, FileDetail, NotebookRename,
  RecentResource, WorkspacesService
} from '../../../generated';
import {ConfirmDeleteModalComponent} from '../confirm-delete-modal/component';
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


  @ViewChild(RenameModalComponent)
  renameModal: RenameModalComponent;

  @ViewChild(ConfirmDeleteModalComponent)
  deleteModal: ConfirmDeleteModalComponent;

  constructor(
      private cohortsService: CohortsService,
      private workspacesService: WorkspacesService,
      private route: ActivatedRoute,
  ) {
    this.actionList = [
      {
        type: 'notebook',
        class: 'pencil',
        link: 'renameThis',
        text: 'Rename'
      }, {
        type: 'notebook',
        class: 'copy',
        link: 'cloneThis',
        text: 'Clone'
      }, {
        type: 'notebook',
        class: 'trash',
        text: 'Delete',
        link: 'confirmDelete'
      }, {
        type: 'cohort',
        class: 'copy',
        text: 'Clone'
      }, {
        type: 'cohort',
        class: 'pencil',
        text: 'Edit'
      },  {
        type: 'cohort',
        class: 'grid-view',
        text: 'Review'
      }, {
        type: 'cohort',
        class: 'trash',
        text: 'Delete',
      }
    ];
  }

  renameThis(recentResource: RecentResource): void {
    console.log('in rename');
    this.notebookInFocus = this.card.notebook;
    this.renameModal.open();

  }
  receiveRename(rename: NotebookRename): void {
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
