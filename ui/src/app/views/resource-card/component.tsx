import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Router} from '@angular/router';

import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';

import {SignInService} from 'app/services/sign-in.service';
import {ReactWrapperBase, switchCase} from 'app/utils/index';
import {ResourceType} from 'app/utils/resourceActions';

import {
  CohortsService,
  ConceptSetsService,
  NotebookRename,
  RecentResource,
  WorkspacesService
} from 'generated';

import * as React from 'react';

const MenuItem = ({icon, children, ...props}) => {
  return <Clickable
    {...props}
    data-test-id={icon}
    style={{
      display: 'flex', alignItems: 'center',
      minWidth: '5rem', height: '1.3333rem',
      padding: '0 1rem', color: '#4A4A4A'
    }}
    hover={{backgroundColor: '#E0EAF1'}}
  ><ClrIcon shape={icon}/>&nbsp;{children}</Clickable>;
};

const ResourceCardMenu: React.FunctionComponent<{
  disabled: boolean, resourceType: ResourceType, onRenameNotebook: Function,
  onCloneResource: Function, onDeleteResource: Function, onEditCohort: Function,
  onReviewCohort: Function, onEditConceptSet: Function
}> = ({
        disabled, resourceType, onRenameNotebook, onCloneResource,
        onDeleteResource, onEditCohort, onReviewCohort, onEditConceptSet
      }) => {
  return <PopupTrigger
    side='bottom'
    closeOnClick
    content={
      switchCase(resourceType,
        ['notebook', () => {
          return <React.Fragment>
            <MenuItem icon='pencil' onClick={onRenameNotebook}>Rename</MenuItem>
            <MenuItem icon='copy' onClick={onCloneResource}>Clone</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource}>Delete</MenuItem>
          </React.Fragment>;
        }],
        ['cohort', () => {
          return <React.Fragment>
            <MenuItem icon='copy' onClick={onCloneResource}>Clone</MenuItem>
            <MenuItem icon='pencil' onClick={onEditCohort}>Edit</MenuItem>
            <MenuItem icon='grid-view' onClick={onReviewCohort}>Review</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource}>Delete</MenuItem>
          </React.Fragment>;
        }],
        ['conceptSet', () => {
          return <React.Fragment>
            <MenuItem icon='pencil' onClick={onEditConceptSet}>Edit</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource}>Delete</MenuItem>
          </React.Fragment>;
        }]
      )
    }
  >
    <Clickable disabled={disabled} data-test-id='resource-menu'>
      <ClrIcon shape='ellipsis-vertical' size={21} style={{color: '#2691D0', marginLeft: -9}}/>
    </Clickable>
  </PopupTrigger>;
};

@Component({
  selector: 'app-resource-card-menu',
  template: '<div #root></div>'
})
export class ResourceCardMenuComponent extends ReactWrapperBase {
  @Input() disabled;
  @Input() resourceType;
  @Input() onRenameNotebook;
  @Input() onCloneResource;
  @Input() onDeleteResource;
  @Input() onEditCohort;
  @Input() onReviewCohort;
  @Input() onEditConceptSet;

  constructor() {
    super(ResourceCardMenu, [
      'disabled', 'resourceType', 'onRenameNotebook', 'onCloneResource',
      'onDeleteResource', 'onEditCohort', 'onReviewCohort', 'onEditConceptSet'
    ]);
  }
}

@Component({
  selector: 'app-resource-card',
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
  wsNamespace: string;
  wsId: string;
  resource: any;
  router: Router;
  invalidResourceError = false;
  confirmDeleting = false;

  renaming = false;
  editing = false;

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
  }

  renameNotebook(): void {
    this.renaming = true;
  }

  cancelRename(): void {
    this.renaming = false;
  }

  openConfirmDelete(): void {
    this.confirmDeleting = true;
  }

  closeConfirmDelete(): void {
    this.confirmDeleting = false;
  }

  receiveNotebookRename(): void {
    this.renaming = false;
    this.onUpdate.emit();
  }

  receiveEdit(resource: RecentResource): void {
    if (resource.cohort) {
      this.cohortsService.updateCohort(
        this.wsNamespace,
        this.wsId,
        resource.cohort.id,
        resource.cohort
      ).subscribe(() => {
        this.closeEditModal();
        this.onUpdate.emit();
      });
    } else if (resource.conceptSet) {
      this.conceptSetsService.updateConceptSet(
        this.wsNamespace,
        this.wsId,
        resource.conceptSet.id,
        resource.conceptSet
      ).subscribe(() => {
        this.closeEditModal();
        this.onUpdate.emit();
      });
    }
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
    this.openConfirmDelete();
  }

  receiveDelete(): void {
    switch (this.resourceType) {
      case ResourceType.NOTEBOOK: {
        this.workspacesService.deleteNotebook(this.wsNamespace, this.wsId, this.resource.name)
          .subscribe(() => {
            this.closeConfirmDelete();
            this.onUpdate.emit();
          });
        break;
      }
      case ResourceType.COHORT: {
        this.cohortsService.deleteCohort(this.wsNamespace, this.wsId, this.resource.id)
          .subscribe(() => {
            this.closeConfirmDelete();
            this.onUpdate.emit();
          });
        break;
      }
      case ResourceType.CONCEPT_SET: {
        this.conceptSetsService.deleteConceptSet(this.wsNamespace, this.wsId, this.resource.id)
          .subscribe(() => {
            this.closeConfirmDelete();
            this.onUpdate.emit();
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
          queryParams = {playgroundMode: true};
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
    setTimeout(_ => this.editing = true, 0);
  }

  editConceptSet(): void {
    this.editing = true;
  }

  closeEditModal(): void {
    this.editing = false;
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
