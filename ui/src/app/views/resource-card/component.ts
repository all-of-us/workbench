import {Component, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {Router} from '@angular/router';

import {resourceActionList, ResourceType} from 'app/utils/resourceActions';

import {
  CohortsService, NotebookRename,
  RecentResource, WorkspacesService
} from 'generated';

import {SignInService} from 'app/services/sign-in.service';
import {environment} from 'environments/environment';

import {CohortEditModalComponent} from 'app/views/cohort-edit-modal/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {RenameModalComponent} from 'app/views/rename-modal/component';

@Component ({
  selector : 'app-resource-card',
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/template.css',
    './component.css'],
  templateUrl: './component.html'
})

export class ResourceCardComponent implements OnInit, OnDestroy {
  resourceType: ResourceType;
  @Input('resourceCard')
  resourceCard: RecentResource;
  @Input('incomingResource')
  incomingResource: any;
  @Input('forList')
  forList: string;
  @Output() onUpdate: EventEmitter<void | NotebookRename> = new EventEmitter();
  @Output() duplicateNameError: EventEmitter<void | string> = new EventEmitter();
  @Output() invalidNameError: EventEmitter<void | string> = new EventEmitter();
  actions = [];
  wsNamespace: string;
  wsId: string;
  resource: any;
  router: Router;
  actionList = resourceActionList;
  invalidResourceError = false;
  notebookAuthListeners: EventListenerOrEventListenerObject[] = [];

  @ViewChild(RenameModalComponent)
  renameModal: RenameModalComponent;

  @ViewChild(ConfirmDeleteModalComponent)
  deleteModal: ConfirmDeleteModalComponent;

  @ViewChild(CohortEditModalComponent)
  editModal: CohortEditModalComponent;

  constructor(
      private cohortsService: CohortsService,
      private workspacesService: WorkspacesService,
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

  receiveCohortRename(): void {
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
          '/workspaces/' + this.wsNamespace + '/' + this.wsId + '/cohorts/build?criteria=';
        this.route.navigateByUrl(url + resource.cohort.criteria);
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
    }
    this.deleteModal.open();
  }

  receiveDelete($event): void {
    switch (this.resourceType) {
      case ResourceType.NOTEBOOK: {
        this.workspacesService.deleteNotebook(this.wsNamespace, this.wsId, $event.name)
          .subscribe(() => this.onUpdate.emit());
        break;
      }
      case ResourceType.COHORT: {
        this.cohortsService.deleteCohort(this.wsNamespace, this.wsId, $event.id)
          .subscribe(() => this.onUpdate.emit());
      }
    }
    this.deleteModal.close();
  }

  openResource(resource: RecentResource): void {
    switch (this.resourceType) {
      case ResourceType.COHORT: {
        this.reviewCohort(resource);
        break;
      }
      case ResourceType.NOTEBOOK: {
        const nbUrl = '/workspaces/' + this.wsNamespace + '/' + this.wsId + '/notebooks/'
          + encodeURIComponent(this.resourceCard.notebook.name);
        const notebook = window.open(nbUrl, '_blank');

        // TODO(RW-474): Remove the authHandler integration. This is messy,
        // non-standard, and currently will break in the following situation:
        // - User opens a new notebook tab.
        // - While that tab is loading, user immediately navigates away from this
        //   page.
        // This is not easily fixed without leaking listeners outside the lifespan
        // of the workspace component.
        const authHandler = (e: MessageEvent) => {
          if (e.source !== notebook) {
            return;
          }
          if (e.origin !== environment.leoApiUrl) {
            return;
          }
          if (e.data.type !== 'bootstrap-auth.request') {
            return;
          }
          notebook.postMessage({
            'type': 'bootstrap-auth.response',
            'body': {
              'googleClientId': this.signInService.clientId
            }
          }, environment.leoApiUrl);
        };
        window.addEventListener('message', authHandler);
        this.notebookAuthListeners.push(authHandler);
      }
    }
  }

  editCohort(): void {
    // This ensures the cohort binding is picked up before the open resolves.
    setTimeout(_ => this.editModal.open(), 0);
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

  ngOnDestroy(): void {
    this.notebookAuthListeners.forEach(f => window.removeEventListener('message', f));
  }

  get actionsDisabled(): boolean {
    return !this.writePermission;
  }

  get writePermission(): boolean {
    return this.resourceCard.permission === 'OWNER'
      || this.resourceCard.permission === 'WRITER';
  }

}
