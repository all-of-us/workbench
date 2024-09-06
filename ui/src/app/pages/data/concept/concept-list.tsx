import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  CardCount,
  ConceptSet,
  Criteria,
  Domain,
  UpdateConceptSetRequest,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexRow, FlexRowWrap } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { domainToTitle } from 'app/pages/data/cohort/utils';
import {
  CONCEPT_SET_CONCEPT_LIMIT,
  ConceptAddModal,
} from 'app/pages/data/concept/concept-add-modal';
import { LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS } from 'app/pages/data/criteria-search';
import { conceptSetsApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  reactStyles,
  withCurrentCohortSearchContext,
  withCurrentConcept,
  withCurrentConceptSet,
  withCurrentWorkspace,
} from 'app/utils';
import {
  conceptSetUpdating,
  currentConceptSetStore,
  currentConceptStore,
  NavigationProps,
  sidebarActiveIconStore,
} from 'app/utils/navigation';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';

const styles = reactStyles({
  sectionTitle: {
    marginTop: '0',
    fontWeight: 600,
    color: colors.primary,
    marginBottom: '1.5rem',
  },
  selectionContainer: {
    background: colors.white,
    border: `2px solid ${colors.primary}`,
    borderRadius: '5px',
    height: 'calc(100% - 25.5rem)',
    lineHeight: '1.125rem',
    minHeight: 'calc(100vh - 22.5rem)',
    padding: '0.75rem',
    position: 'relative',
    overflowX: 'hidden',
    overflowY: 'auto',
  },
  selectionHeader: {
    borderBottom: `1px solid ${colors.disabled}`,
    color: colors.primary,
    display: 'inline-block',
    fontSize: '13px',
    fontWeight: 600,
    paddingRight: '0.375rem',
  },
  removeSelection: {
    background: 'none',
    border: 0,
    color: colors.danger,
    cursor: 'pointer',
    marginRight: '0.375rem',
    padding: 0,
  },
  saveButton: {
    height: '3rem',
    borderRadius: '5px',
    fontWeight: 600,
    marginRight: '0.75rem',
  },
});

const getConceptIdsToAddOrRemove = (
  conceptsToFilter: Array<Criteria>,
  conceptsToCompare: Array<Criteria>
) => {
  return conceptsToFilter.reduce((conceptIds, concept) => {
    if (
      !conceptsToCompare.find(
        (con) =>
          con.conceptId === concept.conceptId &&
          con.standard === concept.standard
      )
    ) {
      conceptIds.push({
        conceptId: concept.conceptId,
        standard: concept.standard,
      });
    }
    return conceptIds;
  }, []);
};

interface Props extends NavigationProps {
  workspace: WorkspaceData;
  concept: Array<any>;
  conceptSet: ConceptSet;
  cohortContext: any;
}

export const ConceptListPage = fp.flow(
  withCurrentCohortSearchContext(),
  withCurrentConcept(),
  withCurrentConceptSet(),
  withCurrentWorkspace(),
  withNavigation
)(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        conceptAddModalOpen: false,
        updating: false,
      };
    }

    async updateConceptSet() {
      const {
        concept,
        conceptSet,
        workspace: { namespace, terraName },
      } = this.props;
      conceptSetUpdating.next(true);
      this.setState({ updating: true });
      // Selections that don't exist on the existing concept set are added
      const addedConceptSetConceptIds = getConceptIdsToAddOrRemove(
        concept,
        conceptSet.criteriums
      );
      // Concept ids on the existing concept set that don't exist on the selections get removed
      const removedConceptSetConceptIds = getConceptIdsToAddOrRemove(
        conceptSet.criteriums,
        concept
      );
      const updateConceptSetReq: UpdateConceptSetRequest = {
        etag: conceptSet.etag,
        addedConceptSetConceptIds,
        removedConceptSetConceptIds,
      };
      try {
        const updatedConceptSet =
          await conceptSetsApi().updateConceptSetConcepts(
            namespace,
            terraName,
            conceptSet.id,
            updateConceptSetReq
          );
        currentConceptSetStore.next(updatedConceptSet);
        this.props.navigate([
          'workspaces',
          namespace,
          terraName,
          'data',
          'concepts',
          'sets',
          conceptSet.id?.toString(),
          'actions',
        ]);
      } catch (error) {
        console.error(error);
      }
    }

    removeSelection(conceptToDel) {
      const updatedConceptList = this.props.concept.filter(
        (concept) => concept !== conceptToDel
      );
      localStorage.setItem(
        LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS,
        JSON.stringify(updatedConceptList)
      );
      currentConceptStore.next(updatedConceptList);
    }

    afterConceptsSaved(conceptSet: ConceptSet) {
      const { namespace, terraName } = this.props.workspace;
      this.props.navigate([
        'workspaces',
        namespace,
        terraName,
        'data',
        'concepts',
        'sets',
        conceptSet.id?.toString(),
        'actions',
      ]);
    }

    get disableSaveConceptButton() {
      const { concept, conceptSet } = this.props;
      const { updating } = this.state;
      return (
        updating ||
        !concept ||
        concept.length === 0 ||
        concept.length > CONCEPT_SET_CONCEPT_LIMIT ||
        (!!conceptSet &&
          JSON.stringify(conceptSet.criteriums.sort()) ===
            JSON.stringify(concept.sort()))
      );
    }

    getDomainCount() {
      const { domain, type } = this.props.cohortContext;
      const domainCount: CardCount = {
        domain:
          domain === 'Measurement' && type === 'PPI'
            ? Domain.PHYSICAL_MEASUREMENT
            : (domain as Domain),
        name: domainToTitle(domain),
        count: this.props.concept.length,
      };
      return domainCount;
    }

    onSaveConceptSetClick() {
      if (this.props.conceptSet) {
        this.updateConceptSet();
      } else {
        this.setState({ conceptAddModalOpen: true });
      }
    }

    closeConceptAddModal() {
      this.setState({ conceptAddModalOpen: false });
      sidebarActiveIconStore.next(null);
    }

    renderSelection(selection: any, index: number) {
      return (
        <FlexRow key={index} style={{ lineHeight: '1.875rem' }}>
          <button
            style={styles.removeSelection}
            onClick={() => this.removeSelection(selection)}
          >
            <ClrIcon shape='times-circle' />
          </button>
          <b style={{ paddingRight: '0.375rem' }}>{selection.conceptCode}</b>
          {selection.name ? selection.name : selection.question}
        </FlexRow>
      );
    }

    renderSelections() {
      const { concept } = this.props;
      if ([Domain.CONDITION, Domain.PROCEDURE].includes(concept[0].domainId)) {
        // Separate selections by standard and source concepts for Condition and Procedures
        const standardConcepts = concept.filter((con) => con.standard);
        const sourceConcepts = concept.filter((con) => !con.standard);
        return (
          <React.Fragment>
            {standardConcepts.length > 0 && (
              <div style={{ marginBottom: '0.75rem' }}>
                <div style={styles.selectionHeader}>Standard Concepts</div>
                {standardConcepts.map((con, index) =>
                  this.renderSelection(con, index)
                )}
              </div>
            )}
            {sourceConcepts.length > 0 && (
              <div>
                <div style={styles.selectionHeader}>Source Concepts</div>
                {sourceConcepts.map((con, index) =>
                  this.renderSelection(con, index)
                )}
              </div>
            )}
          </React.Fragment>
        );
      } else {
        return concept.map((con, index) => this.renderSelection(con, index));
      }
    }

    render() {
      const { concept } = this.props;
      const { conceptAddModalOpen, updating } = this.state;
      return (
        <div>
          <div style={styles.selectionContainer}>
            {updating && <SpinnerOverlay />}
            {!!concept && concept.length > 0 && this.renderSelections()}
          </div>
          <FlexRowWrap
            style={{ flexDirection: 'row-reverse', marginTop: '1.5rem' }}
          >
            <TooltipTrigger
              content={
                <div>
                  Concept count cannot exceed{' '}
                  {CONCEPT_SET_CONCEPT_LIMIT.toLocaleString()}
                </div>
              }
              disabled={concept.length <= CONCEPT_SET_CONCEPT_LIMIT}
            >
              <Button
                type='primary'
                style={styles.saveButton}
                disabled={this.disableSaveConceptButton}
                onClick={() => this.onSaveConceptSetClick()}
              >
                Save Concept Set
              </Button>
            </TooltipTrigger>
            <Button
              type='link'
              style={{ color: colors.primary, left: 0 }}
              onClick={() => sidebarActiveIconStore.next(null)}
            >
              Close
            </Button>
          </FlexRowWrap>
          {conceptAddModalOpen && (
            <ConceptAddModal
              activeDomainTab={this.getDomainCount()}
              selectedConcepts={this.props.concept}
              onSave={(conceptSet) => this.afterConceptsSaved(conceptSet)}
              onClose={() => this.closeConceptAddModal()}
            />
          )}
        </div>
      );
    }
  }
);
interface State {
  conceptAddModalOpen: boolean;
  updating: boolean;
}
