import {RenameModal} from 'app/components/rename-modal';
import {Action, ResourceCardTemplate} from 'app/components/resource-card-template';
import {withConfirmDeleteModal, WithConfirmDeleteModalProps} from 'app/components/with-confirm-delete-modal';
import {withErrorModal, WithErrorModalProps} from 'app/components/with-error-modal';
import {withSpinnerOverlay, WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {cohortsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {formatRecentResourceDisplayDate} from 'app/utils';
import {navigateByUrl} from 'app/utils/navigation';
import {RecentResource, ResourceType} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {toDisplay} from "app/utils/resourceActions";

interface Props extends WithConfirmDeleteModalProps, WithErrorModalProps, WithSpinnerOverlayProps {
  resource: RecentResource;
  existingNameList: string[];
  onUpdate: Function;
}

interface State {
  showRenameModal: boolean;
}

export const CohortResourceCard = fp.flow(
  withErrorModal(),
  withConfirmDeleteModal(),
  withSpinnerOverlay(),
)(class extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      showRenameModal: false
    };
  }

  get resourceType(): ResourceType {
    return ResourceType.COHORT;
  }

  get displayName(): string {
    return this.props.resource.cohort.name;
  }

  // this is duplicated
  get canWrite(): boolean {
    return this.props.resource.permission === 'OWNER'
      || this.props.resource.permission === 'WRITER';
  }

  get canDelete(): boolean {
    return this.props.resource.permission === 'OWNER';
  }

  get resourceUrl(): string {
    const {workspaceNamespace, workspaceFirecloudName, cohort} = this.props.resource;
    return `/workspaces/${workspaceNamespace}/${workspaceFirecloudName}` +
      `/data/cohorts/build?cohortId=${cohort.id}`;
  }

  get reviewCohortUrl(): string {
    const {workspaceNamespace, workspaceFirecloudName, cohort} = this.props.resource;

    return `/workspaces/${workspaceNamespace}/${workspaceFirecloudName}` +
      `/data/cohorts/${cohort.id}/review`;
  }

  get actions(): Action[] {
    return [
      {
        icon: 'note',
        displayName: 'Rename',
        onClick: () => {
          this.setState({showRenameModal: true});
        },
        disabled: !this.canWrite
      },
      {
        icon: 'copy',
        displayName: 'Duplicate',
        onClick: () => this.duplicate(),
        disabled: !this.canWrite
      },
      {
        icon: 'pencil',
        displayName: 'Edit',
        onClick: () => navigateByUrl(this.resourceUrl),
        disabled: !this.canWrite
      },
      {
        icon: 'grid-view',
        displayName: 'Review',
        onClick: () => navigateByUrl(this.reviewCohortUrl),
        disabled: !this.canWrite
      },
      {
        icon: 'trash',
        displayName: 'Delete',
        onClick: () => {
          this.props.showConfirmDeleteModal(this.displayName,
            this.resourceType, () => this.delete());
        },
        disabled: !this.canDelete
      }
    ];
  }

  delete() {
    return cohortsApi().deleteCohort(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      this.props.resource.cohort.id)
      .then(() => {
        this.props.onUpdate();
      });
  }

  duplicate() {
    this.props.showSpinner();

    return cohortsApi().duplicateCohort(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      {
        originalCohortId: this.props.resource.cohort.id,
        newName: `Duplicate of ${this.displayName}`
      }
    ).then(() => {
      this.props.onUpdate();
    }).catch(e => {
      this.props.showErrorModal('Duplicating Cohort Error',
        'Cohort with the same name already exists.');
    }).finally(() => {
      this.props.hideSpinner();
    });
  }

  rename(name, description) {
    const request = {
      ...this.props.resource.cohort,
      name: name,
      description: description
    };

    return cohortsApi().updateCohort(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      this.props.resource.cohort.id,
      request
    ).then(() => {
      this.props.onUpdate();
    }).catch(error => console.error(error)
    ).finally(() => {
      this.setState({showRenameModal: false});
    });
  }

  render() {
    return <React.Fragment>
      {this.state.showRenameModal &&
      <RenameModal onRename={(name, description) => this.rename(name, description)}
                   resourceType={this.resourceType}
                   onCancel={() => this.setState({showRenameModal: false})}
                   oldDescription={this.props.resource.cohort.description}
                   oldName={this.displayName}
                   existingNames={this.props.existingNameList}/>
      }

      <ResourceCardTemplate
        actions={this.actions}
        disabled={!this.canWrite}
        resourceUrl={this.resourceUrl}
        displayName={this.displayName}
        description={this.props.resource.cohort.description}
        displayDate={formatRecentResourceDisplayDate(this.props.resource.modifiedTime)}
        footerText={toDisplay(this.resourceType)}
        footerColor={colors.resourceCardHighlights.cohort}
      />
    </React.Fragment>;
  }
});
