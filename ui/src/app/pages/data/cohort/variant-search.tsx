import * as React from 'react';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  CriteriaType,
  Domain,
  Variant,
  VariantFilter,
  VariantFilterRequest,
} from 'generated/fetch';

import { AlertDanger } from 'app/components/alert';
import { Clickable } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import { TextInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { Selection } from 'app/pages/data/cohort/selection-list';
import { domainToTitle, withDisabledStyle } from 'app/pages/data/cohort/utils';
import { VariantSearchFilters } from 'app/pages/data/cohort/variant-search-filters';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  reactStyles,
  validateInputForMySQL,
  withCurrentCohortCriteria,
  withCurrentCohortSearchContext,
  withCurrentWorkspace,
} from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import {
  currentCohortCriteriaStore,
  currentCohortSearchContextStore,
} from 'app/utils/navigation';

const { useEffect, useState } = React;

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
  clearSearchIcon: {
    color: colors.accent,
    display: 'inline-block',
    float: 'right',
    marginTop: '0.375rem',
  },
  infoIcon: {
    color: colorWithWhiteness(colors.accent, 0.1),
    marginLeft: '0.375rem',
    height: '100%',
  },
  inputAlert: {
    justifyContent: 'space-between',
    padding: '0.3rem',
    width: '64.3%',
  },
  table: {
    width: '100%',
    border: borderStyle,
    borderRadius: '3px',
    borderBottom: 0,
    tableLayout: 'fixed',
  },
  columnHeader: {
    background: '#f4f4f4',
    borderBottom: 0,
    color: colors.primary,
    fontWeight: 600,
  },
  columnBody: {
    background: '#ffffff',
    padding: '0.75rem 0.75rem 0.45rem 0.25rem',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    borderBottom: 'none',
    lineHeight: '0.9rem',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  selectAll: {
    alignItems: 'center',
    color: colors.primary,
    cursor: 'pointer',
    display: 'flex',
    marginLeft: '1rem',
  },
  selectIcon: {
    margin: '2px 0.25rem 2px 2px',
    color: colorWithWhiteness(colors.success, -0.5),
    cursor: 'pointer',
  },
  selectedIcon: {
    marginRight: '0.125rem',
    color: colorWithWhiteness(colors.success, -0.5),
    opacity: 0.4,
    cursor: 'not-allowed',
  },
  excludeIcon: {
    marginRight: '0.125rem',
    color: colors.danger,
    cursor: 'pointer',
  },
  disabled: {
    opacity: 0.4,
    cursor: 'not-allowed',
  },
  exclusionText: {
    color: colors.primary,
    cursor: 'pointer',
    marginLeft: '1rem',
  },
  error: {
    width: '80%',
    marginTop: '0.5rem',
    padding: '0.375rem',
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    borderRadius: '5px',
  },
});

const pageSize = 25;
const searchTrigger = 2;
const searchTooltip = (
  <div style={{ marginLeft: '0.5rem' }}>
    Examples by query type:
    <ul>
      <li>
        <b>Gene:</b> WFDC2
      </li>
      <li>
        <b>Variant:</b> 20-38623282-G-A
      </li>
      <li>
        <b>RS Number:</b> rs558865434
      </li>
      <li>
        <b>Genomic Region:</b> chr20:38623000-38623379
      </li>
    </ul>
  </div>
);
const resultsTooltip = (
  <div style={{ marginLeft: '0.5rem' }}>
    Number of results must be between {pageSize} and 10,000 to Select All
    Results. For results less than {pageSize}, selections must be made manually.
  </div>
);
const duplicateFilterTooltip = (
  <div style={{ marginLeft: '0.5rem' }}>
    A Select All Group with these filter selections has already been selected
  </div>
);

export const VariantSearch = fp.flow(
  withCurrentWorkspace(),
  withCurrentCohortCriteria(),
  withCurrentCohortSearchContext()
)(
  ({
    cohortContext,
    criteria,
    select,
    selectedIds,
    workspace: { namespace, terraName },
  }) => {
    const [first, setFirst] = useState(0);
    const [apiError, setApiError] = useState(false);
    const [inputErrors, setInputErrors] = useState([]);
    const [loading, setLoading] = useState(false);
    const [loadingMore, setLoadingMore] = useState(false);
    const [pageToken, setPageToken] = useState(null);
    const [searching, setSearching] = useState(false);
    const [searchResults, setSearchResults] = useState<Variant[]>([]);
    const [searchTerms, setSearchTerms] = useState('');
    const [totalCount, setTotalCount] = useState(null);
    const [filtersOpen, setFiltersOpen] = useState(false);
    const [editSelectAllResults, setEditSelectAllResults] = useState(false);
    const [excludeFromSelectAll, setExcludeFromSelectAll] = useState([]);
    const [selectAllFilters, setSelectAllFilters] = useState<VariantFilter>();
    const [selectedFilters, setSelectedFilters] =
      useState<VariantFilterRequest>({
        searchTerm: '',
        geneList: [],
        consequenceList: [],
        clinicalSignificanceList: [],
        countMin: null,
        countMax: null,
        numberMin: null,
        numberMax: null,
        frequencyMin: null,
        frequencyMax: null,
        sortBy: 'Participant Count',
      });
    const [variantFilters, setVariantFilters] = useState(null);
    const [resetResults, setResetResults] = useState(0);

    const searchVariants = async (newSearch: boolean, firstPage?: number) => {
      try {
        const [{ items, nextPageToken, totalSize }, filterResponse] =
          await Promise.all([
            cohortBuilderApi().findVariants(namespace, terraName, {
              ...selectedFilters,
              searchTerm: searchTerms.trim(),
              pageSize,
              pageToken: !!firstPage ? pageToken : null,
            }),
            newSearch
              ? cohortBuilderApi().findVariantFilters(namespace, terraName, {
                  searchTerm: searchTerms.trim(),
                })
              : null,
          ]);
        if (filterResponse) {
          setVariantFilters(filterResponse);
        }
        setSelectAllFilters({
          ...selectedFilters,
          searchTerm: searchTerms.trim(),
        });
        setPageToken(nextPageToken);
        setSearchResults((prevState) =>
          firstPage ? [...prevState, ...items] : items
        );
        setTotalCount(totalSize);
        setFirst(firstPage || 0);
      } catch (error) {
        console.error(error);
        setApiError(true);
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    };

    useEffect(() => {
      if (resetResults > 0) {
        setApiError(false);
        setLoading(true);
        searchVariants(false);
      }
    }, [resetResults]);

    useEffect(() => {
      if (cohortContext.editSelectAll) {
        const { variantFilter } = cohortContext.editSelectAll;
        setSearchTerms(variantFilter.searchTerm);
        setSelectedFilters(variantFilter);
        setVariantFilters(variantFilter);
        setExcludeFromSelectAll(variantFilter.exclusionList ?? []);
        setEditSelectAllResults(true);
        setLoading(true);
        setSearching(true);
      }
    }, [cohortContext]);

    useEffect(() => {
      if (editSelectAllResults) {
        searchVariants(false);
      }
    }, [editSelectAllResults]);

    const clearFilters = (callApi: boolean) => {
      setSelectedFilters({
        searchTerm: '',
        geneList: [],
        consequenceList: [],
        clinicalSignificanceList: [],
        countMin: null,
        countMax: null,
        numberMin: null,
        numberMax: null,
        frequencyMin: null,
        frequencyMax: null,
        sortBy: 'Participant Count',
      });
      setFiltersOpen(false);
      if (callApi) {
        setResetResults((prevState) => prevState + 1);
      }
    };

    const validateInputForVariantSearch = (searchTerm: string) => {
      const newInputErrors = [];
      if (searchTerm.length < searchTrigger) {
        newInputErrors.push(
          `Minimum criteria search length is ${searchTrigger} characters`
        );
      }
      // No variant search terms should contain whitespace in the middle
      if (searchTerm.includes(' ')) {
        newInputErrors.push('The search term cannot contain whitespace');
      }
      // Only genomic region should contain ':' and should always start with 'chr'
      if (
        searchTerm.includes(':') &&
        searchTerm.substring(0, 3).toLowerCase() !== 'chr'
      ) {
        console.log(searchTerm.substring(0, 2));
        newInputErrors.push(
          "The search term must begin with 'chr' when searching Genomic Region"
        );
      }
      return newInputErrors;
    };

    const handleInput = (event: any) => {
      const {
        key,
        target: { value },
      } = event;
      if (key === 'Enter') {
        const newInputErrors = [
          ...validateInputForMySQL(value),
          ...validateInputForVariantSearch(value.trim()),
        ];
        if (newInputErrors.length > 0) {
          setInputErrors(newInputErrors);
        } else {
          setApiError(false);
          setInputErrors([]);
          setLoading(true);
          setSearching(true);
          clearFilters(false);
          searchVariants(true);
        }
      }
    };

    const handlePage = (firstPage: number, rows: number) => {
      if (searchResults.length >= rows + firstPage - 1) {
        setFirst(firstPage);
        return;
      }
      if (searchResults && !pageToken) {
        // We've loaded at least one page, and there's no more data to load.
        setFirst(firstPage);
        return;
      }
      setLoadingMore(true);
      searchVariants(false, firstPage);
    };

    const clearSearch = () => {
      setApiError(false);
      setInputErrors([]);
      setSearching(false);
      setSearchTerms('');
      setSearchResults([]);
      setVariantFilters(null);
      clearFilters(false);
      if (editSelectAllResults) {
        setEditSelectAllResults(false);
        currentCohortSearchContextStore.next({
          ...cohortContext,
          editSelectAll: null,
        });
      }
    };

    const getParamId = (row: Variant) => `param${row.vid}`;

    // Generate a param id based on current filter selections
    const getFilterParamId = () =>
      Object.entries(selectAllFilters).reduce((acc, [key, value]) => {
        if (value === null || key === 'sortBy') {
          return acc;
        }
        switch (typeof value) {
          case 'string':
            return acc + value.replace(/\s/g, '');
          case 'number':
            return acc + value.toString();
          default:
            // Can only be an array
            return acc + value.join('').replace(/\s/g, '');
        }
      }, '');

    useEffect(() => {
      if (
        editSelectAllResults &&
        !criteria.some(({ parameterId }) => parameterId === getFilterParamId())
      ) {
        // The filter group was removed from the selection list, clear the select all state
        setEditSelectAllResults(false);
        setExcludeFromSelectAll([]);
      }
    }, [criteria]);

    const isSelected = (row: any) => {
      const paramId = getParamId(row);
      return (
        selectedIds.includes(paramId) ||
        (editSelectAllResults && !excludeFromSelectAll.includes(row.vid))
      );
    };

    const selectItem = (row: any) => {
      if (editSelectAllResults) {
        // Remove item from excludes list
        setExcludeFromSelectAll((prevState) =>
          prevState.filter((excluded) => excluded !== row.vid)
        );
      } else {
        const param = {
          parameterId: getParamId(row),
          parentId: null,
          type: CriteriaType.NONE,
          name: `Variant ${row.vid}`,
          group: false,
          domainId: Domain.SNP_INDEL_VARIANT,
          hasAttributes: false,
          selectable: true,
          variantId: row.vid,
          attributes: [],
        };
        AnalyticsTracker.CohortBuilder.SelectCriteria(
          `Select ${domainToTitle(row.domainId)} - '${row.name}'`
        );
        select(param);
      }
    };

    const handleCheckboxChange = (
      filter: string,
      name: string,
      checked: boolean
    ) =>
      setSelectedFilters((prevState) => ({
        ...prevState,
        [filter]: checked
          ? [...prevState[filter], name]
          : prevState[filter].filter((val) => val !== name),
      }));

    const handleSliderChange = (filterName: string, range: number[]) =>
      setSelectedFilters((prevState) => ({
        ...prevState,
        [`${filterName}Min`]: range[0],
        [`${filterName}Max`]: range[1],
      }));

    const handleSortByChange = (value: string) =>
      setSelectedFilters((prevState) => ({
        ...prevState,
        sortBy: value,
      }));

    const handleSelectAllResults = () => {
      // Add filter object to criteria selection list
      const param: Selection = {
        id: null,
        parameterId: getFilterParamId(),
        parentId: null,
        type: CriteriaType.NONE,
        name: `Select All Group: ${selectAllFilters.searchTerm}`,
        group: false,
        domainId: Domain.SNP_INDEL_VARIANT,
        hasAttributes: false,
        selectable: true,
        attributes: [],
        variantFilter: selectAllFilters,
      };
      AnalyticsTracker.CohortBuilder.SelectCriteria(
        `Select All Variant Filter Group - '${selectAllFilters.searchTerm}'`
      );
      select(param);
      clearSearch();
    };

    const handleSelectAllEdit = () => {
      // Remove filter object from criteria selection list before adding updated selection
      const updatedCriteria = criteria.filter(
        ({ parameterId }) =>
          parameterId !== cohortContext.editSelectAll.parameterId
      );
      // Add filter object to criteria selection list with updated exclusion list
      updatedCriteria.push({
        ...cohortContext.editSelectAll,
        variantFilter: {
          ...selectAllFilters,
          exclusionList: excludeFromSelectAll,
        },
      });
      currentCohortCriteriaStore.next(updatedCriteria);
      AnalyticsTracker.CohortBuilder.SelectCriteria(
        `Edit Select All Variant Filter Group - '${selectAllFilters.searchTerm}'`
      );
      clearSearch();
    };

    const disableSelectAll =
      totalCount < pageSize ||
      totalCount > 10000 ||
      criteria.some((crit) => crit.parameterId === getFilterParamId());
    const disableSelectAllSave =
      excludeFromSelectAll.length ===
      (cohortContext.editSelectAll?.variantFilter.exclusionList?.length ?? 0);
    const displayResults = searchResults?.slice(first, first + pageSize);
    return (
      <>
        <div style={{ display: 'flex' }}>
          <div style={styles.searchContainer}>
            <div style={styles.searchBar}>
              <ClrIcon shape='search' size='18' />
              <TextInput
                data-test-id='list-search-input'
                style={styles.searchInput}
                value={searchTerms}
                placeholder='Search Variants'
                disabled={editSelectAllResults}
                onChange={(e) => setSearchTerms(e)}
                onKeyPress={handleInput}
              />
              {searching && (
                <Clickable
                  style={styles.clearSearchIcon}
                  onClick={() => clearSearch()}
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
        {apiError && (
          <div style={styles.error}>
            <ClrIcon
              style={{
                margin: '0 0.75rem 0 0.375rem',
              }}
              className='is-solid'
              shape='exclamation-triangle'
              size='22'
            />
            Sorry, the request cannot be completed. Please try again or contact
            Support in the left hand navigation.
          </div>
        )}
        {!loading && variantFilters && (
          <div style={{ display: 'flex', position: 'relative' }}>
            <Clickable
              style={withDisabledStyle(
                { color: colors.primary },
                loadingMore || !!cohortContext.editSelectAll
              )}
              onClick={() => setFiltersOpen((prevState) => !prevState)}
              disabled={loadingMore || !!cohortContext.editSelectAll}
            >
              <ClrIcon shape='filter-2' className='is-solid' size={30} />
              Filter & Sort
            </Clickable>
            {filtersOpen && (
              <VariantSearchFilters
                filters={variantFilters}
                formState={selectedFilters}
                checkboxFn={handleCheckboxChange}
                sliderFn={handleSliderChange}
                sortFn={handleSortByChange}
                clearFn={() => clearFilters(true)}
                submitFn={() => {
                  setFiltersOpen(false);
                  setLoading(true);
                  searchVariants(false);
                }}
              />
            )}
            {!!cohortContext.editSelectAll ? (
              <>
                <Clickable
                  style={withDisabledStyle(
                    styles.exclusionText,
                    disableSelectAllSave
                  )}
                  onClick={() => handleSelectAllEdit()}
                  disabled={disableSelectAllSave}
                >
                  <ClrIcon
                    shape='check'
                    class='is-solid'
                    size={18}
                    style={{
                      marginRight: '0.25rem',
                      color: colors.select,
                    }}
                  />
                  Save Select All Exclusions (
                  {excludeFromSelectAll.length -
                    (cohortContext.editSelectAll.variantFilter.exclusionList
                      ?.length || 0)}
                  )
                </Clickable>
                <Clickable
                  style={styles.exclusionText}
                  onClick={() => clearSearch()}
                >
                  <ClrIcon
                    shape='times'
                    class='is-solid'
                    size={18}
                    style={{
                      marginRight: '0.25rem',
                      color: colors.danger,
                    }}
                  />
                  Cancel Edit
                </Clickable>
              </>
            ) : (
              <TooltipTrigger
                side='top'
                content={
                  totalCount < pageSize || totalCount > 10000
                    ? resultsTooltip
                    : duplicateFilterTooltip
                }
                disabled={!disableSelectAll}
              >
                <Clickable
                  style={withDisabledStyle(styles.selectAll, disableSelectAll)}
                  onClick={() => handleSelectAllResults()}
                  disabled={disableSelectAll}
                >
                  <ClrIcon
                    shape='plus-circle'
                    class='is-solid'
                    size={18}
                    style={{
                      marginRight: '0.25rem',
                    }}
                  />
                  Select All Results
                </Clickable>
              </TooltipTrigger>
            )}
          </div>
        )}
        {loading ? (
          <SpinnerOverlay />
        ) : (
          searching &&
          !apiError && (
            <DataTable
              currentPageReportTemplate={
                displayResults.length > 0
                  ? `${first + 1} - ${
                      first + displayResults.length
                    } of ${totalCount.toLocaleString()}`
                  : ''
              }
              first={first}
              lazy
              loading={loadingMore}
              onPage={(e) => handlePage(e.first, e.rows)}
              paginator
              paginatorTemplate='PrevPageLink CurrentPageReport NextPageLink'
              rows={pageSize}
              scrollable
              scrollHeight='80%'
              style={{ fontSize: '12px', height: '100%', minHeight: '22rem' }}
              totalRecords={totalCount}
              value={displayResults}
            >
              <Column
                field='vid'
                header='Variant Id'
                headerStyle={{ ...styles.columnHeader, width: '20%' }}
                body={(variant) => (
                  <div title={variant.vid}>
                    {isSelected(variant) ? (
                      <>
                        <ClrIcon
                          style={styles.selectedIcon}
                          shape='check-circle'
                          size='20'
                        />
                        {editSelectAllResults &&
                          !excludeFromSelectAll.includes(variant.vid) && (
                            <ClrIcon
                              style={styles.excludeIcon}
                              shape='times-circle'
                              size='20'
                              onClick={() =>
                                setExcludeFromSelectAll((prevState) => [
                                  ...prevState,
                                  variant.vid,
                                ])
                              }
                              title='Exclude From Select All Group'
                            />
                          )}
                      </>
                    ) : (
                      <ClrIcon
                        style={styles.selectIcon}
                        shape='plus-circle'
                        size='16'
                        onClick={() => selectItem(variant)}
                      />
                    )}
                    {variant.vid}
                  </div>
                )}
                bodyStyle={{ ...styles.columnBody, borderLeft: borderStyle }}
              />
              <Column
                field='gene'
                header='Gene'
                headerStyle={styles.columnHeader}
                body={({ gene }) => <div title={gene}>{gene}</div>}
                bodyStyle={styles.columnBody}
              />
              <Column
                field='consequence'
                header='Consequence'
                headerStyle={{ ...styles.columnHeader, width: '15%' }}
                body={({ consequence }) => (
                  <div title={consequence}>{consequence}</div>
                )}
                bodyStyle={styles.columnBody}
              />
              <Column
                field='proteinChange'
                header='Protein Change'
                headerStyle={styles.columnHeader}
                body={({ proteinChange }) => (
                  <div title={proteinChange}>{proteinChange || '-'}</div>
                )}
                bodyStyle={styles.columnBody}
              />
              <Column
                field='clinVarSignificance'
                header='ClinVar Significance'
                headerStyle={styles.columnHeader}
                body={({ clinVarSignificance }) => (
                  <div title={clinVarSignificance}>
                    {clinVarSignificance || '-'}
                  </div>
                )}
                bodyStyle={styles.columnBody}
              />
              <Column
                field='alleleCount'
                header='Allele Count'
                headerStyle={styles.columnHeader}
                body={({ alleleCount }) => (
                  <div title={alleleCount}>{alleleCount}</div>
                )}
                bodyStyle={styles.columnBody}
              />
              <Column
                field='alleleNumber'
                header='Allele Number'
                headerStyle={styles.columnHeader}
                body={({ alleleNumber }) => (
                  <div title={alleleNumber}>{alleleNumber}</div>
                )}
                bodyStyle={styles.columnBody}
              />
              <Column
                field='alleleFrequency'
                header='Allele Frequency'
                headerStyle={styles.columnHeader}
                body={({ alleleFrequency }) => (
                  <div title={alleleFrequency}>{alleleFrequency}</div>
                )}
                bodyStyle={{ ...styles.columnBody }}
              />
              <Column
                field='participantCount'
                header='Participant Count'
                headerStyle={styles.columnHeader}
                body={({ participantCount }) => (
                  <div title={participantCount}>{participantCount}</div>
                )}
                bodyStyle={{ ...styles.columnBody, borderRight: borderStyle }}
              />
            </DataTable>
          )
        )}
      </>
    );
  }
);
