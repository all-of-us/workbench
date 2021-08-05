import * as fp from 'lodash/fp';
import * as React from 'react';
import {Prompt} from 'react-router';
import {Subscription} from 'rxjs/Subscription';
import * as validate from 'validate.js';

import {Button, Clickable, MenuItem, SnowmanButton} from 'app/components/buttons';
import {ConfirmDeleteModal} from 'app/components/confirm-delete-modal';
import {FadeBox} from 'app/components/containers';
import {CopyModal} from 'app/components/copy-modal';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {TextAreaWithLengthValidationMessage, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {EditComponentReact} from 'app/icons/edit';
import {CriteriaSearch, LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS} from 'app/pages/data/criteria-search';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  reactStyles,
  withCurrentCohortSearchContext,
  withCurrentConcept,
  withCurrentWorkspace,
  withUrlParams
} from 'app/utils';
import {
  conceptSetUpdating,
  currentConceptSetStore,
  currentConceptStore,
  NavigationProps,
  queryParamsStore,
  setSidebarActiveIconStore
} from 'app/utils/navigation';
import {withNavigation} from 'app/utils/with-navigation-hoc';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {ConceptSet, CopyRequest, Criteria, Domain, ResourceType, WorkspaceAccessLevel} from 'generated/fetch';

const styles = reactStyles({
  conceptSetHeader: {
    display: 'flex', flexDirection: 'row', justifyContent: 'space-between'
  },
  conceptSetTitle: {
    color: colors.primary, fontSize: 20, fontWeight: 600, marginBottom: '0.5rem',
    display: 'flex', flexDirection: 'row'
  },
  conceptSetMetadataWrapper: {
    flexDirection: 'column', alignItems: 'space-between', marginLeft: '0.5rem'
  },
  conceptSetData: {
    display: 'flex', flexDirection: 'row', color: colors.primary, fontWeight: 600
  },
  showMore: {
    background: colors.white,
    color: colors.accent,
    cursor: 'pointer',
    marginLeft: '0.25rem'
  },
});

export interface ConceptSetMenuProps {
  canDelete: boolean;
  canEdit: boolean;
  onEdit: Function;
  onDelete: Function;
  onCopy: Function;
}

const ConceptSetMenu: React.FunctionComponent<ConceptSetMenuProps> = ({canDelete, canEdit, onEdit, onDelete, onCopy}) =>
  <PopupTrigger side='right'
    closeOnClick
    content={<React.Fragment>
      <TooltipTrigger>
        <TooltipTrigger content={<div>Requires Write Permission</div>}
                        disabled={canEdit}>
          <MenuItem icon='pencil'
                    onClick={onEdit}
                    disabled={!canEdit}>
            Rename
          </MenuItem>
        </TooltipTrigger>
        <MenuItem icon='copy'
                  onClick={onCopy}>
          Copy to another Workspace
        </MenuItem>
      </TooltipTrigger>
      <TooltipTrigger content={<div>Requires Owner Permission</div>}
                      disabled={canDelete}>
        <MenuItem icon='trash' onClick={onDelete} disabled={!canDelete}>
          Delete
        </MenuItem>
      </TooltipTrigger>
    </React.Fragment>}>
    <SnowmanButton data-test-id='workspace-menu'/>
  </PopupTrigger>;

function sortAndStringify(concepts) {
  return JSON.stringify(concepts.sort((a, b) => a.id - b.id));
}

interface Props extends WithSpinnerOverlayProps, NavigationProps {
  cohortContext: any;
  concept: Array<Criteria>;
  workspace: WorkspaceData;
  urlParams: any;
}

interface State {
  copying: boolean;
  copySaving: boolean;
  conceptSet: ConceptSet;
  deleting: boolean;
  editing: boolean;
  editName: string;
  editDescription: string;
  editSaving: boolean;
  error: boolean;
  errorMessage: string;
  loading: boolean;
  showMoreDescription: boolean;
  // Show if trying to navigate away with unsaved changes
  unsavedChanges: boolean;
  conceptSetUpdating: boolean;
}

export const ConceptSearch = fp.flow(
  withCurrentCohortSearchContext(),
  withCurrentConcept(),
  withCurrentWorkspace(),
  withUrlParams(),
  withNavigation)
(class extends React.Component<Props, State> {
  subscription: Subscription;
  constructor(props: any) {
    super(props);
    this.state = {
      copying: false,
      copySaving: false,
      conceptSet: undefined,
      editing: false,
      editName: '',
      editDescription: '',
      editSaving: false,
      error: false,
      errorMessage: '',
      deleting: false,
      loading: this.isDetailPage,
      showMoreDescription: false,
      unsavedChanges: false,
      conceptSetUpdating: false
    };
  }

  componentDidMount() {
    this.props.hideSpinner();

    if (!this.isDetailPage && !currentConceptStore.getValue()) {
      currentConceptStore.next([]);
    }
    this.subscription = currentConceptStore.subscribe(currentConcepts => {
      if (![null, undefined].includes(currentConcepts)) {
        this.checkUnsavedConceptChanges(currentConcepts);
      }
    });
    this.subscription.add(conceptSetUpdating.subscribe(updating => this.setState({conceptSetUpdating: updating})));
  }

  componentDidUpdate(prevProps, prevState) {
    if (this.isDetailPage) {
      if (currentConceptSetStore.getValue()) {
        if (!this.state.conceptSet) {
          this.setState({conceptSet: currentConceptSetStore.getValue()});
        }
        if (this.state.loading) {
          this.setState({loading: false});
        }
      } else {
        this.getConceptSet();
      }
    }
  }

  componentWillUnmount() {
    localStorage.removeItem(LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS);
    currentConceptStore.next(undefined);
    currentConceptSetStore.next(undefined);
    this.subscription.unsubscribe();
  }

  checkUnsavedConceptChanges(currentConcepts) {
    const currentConceptSet = currentConceptSetStore.getValue();
    const currentConceptsMap = currentConcepts.map(concept => {
      ['attributes', 'parameterId'].forEach(prop => delete concept[prop]);
      return concept;
    });
    const unsavedChanges = (!currentConceptSet && currentConcepts.length > 0) ||
      (!!currentConceptSet && sortAndStringify(currentConceptSet.criteriums) !== sortAndStringify(currentConceptsMap));
    this.setState({unsavedChanges: unsavedChanges});
  }

  async getConceptSet() {
    const {urlParams: {ns, wsid, csid}} = this.props;
    try {
      const resp = await conceptSetsApi().getConceptSet(ns, wsid, csid);
      if (resp.domain === Domain.SURVEY) {
        resp.criteriums = resp.criteriums.filter((survey) => survey.parentCount !== 0);
      }
      this.setState({conceptSet: resp, editName: resp.name, editDescription: resp.description, loading: false});
      currentConceptSetStore.next(JSON.parse(JSON.stringify(resp)));
      currentConceptStore.next(resp.criteriums);
    } catch (error) {
      console.log(error);
      // TODO: what do we do with resources not found?  Currently we just have an endless spinner
      // Maybe want to think about designing an AoU not found page for better UX
    }
  }

  async copyConceptSet(copyRequest: CopyRequest) {
    const {urlParams: {ns, wsid, csid}} = this.props;
    this.setState({copySaving: true});
    return conceptSetsApi().copyConceptSet(ns, wsid, csid, copyRequest);
  }

  async submitEdits() {
    const {urlParams: {ns, wsid, csid}} = this.props;
    const {conceptSet, editName, editDescription} = this.state;
    try {
      this.setState({editSaving: true});
      await conceptSetsApi().updateConceptSet(ns, wsid, csid, {...conceptSet, name: editName, description: editDescription});
      await this.getConceptSet();
    } catch (error) {
      console.log(error);
    } finally {
      this.setState({editing: false, editSaving: false});
    }
  }

  async onDeleteConceptSet() {
    const {urlParams: {ns, wsid, csid}} = this.props;
    try {
      await conceptSetsApi().deleteConceptSet(ns, wsid, csid);
      this.props.navigate(['workspaces', ns, wsid, 'data', 'concepts']);
    } catch (error) {
      console.error(error);
      this.setState({error: true, errorMessage: 'Could not delete concept set \'' + this.state.conceptSet.name + '\''});
    } finally {
      this.setState({deleting: false});
    }
  }

  get conceptSetConceptsCount(): number {
    return !!this.state.conceptSet && this.state.conceptSet.criteriums ? this.state.conceptSet.criteriums.length : 0;
  }

  get displayDomainName() {
    const {conceptSet} = this.state;
    return (conceptSet.domain === Domain.PHYSICALMEASUREMENT ||
      conceptSet.domain === Domain.PHYSICALMEASUREMENTCSS) ? 'Physical Measurements' : fp.capitalize(conceptSet.domain.toString()) ;
  }


  showUnsavedChangesWarning() {
    return !this.state.conceptSetUpdating && this.state.unsavedChanges;
  }

  get disableFinishButton() {
    const {concept, workspace: {accessLevel}} = this.props;
    const {unsavedChanges} = this.state;
    return !concept || !unsavedChanges || ![WorkspaceAccessLevel.OWNER, WorkspaceAccessLevel.WRITER].includes(accessLevel);
  }

  get isDetailPage() {
    return !!this.props.urlParams.csid;
  }

  get searchContext() {
    if (this.isDetailPage) {
      if (this.state.conceptSet) {
        const {conceptSet: {domain, survey}} = this.state;
        return {domain, selectedSurvey: survey, source: 'conceptSetDetails'};
      } else {
        return {}
      }
    } else {
      const {urlParams: {domain}} = this.props;
      const selectedSurvey = queryParamsStore.getValue().survey;
      return {domain, selectedSurvey, source: 'concept'};
    }
  }

  tooltipContent(errors) {
    return !!errors ? <ul>
      {errors.editName && <li>Name cannot be blank</li>}
      {errors.editDescription && <li>Description cannot exceed 1000 characters</li>}
    </ul> : '';
  }

  render() {
    const {cohortContext, workspace: {accessLevel, cdrVersionId, id, namespace, accessTierShortName}} = this.props;
    const {copying, conceptSet, editing, editDescription, editName, error, errorMessage, editSaving, deleting, loading,
      showMoreDescription} = this.state;
    const errors = validate(
      {editDescription, editName},
      {editName: {presence: {allowEmpty: false}}, editDescription: {length: {maximum: 1000}}}
    );
    return <React.Fragment>
      <Prompt
        when={this.showUnsavedChangesWarning()}
        message={'Your concept set has not been saved. If you’d like to save your concepts, please click CANCEL ' +
        'and save your changes in the right sidebar.'}
      />

      <FadeBox style={{margin: 'auto', paddingTop: '1rem', width: '95.7%'}}>
        {this.isDetailPage && conceptSet && <React.Fragment>
          {loading ? <SpinnerOverlay/> :
            <FlexColumn>
              <div style={styles.conceptSetHeader}>
                <FlexRow>
                  <ConceptSetMenu canDelete={accessLevel === WorkspaceAccessLevel.OWNER}
                                  canEdit={WorkspacePermissionsUtil.canWrite(accessLevel)}
                                  onDelete={() => this.setState({deleting: true})}
                                  onEdit={() => this.setState({editing: true})}
                                  onCopy={() => this.setState({copying: true})}/>
                  <div style={styles.conceptSetMetadataWrapper}>
                    {editing ?
                      <FlexColumn>
                        <TextInput value={editName} disabled={editSaving}
                                   id='edit-name'
                                   style={{marginBottom: '0.5rem'}} data-test-id='edit-name'
                                   onChange={v => this.setState({editName: v})}/>
                        <TextAreaWithLengthValidationMessage initialText={editDescription}
                                                             id='edit-description'
                                                             textBoxStyleOverrides={{width: '100%'}}
                                                             maxCharacters={1000}
                                                             tooLongWarningCharacters={950}
                                                             onChange={v => this.setState({editDescription: v})}/>
                        <div style={{margin: '0.5rem 0'}}>
                          <TooltipTrigger content={this.tooltipContent(errors)}
                                          disabled={!errors}>
                          <Button type='primary' style={{marginRight: '0.5rem'}}
                                  data-test-id='save-edit-concept-set'
                                  disabled={editSaving || errors}
                                  onClick={() => this.submitEdits()}>Save</Button>
                          </TooltipTrigger>
                          <Button type='secondary' disabled={editSaving}
                                  data-test-id='cancel-edit-concept-set'
                                  onClick={() => this.setState({
                                    editing: false,
                                    editName: conceptSet.name,
                                    editDescription: conceptSet.description
                                  })}>Cancel</Button>
                        </div>
                      </FlexColumn> :
                      <React.Fragment>
                        <div style={styles.conceptSetTitle} data-test-id='concept-set-title'>
                          {conceptSet.name}
                          <Clickable disabled={!WorkspacePermissionsUtil.canWrite(accessLevel)}
                                     style={{marginLeft: '.5rem'}}
                                     data-test-id='edit-concept-set'
                                     onClick={() => this.setState({editing: true})}>
                            <EditComponentReact enableHoverEffect={true}
                                                disabled={!WorkspacePermissionsUtil.canWrite(accessLevel)}
                                                style={{marginTop: '0.1rem'}}/>
                          </Clickable>
                        </div>
                        <div style={{marginBottom: '1.5rem', color: colors.primary}} data-test-id='concept-set-description'>
                          {showMoreDescription ?
                            conceptSet.description :
                            conceptSet.description.slice(0, 250)
                          }
                          {conceptSet.description.length > 250 &&
                            <span style={styles.showMore}
                                  onClick={() => this.setState({showMoreDescription: !showMoreDescription})}>
                              Show {showMoreDescription ? 'less' : 'more'}
                            </span>
                          }
                        </div>
                      </React.Fragment>}
                    <div style={styles.conceptSetData}>
                      <div data-test-id='participant-count'>
                        Participant Count: {!!conceptSet.participantCount ? conceptSet.participantCount.toLocaleString() : ''}
                      </div>
                      <div style={{marginLeft: '2rem'}} data-test-id='concept-set-domain'>
                        Domain: {this.displayDomainName}
                      </div>
                    </div>
                  </div>
                </FlexRow>
              </div>
            </FlexColumn>
          }
        </React.Fragment>}
        {!loading && <CriteriaSearch backFn={() => this.props.navigate(['workspaces', namespace, id, 'data', 'concepts'])}
                        cohortContext={this.searchContext}
                        conceptSearchTerms={!!cohortContext ? cohortContext.searchTerms : ''}/>}
        <Button style={{float: 'right', marginBottom: '2rem'}}
                disabled={this.disableFinishButton}
                onClick={() => setSidebarActiveIconStore.next('concept')}>Finish & Review</Button>
      </FadeBox>
      {!loading && deleting &&
      <ConfirmDeleteModal closeFunction={() => this.setState({deleting: false})}
                          receiveDelete={() => this.onDeleteConceptSet()}
                          resourceName={conceptSet.name}
                          resourceType={ResourceType.CONCEPTSET}/>}
      {error && <Modal>
        <ModalTitle>Error: {errorMessage}</ModalTitle>
        <ModalFooter>
          <Button type='secondary'
                  onClick={() => this.setState({error: false})}>Close</Button>
        </ModalFooter>
      </Modal>}
      {copying && <CopyModal fromWorkspaceNamespace={namespace}
                             fromWorkspaceFirecloudName={id}
                             fromResourceName={conceptSet.name}
                             fromCdrVersionId={cdrVersionId}
                             fromAccessTierShortName={accessTierShortName}
                             resourceType={ResourceType.CONCEPTSET}
                             onClose={() => this.setState({copying: false})}
                             onCopy={() => this.setState({copySaving: false})}
                             saveFunction={(copyRequest: CopyRequest) => this.copyConceptSet(copyRequest)}/>
      }
    </React.Fragment>;
  }
});
