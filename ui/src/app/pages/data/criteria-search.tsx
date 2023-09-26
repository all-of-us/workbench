import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Toast } from 'primereact/toast';

import { Criteria, Domain } from 'generated/fetch';

import { environment } from 'environments/environment';
import { Clickable, StyledExternalLink } from 'app/components/buttons';
import { FlexRowWrap } from 'app/components/flex';
import { SpinnerOverlay } from 'app/components/spinners';
import { AoU } from 'app/components/text-wrappers';
import { ListSearch } from 'app/pages/data/cohort/list-search';
import { Selection } from 'app/pages/data/cohort/selection-list';
import { CriteriaTree } from 'app/pages/data/cohort/tree';
import { domainToTitle, typeToTitle } from 'app/pages/data/cohort/utils';
import { VariantSearch } from 'app/pages/data/cohort/variant-search';
import colors, { addOpacity, colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import {
  attributesSelectionStore,
  currentCohortCriteriaStore,
  currentCohortStore,
  currentConceptStore,
  setSidebarActiveIconStore,
} from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';
import arrowIcon from 'assets/icons/arrow-left-regular.svg';
import { Subscription } from 'rxjs/Subscription';

export const LOCAL_STORAGE_KEY_COHORT_CONTEXT = 'CURRENT_COHORT_CONTEXT';
export const LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS =
  'CURRENT_CRITERIA_SELECTIONS';

const styles = reactStyles({
  arrowIcon: {
    height: '21px',
    marginTop: '-0.3rem',
    width: '18px',
  },
  backArrow: {
    background: `${addOpacity(colors.accent, 0.15)}`,
    borderRadius: '50%',
    height: '2.25rem',
    lineHeight: '2.4rem',
    textAlign: 'center',
    width: '2.25rem',
  },
  detailExternalLinks: {
    width: '100%',
    lineHeight: '1.125rem',
    textAlign: 'right',
    verticalAlign: 'middle',
  },
  externalLinks: {
    flex: '0 0 calc(55% - 1.875rem)',
    maxWidth: 'calc(55% - 1.875rem)',
    lineHeight: '1.125rem',
    textAlign: 'right',
    verticalAlign: 'middle',
  },
  toast: {
    position: 'absolute',
    right: '0',
    top: 0,
  },
  loadingSubTree: {
    height: '100%',
    minHeight: '22.5rem',
    pointerEvents: 'none',
    opacity: 0.3,
  },
  titleBar: {
    alignItems: 'center',
    color: colors.primary,
    margin: '0 0.375rem',
    width: '80%',
    height: '3rem',
  },
  titleHeader: {
    flex: '0 0 calc(45% - 1.5rem)',
    maxWidth: 'calc(45% - 1.5rem)',
    lineHeight: '1.5rem',
    margin: '0 0 0 1.125rem',
  },
});
export const toastCSS =
  `
  .p-toast {
    position: sticky;
  }
  .p-toast.p-toast-topright {
    height: 1.5rem;
    width: 9.6rem;
    line-height: 1.05rem;
  }
  .p-toast .p-toast-item-container .p-toast-item .p-toast-image {
    font-size: 1.5rem !important;
    margin-top: 0.285rem
  }
  .p-toast-item-container:after {
    content:"";
    position: absolute;
    left: 97.5%;
    top: 0.15rem;
    width: 0px;
    height: 0px;
    border-top: 0.75rem solid transparent;
    border-left: 0.75rem solid ` +
  colorWithWhiteness(colors.success, 0.6) +
  `;
    border-bottom: 0.75rem solid transparent;
  }
  .p-toast-item-container {
    background-color: ` +
  colorWithWhiteness(colors.success, 0.6) +
  `!important;
  }
  .p-toast-item {
    padding: 0rem !important;
    background-color: ` +
  colorWithWhiteness(colors.success, 0.6) +
  `!important;
    margin-left: 0.45rem;
  }
  .p-toast-message {
    margin-left: 0.5em
  }
  .p-toast-details {
    margin-top: 0.15rem;
  }
 `;

interface Props extends RouteComponentProps<MatchParams> {
  backFn?: () => void;
  cohortContext: any;
  conceptSearchTerms?: string;
}

interface State {
  backMode: string;
  autocompleteSelection: Criteria;
  toastVisible: boolean;
  groupSelections: Array<number>;
  hierarchyNode: Criteria;
  mode: string;
  selections: Array<Selection>;
  selectedCriteriaList: Array<any>;
  selectedIds: Array<string>;
  treeSearchTerms: string;
  loadingSubtree: boolean;
}

export const CriteriaSearch = fp.flow(
  withCurrentWorkspace(),
  withRouter
)(
  class extends React.Component<Props, State> {
    toast: any;
    toastTimer: NodeJS.Timer;
    subscription: Subscription;

    constructor(props: Props) {
      super(props);
      this.state = {
        autocompleteSelection: undefined,
        backMode: 'list',
        toastVisible: false,
        hierarchyNode: undefined,
        groupSelections: [],
        mode: 'list',
        selectedIds: [],
        selections: [],
        selectedCriteriaList: [],
        treeSearchTerms:
          props.cohortContext.source !== 'cohort'
            ? props.conceptSearchTerms
            : '',
        loadingSubtree: false,
      };
    }

    componentDidMount(): void {
      const {
        cohortContext: { domain, standard, source, type },
      } = this.props;
      if (this.initTree) {
        this.setState({
          backMode: 'tree',
          hierarchyNode: {
            domainId: domain,
            type: type,
            standard: standard,
            id: 0,
          } as Criteria,
          mode: 'tree',
        });
      }
      const currentCriteriaStore =
        source === 'cohort' ? currentCohortCriteriaStore : currentConceptStore;
      this.subscription = currentCriteriaStore.subscribe(
        (selectedCriteriaList) => this.setState({ selectedCriteriaList })
      );
      if (source !== 'cohort') {
        const existingCriteria = JSON.parse(
          localStorage.getItem(LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS)
        );
        if (!!existingCriteria && existingCriteria[0].domainId === domain) {
          currentCriteriaStore.next(existingCriteria);
        }
      }
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
    }

    get initTree() {
      const {
        cohortContext: { domain, source },
        conceptSearchTerms,
      } = this.props;
      return (
        (domain === Domain.VISIT ||
          (source === 'cohort' && domain === Domain.PHYSICAL_MEASUREMENT) ||
          (source === 'cohort' && domain === Domain.SURVEY)) &&
        !conceptSearchTerms
      );
    }

    get isConcept() {
      const {
        cohortContext: { source },
      } = this.props;
      return source === 'concept' || source === 'conceptSetDetails';
    }

    getToastStyle() {
      return !this.isConcept
        ? styles.toast
        : { ...styles.toast, marginRight: '3.75rem', paddingTop: '4.125rem' };
    }

    searchContentStyle(mode: string) {
      let style = {
        display: 'none',
        flex: 1,
        minWidth: '21rem',
        overflowY: 'auto',
        overflowX: 'hidden',
        width: '100%',
        height: '100%',
      } as React.CSSProperties;
      if (this.state.mode === mode) {
        style = { ...style, display: 'block', animation: 'fadeEffect 1s' };
      }
      return style;
    }

    showHierarchy = (criterion: Criteria) => {
      this.setState({
        autocompleteSelection: criterion,
        backMode: 'tree',
        hierarchyNode: { ...criterion, id: 0 },
        mode: 'tree',
        loadingSubtree: true,
        treeSearchTerms: criterion.name,
      });
    };

    closeSidebar() {
      attributesSelectionStore.next(undefined);
      setSidebarActiveIconStore.next(null);
    }

    addSelection = (selectCriteria) => {
      const {
        cohortContext,
        cohortContext: { source },
        match: { params },
      } = this.props;
      const { selectedCriteriaList } = this.state;
      // In case of Criteria/Cohort, close existing attribute sidebar before selecting a new value
      if (!this.isConcept && !!attributesSelectionStore.getValue()) {
        this.closeSidebar();
      }
      const criteriaList =
        selectedCriteriaList && selectedCriteriaList.length > 0
          ? [...selectedCriteriaList, selectCriteria]
          : [selectCriteria];
      // Save selections in local storage in case of error or page refresh
      if (source === 'cohort') {
        const { wsid } = params;
        const cohort = currentCohortStore.getValue();
        cohortContext.item.searchParameters = criteriaList;
        const localStorageContext = {
          workspaceId: wsid,
          cohortId: !!cohort ? cohort.id : null,
          cohortContext,
        };
        localStorage.setItem(
          LOCAL_STORAGE_KEY_COHORT_CONTEXT,
          JSON.stringify(localStorageContext)
        );
      } else {
        localStorage.setItem(
          LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS,
          JSON.stringify(criteriaList)
        );
      }
      this.setState({ selectedCriteriaList: criteriaList });
      this.isConcept
        ? currentConceptStore.next(criteriaList)
        : currentCohortCriteriaStore.next(criteriaList);
      const toastMessage = this.isConcept ? 'Concept Added' : 'Criteria Added';
      this.toast.show({
        severity: 'success',
        detail: toastMessage,
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

    getListSearchSelectedIds() {
      const {
        cohortContext: { source },
      } = this.props;
      const { selectedCriteriaList } = this.state;
      return source === 'cohort'
        ? fp.map((selected) => selected.parameterId, selectedCriteriaList)
        : fp.map(
            (selected) =>
              'param' + selected.conceptId + selected.code + selected.standard,
            selectedCriteriaList
          );
    }

    setScroll = (id: string) => {
      const nodeId = `node${id}`;
      const node = document.getElementById(nodeId);
      if (node) {
        node.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
      this.setState({ loadingSubtree: false });
    };

    back = () => {
      if (this.state.mode === 'tree') {
        this.setState({
          autocompleteSelection: undefined,
          backMode: 'list',
          hierarchyNode: undefined,
          mode: 'list',
        });
      } else {
        attributesSelectionStore.next(undefined);
        this.setState({ mode: this.state.backMode });
      }
    };

    setTreeSearchTerms = (input: string) => {
      this.setState({ treeSearchTerms: input });
    };

    setAutocompleteSelection = (selection: any) => {
      this.setState({ loadingSubtree: true, autocompleteSelection: selection });
    };

    get showDataBrowserLink() {
      return [
        Domain.CONDITION,
        Domain.PROCEDURE,
        Domain.MEASUREMENT,
        Domain.DRUG,
      ].includes(this.props.cohortContext.domain);
    }

    get domainTitle() {
      const {
        cohortContext: { domain, type, selectedSurvey },
      } = this.props;
      if (!!selectedSurvey) {
        return selectedSurvey;
      } else {
        return domain === Domain.PERSON
          ? typeToTitle(type)
          : domainToTitle(domain);
      }
    }

    render() {
      const {
        backFn,
        cohortContext,
        cohortContext: { domain, selectedSurvey, source },
        conceptSearchTerms,
      } = this.props;
      const {
        autocompleteSelection,
        groupSelections,
        hierarchyNode,
        loadingSubtree,
        treeSearchTerms,
        toastVisible,
      } = this.state;
      return (
        <div id='criteria-search-container'>
          {loadingSubtree && <SpinnerOverlay />}
          <Toast
            ref={(el) => (this.toast = el)}
            style={
              !toastVisible
                ? { ...styles.toast, display: 'none' }
                : styles.toast
            }
          />
          <FlexRowWrap
            style={{
              ...styles.titleBar,
              marginTop: source === 'cohort' ? '1.5rem' : 0,
            }}
          >
            {source !== 'conceptSetDetails' && (
              <React.Fragment>
                <Clickable style={styles.backArrow} onClick={() => backFn()}>
                  <img src={arrowIcon} style={styles.arrowIcon} alt='Go back' />
                </Clickable>
                <h2 style={styles.titleHeader}>{this.domainTitle}</h2>
              </React.Fragment>
            )}
            <div
              style={
                source === 'conceptSetDetails'
                  ? styles.detailExternalLinks
                  : styles.externalLinks
              }
            >
              {domain === Domain.DRUG && (
                <div>
                  <StyledExternalLink
                    href='https://mor.nlm.nih.gov/RxNav/'
                    target='_blank'
                    rel='noopener noreferrer'
                  >
                    Explore
                  </StyledExternalLink>
                  &nbsp;drugs by brand names outside of <AoU />
                </div>
              )}
              {domain === Domain.SURVEY && (
                <div>
                  Find more information about each survey in the&nbsp;
                  <StyledExternalLink
                    href='https://www.researchallofus.org/survey-explorer/'
                    target='_blank'
                    rel='noopener noreferrer'
                  >
                    Survey Explorer
                  </StyledExternalLink>
                </div>
              )}
              {this.showDataBrowserLink && (
                <div>
                  Explore Source information on the&nbsp;
                  <StyledExternalLink
                    href={environment.publicUiUrl}
                    target='_blank'
                    rel='noopener noreferrer'
                  >
                    Data Browser
                  </StyledExternalLink>
                </div>
              )}
            </div>
          </FlexRowWrap>
          <div
            style={
              loadingSubtree
                ? styles.loadingSubTree
                : { height: '100%', minHeight: '22.5rem' }
            }
          >
            <style>{toastCSS}</style>
            <Toast
              ref={(el) => (this.toast = el)}
              style={
                !toastVisible
                  ? { ...this.getToastStyle(), display: 'none' }
                  : this.getToastStyle()
              }
            />
            {hierarchyNode && (
              <CriteriaTree
                autocompleteSelection={autocompleteSelection}
                back={this.back}
                domain={domain}
                groupSelections={groupSelections}
                node={hierarchyNode}
                scrollToMatch={this.setScroll}
                searchTerms={treeSearchTerms}
                select={this.addSelection}
                selectedIds={this.getListSearchSelectedIds()}
                selectedSurvey={selectedSurvey}
                selectOption={this.setAutocompleteSelection}
                setSearchTerms={this.setTreeSearchTerms}
                source={source}
              />
            )}
            {/* List View (using duplicated version of ListSearch) */}
            {!this.initTree &&
            cohortContext.domain === Domain.SNP_INDEL_VARIANT ? (
              <VariantSearch
                searchTerms={conceptSearchTerms}
                select={this.addSelection}
                selectedIds={this.getListSearchSelectedIds()}
              />
            ) : (
              <div style={this.searchContentStyle('list')}>
                <ListSearch
                  hierarchy={this.showHierarchy}
                  searchContext={cohortContext}
                  searchTerms={conceptSearchTerms}
                  select={this.addSelection}
                  selectedIds={this.getListSearchSelectedIds()}
                />
              </div>
            )}
          </div>
        </div>
      );
    }
  }
);
