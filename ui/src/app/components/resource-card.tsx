import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button, Clickable} from 'app/components/buttons';
import {ResourceCardBase} from 'app/components/card';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ResourceCardMenu} from 'app/components/resources';
import {TextModal} from 'app/components/text-modal';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {navigate, navigateAndPreventDefaultIfNoKeysPressed} from 'app/utils/navigation';
import {toDisplay} from 'app/utils/resourceActions';

import {ConfirmDeleteModal} from 'app/components/confirm-delete-modal';
import {CopyModal} from 'app/components/copy-modal';
import {ExportDataSetModal} from 'app/pages/data/data-set/export-data-set-modal';
import {CopyRequest, DataSet, RecentResource, ResourceType} from 'generated/fetch';

import {Modal, ModalBody, ModalTitle} from 'app/components/modals';
import {RenameModal} from 'app/components/rename-modal';
import {cohortReviewApi, conceptSetsApi, dataSetApi} from 'app/services/swagger-fetch-clients';
import {AnalyticsTracker} from 'app/utils/analytics';

const styles = reactStyles({
  card: {
    marginTop: '1rem',
    justifyContent: 'space-between',
    marginRight: '1rem',
    padding: '0.75rem 0.75rem 0rem 0.75rem',
    boxShadow: '0 0 0 0'
  },
  cardName: {
    fontSize: '18px', fontWeight: 500, lineHeight: '22px', color: colors.accent,
    cursor: 'pointer', wordBreak: 'break-all', textOverflow: 'ellipsis',
    overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 3,
    WebkitBoxOrient: 'vertical', textDecoration: 'none'
  },
  cardDescription: {
    textOverflow: 'ellipsis', overflow: 'hidden', display: '-webkit-box',
    WebkitLineClamp: 4, WebkitBoxOrient: 'vertical'
  },
  lastModified: {
    color: colors.primary,
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
    color: colors.white,
    fontFamily: 'Montserrat, sans-serif',
    fontSize: '12px',
    fontWeight: 500
  },
  cardFooter: {
    display: 'flex',
    flexDirection: 'column'
  }
});

const resourceTypeColor = (resourceType: ResourceType) => {
  switch (resourceType) {
    case ResourceType.COHORTREVIEW:
      return colors.resourceCardHighlights.cohortReview;
    case ResourceType.CONCEPTSET:
      return colors.resourceCardHighlights.conceptSet;
  }
};

export interface Props {
  resourceCard: RecentResource;
  onDuplicateResource: Function;
  onUpdate: Function;
  existingNameList?: string[];
}

export interface State {
  confirmDeleting: boolean;
  copyingConceptSet: boolean;
  errorModalBody: string;
  errorModalTitle: string;
  invalidResourceError: boolean;
  renaming: boolean;
  showErrorModal: boolean;
  dataSetByResourceIdList: Array<DataSet>;
}

export class ResourceCard extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      confirmDeleting: false,
      copyingConceptSet: false,
      errorModalTitle: 'Error Title',
      errorModalBody: 'Error Body',
      invalidResourceError: !(props.resourceCard.cohortReview ||
        props.resourceCard.conceptSet),
      renaming: false,
      showErrorModal: false,
      dataSetByResourceIdList: []
    };
  }

  get resourceType(): ResourceType {
    if (this.props.resourceCard.cohortReview) {
      return ResourceType.COHORTREVIEW;
    } else if (this.props.resourceCard.conceptSet) {
      return ResourceType.CONCEPTSET;
    }
  }

  get isCohortReview(): boolean {
    return this.resourceType === ResourceType.COHORTREVIEW;
  }

  get isConceptSet(): boolean {
    return this.resourceType === ResourceType.CONCEPTSET;
  }

  get writerPermission(): boolean {
    return this.props.resourceCard.permission === 'OWNER'
      || this.props.resourceCard.permission === 'WRITER';
  }

  get ownerPermission(): boolean {
    return this.props.resourceCard.permission === 'OWNER';
  }

  get displayName(): string {
    if (this.isCohortReview) {
      return this.props.resourceCard.cohortReview.cohortName;
    } else if (this.isConceptSet) {
      return this.props.resourceCard.conceptSet.name;
    }
  }

  get displayDate(): string {
    if (!this.props.resourceCard.modifiedTime) {
      return '';
    }

    const date = new Date(this.props.resourceCard.modifiedTime);
    // datetime formatting to slice off weekday from readable date string
    return date.toDateString().split(' ').slice(1).join(' ');
  }

  get description(): string {
    if (this.isCohortReview) {
      return this.props.resourceCard.cohortReview.description;
    } else if (this.isConceptSet) {
      return this.props.resourceCard.conceptSet.description;
    }
  }

  renameResource(): void {
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

  async getDataSetByResourceId(id) {
    if (this.state.dataSetByResourceIdList.length === 0) {
      const dataSetList = await dataSetApi().getDataSetByResourceId(
        this.props.resourceCard.workspaceNamespace,
        this.props.resourceCard.workspaceFirecloudName,
        this.resourceType, id);
      return dataSetList.items;
    } else {
      this.setState({dataSetByResourceIdList: []});
    }
    return false;
  }

  async receiveDelete() {
    switch (this.resourceType) {
      case ResourceType.COHORTREVIEW: {
        const dataSetByResourceIdList = await
            this.getDataSetByResourceId(this.props.resourceCard.cohortReview.cohortReviewId);
        if (dataSetByResourceIdList && dataSetByResourceIdList.length > 0) {
          this.setState({dataSetByResourceIdList: dataSetByResourceIdList});
          return;
        }
        cohortReviewApi().deleteCohortReview(
          this.props.resourceCard.workspaceNamespace,
          this.props.resourceCard.workspaceFirecloudName,
          this.props.resourceCard.cohortReview.cohortReviewId)
          .then(() => {
            this.closeConfirmDelete();
            this.props.onUpdate();
          });
        break;
      }
      case ResourceType.CONCEPTSET: {
        const dataSetByResourceIdList = await
            this.getDataSetByResourceId(this.props.resourceCard.conceptSet.id);
        if (dataSetByResourceIdList && dataSetByResourceIdList.length > 0) {
          this.setState({dataSetByResourceIdList: dataSetByResourceIdList});
          return;
        }
        conceptSetsApi().deleteConceptSet(
          this.props.resourceCard.workspaceNamespace,
          this.props.resourceCard.workspaceFirecloudName,
          this.props.resourceCard.conceptSet.id)
          .then(() => {
            this.closeConfirmDelete();
            this.props.onUpdate();
          });
        break;
      }
    }
  }

  receiveRename(name, description): void {
    if (this.isCohortReview) {
      const request = {
        ...this.props.resourceCard.cohortReview,
        cohortName: name,
        description: description
      };
      cohortReviewApi().updateCohortReview(
        this.props.resourceCard.workspaceNamespace,
        this.props.resourceCard.workspaceFirecloudName,
        this.props.resourceCard.cohortReview.cohortReviewId,
        request
      ).then(() => {
        this.cancelRename();
        this.props.onUpdate();
      });
    } else if (this.isConceptSet) {
      const request = {
        ...this.props.resourceCard.conceptSet,
        name: name,
        description: description
      };
      conceptSetsApi().updateConceptSet(
        this.props.resourceCard.workspaceNamespace,
        this.props.resourceCard.workspaceFirecloudName,
        this.props.resourceCard.conceptSet.id,
        request
      ).then(() => {
        this.cancelRename();
        this.props.onUpdate();
      });
    }

  }

  getResourceUrl(): string {
    const {workspaceNamespace, workspaceFirecloudName, conceptSet, cohortReview} =
      this.props.resourceCard;
    const workspacePrefix = `/workspaces/${workspaceNamespace}/${workspaceFirecloudName}`;

    switch (this.resourceType) {
      case ResourceType.COHORTREVIEW: {
        return `${workspacePrefix}/data/cohorts/${cohortReview.cohortId}/review`;
      }
      case ResourceType.CONCEPTSET: {
        return `${workspacePrefix}/data/concepts/sets/${conceptSet.id}`;
      }
    }
  }

  async markDataSetDirty() {
    let id = 0;
    switch (this.resourceType) {
      case ResourceType.CONCEPTSET: {
        id = this.props.resourceCard.conceptSet.id;
        break;
      }
    }
    try {
      await dataSetApi().markDirty(this.props.resourceCard.workspaceNamespace,
        this.props.resourceCard.workspaceFirecloudName, {
          id: id,
          resourceType: this.resourceType
        });
      this.receiveDelete();
    } catch (ex) {
      console.log(ex);
    }
  }

  async copyConceptSet(copyRequest: CopyRequest) {
    return conceptSetsApi().copyConceptSet(this.props.resourceCard.workspaceNamespace,
      this.props.resourceCard.workspaceFirecloudName,
      this.props.resourceCard.conceptSet.id.toString(), copyRequest);
  }

  render() {
    return <React.Fragment>
      {this.state.invalidResourceError &&
        <TextModal
          title='Invalid Resource Type'
          body='Please Report a Bug.'
          closeFunction={() => this.setState({invalidResourceError: false})}/>
      }
      {this.state.showErrorModal &&
        <TextModal
          title={this.state.errorModalTitle}
          body={this.state.errorModalBody}
          closeFunction={() => this.setState({showErrorModal: false})}/>
      }
      <ResourceCardBase style={styles.card}
                        data-test-id='card'>
        <FlexColumn style={{alignItems: 'flex-start'}}>
          <FlexRow style={{alignItems: 'flex-start'}}>
            <ResourceCardMenu resourceType={this.resourceType}
                              onCopyConceptSet={() => this.setState({copyingConceptSet: true})}
                              canDelete={this.ownerPermission}
                              onDeleteResource={() => this.openConfirmDelete()}
                              onRenameResource={() => this.renameResource()}
                              canEdit={this.writerPermission}/>
            <Clickable>
              <a style={styles.cardName}
                   data-test-id='card-name'
                 href={this.getResourceUrl()}
                 onClick={e => {
                   if (this.resourceType === ResourceType.DATASET) {
                     AnalyticsTracker.DatasetBuilder.OpenEditPage('From Clicking Card');
                   }
                   navigateAndPreventDefaultIfNoKeysPressed(e, this.getResourceUrl());
                 }}>{this.displayName}
              </a>
            </Clickable>
          </FlexRow>
          <div style={styles.cardDescription}>{this.description}</div>
        </FlexColumn>
        <div style={styles.cardFooter}>
          <div style={styles.lastModified} data-test-id='last-modified'>
            Last Modified: {this.displayDate}</div>
          <div style={{...styles.resourceType, ...{backgroundColor: resourceTypeColor(this.resourceType)}}}
               data-test-id='card-type'>
            {toDisplay(this.resourceType)}</div>
        </div>
      </ResourceCardBase>

      {this.state.renaming && this.isCohortReview &&
        <RenameModal
          onRename={(newName, newDescription) => this.receiveRename(newName, newDescription)}
          resourceType={ResourceType.COHORTREVIEW}
          onCancel={() => this.cancelRename()}
          oldDescription={this.props.resourceCard.cohortReview.description}
          oldName={this.props.resourceCard.cohortReview.cohortName}
          existingNames={this.props.existingNameList}/>
      }

      {this.state.renaming && this.isConceptSet &&
        <RenameModal
          onRename={(newName, newDescription) => this.receiveRename(newName, newDescription)}
          resourceType={ResourceType.CONCEPTSET}
          onCancel={() => this.cancelRename()}
          oldDescription={this.props.resourceCard.conceptSet.description}
          oldName={this.props.resourceCard.conceptSet.name}
          existingNames={this.props.existingNameList}/>}
      {this.state.confirmDeleting &&
      <ConfirmDeleteModal resourceName={this.displayName}
                          resourceType={this.resourceType}
                          receiveDelete={() => this.receiveDelete()}
                          closeFunction={() => this.closeConfirmDelete()}/>}
      {this.state.copyingConceptSet && <CopyModal
        fromWorkspaceNamespace={this.props.resourceCard.workspaceNamespace}
        fromWorkspaceName={this.props.resourceCard.workspaceFirecloudName}
        fromResourceName={this.props.resourceCard.conceptSet.name}
        resourceType={this.resourceType}
        onClose={() => this.setState({copyingConceptSet: false})}
        onCopy={() => this.props.onUpdate()}
        saveFunction={(copyRequest: CopyRequest) => this.copyConceptSet(copyRequest)}/>
      }
      {this.state.dataSetByResourceIdList.length > 0 && <Modal>
        <ModalTitle>WARNING</ModalTitle>
        <ModalBody>
          <div style={{paddingBottom: '1rem'}}>
            The {toDisplay(this.resourceType)} <b>{fp.startCase(this.displayName)}&nbsp;</b>
            is referenced by the following datasets:
            <b>
              &nbsp;
              {fp.join(', ' ,
                this.state.dataSetByResourceIdList.map((data) => data.name))}
            </b>.
            Deleting the {toDisplay(this.resourceType)}
            <b>{fp.startCase(this.displayName)} </b> will make these datasets unavailable for use.
            Are you sure you want to delete <b>{fp.startCase(this.displayName)}</b> ?
          </div>
          <div style={{float: 'right'}}>
            <Button type='secondary' style={{ marginRight: '2rem'}} onClick={() => {
              this.setState({dataSetByResourceIdList: []});  this.closeConfirmDelete(); }}>
              Cancel
            </Button>
            <Button type='primary'
                  onClick={() => this.markDataSetDirty()}>YES, DELETE</Button>
          </div>
        </ModalBody>
      </Modal>}
    </React.Fragment>;
  }
}
