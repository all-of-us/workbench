import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Toast } from 'primereact/toast';

import {
  CriteriaType,
  Domain,
  TemporalMention,
  TemporalTime,
} from 'generated/fetch';

import { Button, Clickable } from 'app/components/buttons';
import { FlexRowWrap } from 'app/components/flex';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { AddConceptSetToCohortModal } from 'app/pages/data/cohort/addConceptSetToCohortModal';
import { ConceptQuickAddModal } from 'app/pages/data/cohort/concept-quick-add-modal';
import { Demographics } from 'app/pages/data/cohort/demographics';
import { searchRequestStore } from 'app/pages/data/cohort/search-state.service';
import { Selection } from 'app/pages/data/cohort/selection-list';
import {
  domainToTitle,
  generateId,
  typeToTitle,
} from 'app/pages/data/cohort/utils';
import {
  CriteriaSearch,
  LOCAL_STORAGE_KEY_COHORT_CONTEXT,
  toastCSS,
} from 'app/pages/data/criteria-search';
import colors, { addOpacity } from 'app/styles/colors';
import { reactStyles, withCurrentCohortSearchContext } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import {
  attributesSelectionStore,
  currentCohortCriteriaStore,
  currentCohortSearchContextStore,
  currentCohortStore,
  setSidebarActiveIconStore,
} from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';
import arrowIcon from 'assets/icons/arrow-left-regular.svg';
import { Subscription } from 'rxjs/Subscription';

const styles = reactStyles({
  arrowIcon: {
    height: '21px',
    marginTop: '-0.3rem',
    width: '18px',
  },
  backArrow: {
    background: `${addOpacity(colors.accent, 0.15)}`,
    borderRadius: '50%',
    display: 'inline-block',
    height: '2.25rem',
    lineHeight: '2.4rem',
    textAlign: 'center',
    width: '2.25rem',
  },
  finishButton: {
    marginTop: '2.25rem',
    borderRadius: '5px',
    bottom: '1.5rem',
    position: 'absolute',
    right: '4.5rem',
  },
  toast: {
    position: 'absolute',
    right: '0',
    top: 0,
  },
  searchContainer: {
    display: 'flex',
    flexWrap: 'wrap',
    height: '70vh',
    position: 'relative',
    width: '100%',
  },
  searchContent: {
    height: '100%',
    padding: '0 0.75rem',
    position: 'relative',
    width: '100%',
  },
  titleBar: {
    color: colors.primary,
    display: 'table',
    margin: '1.5rem 0 0.375rem',
    width: '65%',
    height: '2.25rem',
  },
  titleHeader: {
    display: 'inline-block',
    lineHeight: '2.25rem',
    margin: '0 0 0 1.125rem',
  },
});

function initGroup(role: string, item: any) {
  return {
    id: generateId(role),
    items: [item],
    count: null,
    temporal: false,
    mention: TemporalMention.ANYMENTION,
    time: TemporalTime.DURINGSAMEENCOUNTERAS,
    timeValue: '',
    timeFrame: '',
    isRequesting: false,
    status: 'active',
  };
}

export function getItemFromSearchRequest(
  groupId: string,
  itemId: string,
  role: string
) {
  const searchRequest = searchRequestStore.getValue();
  const groupIndex = searchRequest[role].findIndex((grp) => grp.id === groupId);
  return groupIndex > -1
    ? searchRequest[role][groupIndex].items.find((it) => it.id === itemId)
    : null;
}

export function saveCriteria(selections?: Array<Selection>) {
  const { domain, groupId, item, role } =
    currentCohortSearchContextStore.getValue();
  AnalyticsTracker.CohortBuilder.SaveCriteria(domainToTitle(domain));
  const searchRequest = searchRequestStore.getValue();
  if (domain === Domain.CONCEPTSET || domain === Domain.CONCEPTQUICKADD) {
    item.type = selections[0]?.domainId;
  }
  item.searchParameters = selections || currentCohortCriteriaStore.getValue();
  if (groupId) {
    const groupIndex = searchRequest[role].findIndex(
      (grp) => grp.id === groupId
    );
    if (groupIndex > -1) {
      const itemIndex = searchRequest[role][groupIndex].items.findIndex(
        (it) => it.id === item.id
      );
      if (itemIndex > -1) {
        searchRequest[role][groupIndex].items[itemIndex] = item;
      } else {
        searchRequest[role][groupIndex].items.push(item);
      }
    }
  } else {
    searchRequest[role].push(initGroup(role, item));
  }
  searchRequestStore.next(searchRequest);
  currentCohortSearchContextStore.next(undefined);
  currentCohortCriteriaStore.next(undefined);
}

interface Props extends RouteComponentProps<MatchParams> {
  cohortContext: any;
  selections?: Array<Selection>;
  setUnsavedChanges: (unsavedChanges: boolean) => void;
}

interface State {
  initCriteriaSearch: boolean;
  selectedIds: Array<string>;
  selections: Array<Selection>;
  showAddConceptSetModal: boolean;
  showConceptQuickAddModal: boolean;
  showUnsavedModal: boolean;
  toastVisible: boolean;
  unsavedChanges: boolean;
}

export const CohortSearch = fp.flow(
  withCurrentCohortSearchContext(),
  withRouter
)(
  class extends React.Component<Props, State> {
    toast: any;
    toastTimer: NodeJS.Timer;
    subscription: Subscription;
    constructor(props: Props) {
      super(props);
      this.state = {
        initCriteriaSearch: false,
        selectedIds: [],
        selections: [],
        showAddConceptSetModal: false,
        showConceptQuickAddModal: false,
        showUnsavedModal: false,
        toastVisible: false,
        unsavedChanges: false,
      };
    }

    componentDidMount(): void {
      const {
        cohortContext: { domain, item, type },
      } = this.props;
      // JSON stringify and parse prevents changes to selections from being passed to the cohortContext
      const selections = JSON.parse(JSON.stringify(item.searchParameters));
      if (type === CriteriaType.DECEASED) {
        this.selectDeceased();
      } else if (domain === Domain.FITBIT) {
        this.selectFitbit();
      } else if (domain === Domain.WHOLEGENOMEVARIANT) {
        this.selectGenome();
      } else if (domain === Domain.LRWHOLEGENOMEVARIANT) {
        this.selectLongReadGenome();
      } else if (domain === Domain.ARRAYDATA) {
        this.selectArrayData();
      } else if (domain === Domain.STRUCTURALVARIANTDATA) {
        this.selectStructuralVariantData();
      } else if (domain === Domain.CONCEPTSET) {
        this.setState({ showAddConceptSetModal: true });
      } else if (domain === Domain.CONCEPTQUICKADD) {
        this.setState({ showConceptQuickAddModal: true });
      } else {
        this.setState({ initCriteriaSearch: true });
      }

      currentCohortCriteriaStore.next(selections);
      this.subscription = currentCohortCriteriaStore.subscribe(
        (newSelections) => {
          if (!!newSelections) {
            this.setState(
              {
                selectedIds: newSelections.map((s) => s.parameterId),
                selections: newSelections,
              },
              () => this.setUnsavedChanges()
            );
          }
        }
      );
      // Check for changes when the search context changes, mainly to detect modifier changes
      this.subscription.add(
        currentCohortSearchContextStore.subscribe(() =>
          this.setUnsavedChanges()
        )
      );
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
      currentCohortCriteriaStore.next(undefined);
      localStorage.removeItem(LOCAL_STORAGE_KEY_COHORT_CONTEXT);
    }

    closeSearch() {
      currentCohortSearchContextStore.next(undefined);
      // Delay hiding attributes page until sidebar is closed
      setTimeout(() => attributesSelectionStore.next(undefined), 500);
    }

    setUnsavedChanges() {
      const {
        cohortContext: { groupId, item, role },
      } = this.props;
      const { selections } = this.state;
      let unsavedChanges = selections.length > 0;
      if (groupId) {
        const requestItem = getItemFromSearchRequest(groupId, item.id, role);
        if (requestItem) {
          // Use clone to compare to prevent passing changes back to actual selections
          const selectionsClone = JSON.parse(JSON.stringify(selections));
          const sortAndStringify = (params) =>
            JSON.stringify(params.sort((a, b) => a.id - b.id));
          if (this.criteriaIdsLookedUp(requestItem.searchParameters)) {
            // If a lookup has been done, we delete the ids before comparing search parameters and selections
            selectionsClone.forEach((selection) => delete selection.id);
          }
          unsavedChanges =
            sortAndStringify(requestItem.searchParameters) !==
              sortAndStringify(selectionsClone) ||
            JSON.stringify(item.modifiers) !==
              JSON.stringify(requestItem.modifiers);
        }
      }
      this.setState({ unsavedChanges });
      this.props.setUnsavedChanges(unsavedChanges);
    }

    checkUnsavedChanges() {
      const { unsavedChanges } = this.state;
      if (unsavedChanges) {
        this.setState({ showUnsavedModal: true });
      } else {
        this.closeSearch();
      }
    }

    // Checks if a lookup has been done to add the criteria ids to the selections
    criteriaIdsLookedUp(searchParameters: Array<Selection>) {
      const { selections } = this.state;
      return (
        selections.length &&
        searchParameters.length &&
        selections.some((sel) => !!sel.id) &&
        searchParameters.some((sel) => !sel.id)
      );
    }

    addSelection = (param: any) => {
      const {
        cohortContext,
        match: {
          params: { wsid },
        },
      } = this.props;
      let { selectedIds, selections } = this.state;
      if (selectedIds.includes(param.parameterId)) {
        selections = selections.filter(
          (p) => p.parameterId !== param.parameterId
        );
      } else {
        selectedIds = [...selectedIds, param.parameterId];
      }
      selections = [...selections, param];
      currentCohortCriteriaStore.next(selections);
      const cohort = currentCohortStore.getValue();
      cohortContext.item.searchParameters = selections;
      const localStorageContext = {
        workspaceId: wsid,
        cohortId: !!cohort ? cohort.id : null,
        cohortContext,
      };
      localStorage.setItem(
        LOCAL_STORAGE_KEY_COHORT_CONTEXT,
        JSON.stringify(localStorageContext)
      );
      this.setState({ selections, selectedIds });
      this.toast.show({
        severity: 'success',
        detail: 'Criteria Added',
        closable: false,
        life: 2000,
      });
      if (!!this.toastTimer) {
        clearTimeout(this.toastTimer);
      }
      // This is to set style display: 'none' on the toast so it doesn't block the nav icons in the sidebar
      this.toastTimer = global.setTimeout(
        () => this.setState({ toastVisible: false }),
        2500
      );
      this.setState({ toastVisible: true });
    };

    selectDeceased() {
      const param = {
        id: null,
        parentId: null,
        parameterId: '',
        type: CriteriaType.DECEASED.toString(),
        name: 'Deceased',
        group: false,
        domainId: Domain.PERSON.toString(),
        hasAttributes: false,
        selectable: true,
        attributes: [],
      } as Selection;
      AnalyticsTracker.CohortBuilder.SelectDemographics('Select Deceased');
      saveCriteria([param]);
    }

    selectFitbit() {
      const param = {
        id: null,
        parentId: null,
        parameterId: '',
        type: CriteriaType.PPI.toString(),
        name: 'Has any Fitbit data',
        group: false,
        domainId: Domain.FITBIT.toString(),
        hasAttributes: false,
        selectable: true,
        attributes: [],
      } as Selection;
      AnalyticsTracker.CohortBuilder.SelectCriteria(
        'Select Has any Fitbit data'
      );
      saveCriteria([param]);
    }

    selectGenome() {
      const param = {
        id: null,
        parentId: null,
        parameterId: '',
        type: CriteriaType.PPI.toString(),
        name: 'Short Read WGS',
        group: false,
        domainId: Domain.WHOLEGENOMEVARIANT.toString(),
        hasAttributes: false,
        selectable: true,
        attributes: [],
      } as Selection;
      AnalyticsTracker.CohortBuilder.SelectCriteria('Select Short Read WGS');
      saveCriteria([param]);
    }

    selectLongReadGenome() {
      const param = {
        id: null,
        parentId: null,
        parameterId: '',
        type: CriteriaType.PPI.toString(),
        name: 'Long Read WGS',
        group: false,
        domainId: Domain.LRWHOLEGENOMEVARIANT.toString(),
        hasAttributes: false,
        selectable: true,
        attributes: [],
      } as Selection;
      AnalyticsTracker.CohortBuilder.SelectCriteria('Select Long Read WGS');
      saveCriteria([param]);
    }

    selectArrayData() {
      const param = {
        id: null,
        parentId: null,
        parameterId: '',
        type: '',
        name: 'Global Diversity Array',
        group: false,
        domainId: Domain.ARRAYDATA.toString(),
        hasAttributes: false,
        selectable: true,
        attributes: [],
      } as Selection;
      AnalyticsTracker.CohortBuilder.SelectCriteria(
        'Select Global Diversity Array'
      );
      saveCriteria([param]);
    }

    selectStructuralVariantData() {
      const param = {
        id: null,
        parentId: null,
        parameterId: '',
        type: CriteriaType.PPI.toString(),
        name: 'Structural Variant Data',
        group: false,
        domainId: Domain.STRUCTURALVARIANTDATA.toString(),
        hasAttributes: false,
        selectable: true,
        attributes: [],
      } as Selection;
      AnalyticsTracker.CohortBuilder.SelectCriteria(
        'Select Structural Variant Data'
      );
      saveCriteria([param]);
    }

    render() {
      const {
        cohortContext,
        cohortContext: { domain, type },
      } = this.props;
      const {
        initCriteriaSearch,
        selectedIds,
        selections,
        showAddConceptSetModal,
        showConceptQuickAddModal,
        showUnsavedModal,
        toastVisible,
      } = this.state;
      return (
        !!cohortContext && (
          <>
            {showAddConceptSetModal || showConceptQuickAddModal ? (
              <>
                {showAddConceptSetModal && (
                  <AddConceptSetToCohortModal
                    onClose={() =>
                      currentCohortSearchContextStore.next(undefined)
                    }
                  />
                )}
                {showConceptQuickAddModal && (
                  <ConceptQuickAddModal
                    onClose={() =>
                      currentCohortSearchContextStore.next(undefined)
                    }
                  />
                )}
              </>
            ) : (
              <FlexRowWrap style={styles.searchContainer}>
                <style>{toastCSS}</style>
                <Toast
                  ref={(el) => (this.toast = el)}
                  style={
                    !toastVisible
                      ? { ...styles.toast, display: 'none' }
                      : styles.toast
                  }
                />
                <div id='cohort-search-container' style={styles.searchContent}>
                  {domain === Domain.PERSON && (
                    <div style={styles.titleBar}>
                      <Clickable
                        data-test-id='cohort-search-back-arrow'
                        style={styles.backArrow}
                        onClick={() => this.checkUnsavedChanges()}
                      >
                        <img
                          src={arrowIcon}
                          style={styles.arrowIcon}
                          alt='Go back'
                        />
                      </Clickable>
                      <h2 style={styles.titleHeader}>{typeToTitle(type)}</h2>
                    </div>
                  )}
                  <div
                    style={
                      domain === Domain.PERSON && type !== CriteriaType.AGE
                        ? { marginBottom: '5.25rem' }
                        : { height: 'calc(100% - 5.25rem)' }
                    }
                  >
                    {domain === Domain.PERSON ? (
                      <div data-test-id='demographics'>
                        <Demographics
                          criteriaType={type}
                          select={this.addSelection}
                          selectedIds={selectedIds}
                          selections={selections}
                        />
                      </div>
                    ) : (
                      initCriteriaSearch && (
                        <CriteriaSearch
                          backFn={() => this.checkUnsavedChanges()}
                          cohortContext={cohortContext}
                          conceptSearchTerms={cohortContext.searchTerms}
                        />
                      )
                    )}
                  </div>
                </div>
                <Button
                  type='primary'
                  style={styles.finishButton}
                  disabled={!!selectedIds && selectedIds.length === 0}
                  onClick={() => setSidebarActiveIconStore.next('criteria')}
                >
                  Finish & Review
                </Button>
                {showUnsavedModal && (
                  <Modal>
                    <ModalTitle>Warning! </ModalTitle>
                    <ModalBody data-test-id='cohort-search-unsaved-message'>
                      Your cohort has not been saved. If you’d like to save your
                      cohort criteria, please click CANCEL and save your changes
                      in the right sidebar.
                    </ModalBody>
                    <ModalFooter>
                      <Button
                        type='link'
                        onClick={() =>
                          this.setState({ showUnsavedModal: false })
                        }
                      >
                        Cancel
                      </Button>
                      <Button type='primary' onClick={() => this.closeSearch()}>
                        Discard Changes
                      </Button>
                    </ModalFooter>
                  </Modal>
                )}
              </FlexRowWrap>
            )}
          </>
        )
      );
    }
  }
);
