import * as fp from 'lodash/fp';
import * as React from 'react';

import {domainToTitle} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {FlexRow, FlexRowWrap} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {ConceptAddModal} from 'app/pages/data/concept/concept-add-modal';
import {LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS} from 'app/pages/data/criteria-search';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, withCurrentConcept, withCurrentConceptSet, withCurrentWorkspace} from 'app/utils';
import {conceptSetUpdating, currentConceptSetStore, currentConceptStore, NavStore, setSidebarActiveIconStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {ConceptSet, Criteria, Domain, DomainCount, UpdateConceptSetRequest} from 'generated/fetch';

const styles = reactStyles({
  sectionTitle: {
    marginTop: '0',
    fontWeight: 600,
    color: colors.primary,
    marginBottom: '1rem'
  },
  selectionContainer: {
    background: colors.white,
    border: `2px solid ${colors.primary}`,
    borderRadius: '5px',
    height: 'calc(100% - 17rem)',
    lineHeight: '0.75rem',
    minHeight: 'calc(100vh - 15rem)',
    padding: '0.5rem',
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
    paddingRight: '0.25rem'
  },
  removeSelection: {
    background: 'none',
    border: 0,
    color: colors.danger,
    cursor: 'pointer',
    marginRight: '0.25rem',
    padding: 0
  },
  saveButton: {
    height: '2rem',
    borderRadius: '5px',
    fontWeight: 600,
    marginRight: '0.5rem'
  }
});

const getConceptIdsToAddOrRemove = (conceptsToFilter: Array<Criteria>, conceptsToCompare: Array<Criteria>) => {
  return conceptsToFilter.reduce((conceptIds, concept) => {
    if (!conceptsToCompare.find(con => con.conceptId === concept.conceptId && con.isStandard === concept.isStandard)) {
      conceptIds.push({conceptId: concept.conceptId, standard: concept.isStandard});
    }
    return conceptIds;
  }, []);
};

interface Props {
  workspace: WorkspaceData;
  concept: Array<any>;
  conceptSet: ConceptSet;
}

interface State {
  conceptAddModalOpen: boolean;
  updating: boolean;
}
export const  ConceptListPage = fp.flow(withCurrentWorkspace(), withCurrentConcept(), withCurrentConceptSet())(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        conceptAddModalOpen: false,
        updating: false
      };
    }

    async updateConceptSet() {
      const {concept, conceptSet, workspace: {namespace, id}} = this.props;
      conceptSetUpdating.next(true);
      this.setState({updating: true});
      // Selections that don't exist on the existing concept set are added
      const addedConceptSetConceptIds = getConceptIdsToAddOrRemove(concept, conceptSet.criteriums);
      // Concept ids on the existing concept set that don't exist on the selections get removed
      const removedConceptSetConceptIds = getConceptIdsToAddOrRemove(conceptSet.criteriums, concept);
      const updateConceptSetReq: UpdateConceptSetRequest = {
        etag: conceptSet.etag,
        addedConceptSetConceptIds,
        removedConceptSetConceptIds
      };
      try {
        const updatedConceptSet = await conceptSetsApi().updateConceptSetConcepts(namespace, id, conceptSet.id, updateConceptSetReq);
        currentConceptSetStore.next(updatedConceptSet);
        NavStore.navigate(['workspaces', namespace, id, 'data', 'concepts', 'sets', conceptSet.id, 'actions']);
      } catch (error) {
        console.error(error);
      }
    }

    removeSelection(conceptToDel) {
      const updatedConceptList = this.props.concept.filter((concept) => concept !== conceptToDel);
      localStorage.setItem(LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS, JSON.stringify(updatedConceptList));
      currentConceptStore.next(updatedConceptList);
    }

    afterConceptsSaved(conceptSet: ConceptSet) {
      const {namespace, id} = this.props.workspace;
      NavStore.navigate(['workspaces', namespace, id, 'data', 'concepts', 'sets', conceptSet.id, 'actions']);
    }

    get disableSaveConceptButton() {
      const {concept, conceptSet} = this.props;
      const {updating} = this.state;
      return updating || !concept || concept.length === 0 ||
        (!!conceptSet && JSON.stringify(conceptSet.criteriums.sort()) === JSON.stringify(concept.sort()));
    }

    getDomainCount() {
      const {domainId, type} = this.props.concept[0];
      const domain: Domain = domainId === 'Measurement' && type === 'PPI' ? Domain.PHYSICALMEASUREMENT : domainId as Domain;
      const domainCount: DomainCount = {
        domain: domain,
        name: domainToTitle(domain),
        conceptCount: this.props.concept.length
      };
      return domainCount;
    }

    onSaveConceptSetClick() {
      if (this.props.conceptSet) {
        this.updateConceptSet();
      } else {
        this.setState({conceptAddModalOpen: true});
      }
    }

    closeConceptAddModal() {
      this.setState({conceptAddModalOpen: false});
      setSidebarActiveIconStore.next(undefined);
    }

    renderSelection(selection: any, index: number) {
      return <FlexRow key={index} style={{lineHeight: '1.25rem'}}>
        <button style={styles.removeSelection} onClick={() => this.removeSelection(selection)}>
          <ClrIcon shape='times-circle'/>
        </button>
        <b style={{paddingRight: '0.25rem'}}>{selection.conceptCode}</b>
        {selection.name ? selection.name : selection.question}
      </FlexRow>;
    }

    renderSelections() {
      const {concept} = this.props;
      if ([Domain.CONDITION, Domain.PROCEDURE].includes(concept[0].domainId)) {
        // Separate selections by standard and source concepts for Condition and Procedures
        const standardConcepts = concept.filter(con => con.isStandard);
        const sourceConcepts = concept.filter(con => !con.isStandard);
        return <React.Fragment>
          {standardConcepts.length > 0 && <div style={{marginBottom: '0.5rem'}}>
            <div style={styles.selectionHeader}>Standard Concepts</div>
            {standardConcepts.map((con, index) => this.renderSelection(con, index))}
          </div>}
          {sourceConcepts.length > 0 && <div>
            <div style={styles.selectionHeader}>Source Concepts</div>
            {sourceConcepts.map((con, index) => this.renderSelection(con, index))}
          </div>}
        </React.Fragment>;
      } else {
        return concept.map((con, index) => this.renderSelection(con, index));
      }
    }

    render() {
      const {concept} = this.props;
      const {conceptAddModalOpen, updating} = this.state;
      return <div>
        <FlexRow>
          <h3 style={styles.sectionTitle}>Selected Concepts</h3>
          <Clickable style={{marginRight: '1rem', position: 'absolute', right: '0px'}}
                     onClick={() => setSidebarActiveIconStore.next(undefined)}>
            <img src={'/assets/icons/times-light.svg'}
                 style={{height: '27px', width: '17px'}}
                 alt='Close'/>
          </Clickable>
        </FlexRow>
        <div style={styles.selectionContainer}>
          {updating && <SpinnerOverlay/>}
          {!!concept && concept.length > 0 && this.renderSelections()}
        </div>
        <FlexRowWrap style={{flexDirection: 'row-reverse', marginTop: '1rem'}}>
          <Button type='primary'
                  style={styles.saveButton}
                  disabled={this.disableSaveConceptButton}
                  onClick={() => this.onSaveConceptSetClick()}>
            Save Concept Set
          </Button>
          <Button type='link'
                  style={{color: colors.primary, left: 0}}
                  onClick={() => setSidebarActiveIconStore.next(undefined)}>
            Close
          </Button>
        </FlexRowWrap>
        {conceptAddModalOpen && <ConceptAddModal activeDomainTab={this.getDomainCount()}
                         selectedConcepts={this.props.concept}
                         onSave={(conceptSet) => this.afterConceptsSaved(conceptSet)}
                         onClose={() => this.closeConceptAddModal()}/>}
        </div>;
    }
  });
