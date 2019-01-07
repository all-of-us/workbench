import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Router} from '@angular/router';

import {resourceActionList, ResourceType} from 'app/utils/resourceActions';

import {
  CohortsService, ConceptSetsService,
  NotebookRename, RecentResource,
  WorkspacesService
} from 'generated';

import {SignInService} from 'app/services/sign-in.service';
import {environment} from 'environments/environment';

import {EditModalComponent} from 'app/views/edit-modal/component';
import {RenameModalComponent} from 'app/views/rename-modal/component';

@Component ({
  selector : 'app-resource-card',
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/template.css',
    './component.css'],
  templateUrl: './component.html'
})
export class ResourceCardComponent implements OnInit {
  resourceType: ResourceType;
  @Input('resourceCard')
  resourceCard: RecentResource;
  @Input('cssClass')
  cssClass: string;
  @Output() onUpdate: EventEmitter<void | NotebookRename> = new EventEmitter();
  @Output() duplicateNameError: EventEmitter<string> = new EventEmitter();
  @Output() invalidNameError: EventEmitter<string> = new EventEmitter();
  actions = [];
  wsNamespace: string;
  wsId: string;
  resource: any;
  router: Router;
  actionList = resourceActionList;
  invalidResourceError = false;
  // deleting = false;
  confirmDeleting = false;

  @ViewChild(RenameModalComponent)
  renameModal: RenameModalComponent;

  @ViewChild(EditModalComponent)
  editModal: EditModalComponent;

  constructor(
      private cohortsService: CohortsService,
      private workspacesService: WorkspacesService,
      private conceptSetsService: ConceptSetsService,
      private signInService: SignInService,
      private route: Router,
  ) {}

  ngOnInit() {
    this.wsNamespace = this.resourceCard.workspaceNamespace;
    // RW-1298 this should be updated
    this.wsId = this.resourceCard.workspaceFirecloudName;
    if (this.resourceCard) {
      if (this.resourceCard.notebook) {
        this.resourceType = ResourceType.NOTEBOOK;
      } else if (this.resourceCard.cohort) {
        this.resourceType = ResourceType.COHORT;
      } else if (this.resourceCard.conceptSet) {
        this.resourceType = ResourceType.CONCEPT_SET;
      } else {
        this.resourceType = ResourceType.INVALID;
        this.invalidResourceError = true;
      }
    }
    this.actions = this.actionList.filter(elem =>  elem.type === this.resourceType);
  }

  renameNotebook(): void {
    this.renameModal.open();
  }

  toggleConfirmDelete(): void {
    this.confirmDeleting = !this.confirmDeleting;
  }

  receiveNotebookRename(rename: NotebookRename): void {
    let newName = rename.newName;
    if (!(new RegExp('^.+\.ipynb$').test(newName))) {
      newName = rename.newName + '.ipynb';
      rename.newName = newName;
    }
    if (new RegExp('.*\/.*').test(newName)) {
      this.renameModal.close();
      this.invalidNameError.emit(newName);
      return;
    }
    this.workspacesService.getNoteBookList(this.wsNamespace, this.wsId)
      .switchMap((fileList) => {
        if (fileList.filter((nb) => nb.name === newName).length > 0) {
          throw new Error(newName);
        } else {
          return this.workspacesService.renameNotebook(this.wsNamespace, this.wsId, rename);
        }
      })
      .subscribe(() => {
          this.renameModal.close();
          this.onUpdate.emit(rename);
        },
        (dupName) => {
          this.duplicateNameError.emit(dupName);
          this.renameModal.close();
    });
  }

  receiveRename(): void {
    this.onUpdate.emit();
  }

  cloneResource(resource: RecentResource): void {
    switch (this.resourceType) {
      case ResourceType.NOTEBOOK: {
        this.workspacesService.cloneNotebook(this.wsNamespace, this.wsId, resource.notebook.name)
          .map(() => this.onUpdate.emit())
          .subscribe(() => {});
        break;
      }
      case ResourceType.COHORT: {
        const url =
          '/workspaces/' + this.wsNamespace + '/' + this.wsId + '/cohorts/build?cohortId=';
        this.route.navigateByUrl(url + resource.cohort.id);
        this.onUpdate.emit();
        break;
      }
    }
  }

  deleteResource(resource: RecentResource): void {
    switch (this.resourceType) {
      case ResourceType.NOTEBOOK: {
        this.resource = resource.notebook;
        break;
      }
      case ResourceType.COHORT: {
        this.resource = resource.cohort;
        break;
      }
      case ResourceType.CONCEPT_SET: {
        this.resource = resource.conceptSet;
        break;
      }
    }
    console.log(this.resource);
    this.toggleConfirmDelete();
  }

  receiveDelete($event): void {
    switch (this.resourceType) {
      case ResourceType.NOTEBOOK: {
        this.workspacesService.deleteNotebook(this.wsNamespace, this.wsId, $event.name)
          .subscribe(() => {
            this.onUpdate.emit();
            this.toggleConfirmDelete();
          });
        break;
      }
      case ResourceType.COHORT: {
        this.cohortsService.deleteCohort(this.wsNamespace, this.wsId, $event.id)
          .subscribe(() => {
            this.onUpdate.emit();
            this.toggleConfirmDelete();
          });
        break;
      }
      case ResourceType.CONCEPT_SET: {
        this.conceptSetsService.deleteConceptSet(this.wsNamespace, this.wsId, $event.id)
          .subscribe(() => {
            this.onUpdate.emit();
            this.toggleConfirmDelete();
          });
      }
    }
  }

  openResource(resource: RecentResource): void {
    switch (this.resourceType) {
      case ResourceType.COHORT: {
        this.reviewCohort(resource);
        break;
      }
      case ResourceType.CONCEPT_SET: {
        this.route.navigate(['workspaces', this.wsNamespace, this.wsId, 'concepts',
        'sets', resource.conceptSet.id], {relativeTo: null});
        break;
      }
      case ResourceType.NOTEBOOK: {
        let queryParams = null;
        if (this.notebookReadOnly) {
          queryParams = { playgroundMode: true };
        }
        this.route.navigate(
          ['workspaces', this.wsNamespace, this.wsId, 'notebooks',
           encodeURIComponent(this.resourceCard.notebook.name)], {
             queryParams,
             relativeTo: null,
           });
      }
    }
  }

  editCohort(): void {
    // This ensures the cohort binding is picked up before the open resolves.
    setTimeout(_ => this.editModal.open(), 0);
  }

  editConceptSet(): void {
    this.editModal.open();
  }

  reviewCohort(resource: RecentResource): void {
    const url =
        '/workspaces/' + this.wsNamespace
        + '/' + this.wsId + '/cohorts/' + resource.cohort.id + '/review';
    this.route.navigateByUrl(url);
  }

  get resourceTypeInvalidError(): boolean {
    return this.invalidResourceError;
  }

  get actionsDisabled(): boolean {
    return !this.writePermission;
  }

  get writePermission(): boolean {
    return this.resourceCard.permission === 'OWNER'
      || this.resourceCard.permission === 'WRITER';
  }

  get notebookReadOnly(): boolean {
    return this.resourceType === ResourceType.NOTEBOOK
      && this.resourceCard.permission === 'READER';
  }

  get notebookDisplayName(): string {
    if (this.resourceType === ResourceType.NOTEBOOK) {
      return this.resourceCard.notebook.name.replace(/\.ipynb$/, '');
    }
  }

}
