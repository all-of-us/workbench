import * as React from 'react';
import { Prompt, RouteComponentProps, withRouter } from 'react-router';
import * as fp from 'lodash/fp';
import validate from 'validate.js';

import {
  ConceptSet,
  CopyRequest,
  Criteria,
  Domain,
  ResourceType,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { parseQueryParams } from 'app/components/app-router';
import {
  Button,
  Clickable,
  MenuItem,
  SnowmanButton,
} from 'app/components/buttons';
import { ConfirmDeleteModal } from 'app/components/confirm-delete-modal';
import { FadeBox } from 'app/components/containers';
import { CopyModal } from 'app/components/copy-modal';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  TextAreaWithLengthValidationMessage,
  TextInput,
  ValidationError,
} from 'app/components/inputs';
import { Modal, ModalFooter, ModalTitle } from 'app/components/modals';
import { PopupTrigger, TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { EditComponentReact } from 'app/icons/edit';
import {
  CriteriaSearch,
  LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS,
} from 'app/pages/data/criteria-search';
import { conceptSetsApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  reactStyles,
  summarizeErrors,
  withCurrentCohortSearchContext,
  withCurrentConcept,
  withCurrentWorkspace,
} from 'app/utils';
import {
  conceptSetUpdating,
  currentConceptSetStore,
  currentConceptStore,
  NavigationProps,
  setSidebarActiveIconStore,
} from 'app/utils/navigation';
import { nameValidationFormat } from 'app/utils/resources';
import { MatchParams } from 'app/utils/stores';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';
import { Subscription } from 'rxjs/Subscription';

const styles = reactStyles({
  conceptSetHeader: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  conceptSetTitle: {
    color: colors.primary,
    fontSize: 20,
    fontWeight: 600,
    marginBottom: '0.75rem',
    display: 'flex',
    flexDirection: 'row',
  },
  conceptSetMetadataWrapper: {
    flexDirection: 'column',
    alignItems: 'space-between',
    marginLeft: '0.75rem',
  },
  conceptSetData: {
    display: 'flex',
    flexDirection: 'row',
    color: colors.primary,
    fontWeight: 600,
  },
  showMore: {
    background: colors.white,
    color: colors.accent,
    cursor: 'pointer',
    marginLeft: '0.375rem',
  },
});

export interface ConceptSetMenuProps {
  canDelete: boolean;
  canEdit: boolean;
  onEdit: Function;
  onDelete: Function;
  onCopy: Function;
}

const ConceptSetMenu: React.FunctionComponent<
  React.PropsWithChildren<ConceptSetMenuProps>
> = ({ canDelete, canEdit, onEdit, onDelete, onCopy }) => (
  <PopupTrigger
    side='right'
    closeOnClick
    content={
      <React.Fragment>
        <TooltipTrigger>
          <TooltipTrigger
            content={<div>Requires Write Permission</div>}
            disabled={canEdit}
          >
            <MenuItem icon='pencil' onClick={onEdit} disabled={!canEdit}>
              Rename
            </MenuItem>
          </TooltipTrigger>
          <MenuItem icon='copy' onClick={onCopy}>
            Copy to another Workspace
          </MenuItem>
        </TooltipTrigger>
        <TooltipTrigger
          content={<div>Requires Owner Permission</div>}
          disabled={canDelete}
        >
          <MenuItem icon='trash' onClick={onDelete} disabled={!canDelete}>
            Delete
          </MenuItem>
        </TooltipTrigger>
      </React.Fragment>
    }
  >
    <SnowmanButton data-test-id='workspace-menu' />
  </PopupTrigger>
);

function sortAndStringify(concepts) {
  return JSON.stringify(concepts.sort((a, b) => a.id - b.id));
}

interface Props
  extends WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps<MatchParams> {
  cohortContext: any;
  concept: Array<Criteria>;
  workspace: WorkspaceData;
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
  existingNames: string[];
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
  withNavigation,
  withRouter
)(
  class extends React.Component<Props, State> {
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
        existingNames: [],
        deleting: false,
        loading: false,
        showMoreDescription: false,
        unsavedChanges: false,
        conceptSetUpdating: false,
      };
    }

    componentDidMount() {
      this.props.hideSpinner();

      if (!this.isDetailPage && !currentConceptStore.getValue()) {
        currentConceptStore.next([]);
      }
      if (this.isDetailPage) {
        this.setState({ loading: true });
        this.getConceptSet();
      }
      this.subscription = currentConceptStore.subscribe((currentConcepts) => {
        if (![null, undefined].includes(currentConcepts)) {
          this.checkUnsavedConceptChanges(currentConcepts);
        }
      });
      this.subscription.add(
        conceptSetUpdating.subscribe((updating) =>
          this.setState({ conceptSetUpdating: updating })
        )
      );
    }

    componentDidUpdate(prevProps: Readonly<Props>) {
      if (this.isDetailPage) {
        if (currentConceptSetStore.getValue()) {
          if (!this.state.conceptSet) {
            this.setState({ conceptSet: currentConceptSetStore.getValue() });
          }
          if (this.state.loading) {
            this.setState({ loading: false });
          }
        } else if (
          prevProps.match.params.csid !== this.props.match.params.csid
        ) {
          this.setState({ loading: true });
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
      const currentConceptsMap = currentConcepts.map((concept) => {
        ['attributes', 'parameterId'].forEach((prop) => delete concept[prop]);
        return concept;
      });
      const unsavedChanges =
        (!currentConceptSet && currentConcepts.length > 0) ||
        (!!currentConceptSet &&
          sortAndStringify(currentConceptSet.criteriums) !==
            sortAndStringify(currentConceptsMap));
      this.setState({ unsavedChanges });
    }

    async getConceptSet() {
      const { ns, wsid, csid } = this.props.match.params;
      try {
        const resp = await conceptSetsApi().getConceptSet(ns, wsid, +csid);
        if (resp.domain === Domain.SURVEY) {
          resp.criteriums = resp.criteriums.filter(
            (survey) => survey.parentCount !== 0
          );
        }
        this.setState({
          conceptSet: resp,
          editName: resp.name,
          editDescription: resp.description,
          loading: false,
        });
        currentConceptSetStore.next(JSON.parse(JSON.stringify(resp)));
        currentConceptStore.next(resp.criteriums);
      } catch (error) {
        console.log(error);
        // TODO: what do we do with resources not found?  Currently we just have an endless spinner
        // Maybe want to think about designing an AoU not found page for better UX
      }
    }

    async copyConceptSet(copyRequest: CopyRequest) {
      const { ns, wsid, csid } = this.props.match.params;
      this.setState({ copySaving: true });
      return conceptSetsApi().copyConceptSet(ns, wsid, csid, copyRequest);
    }

    async submitEdits() {
      const { ns, wsid, csid } = this.props.match.params;
      const { conceptSet, editName, editDescription } = this.state;
      try {
        this.setState({ editSaving: true });
        await conceptSetsApi().updateConceptSet(ns, wsid, +csid, {
          ...conceptSet,
          name: editName.trim(),
          description: editDescription,
        });
        await this.getConceptSet();
      } catch (error) {
        console.log(error);
      } finally {
        this.setState({ editing: false, editSaving: false });
      }
    }

    async onDeleteConceptSet() {
      const { ns, wsid, csid } = this.props.match.params;
      try {
        await conceptSetsApi().deleteConceptSet(ns, wsid, +csid);
        this.props.navigate(['workspaces', ns, wsid, 'data', 'concepts']);
      } catch (error) {
        console.error(error);
        this.setState({
          error: true,
          errorMessage:
            "Could not delete concept set '" + this.state.conceptSet.name + "'",
        });
      } finally {
        this.setState({ deleting: false });
      }
    }

    get conceptSetConceptsCount(): number {
      return !!this.state.conceptSet && this.state.conceptSet.criteriums
        ? this.state.conceptSet.criteriums.length
        : 0;
    }

    onEditOpen() {
      const { ns, wsid, csid } = this.props.match.params;
      conceptSetsApi()
        .getConceptSetsInWorkspace(ns, wsid)
        .then((conceptSets) => {
          this.setState({
            editing: true,
            existingNames: conceptSets.items
              .filter((conceptSet) => conceptSet.id !== +csid)
              .map((conceptSet) => conceptSet.name),
          });
        });
    }

    get displayDomainName() {
      const { conceptSet } = this.state;
      return conceptSet.domain === Domain.PHYSICALMEASUREMENT ||
        conceptSet.domain === Domain.PHYSICALMEASUREMENTCSS
        ? 'Physical Measurements'
        : fp.capitalize(conceptSet.domain.toString());
    }

    showUnsavedChangesWarning() {
      return !this.state.conceptSetUpdating && this.state.unsavedChanges;
    }

    get disableFinishButton() {
      const {
        concept,
        workspace: { accessLevel },
      } = this.props;
      const { unsavedChanges } = this.state;
      return (
        !concept ||
        !unsavedChanges ||
        ![WorkspaceAccessLevel.OWNER, WorkspaceAccessLevel.WRITER].includes(
          accessLevel
        )
      );
    }

    get isDetailPage() {
      return !!this.props.match.params.csid;
    }

    get searchContext() {
      if (this.isDetailPage) {
        if (this.state.conceptSet) {
          const {
            conceptSet: { domain, survey },
          } = this.state;
          return {
            domain,
            selectedSurvey: survey,
            source: 'conceptSetDetails',
          };
        } else {
          return {};
        }
      } else {
        const { domain } = this.props.match.params;
        const survey = parseQueryParams(this.props.location.search).get(
          'survey'
        );
        return { domain, selectedSurvey: survey, source: 'concept' };
      }
    }

    tooltipContent(errors) {
      return !!errors ? (
        <ul>
          {errors.editName && <li>{errors.editName}</li>}
          {errors.editDescription && (
            <li>Description cannot exceed 1000 characters</li>
          )}
        </ul>
      ) : (
        ''
      );
    }

    render() {
      const {
        cohortContext,
        workspace: {
          accessLevel,
          cdrVersionId,
          id,
          namespace,
          accessTierShortName,
        },
      } = this.props;
      const {
        copying,
        conceptSet,
        editing,
        editDescription,
        editName,
        error,
        errorMessage,
        editSaving,
        existingNames,
        deleting,
        loading,
        showMoreDescription,
      } = this.state;
      const errors = validate(
        { editDescription, editName: editName?.trim() },
        {
          editName: nameValidationFormat(
            existingNames,
            ResourceType.CONCEPTSET
          ),
          editDescription: { length: { maximum: 1000 } },
        }
      );
      return (
        <React.Fragment>
          <Prompt
            when={this.showUnsavedChangesWarning()}
            message={
              'Your concept set has not been saved. If you’d like to save your concepts, please click CANCEL ' +
              'and save your changes in the right sidebar.'
            }
          />

          <FadeBox
            style={{ margin: 'auto', paddingTop: '1.5rem', width: '95.7%' }}
          >
            {loading ? (
              <SpinnerOverlay />
            ) : (
              <React.Fragment>
                {this.isDetailPage && conceptSet && (
                  <FlexColumn>
                    <div style={styles.conceptSetHeader}>
                      <FlexRow>
                        <ConceptSetMenu
                          canDelete={accessLevel === WorkspaceAccessLevel.OWNER}
                          canEdit={WorkspacePermissionsUtil.canWrite(
                            accessLevel
                          )}
                          onDelete={() => this.setState({ deleting: true })}
                          onEdit={() => this.onEditOpen()}
                          onCopy={() => this.setState({ copying: true })}
                        />
                        <div style={styles.conceptSetMetadataWrapper}>
                          {editing ? (
                            <FlexColumn>
                              <TextInput
                                value={editName}
                                disabled={editSaving}
                                id='edit-name'
                                style={{ marginBottom: '0.75rem' }}
                                data-test-id='edit-name'
                                onChange={(v) => this.setState({ editName: v })}
                              />
                              <ValidationError>
                                {summarizeErrors(errors?.editName)}
                              </ValidationError>
                              <TextAreaWithLengthValidationMessage
                                initialText={editDescription}
                                id='edit-description'
                                textBoxStyleOverrides={{ width: '100%' }}
                                maxCharacters={1000}
                                onChange={(v) =>
                                  this.setState({ editDescription: v })
                                }
                              />
                              <div style={{ margin: '0.75rem 0' }}>
                                <TooltipTrigger
                                  content={this.tooltipContent(errors)}
                                  disabled={!errors}
                                >
                                  <Button
                                    type='primary'
                                    style={{ marginRight: '0.75rem' }}
                                    data-test-id='save-edit-concept-set'
                                    disabled={editSaving || errors}
                                    onClick={() => this.submitEdits()}
                                  >
                                    Save
                                  </Button>
                                </TooltipTrigger>
                                <Button
                                  type='secondary'
                                  disabled={editSaving}
                                  data-test-id='cancel-edit-concept-set'
                                  onClick={() =>
                                    this.setState({
                                      editing: false,
                                      editName: conceptSet.name,
                                      editDescription: conceptSet.description,
                                    })
                                  }
                                >
                                  Cancel
                                </Button>
                              </div>
                            </FlexColumn>
                          ) : (
                            <React.Fragment>
                              <div
                                style={styles.conceptSetTitle}
                                data-test-id='concept-set-title'
                              >
                                {conceptSet.name}
                                <Clickable
                                  disabled={
                                    !WorkspacePermissionsUtil.canWrite(
                                      accessLevel
                                    )
                                  }
                                  style={{ marginLeft: '.75rem' }}
                                  data-test-id='edit-concept-set'
                                  onClick={() => this.onEditOpen()}
                                >
                                  <EditComponentReact
                                    enableHoverEffect={true}
                                    disabled={
                                      !WorkspacePermissionsUtil.canWrite(
                                        accessLevel
                                      )
                                    }
                                    style={{ marginTop: '0.15rem' }}
                                  />
                                </Clickable>
                              </div>
                              <div
                                style={{
                                  marginBottom: '2.25rem',
                                  color: colors.primary,
                                }}
                                data-test-id='concept-set-description'
                              >
                                {showMoreDescription
                                  ? conceptSet.description
                                  : conceptSet.description.slice(0, 250)}
                                {conceptSet.description.length > 250 && (
                                  <span
                                    style={styles.showMore}
                                    onClick={() =>
                                      this.setState({
                                        showMoreDescription:
                                          !showMoreDescription,
                                      })
                                    }
                                  >
                                    Show {showMoreDescription ? 'less' : 'more'}
                                  </span>
                                )}
                              </div>
                            </React.Fragment>
                          )}
                          <div style={styles.conceptSetData}>
                            <div data-test-id='participant-count'>
                              Participant Count:{' '}
                              {!!conceptSet.participantCount
                                ? conceptSet.participantCount.toLocaleString()
                                : ''}
                            </div>
                            <div
                              style={{ marginLeft: '3rem' }}
                              data-test-id='concept-set-domain'
                            >
                              Domain: {this.displayDomainName}
                            </div>
                          </div>
                        </div>
                      </FlexRow>
                    </div>
                  </FlexColumn>
                )}
                <CriteriaSearch
                  backFn={() =>
                    this.props.navigate([
                      'workspaces',
                      namespace,
                      id,
                      'data',
                      'concepts',
                    ])
                  }
                  cohortContext={this.searchContext}
                  conceptSearchTerms={
                    !!cohortContext ? cohortContext.searchTerms : ''
                  }
                />
                <Button
                  style={{ float: 'right', marginBottom: '3rem' }}
                  disabled={this.disableFinishButton}
                  onClick={() => setSidebarActiveIconStore.next('concept')}
                >
                  Finish & Review
                </Button>
              </React.Fragment>
            )}
          </FadeBox>
          {!loading && deleting && (
            <ConfirmDeleteModal
              closeFunction={() => this.setState({ deleting: false })}
              receiveDelete={() => this.onDeleteConceptSet()}
              resourceName={conceptSet.name}
              resourceType={ResourceType.CONCEPTSET}
            />
          )}
          {error && (
            <Modal>
              <ModalTitle>Error: {errorMessage}</ModalTitle>
              <ModalFooter>
                <Button
                  type='secondary'
                  onClick={() => this.setState({ error: false })}
                >
                  Close
                </Button>
              </ModalFooter>
            </Modal>
          )}
          {copying && (
            <CopyModal
              fromWorkspaceNamespace={namespace}
              fromWorkspaceFirecloudName={id}
              fromResourceName={conceptSet.name}
              fromCdrVersionId={cdrVersionId}
              fromAccessTierShortName={accessTierShortName}
              resourceType={ResourceType.CONCEPTSET}
              onClose={() => this.setState({ copying: false })}
              onCopy={() => this.setState({ copySaving: false })}
              saveFunction={(copyRequest: CopyRequest) =>
                this.copyConceptSet(copyRequest)
              }
            />
          )}
        </React.Fragment>
      );
    }
  }
);
