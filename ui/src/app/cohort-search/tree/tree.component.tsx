import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  CdrVersionTiersResponse,
  Criteria,
  CriteriaSubType,
  CriteriaType,
  Domain,
} from 'generated/fetch';

import { SearchBar } from 'app/cohort-search/search-bar/search-bar.component';
import { ppiSurveys } from 'app/cohort-search/search-state.service';
import { TreeNode } from 'app/cohort-search/tree-node/tree-node.component';
import { ClrIcon } from 'app/components/icons';
import { CheckBox } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  reactStyles,
  withCdrVersions,
  withCurrentConcept,
  withCurrentWorkspace,
} from 'app/utils';
import { getCdrVersion } from 'app/utils/cdr-versions';
import {
  currentCohortCriteriaStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';
import arrow from 'assets/icons/arrow-left-regular.svg';

const styles = reactStyles({
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: `1px solid ${colorWithWhiteness(colors.danger, 0.5)}`,
    borderRadius: '5px',
    marginTop: '0.25rem',
    padding: '8px',
  },
  ingredients: {
    float: 'left',
    fontWeight: 'bold',
    padding: '0.5rem',
  },
  arrowIcon: {
    height: '0.7rem',
    marginTop: '-0.2rem',
    width: '0.7rem',
    marginRight: '0.5rem',
  },
  returnLink: {
    backgroundColor: colorWithWhiteness(colors.accent, 0.87),
    border: 0,
    color: colors.accent,
    cursor: 'pointer',
    float: 'left',
    fontSize: '14px',
    height: '2rem',
    padding: '0 0.5rem',
    marginRight: '0.7rem',
    fontWeight: 600,
  },
  searchBarContainer: {
    width: '80%',
    marginTop: '-1px',
    display: 'flex',
    padding: '0.4rem 0',
    backgroundColor: 'transparent',
    zIndex: 1,
  },
  treeHeader: {
    position: 'sticky',
    top: 0,
    overflow: 'auto',
    background: colorWithWhiteness(colors.black, 0.97),
    borderBottom: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
  },
  node: {
    height: '16rem',
    overflow: 'auto',
    border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
    borderTop: 'none',
  },
  orCircle: {
    backgroundColor: colors.primary,
    borderRadius: '50%',
    color: colors.white,
    width: '1.25rem',
    height: '1.25rem',
    margin: '0.5rem 2rem',
    lineHeight: '1.25rem',
    textAlign: 'center',
    fontSize: '11px',
    fontWeight: 600,
  },
});

const scrollbarCSS = `
  .show-scrollbar::-webkit-scrollbar {
    -webkit-appearance: none;
    width: 7px;
  }
  .show-scrollbar::-webkit-scrollbar-thumb {
    border-radius: 4px;
    background-color: rgba(0, 0, 0, .5);
    box-shadow: 0 0 1px rgba(255, 255, 255, .5);
  }
`;

const ANY_PM_SELECTION = {
  parameterId: 'hasAnyPM',
  domainId: Domain.PHYSICALMEASUREMENT.toString(),
  group: false,
  hasAncestorData: false,
  isStandard: false,
  name: 'Has Any Physical Measurements Data',
  type: CriteriaType.PPI.toString(),
  attributes: [],
};

interface Props {
  autocompleteSelection: any;
  back: Function;
  domain: Domain;
  groupSelections: Array<number>;
  node: Criteria;
  source?: string;
  scrollToMatch: Function;
  searchTerms: string;
  select: Function;
  selectedSurvey?: string;
  selectedIds: Array<string>;
  selectOption: Function;
  setAttributes?: Function;
  setSearchTerms: Function;
  concept: Array<any>;
  workspace: WorkspaceData;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
}

interface State {
  autocompleteSelection: any;
  children: any;
  error: boolean;
  hasAnyPM: boolean;
  ingredients: any;
  loading: boolean;
}

export const CriteriaTree = fp.flow(
  withCurrentWorkspace(),
  withCurrentConcept(),
  withCdrVersions()
)(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        autocompleteSelection: undefined,
        children: undefined,
        error: false,
        hasAnyPM: false,
        ingredients: undefined,
        loading: true,
      };
    }

    componentDidMount(): void {
      const { domain, selectedIds } = this.props;
      this.loadRootNodes();
      if (domain === Domain.PHYSICALMEASUREMENT && selectedIds.length) {
        // Set hasAnyPM if editing a PM item
        this.setState({
          hasAnyPM: selectedIds.includes(ANY_PM_SELECTION.parameterId),
        });
      }
    }

    componentDidUpdate(prevProps: Readonly<Props>) {
      const { concept, domain, selectedIds, source } = this.props;
      if (source === 'conceptSetDetails') {
        if (prevProps.concept !== concept) {
          const { cdrVersionId } = currentWorkspaceStore.getValue();
          this.setState({ children: concept, loading: false });
          if (domain === Domain.SURVEY) {
            const rootSurveys = ppiSurveys.getValue();
            if (!rootSurveys[cdrVersionId]) {
              rootSurveys[cdrVersionId] = concept;
              ppiSurveys.next(rootSurveys);
            }
          }
        }
      }
      if (
        domain === Domain.PHYSICALMEASUREMENT &&
        prevProps.selectedIds !== selectedIds
      ) {
        // Check for hasAnyPM changes if in PM tree and selections have changed
        this.setState({
          hasAnyPM: selectedIds.includes(ANY_PM_SELECTION.parameterId),
        });
      }
    }

    sendOnlyCriteriaType(domain: Domain) {
      return domain === Domain.VISIT || domain === Domain.PHYSICALMEASUREMENT;
    }

    async loadRootNodes() {
      try {
        const {
          domain,
          node: { id, isStandard, parentId, subtype, type },
          selectedSurvey,
        } = this.props;
        this.setState({ loading: true });
        const {
          cdrVersionId,
          id: workspaceId,
          namespace,
        } = currentWorkspaceStore.getValue();
        const criteriaType =
          domain === Domain.DRUG ? CriteriaType.ATC.toString() : type;
        const promises = this.sendOnlyCriteriaType(domain)
          ? [
              cohortBuilderApi().findCriteriaBy(
                namespace,
                workspaceId,
                domain.toString(),
                criteriaType
              ),
            ]
          : [
              cohortBuilderApi().findCriteriaBy(
                namespace,
                workspaceId,
                domain.toString(),
                criteriaType,
                isStandard,
                id
              ),
            ];
        if (this.criteriaLookupNeeded) {
          const criteriaRequest = {
            sourceConceptIds: currentCohortCriteriaStore
              .getValue()
              .filter((s) => !s.isStandard)
              .map((s) => s.conceptId),
            standardConceptIds: currentCohortCriteriaStore
              .getValue()
              .filter((s) => s.isStandard)
              .map((s) => s.conceptId),
          };
          promises.push(
            cohortBuilderApi().findCriteriaForCohortEdit(
              namespace,
              workspaceId,
              domain.toString(),
              criteriaRequest
            )
          );
        }
        const [rootNodes, criteriaLookup] = await Promise.all(promises);
        if (criteriaLookup) {
          this.updateCriteriaSelectionStore(criteriaLookup.items);
        }
        if (domain === Domain.PHYSICALMEASUREMENT || domain === Domain.VISIT) {
          let children = [];
          const rootParentId = domain === Domain.VISIT ? -1 : 0;
          rootNodes.items.forEach((child) => {
            // Property 'children' does not exist on type 'Criteria'
            // TODO RW-5572 confirm proper behavior and fix
            // eslint-disable-next-line @typescript-eslint/dot-notation
            child['children'] = [];
            if (child.parentId === rootParentId) {
              children.push(child);
            } else {
              children = this.addChildToParent(child, children);
            }
          });
          this.setState({ children });
        } else if (domain === Domain.SURVEY && selectedSurvey) {
          // Temp: This should be handle in API
          this.updatePpiSurveys(
            rootNodes,
            rootNodes.items.filter((child) => child.name === selectedSurvey)
          );
        } else if (
          domain === Domain.SURVEY &&
          this.props.source === 'conceptSetDetails'
        ) {
          this.updatePpiSurveys(
            rootNodes,
            rootNodes.items.filter((child) => child.id === parentId)
          );
        } else {
          this.setState({
            children:
              domain === Domain.MEASUREMENT
                ? // For Measurements, only show the subtype of the node selected in list search and don't display the code
                  rootNodes.items
                    .filter((node) => node.subtype === subtype)
                    .map((node) => ({ ...node, code: null }))
                : rootNodes.items,
          });
          if (domain === Domain.SURVEY) {
            const rootSurveys = ppiSurveys.getValue();
            if (!rootSurveys[cdrVersionId]) {
              rootSurveys[cdrVersionId] = rootNodes.items;
              ppiSurveys.next(rootSurveys);
            }
          }
        }
      } catch (error) {
        console.error(error);
        this.setState({ error: true });
      } finally {
        this.setState({ loading: false });
      }
    }

    get criteriaLookupNeeded() {
      return (
        this.props.source === 'cohort' &&
        ![Domain.PHYSICALMEASUREMENT, Domain.VISIT].includes(
          this.props.domain
        ) &&
        currentCohortCriteriaStore.getValue().some((crit) => !crit.id)
      );
    }

    updateCriteriaSelectionStore(criteriaLookupItems: Criteria[]) {
      const { domain } = this.props;
      const updatedSelections = currentCohortCriteriaStore
        .getValue()
        .map((sel) => {
          const criteriaMatch = criteriaLookupItems.find(
            (item) =>
              item.conceptId === sel.conceptId &&
              item.isStandard === sel.isStandard &&
              (domain !== Domain.SURVEY || item.subtype === sel.subtype) &&
              (sel.subtype !== CriteriaSubType.ANSWER.toString() ||
                item.value === sel.code)
          );
          if (criteriaMatch) {
            sel.id = criteriaMatch.id;
          }
          return sel;
        });
      currentCohortCriteriaStore.next(updatedSelections);
    }

    updatePpiSurveys(resp, selectedSurveyChild) {
      const {
        node: { domainId, isStandard, type },
      } = this.props;
      const { cdrVersionId, id, namespace } = currentWorkspaceStore.getValue();
      if (selectedSurveyChild && selectedSurveyChild.length > 0) {
        cohortBuilderApi()
          .findCriteriaBy(
            namespace,
            id,
            domainId,
            type,
            isStandard,
            selectedSurveyChild[0].id
          )
          .then((surveyResponse) =>
            this.setState({ children: surveyResponse.items })
          );
      } else {
        this.setState({ children: resp.items });
        const rootSurveys = ppiSurveys.getValue();
        if (!rootSurveys[cdrVersionId]) {
          rootSurveys[cdrVersionId] = resp.items;
          ppiSurveys.next(rootSurveys);
        }
      }
    }

    addChildToParent(child, nodeList) {
      for (const node of nodeList) {
        if (!node.group) {
          continue;
        }
        if (node.id === child.parentId) {
          node.children.push(child);
          return nodeList;
        }
        if (node.children.length) {
          const nodeChildren = this.addChildToParent(child, node.children);
          if (nodeChildren) {
            node.children = nodeChildren;
            return nodeList;
          }
        }
      }
    }

    get showHeader() {
      const { domain, source } = this.props;
      return (
        !(source === 'cohort' && domain === Domain.SURVEY) &&
        domain !== Domain.PHYSICALMEASUREMENT &&
        domain !== Domain.VISIT
      );
    }

    // Hides the tree node for COPE survey if config hasCopeSurveyData is set to false
    showNode(node: Criteria) {
      const { workspace, cdrVersionTiersResponse } = this.props;
      return node.subtype === CriteriaSubType.SURVEY.toString() &&
        node.name.includes('COPE')
        ? getCdrVersion(workspace, cdrVersionTiersResponse).hasCopeSurveyData
        : true;
    }

    selectIconDisabled() {
      const { selectedIds, source } = this.props;
      return source !== 'cohort' && selectedIds && selectedIds.length >= 1000;
    }

    onHasAnyPMChange() {
      currentCohortCriteriaStore.next([]);
      if (!this.state.hasAnyPM) {
        // timeout allows selections to clear before adding ANY_PM_SELECTION
        setTimeout(() => this.props.select(ANY_PM_SELECTION));
      }
    }

    render() {
      const {
        autocompleteSelection,
        back,
        domain,
        groupSelections,
        node,
        scrollToMatch,
        searchTerms,
        select,
        selectedIds,
        selectOption,
        setAttributes,
        setSearchTerms,
        source,
      } = this.props;
      const { children, error, hasAnyPM, ingredients, loading } = this.state;
      return (
        <React.Fragment>
          <style>{scrollbarCSS}</style>
          {this.selectIconDisabled() && (
            <div
              style={{
                color: colors.warning,
                fontWeight: 'bold',
                maxWidth: '1000px',
              }}
            >
              NOTE: Concept Set can have only 1000 concepts. Please delete some
              concepts before adding more.
            </div>
          )}
          <div style={styles.searchBarContainer}>
            <SearchBar
              node={node}
              searchTerms={searchTerms}
              selectOption={selectOption}
              setIngredients={(i) => this.setState({ ingredients: i })}
              setInput={(v) => setSearchTerms(v)}
            />
          </div>
          {!loading && (
            <div
              style={{
                paddingTop: this.showHeader ? '1.5rem' : 0,
                width: '99%',
              }}
            >
              {this.showHeader && (
                <div
                  style={{
                    ...styles.treeHeader,
                    border: `1px solid ${colorWithWhiteness(
                      colors.black,
                      0.8
                    )}`,
                  }}
                >
                  {!!ingredients && (
                    <div style={styles.ingredients}>
                      Ingredients in this brand: {ingredients.join(', ')}
                    </div>
                  )}
                  <button style={styles.returnLink} onClick={() => back()}>
                    <img src={arrow} style={styles.arrowIcon} alt='Go back' />
                    RETURN TO LIST
                  </button>
                </div>
              )}
              {error && (
                <div style={styles.error}>
                  <ClrIcon
                    style={{ color: colors.white }}
                    className='is-solid'
                    shape='exclamation-triangle'
                  />
                  Sorry, the request cannot be completed. Please try again or
                  contact Support in the left hand navigation
                </div>
              )}
              <div
                style={
                  this.showHeader
                    ? styles.node
                    : { ...styles.node, border: 'none' }
                }
                className='show-scrollbar'
              >
                {domain === Domain.PHYSICALMEASUREMENT && (
                  <div style={{ margin: '0.5rem 0 0 0.5rem' }}>
                    <CheckBox
                      manageOwnState={false}
                      checked={hasAnyPM}
                      onChange={() => this.onHasAnyPMChange()}
                    />
                    <span
                      style={{ color: colors.primary, marginLeft: '0.5rem' }}
                    >
                      Has Any Physical Measurements Data
                    </span>
                    {!hasAnyPM && <div style={styles.orCircle}>OR</div>}
                  </div>
                )}
                {!!children &&
                  !hasAnyPM &&
                  children.map(
                    (child, c) =>
                      this.showNode(child) && (
                        <TreeNode
                          key={c}
                          autocompleteSelection={autocompleteSelection}
                          domain={domain}
                          groupSelections={groupSelections}
                          node={child}
                          scrollToMatch={scrollToMatch}
                          searchTerms={searchTerms}
                          select={(s) => select(s)}
                          selectedIds={selectedIds}
                          setAttributes={setAttributes}
                          source={source}
                        />
                      )
                  )}
              </div>
            </div>
          )}
          {loading && !this.showHeader && <SpinnerOverlay />}
        </React.Fragment>
      );
    }
  }
);
