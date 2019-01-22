import {Component, Input,} from '@angular/core';
import * as React from 'react';

import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {Card} from 'app/components/card';
import {PopupTrigger} from 'app/components/popups';
import {decamelize, reactStyles, ReactWrapperBase, switchCase} from 'app/utils';
import {ResourceType} from 'app/utils/resourceActions';
import {navigate, navigateByUrl} from 'app/utils/navigation';

import {
  Cohort, CohortsService, ConceptSetsService,
  ConceptSet,FileDetail,
  NotebookRename, RecentResource,
  WorkspacesService
} from 'generated';

const MenuItem = ({ icon, children, ...props }) => {
  return <Clickable
    {...props}
    data-test-id={icon}
    style={{
      display: 'flex', alignItems: 'center',
      minWidth: '5rem', height: '1.3333rem',
      padding: '0 1rem', color: '#4A4A4A'
     }}
     hover={{backgroundColor: '#E0EAF1'}}
  ><ClrIcon shape={icon} />&nbsp;{children}</Clickable>;
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
      <ClrIcon shape='ellipsis-vertical' size={21} style={{color: '#2691D0', marginLeft: -9}} />
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

const styles = reactStyles({
  card: {
    marginTop: '1rem',
    justifyContent: 'space-between',
    width: '200px',
    height: '223px',
    marginRight: '1rem',
    padding: '0.75rem 0.75rem 0rem 0.75rem'
  },
  cardName: {
    fontSize: '18px',
    fontWeight: 500,
    lineHeight: '22px',
    color: '#2691D0',
    cursor: 'pointer',
    wordBreak: 'break-all'
  },
  lastModified: {
    color: '#4A4A4A',
    fontSize: '11px',
    display: 'inline-block',
    lineHeight: '14px',
    fontWeight: 300,
    marginBottom: '0.2rem'
  },
  resourceType: {
    height: '22px',
    width: 'max-content',
    paddingLeft: '10px',
    paddingRight: '10px',
    borderRadius: '4px 4px 0 0',
    display: 'flex',
    justifyContent: 'center',
    color: '#FFFFFF',
    fontFamily: 'Montserrat, sans-serif',
    fontSize: '12px',
    fontWeight: 500
  },
  cardFooter: {
    display: 'flex',
    flexDirection: 'column'
  }
});

const resourceTypeStyles = reactStyles({
  cohort: {
    backgroundColor: '#F8C954'
  },
  conceptSet: {
    backgroundColor: '#AB87B3'
  },
  notebook: {
    backgroundColor: '#8BC990'
  }
});

export interface ResourceCardProps {
  resourceCard: RecentResource,
  onUpdate: Function,
  duplicateNameError: Function,
  invalidNameError: Function
}

export interface ResourceCardState {
  resourceType: ResourceType,
  renaming: boolean,
  editing: boolean,
  confirmDeleting: boolean,
  invalidResourceError: boolean,
}

export class ResourceCard extends React.Component<ResourceCardProps, ResourceCardState> {

  constructor(props: ResourceCardProps) {
    super(props);
    const defaultState = {
      editing: false,
      renaming: false,
      confirmDeleting: false
    };
    if (props.resourceCard) {
      if (props.resourceCard.notebook) {
        this.state = {
          resourceType: ResourceType.NOTEBOOK,
          invalidResourceError: false,
          ...defaultState
        };
      } else if (props.resourceCard.cohort) {
        this.state  = {
          resourceType: ResourceType.COHORT,
          invalidResourceError: false,
            ...defaultState
        };
      } else if (props.resourceCard.conceptSet) {
        this.state = {
          resourceType: ResourceType.CONCEPT_SET,
          invalidResourceError: false,
          ...defaultState
        };
      } else {
        this.state = {
          resourceType: ResourceType.INVALID,
          invalidResourceError: true,
          ...defaultState
        };
      }
    }
    console.log(Date.now());
  }

  get actionsDisabled(): boolean {
    return !this.writePermission;
  }

  get writePermission(): boolean {
    return this.props.resourceCard.permission === 'OWNER'
        || this.props.resourceCard.permission === 'WRITER';
  }

  get notebookReadOnly(): boolean {
    return this.state.resourceType === ResourceType.NOTEBOOK
        && this.props.resourceCard.permission === 'READER';
  }

  get displayName(): string {
    if (this.state.resourceType === ResourceType.NOTEBOOK) {
      return this.props.resourceCard.notebook.name.replace(/\.ipynb$/, '');
    } else if (this.state.resourceType === ResourceType.COHORT) {
      return this.props.resourceCard.cohort.name;
    } else if (this.state.resourceType === ResourceType.CONCEPT_SET) {
      return this.props.resourceCard.conceptSet.name;
    }
  }

  get description(): string {
    if (this.state.resourceType === ResourceType.COHORT) {
      return this.props.resourceCard.cohort.description;
    } else if (this.state.resourceType === ResourceType.CONCEPT_SET) {
      return this.props.resourceCard.conceptSet.description;
    }
  }

  editCohort(): void {
    // This ensures the cohort binding is picked up before the open resolves.
    setTimeout(_ => this.editConceptSet(), 0);
  }

  editConceptSet(): void {
    this.setState({editing: true});
  }

  reviewCohort(): void {
    navigateByUrl('/workspaces/' + this.props.resourceCard.workspaceNamespace
        + '/' + this.props.resourceCard.workspaceFirecloudName + '/cohorts/'
        + this.props.resourceCard.cohort.id + '/review');
  }

  closeEditModal(): void {
    this.setState({editing: false});
  }

  renameNotebook(): void {
    this.setState({renaming: true});
  }

  cancelRename(): void {
    this.setState({renaming: false});
  }

  openConfirmDelete(): void {
    this.setState({confirmDeleting: true});
  }

  closeConfirmDelete(): void {
    this.setState({confirmDeleting: false});
  }

  cloneResource(): void {
    // switch (this.state.resourceType) {
    //   case ResourceType.NOTEBOOK: {
    //     this.workspacesService.cloneNotebook(this.wsNamespace, this.wsId, resource.notebook.name)
    //         .map(() => this.onUpdate.emit())
    //         .subscribe(() => {});
    //     break;
    //   }
    //   case ResourceType.COHORT: {
    //     const url =
    //         '/workspaces/' + this.wsNamespace + '/' + this.wsId + '/cohorts/build?cohortId=';
    //     navigateByUrl(url + resource.cohort.id);
    //     this.onUpdate.emit();
    //     break;
    //   }
    // }
  }

  // TODO: how do i call the different services?
  receiveDelete(): void {
    // switch (this.state.resourceType) {
    //   case ResourceType.NOTEBOOK: {
    //     this.workspacesService.deleteNotebook(
    //         this.props.resourceCard.workspaceNamespace,
    //         this.props.resourceCard.workspaceFirecloudName,
    //         this.state.resource.name)
    //         .subscribe(() => {
    //           this.closeConfirmDelete();
    //           this.onUpdate.emit();
    //         });
    //     break;
    //   }
    //   case ResourceType.COHORT: {
    //     this.cohortsService.deleteCohort(
    //         this.props.resourceCard.workspaceNamespace,
    //         this.props.resourceCard.workspaceFirecloudName,
    //         this.state.resource.id)
    //         .subscribe(() => {
    //           this.closeConfirmDelete();
    //           this.onUpdate.emit();
    //         });
    //     break;
    //   }
    //   case ResourceType.CONCEPT_SET: {
    //     this.conceptSetsService.deleteConceptSet(
    //         this.props.resourceCard.workspaceNamespace,
    //         this.props.resourceCard.workspaceFirecloudName,
    //         this.state.resource.id)
    //         .subscribe(() => {
    //           this.closeConfirmDelete();
    //           this.onUpdate.emit();
    //         });
    //   }
    // }
  }

  receiveEdit(): void {
    // if (resource.cohort) {
    //   this.cohortsService.updateCohort(
    //       this.wsNamespace,
    //       this.wsId,
    //       resource.cohort.id,
    //       resource.cohort
    //   ).subscribe( () => {
    //     this.closeEditModal();
    //     this.onUpdate.emit();
    //   });
    // } else if (resource.conceptSet) {
    //   this.conceptSetsService.updateConceptSet(
    //       this.wsNamespace,
    //       this.wsId,
    //       resource.conceptSet.id,
    //       resource.conceptSet
    //   ).subscribe( () => {
    //     this.closeEditModal();
    //     this.onUpdate.emit();
    //   });
    // }
  }

  receiveNotebookRename(rename: NotebookRename): void {
    // let newName = rename.newName;
    // if (!(new RegExp('^.+\.ipynb$').test(newName))) {
    //   newName = rename.newName + '.ipynb';
    //   rename.newName = newName;
    // }
    // if (new RegExp('.*\/.*').test(newName)) {
    //   this.renaming = false;
    //   this.invalidNameError.emit(newName);
    //   return;
    // }
    // this.workspacesService.getNoteBookList(this.wsNamespace, this.wsId)
    //     .switchMap((fileList) => {
    //       if (fileList.filter((nb) => nb.name === newName).length > 0) {
    //         throw new Error(newName);
    //       } else {
    //         return this.workspacesService.renameNotebook(this.wsNamespace, this.wsId, rename);
    //       }
    //     })
    //     .subscribe(() => {
    //           this.renaming = false;
    //           this.onUpdate.emit(rename);
    //         },
    //         (dupName) => {
    //           this.duplicateNameError.emit(dupName);
    //           this.renaming = false;
    //         });
  }

  openResource(): void {
    switch (this.state.resourceType) {
      case ResourceType.COHORT: {
        this.reviewCohort();
        break;
      }
      case ResourceType.CONCEPT_SET: {
        navigate(['workspaces', this.props.resourceCard.workspaceNamespace,
          this.props.resourceCard.workspaceFirecloudName, 'concepts', 'sets',
          this.props.resourceCard.conceptSet.id], {relativeTo: null});
        break;
      }
      case ResourceType.NOTEBOOK: {
        let queryParams = null;
        if (this.notebookReadOnly) {
          queryParams = { playgroundMode: true };
        }
        navigate(
            ['workspaces', this.props.resourceCard.workspaceNamespace,
              this.props.resourceCard.workspaceFirecloudName, 'notebooks',
              encodeURIComponent(this.props.resourceCard.notebook.name)], {
              queryParams,
              relativeTo: null,
            });
      }
    }
  }

  render() {
    return <React.Fragment>
      <Card style={styles.card}>
        <div style={{display: 'flex', flexDirection: 'column', alignItems: 'flex-start'}}>
          <div style={{display: 'flex', flexDirection: 'row', alignItems: 'flex-start'}}>
            <ResourceCardMenu disabled={this.actionsDisabled}
                              resourceType={this.state.resourceType}
                              onCloneResource={this.cloneResource}
                              onDeleteResource={this.openConfirmDelete}
                              onRenameNotebook={this.renameNotebook}
                              onEditCohort={this.editCohort}
                              onEditConceptSet={this.editConceptSet}
                              onReviewCohort={this.reviewCohort}/>
            <Clickable disabled={this.actionsDisabled && !this.notebookReadOnly}>
              <div style={styles.cardName}
                   onClick={() => this.openResource()}>{this.displayName}
              </div>
            </Clickable>
          </div>
          <div>{this.description}</div>
        </div>
        <div style={styles.cardFooter}>
          <div style={styles.lastModified}>
            Last Modified: {this.props.resourceCard.modifiedTime}</div>
          <div style={{...styles.resourceType, ...resourceTypeStyles[this.state.resourceType]}}>
            {decamelize(this.state.resourceType, ' ')}</div>
        </div>
      </Card>
    </React.Fragment>
  }
}

@Component ({
  selector : 'app-resource-card',
  template: '<div #root></div>'
})
export class ResourceCardComponent extends ReactWrapperBase {
  resourceType: ResourceType;
  @Input('resourceCard') resourceCard: ResourceCardProps['resourceCard'];
  @Input('onUpdate') onUpdate: ResourceCardProps['onUpdate'];
  @Input('duplicateNameError') duplicateNameError: ResourceCardProps['duplicateNameError'];
  @Input('invalidNameError') invalidNameError: ResourceCardProps['invalidNameError'];

  constructor() {
    super(ResourceCard, ['resourceCard', 'onUpdate'])
  }
}
