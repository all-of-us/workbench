import * as React from 'react';
import { CSSProperties } from 'react';
import * as fp from 'lodash/fp';
import { InputSwitch } from 'primereact/inputswitch';

import {
  AttrName,
  CdrVersion,
  CdrVersionTiersResponse,
  Criteria,
  CriteriaSearchRequest,
  CriteriaSubType,
  CriteriaType,
  Domain,
  Operator,
} from 'generated/fetch';

import { environment } from 'environments/environment';
import { AlertDanger } from 'app/components/alert';
import { Clickable, StyledExternalLink } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { TextInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner, SpinnerOverlay } from 'app/components/spinners';
import { AoU } from 'app/components/text-wrappers';
import { ppiQuestions } from 'app/pages/data/cohort/search-state.service';
import { domainToTitle } from 'app/pages/data/cohort/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  reactStyles,
  updateCriteriaSelectionStore,
  validateInputForMySQL,
  withCdrVersions,
  withCurrentCohortCriteria,
  withCurrentConcept,
  withCurrentWorkspace,
} from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { findCdrVersion } from 'app/utils/cdr-versions';
import {
  attributesSelectionStore,
  currentCohortCriteriaStore,
  currentCohortSearchContextStore,
  setSidebarActiveIconStore,
} from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';

const borderStyle = `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`;
const styles = reactStyles({
  searchContainer: {
    float: 'left',
    width: '80%',
    padding: '0.6rem 0',
    zIndex: 10,
  },
  searchBar: {
    height: '3.15rem',
    width: '100%',
    padding: '7px 14px',
    borderRadius: '5px',
    backgroundColor: colorWithWhiteness(colors.secondary, 0.8),
  },
  searchInput: {
    width: '94%',
    height: '2.25rem',
    marginLeft: '0.375rem',
    padding: '0',
    background: 'transparent',
    border: 0,
    outline: 'none',
  },
  attrIcon: {
    marginRight: '0.75rem',
    color: colors.accent,
    cursor: 'pointer',
  },
  selectIcon: {
    margin: '2px 0.75rem 2px 2px',
    color: colorWithWhiteness(colors.success, -0.5),
    cursor: 'pointer',
  },
  disableSelectIcon: {
    opacity: 0.4,
    cursor: 'not-allowed',
  },
  selectedIcon: {
    marginRight: '0.6rem',
    color: colorWithWhiteness(colors.success, -0.5),
    opacity: 0.4,
    cursor: 'not-allowed',
  },
  disabledIcon: {
    marginRight: '0.6rem',
    color: colorWithWhiteness(colors.dark, 0.5),
    opacity: 0.4,
    cursor: 'not-allowed',
    pointerEvents: 'none',
  },
  brandIcon: {
    marginRight: '0.6rem',
    color: colorWithWhiteness(colors.dark, 0.5),
    cursor: 'pointer',
  },
  treeIcon: {
    color: colors.accent,
    cursor: 'pointer',
    fontSize: '1.725rem',
  },
  listContainer: {
    margin: '0.75rem 0 1.5rem',
    fontSize: '12px',
    color: colors.primary,
  },
  vocabLink: {
    display: 'inline-block',
    color: colors.accent,
  },
  table: {
    width: '100%',
    border: borderStyle,
    borderRadius: '3px 3px 0 0',
    borderBottom: 0,
    tableLayout: 'fixed',
  },
  tableBody: {
    borderRadius: '0 3px 3px 0',
    borderTop: 0,
  },
  columnNameHeader: {
    padding: '0 0 0 0.375rem',
    background: colorWithWhiteness(colors.dark, 0.93),
    color: colors.primary,
    border: 0,
    borderBottom: borderStyle,
    fontWeight: 600,
    textAlign: 'left',
    verticalAlign: 'middle',
    lineHeight: '1.125rem',
  },
  columnBodyName: {
    background: colors.white,
    verticalAlign: 'middle',
    padding: 0,
    border: 0,
    borderBottom: borderStyle,
    color: colors.primary,
    lineHeight: '1.2rem',
  },
  selectDiv: {
    minWidth: '6%',
    float: 'left',
    lineHeight: '0.9rem',
  },
  nameDiv: {
    width: '80%',
    float: 'left',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  error: {
    width: '99%',
    marginTop: '4.125rem',
    padding: '0.375rem',
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    borderRadius: '5px',
  },
  helpText: {
    color: colors.primary,
    display: 'table-cell',
    height: '100%',
    verticalAlign: 'middle',
  },
  infoIcon: {
    color: colorWithWhiteness(colors.accent, 0.1),
    marginLeft: '0.375rem',
    height: '100%',
  },
  clearSearchIcon: {
    color: colors.accent,
    display: 'inline-block',
    float: 'right',
    marginTop: '0.375rem',
  },
  inputAlert: {
    justifyContent: 'space-between',
    padding: '0.3rem',
    width: '64.3%',
  },
});

const columnBodyStyle = {
  ...styles.columnBodyName,
  width: '9%',
};

const columnHeaderStyle = {
  ...styles.columnNameHeader,
  width: '9%',
};

const columns = [
  {
    name: 'Name',
    tooltip: 'Name from vocabulary',
    style: { ...styles.columnNameHeader, width: '31%', borderLeft: 0 },
  },
  {
    name: 'Concept Id',
    tooltip: 'Unique ID for concept in OMOP',
    style: {
      ...styles.columnNameHeader,
      width: '10%',
      paddingLeft: '0',
      paddingRight: '0.75rem',
    },
  },
  {
    name: 'Source/Standard',
    tooltip:
      'Indicates if code is an OMOP standard or a source vocabulary code (ICD9/10, CPT, etc)',
    style: {
      ...styles.columnNameHeader,
      width: '10%',
      paddingLeft: '0',
      paddingRight: '0.75rem',
    },
  },
  {
    name: 'Vocab',
    tooltip: 'Vocabulary for concept',
    style: { ...columnHeaderStyle, paddingLeft: '0' },
  },
  {
    name: 'Code',
    tooltip: 'Code from vocabulary',
    style: columnHeaderStyle,
  },
  {
    name: 'Roll-up Count',
    tooltip:
      'Number of distinct participants that have either the parent concept OR any of the parent’s child concepts',
    style: columnHeaderStyle,
  },
  {
    name: 'Item Count',
    tooltip: 'Number of distinct participants for this concept',
    style: columnHeaderStyle,
  },
  {
    name: 'View Hierarchy',
    tooltip: null,
    style: { ...styles.columnNameHeader, textAlign: 'center', width: '12%' },
  },
];

const searchTrigger = 2;
const sourceStandardTooltip = (
  <span>
    The <AoU /> program receives EHR data from a number of healthcare provider
    sources across the United States. These sources may code patient health data
    using a variety of vocabularies, such as ICD10 or SNOMED for conditions. To
    simplify analysis, the <AoU /> dataset offers a single standardized
    vocabulary for each domain (e.g. SNOMED for conditions), and corresponding
    codes from all source vocabularies are mapped to the standard. Therefore, we
    recommend most users use the standardized items in defining their dataset.
    However, if you do not wish to rely on this standardization, you may select
    concepts as coded in the source data.
  </span>
);
const searchTooltip = (
  <div style={{ marginLeft: '0.75rem' }}>
    The following special operators can be used to augment search terms:
    <ul style={{ listStylePosition: 'outside' }}>
      <li>
        (*) is the wildcard operator. This operator can be used with a prefix or
        suffix. For example: ceph* (starts with) or *statin (ends with - NOTE:
        when searching for ends with it will only match with end of concept
        name)
      </li>
      <li>
        (-) indicates that this word must <b>not</b> be present. For example:
        lung -cancer
      </li>
      <li>
        (") a phrase that is enclosed within double quote (") characters matches
        only rows that contain the phrase literally, as it was typed. For
        example: "lung cancer"
      </li>
      <li>
        These operators can be combined to produce more complex search
        operations. For example: brain tum* -neoplasm
      </li>
    </ul>
  </div>
);

interface Props {
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  concept?: Array<Criteria>;
  criteria: Array<Criteria>;
  hierarchy: Function;
  searchContext: any;
  searchTerms: string;
  select: Function;
  selectedIds: Array<string>;
  setAttributes: Function;
  workspace: WorkspaceData;
}

interface State {
  apiError: boolean;
  cdrVersion: CdrVersion;
  data: any;
  hoverId: string;
  childNodes: any;
  inputErrors: Array<string>;
  loading: boolean;
  removeDrugBrand: boolean;
  searching: boolean;
  searchSource: boolean;
  searchTerms: string;
  standardOnly: boolean;
  sourceMatch: any;
  standardData: any;
  totalCount: number;
}

const tableBodyOverlayStyle = `
  body .tablebody {
    overflow-y: overlay
  }
`;

export const ListSearch = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace(),
  withCurrentConcept(),
  withCurrentCohortCriteria()
)(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        apiError: false,
        cdrVersion: undefined,
        data: null,
        childNodes: {},
        inputErrors: [],
        hoverId: undefined,
        loading: false,
        removeDrugBrand: false,
        searching: false,
        searchSource: [
          Domain.PHYSICAL_MEASUREMENT,
          Domain.PHYSICAL_MEASUREMENT_CSS,
        ].includes(props.searchContext.domain),
        searchTerms: props.searchTerms,
        standardOnly: false,
        sourceMatch: undefined,
        standardData: null,
        totalCount: null,
      };
    }
    async componentDidMount() {
      const {
        cdrVersionTiersResponse,
        searchTerms,
        searchContext: { domain, source },
        workspace: { cdrVersionId, id, namespace },
      } = this.props;
      this.setState({
        cdrVersion: findCdrVersion(cdrVersionId, cdrVersionTiersResponse),
      });
      if (source === 'conceptSetDetails') {
        this.setState({ data: this.props.concept });
      } else {
        if (this.criteriaLookupNeeded) {
          this.setState({ loading: true });
          await cohortBuilderApi()
            .findCriteriaForCohortEdit(namespace, id, domain.toString(), {
              sourceConceptIds: currentCohortCriteriaStore
                .getValue()
                .filter((s) => !s.standard)
                .map((s) => s.conceptId),
              standardConceptIds: currentCohortCriteriaStore
                .getValue()
                .filter((s) => s.standard)
                .map((s) => s.conceptId),
            })
            .then((response) =>
              updateCriteriaSelectionStore(response.items, domain)
            );
        }
        this.getResultsBySourceOrStandard(searchTerms || '');
      }
    }

    componentDidUpdate(prevProps: Readonly<Props>) {
      const {
        concept,
        searchContext: { source },
      } = this.props;
      const { searching } = this.state;
      if (
        source === 'conceptSetDetails' &&
        prevProps.concept !== concept &&
        !searching
      ) {
        this.setState({ data: concept });
      }
    }

    handleInput = (event: any) => {
      const {
        key,
        target: { value },
      } = event;
      if (key === 'Enter') {
        if (value.trim().length < searchTrigger) {
          this.setState({
            inputErrors: [
              `Minimum criteria search length is ${searchTrigger} characters`,
            ],
          });
        } else {
          const inputErrors = validateInputForMySQL(value, searchTrigger);
          if (inputErrors.length > 0) {
            this.setState({ inputErrors });
          } else {
            const { searchContext } = this.props;
            if (searchContext.source === 'cohort') {
              AnalyticsTracker.CohortBuilder.SearchTerms(
                `List search - ${domainToTitle(
                  searchContext.domain
                )} - '${value}'`
              );
            }
            if (searchContext.source === 'concept') {
              // Update search terms so they will persist if user returns to concept homepage
              currentCohortSearchContextStore.next({
                ...searchContext,
                searchTerms: value,
              });
            }
            this.getResultsBySourceOrStandard(value);
          }
        }
      }
    };

    // Searches either source or standard based on value of searchSource in state
    getResultsBySourceOrStandard = async (value: string) => {
      try {
        this.setState({
          data: null,
          apiError: false,
          inputErrors: [],
          loading: true,
          searching: value !== '',
        });
        const {
          searchContext: { domain, source, selectedSurvey },
          workspace: { id, namespace },
        } = this.props;
        const { searchSource } = this.state;
        const { removeDrugBrand } = this.state;
        const surveyName = selectedSurvey || 'All';
        const request: CriteriaSearchRequest = {
          domain,
          standard: !searchSource,
          term: value.trim(),
          surveyName,
          removeDrugBrand,
        };
        const resp = await cohortBuilderApi().findCriteriaByDomain(
          namespace,
          id,
          request
        );
        let data;
        if (this.isSurvey) {
          if (source === 'cohort') {
            const questions = ppiQuestions.getValue();
            resp.items.forEach(({ conceptId, count, id: questionId, name }) => {
              questions[questionId] = { conceptId, count, name };
            });
            ppiQuestions.next(questions);
            data = resp.items;
          } else {
            data = resp.items.filter(
              (survey) => survey.subtype === CriteriaSubType.QUESTION.toString()
            );
          }
        } else {
          data = resp.items;
        }
        this.setState({ data, totalCount: resp.totalCount });
      } catch (err) {
        console.error(err);
        this.setState({ apiError: true });
      } finally {
        this.setState({ loading: false });
      }
    };

    showStandardResults() {
      const {
        searchContext: { domain, source },
      } = this.props;
      if (source === 'cohort') {
        AnalyticsTracker.CohortBuilder.SourceOrStandardLink(
          `Standard Vocab - ${domainToTitle(domain)}`
        );
      }
      this.setState({ standardOnly: true });
    }

    get checkSource() {
      return [Domain.CONDITION, Domain.PROCEDURE].includes(
        this.props.searchContext.domain
      );
    }

    get checkDrug() {
      return [Domain.DRUG].includes(this.props.searchContext.domain);
    }

    selectIconDisabled() {
      const {
        searchContext: { source },
        selectedIds,
      } = this.props;
      return source !== 'cohort' && selectedIds && selectedIds.length >= 1000;
    }

    selectItem = (row: any) => {
      const { conceptId, domainId, group, name, parentId, value } = row;
      let param = { parameterId: this.getParamId(row), ...row, attributes: [] };
      if (domainId === Domain.SURVEY.toString()) {
        param = {
          ...param,
          surveyName: this.props.searchContext.selectedSurvey,
        };
        if (!group) {
          const question = ppiQuestions.getValue()[parentId];
          if (question) {
            param.name = `${question.name} - ${name}`;
          }
          param.attributes.push(
            conceptId === 1585747
              ? {
                  name: AttrName.NUM,
                  operator: Operator.EQUAL,
                  operands: [value],
                }
              : { name: AttrName.CAT, operator: Operator.IN, operands: [value] }
          );
        }
      }
      if (this.props.searchContext.source === 'cohort') {
        AnalyticsTracker.CohortBuilder.SelectCriteria(
          `Select ${domainToTitle(domainId)} - '${name}'`
        );
      }
      this.props.select(param);
    };

    setAttributes(node: any) {
      attributesSelectionStore.next(node);
      setSidebarActiveIconStore.next('criteria');
    }

    showHierarchy = (row: any) => {
      if (this.props.searchContext.source === 'cohort') {
        AnalyticsTracker.CohortBuilder.ViewHierarchy(
          domainToTitle(row.domainId)
        );
      }
      this.props.hierarchy(row);
    };

    showChildren = async (row: any) => {
      const { childNodes } = this.state;
      const { conceptId, domainId, id: rowId, standard, type } = row;
      if (childNodes[rowId]) {
        if (childNodes[rowId].error) {
          childNodes[rowId] = undefined;
        } else {
          childNodes[rowId].open = !childNodes[rowId].open;
        }
        this.setState({ childNodes });
      } else {
        try {
          childNodes[rowId] = {
            open: false,
            loading: true,
            error: false,
            items: [],
          };
          this.setState({ childNodes });
          const {
            workspace: { id, namespace },
          } = this.props;
          const childResponse =
            domainId === Domain.DRUG.toString()
              ? await cohortBuilderApi().findDrugIngredientByConceptId(
                  namespace,
                  id,
                  conceptId
                )
              : await cohortBuilderApi().findCriteriaBy(
                  namespace,
                  id,
                  domainId,
                  type,
                  standard,
                  rowId
                );
          childNodes[rowId] = {
            open: true,
            loading: false,
            error: false,
            items: childResponse.items,
          };
          this.setState({ childNodes });
        } catch (error) {
          console.error(error);
          childNodes[rowId] = {
            open: true,
            loading: false,
            error: true,
            items: [],
          };
          this.setState({ childNodes });
        }
      }
    };

    isSelected = (row: any) => {
      const { selectedIds } = this.props;
      const paramId = this.getParamId(row);
      return selectedIds.includes(paramId);
    };

    getParamId(row: any) {
      return `param${row.conceptId ? row.conceptId + row.code : row.id}${
        row.standard
      }`;
    }

    onNameHover(el: HTMLDivElement, id: string) {
      if (el.offsetWidth < el.scrollWidth) {
        this.setState({ hoverId: id });
      }
    }

    get textInputPlaceholder() {
      const {
        searchContext: { domain, source, selectedSurvey },
      } = this.props;
      switch (source) {
        case 'concept':
          return `Search ${
            !!selectedSurvey ? selectedSurvey : domainToTitle(domain)
          } by code or description`;
        case 'conceptSetDetails':
          return `Search ${this.isSurvey ? 'across all ' : ''}${domainToTitle(
            domain
          )} by code or description`;
        case 'cohort':
          return `Search ${domainToTitle(domain)} by code or description`;
      }
    }

    get isSurvey() {
      return this.props.searchContext.domain === Domain.SURVEY;
    }

    dataBrowserDomainTitle() {
      const domain = this.props.searchContext.domain;
      if (domain === Domain.DRUG) {
        return 'drug-exposures';
      } else if (domain === Domain.MEASUREMENT) {
        return 'labs-and-measurements';
      }
      return domainToTitle(this.props.searchContext.domain).toLowerCase();
    }
    getConceptLink(conceptId) {
      return (
        environment.publicUiUrl +
        '/ehr/' +
        this.dataBrowserDomainTitle() +
        '/' +
        conceptId
      );
    }

    toggleSearchSource() {
      this.setState(
        (state) => ({ searchSource: !state.searchSource }),
        () => this.getResultsBySourceOrStandard(this.state.searchTerms || '')
      );
    }

    toggleDrugBrand() {
      this.setState(
        (state) => ({ removeDrugBrand: !state.removeDrugBrand }),
        () => this.getResultsBySourceOrStandard(this.state.searchTerms || '')
      );
    }

    get criteriaLookupNeeded() {
      return (
        this.props.searchContext.source === 'cohort' &&
        ![Domain.PHYSICAL_MEASUREMENT, Domain.VISIT].includes(
          this.props.searchContext.domain
        ) &&
        currentCohortCriteriaStore.getValue()?.some((crit) => !crit.id)
      );
    }

    clearSearch() {
      if (this.props.searchContext.source === 'conceptSetDetails') {
        this.setState({
          data: this.props.concept,
          inputErrors: [],
          searching: false,
          searchTerms: '',
        });
      } else {
        this.setState({ searchTerms: '' }, () =>
          this.getResultsBySourceOrStandard('')
        );
      }
    }

    renderRow(row: any, child: boolean, elementId: string) {
      const { hoverId, childNodes } = this.state;
      const attributes =
        this.props.searchContext.source === 'cohort' && row.hasAttributes;
      const brand = row.type === CriteriaType.BRAND;
      const parent =
        brand ||
        (this.props.searchContext.source === 'cohort' &&
          row.subtype === CriteriaSubType.QUESTION);
      const parentSelected = this.props.criteria?.find(({ id }) =>
        row.path.split('.').includes(id?.toString())
      );
      const displayName = row.name + (brand ? ' (BRAND NAME)' : '');
      const selected =
        !attributes && !brand && (this.isSelected(row) || parentSelected);
      const unselected =
        !attributes && !brand && !(this.isSelected(row) || parentSelected);
      const open = childNodes[row.id]?.open;
      const loadingChildren = childNodes[row.id]?.loading;
      const columnStyle = child
        ? { ...styles.columnBodyName, paddingLeft: '1.875rem' }
        : styles.columnBodyName;
      const selectIconStyle = this.selectIconDisabled()
        ? { ...styles.selectIcon, ...styles.disableSelectIcon }
        : styles.selectIcon;
      return (
        <tr style={{ height: '2.625rem' }}>
          <td
            style={{
              ...columnStyle,
              width: '31%',
              textAlign: 'left',
              borderLeft: 0,
              padding: '0 0.375rem',
            }}
          >
            {row.selectable && (
              <div style={styles.selectDiv}>
                {loadingChildren && (
                  <Spinner style={{ marginRight: '0.6rem' }} size={16} />
                )}
                {parent && !loadingChildren && (
                  <ClrIcon
                    style={styles.brandIcon}
                    shape={'angle ' + (open ? 'down' : 'right')}
                    size='20'
                    onClick={() => this.showChildren(row)}
                  />
                )}
                {attributes && (
                  <ClrIcon
                    style={styles.attrIcon}
                    shape='slider'
                    dir='right'
                    size='20'
                    onClick={() => this.setAttributes(row)}
                  />
                )}
                {selected && (
                  <ClrIcon
                    style={styles.selectedIcon}
                    shape='check-circle'
                    size='20'
                  />
                )}
                {unselected && (
                  <ClrIcon
                    style={selectIconStyle}
                    shape='plus-circle'
                    size='16'
                    onClick={() => this.selectItem(row)}
                  />
                )}
              </div>
            )}
            <TooltipTrigger
              disabled={hoverId !== elementId}
              content={<div>{displayName}</div>}
            >
              <div
                data-test-id='name-column-value'
                style={styles.nameDiv}
                onMouseOver={(e) =>
                  this.onNameHover(e.target as HTMLDivElement, elementId)
                }
                onMouseOut={() => this.setState({ hoverId: undefined })}
              >
                {displayName}
              </div>
            </TooltipTrigger>
          </td>
          <td
            style={{
              ...columnBodyStyle,
              width: '10%',
              paddingRight: '0.75rem',
            }}
          >
            {row.domainId === Domain.DEVICE ? (
              row.conceptId
            ) : (
              <StyledExternalLink
                href={this.getConceptLink(row.conceptId)}
                target='_blank'
              >
                {row.conceptId}
              </StyledExternalLink>
            )}
          </td>
          <td
            style={{
              ...columnBodyStyle,
              width: '10%',
              paddingRight: '0.75rem',
            }}
          >
            {row.standard ? 'Standard' : 'Source'}
          </td>
          <td style={{ ...columnBodyStyle }}>{!parent && row.type}</td>
          <td
            style={{
              ...columnBodyStyle,
              paddingLeft: '0.3rem',
              paddingRight: '0.75rem',
            }}
          >
            <TooltipTrigger
              disabled={hoverId !== elementId}
              content={<div>{row.code}</div>}
            >
              <div
                data-test-id='code-column-value'
                style={styles.nameDiv}
                onMouseOver={(e) =>
                  this.onNameHover(e.target as HTMLDivElement, elementId)
                }
                onMouseOut={() => this.setState({ hoverId: undefined })}
              >
                {row.code}
              </div>
            </TooltipTrigger>
          </td>
          <td style={{ ...columnBodyStyle, paddingLeft: '0.3rem' }}>
            {row.parentCount > -1 && row.parentCount.toLocaleString()}
          </td>
          <td style={{ ...columnBodyStyle, paddingLeft: '0.3rem' }}>
            {row.childCount > -1 && row.childCount.toLocaleString()}
          </td>
          <td style={{ ...columnBodyStyle, textAlign: 'center', width: '12%' }}>
            {row.hasHierarchy && (
              <i
                className='pi pi-sitemap'
                style={styles.treeIcon}
                onClick={() => this.showHierarchy(row)}
              />
            )}
          </td>
        </tr>
      );
    }

    renderColumnWithToolTip(columnLabel, toolTip) {
      return (
        <FlexRow>
          <label>{columnLabel}</label>
          <TooltipTrigger side='top' content={<div>{toolTip}</div>}>
            <ClrIcon
              style={styles.infoIcon}
              className='is-solid'
              shape='info-standard'
            />
          </TooltipTrigger>
        </FlexRow>
      );
    }

    render() {
      const {
        searchContext: { domain, source },
      } = this.props;
      const {
        apiError,
        cdrVersion,
        data,
        childNodes,
        inputErrors,
        loading,
        searching,
        removeDrugBrand,
        searchSource,
        searchTerms,
        standardOnly,
        sourceMatch,
        standardData,
        totalCount,
      } = this.state;
      const showStandardOption =
        !standardOnly && !!standardData && standardData.length > 0;
      const displayData = standardOnly ? standardData : data;
      return (
        <div style={{ overflow: 'auto' }}>
          <style>{`
          body .p-inputswitch.p-inputswitch-focus .p-inputswitch-slider {
            box-shadow: none
          }
        `}</style>
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
          <div style={{ display: 'flex' }}>
            <div style={styles.searchContainer}>
              <div style={styles.searchBar}>
                <ClrIcon shape='search' size='18' />
                <TextInput
                  data-test-id='list-search-input'
                  style={styles.searchInput}
                  value={searchTerms}
                  placeholder={this.textInputPlaceholder}
                  onChange={(e) => this.setState({ searchTerms: e })}
                  onKeyPress={this.handleInput}
                />
                {searching && (
                  <Clickable
                    style={styles.clearSearchIcon}
                    onClick={() => this.clearSearch()}
                  >
                    <ClrIcon size={24} shape='times-circle' />
                  </Clickable>
                )}
              </div>
              {inputErrors.map((error, e) => (
                <AlertDanger key={e} style={styles.inputAlert}>
                  <span data-test-id='input-error-alert'>{error}</span>
                </AlertDanger>
              ))}
            </div>
            <div style={{ float: 'right', width: '20%' }}>
              <TooltipTrigger side='top' content={searchTooltip}>
                <ClrIcon
                  style={styles.infoIcon}
                  className='is-solid'
                  shape='info-standard'
                />
              </TooltipTrigger>
            </div>
          </div>
          <div style={{ display: 'table', height: '100%', width: '100%' }}>
            <div style={styles.helpText}>
              {domain === Domain.DRUG && (
                <div>
                  Your search may bring back brand names, generics and
                  ingredients. Only ingredients may be added to your search
                  criteria
                </div>
              )}
              {!loading && (
                <React.Fragment>
                  {sourceMatch && !standardOnly && (
                    <div>
                      There are {sourceMatch.count.toLocaleString()}{' '}
                      participants with source code {sourceMatch.code}.
                      {showStandardOption && (
                        <span>
                          {' '}
                          For more results, browse &nbsp;
                          <Clickable
                            style={styles.vocabLink}
                            onClick={() => this.showStandardResults()}
                          >
                            Standard Vocabulary
                          </Clickable>
                          .
                        </span>
                      )}
                    </div>
                  )}
                  {standardOnly && (
                    <div>
                      {!!displayData.length && (
                        <span>
                          There are {displayData[0].count.toLocaleString()}{' '}
                          participants for the standard version of the code you
                          searched
                        </span>
                      )}
                      {!displayData.length && (
                        <span>
                          There are no standard matches for source code{' '}
                          {sourceMatch.code}
                        </span>
                      )}
                      &nbsp;
                      <Clickable
                        style={styles.vocabLink}
                        onMouseDown={() => {
                          if (source === 'cohort') {
                            AnalyticsTracker.CohortBuilder.SourceOrStandardLink(
                              `Source Vocab - ${domainToTitle(domain)}`
                            );
                          }
                        }}
                        onClick={() => this.setState({ standardOnly: false })}
                      >
                        Return to source code
                      </Clickable>
                      .
                    </div>
                  )}
                </React.Fragment>
              )}
              <div>
                {!loading && !!totalCount && (
                  <span>
                    There are {totalCount.toLocaleString()} results
                    {!!cdrVersion && <span> in {cdrVersion.name}</span>}
                  </span>
                )}
                {this.checkSource && (
                  <span style={{ float: 'right' }}>
                    <span
                      style={{
                        display: 'table-cell',
                        paddingRight: '0.525rem',
                      }}
                    >
                      Show results as source concepts (ICD9, ICD10
                      {domain === Domain.PROCEDURE && <span>, CPT</span>})
                      <TooltipTrigger
                        side='top'
                        content={<div>{sourceStandardTooltip}</div>}
                      >
                        <ClrIcon
                          style={styles.infoIcon}
                          className='is-solid'
                          shape='info-standard'
                        />
                      </TooltipTrigger>
                    </span>
                    <InputSwitch
                      checked={searchSource}
                      disabled={loading}
                      onChange={() => this.toggleSearchSource()}
                      style={{ display: 'table-cell', boxShadow: 'none' }}
                    />
                  </span>
                )}
                {this.checkDrug && (
                  <span style={{ float: 'right' }}>
                    <span
                      style={{
                        display: 'table-cell',
                        paddingRight: '0.525rem',
                      }}
                    >
                      Remove Brand Names
                    </span>
                    <InputSwitch
                      checked={removeDrugBrand}
                      disabled={loading}
                      onChange={() => this.toggleDrugBrand()}
                      style={{ display: 'table-cell', boxShadow: 'none' }}
                    />
                  </span>
                )}
              </div>
            </div>
          </div>
          {!loading && !!displayData && (
            <div style={styles.listContainer}>
              {!!displayData.length && (
                <React.Fragment>
                  <table className='p-datatable' style={styles.table}>
                    <thead className='p-datatable-thead'>
                      <tr style={{ height: '3rem' }}>
                        {columns.map((column, index) => (
                          <th key={index} style={column.style as CSSProperties}>
                            {column.tooltip !== null
                              ? this.renderColumnWithToolTip(
                                  column.name,
                                  column.tooltip
                                )
                              : column.name}
                          </th>
                        ))}
                      </tr>
                    </thead>
                  </table>
                  <style>{tableBodyOverlayStyle}</style>
                  <div style={{ height: '22.5rem' }} className='tablebody'>
                    <table
                      data-test-id='list-search-results-table'
                      className='p-datatable'
                      style={{ ...styles.table, ...styles.tableBody }}
                    >
                      <tbody className='p-datatable-tbody'>
                        {displayData.map((row, index) => {
                          const open = childNodes[row.id]?.open;
                          const err = childNodes[row.id]?.error;
                          return (
                            <React.Fragment key={index}>
                              {this.renderRow(row, false, index)}
                              {open &&
                                !err &&
                                childNodes[row.id].items.map((item, i) => {
                                  return (
                                    <React.Fragment key={i}>
                                      {this.renderRow(
                                        item,
                                        true,
                                        `${index}.${i}`
                                      )}
                                    </React.Fragment>
                                  );
                                })}
                              {open && err && (
                                <tr>
                                  <td colSpan={5}>
                                    <div
                                      style={{ ...styles.error, marginTop: 0 }}
                                    >
                                      <ClrIcon
                                        style={{
                                          margin: '0 0.75rem 0 0.375rem',
                                        }}
                                        className='is-solid'
                                        shape='exclamation-triangle'
                                        size='22'
                                      />
                                      Sorry, the request cannot be completed.
                                      Please try again or contact Support in the
                                      left hand navigation.
                                    </div>
                                  </td>
                                </tr>
                              )}
                            </React.Fragment>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                </React.Fragment>
              )}
              {!standardOnly && !displayData.length && (
                <div>No results found</div>
              )}
            </div>
          )}
          {loading && <SpinnerOverlay />}
          {apiError && (
            <div
              style={{
                ...styles.error,
                ...(domain === Domain.DRUG ? { marginTop: '5.625rem' } : {}),
              }}
            >
              <ClrIcon
                style={{ margin: '0 0.75rem 0 0.375rem' }}
                className='is-solid'
                shape='exclamation-triangle'
                size='22'
              />
              Sorry, the request cannot be completed. Please try again or
              contact Support in the left hand navigation.
              {standardOnly && (
                <Clickable
                  style={styles.vocabLink}
                  onClick={() =>
                    this.getResultsBySourceOrStandard(sourceMatch.code)
                  }
                >
                  &nbsp;Return to source code.
                </Clickable>
              )}
            </div>
          )}
        </div>
      );
    }
  }
);
