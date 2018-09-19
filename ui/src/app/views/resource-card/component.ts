import {Component, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {Router} from '@angular/router';

import {resourceActionList} from 'app/utils/resourceActions';

import {
  CohortsService, NotebookRename,
  RecentResource, WorkspacesService
} from 'generated';

import {SignInService} from 'app/services/sign-in.service';
import {environment} from 'environments/environment';

import {CohortEditModalComponent} from 'app/views/cohort-edit-modal/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {RenameModalComponent} from 'app/views/rename-modal/component';
import {Observable} from 'rxjs/Observable';

enum ResourceType {
  NOTEBOOK = 'notebook',
  COHORT = 'cohort',
  INVALID = 'invalid'
}

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
  @Output() onUpdate: EventEmitter<void> = new EventEmitter();
  actions = [];
  notebookRenameError = false;
  wsNamespace: string;
  wsId: string;
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
      this.notebookRenameError = true;
      return;
    }
    this.workspacesService
        .renameNotebook(this.wsNamespace, this.wsId, rename)
        .subscribe(() => {
          this.onUpdate.emit();
          this.renameModal.close();
    });
  }

  cloneResource(resource: RecentResource): void {
    switch (this.resourceType) {
      case ResourceType.NOTEBOOK: {
        this.workspacesService.cloneNotebook(this.wsNamespace, this.wsId, resource.notebook.name)
          .subscribe(() => Observable.empty());
        break;
      }
      case ResourceType.COHORT: {
        const url =
          '/workspaces/' + this.wsNamespace + '/' + this.wsId + '/cohorts/build?criteria=';
        this.route.navigateByUrl(url
          + resource.cohort.criteria);
        break;
      }
    }
    this.onUpdate.emit();
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
          .subscribe(() => Observable.empty());
        break;
      }
      case ResourceType.COHORT: {
        this.cohortsService.deleteCohort(this.wsNamespace, this.wsId, $event.id)
          .subscribe(() => Observable.empty());
      }
    }
    this.onUpdate.emit();
    this.deleteModal.close();
  }

  openResource(resource: RecentResource): void {
    switch (this.resourceType) {
      case ResourceType.COHORT: {
        this.reviewCohort(resource);
        break;
      }
      case ResourceType.NOTEBOOK: {
        const nbUrl = '/workspaces/${this.wsNamespace}/${this.wsId}/notebooks/'
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

  editCohort(resource: RecentResource): void {
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

}
