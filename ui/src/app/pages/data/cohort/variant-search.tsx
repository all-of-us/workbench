import * as React from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { Dropdown } from 'primereact/dropdown';
import Nouislider from 'nouislider-react';

import {
  CriteriaType,
  Domain,
  Variant,
  VariantFilterRequest,
  VariantFilterResponse,
} from 'generated/fetch';

import { AlertDanger } from 'app/components/alert';
import { Button, Clickable } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import { TextInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { domainToTitle } from 'app/pages/data/cohort/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  reactStyles,
  validateInputForMySQL,
  withCurrentWorkspace,
} from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';

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

const VariantFilters = ({
  filters,
  formState,
  checkboxFn,
  sliderFn,
  sortFn,
  clearFn,
  submitFn,
}: {
  filters: VariantFilterResponse;
  formState: VariantFilterRequest;
  checkboxFn: Function;
  sliderFn: Function;
  sortFn: Function;
  clearFn: Function;
  submitFn: Function;
}) => {
  const [expanded, setExpanded] = useState([]);

  const toggleExpanded = (section: string) =>
    setExpanded((prevState) =>
      prevState.includes(section)
        ? prevState.filter((sec) => sec !== section)
        : [...prevState, section]
    );

  return (
    <div
      style={{
        background: 'white',
        border: borderStyle,
        borderRadius: '2px',
        paddingBottom: '4rem',
        position: 'absolute',
        top: '100%',
        width: '15rem',
        zIndex: 2,
      }}
    >
      <div
        style={{
          height: '20rem',
          overflow: 'auto',
          padding: '0.5rem',
        }}
      >
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>Gene</span>
          <Clickable
            style={{ float: 'right' }}
            onClick={() => toggleExpanded('geneList')}
          >
            <ClrIcon
              shape='angle'
              dir={expanded.includes('geneList') ? 'down' : 'right'}
            />
          </Clickable>
          {expanded.includes('geneList') && (
            <div>
              {filters.geneList?.map((checkboxName, index) => (
                <div style={{ display: 'flex' }}>
                  <input
                    key={index}
                    style={{ marginRight: '0.25rem' }}
                    type='checkbox'
                    name={checkboxName}
                    checked={formState.geneList.includes(checkboxName)}
                    onChange={(e) =>
                      checkboxFn('geneList', checkboxName, e.target.checked)
                    }
                  />
                  {checkboxName}
                </div>
              ))}
            </div>
          )}
        </div>
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>Consequence</span>
          <Clickable
            style={{ float: 'right' }}
            onClick={() => toggleExpanded('consequenceList')}
          >
            <ClrIcon
              shape='angle'
              dir={expanded.includes('consequenceList') ? 'down' : 'right'}
            />
          </Clickable>
          {expanded.includes('consequenceList') && (
            <div>
              {filters.consequenceList?.map((checkboxName, index) => (
                <div style={{ display: 'flex' }}>
                  <input
                    key={index}
                    style={{ marginRight: '0.25rem' }}
                    type='checkbox'
                    name={checkboxName}
                    checked={formState.consequenceList.includes(checkboxName)}
                    onChange={(e) =>
                      checkboxFn(
                        'consequenceList',
                        checkboxName,
                        e.target.checked
                      )
                    }
                  />
                  {checkboxName}
                </div>
              ))}
            </div>
          )}
        </div>
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>ClinVar Significance</span>
          <Clickable
            style={{ float: 'right' }}
            onClick={() => toggleExpanded('clinicalSignificanceList')}
          >
            <ClrIcon
              shape='angle'
              dir={
                expanded.includes('clinicalSignificanceList') ? 'down' : 'right'
              }
            />
          </Clickable>
          {expanded.includes('clinicalSignificanceList') && (
            <div>
              {filters.clinicalSignificanceList?.map((checkboxName, index) => (
                <div style={{ display: 'flex' }}>
                  <input
                    key={index}
                    style={{ marginRight: '0.25rem' }}
                    type='checkbox'
                    name={checkboxName}
                    checked={formState.clinicalSignificanceList.includes(
                      checkboxName
                    )}
                    onChange={(e) =>
                      checkboxFn(
                        'clinicalSignificanceList',
                        checkboxName,
                        e.target.checked
                      )
                    }
                  />
                  {checkboxName}
                </div>
              ))}
            </div>
          )}
        </div>
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>Allele Count</span>
          <Clickable
            style={{ float: 'right' }}
            onClick={() => toggleExpanded('alleleCount')}
          >
            <ClrIcon
              shape='angle'
              dir={expanded.includes('alleleCount') ? 'down' : 'right'}
            />
          </Clickable>
          {expanded.includes('alleleCount') && (
            <div style={{ height: '2rem', margin: 'auto', width: '80%' }}>
              <Nouislider
                style={{ marginTop: '3rem' }}
                behaviour='drag'
                onEnd={(value) =>
                  sliderFn(
                    ['countMin', 'countMax'],
                    value.map((val) => +val)
                  )
                }
                range={{ min: filters.countMin, max: filters.countMax }}
                start={[
                  formState.countMin ?? filters.countMin,
                  formState.countMin ?? filters.countMax,
                ]}
                tooltips
                connect
              />
            </div>
          )}
        </div>
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>Allele Number</span>
          <Clickable
            style={{ float: 'right' }}
            onClick={() => toggleExpanded('alleleNumber')}
          >
            <ClrIcon
              shape='angle'
              dir={expanded.includes('alleleNumber') ? 'down' : 'right'}
            />
          </Clickable>
          {expanded.includes('alleleNumber') && (
            <div style={{ height: '2rem', margin: 'auto', width: '80%' }}>
              <Nouislider
                style={{ marginTop: '3rem' }}
                behaviour='drag'
                onEnd={(value) =>
                  sliderFn(
                    ['numberMin', 'numberMax'],
                    value.map((val) => +val)
                  )
                }
                range={{ min: filters.numberMin, max: filters.numberMax }}
                start={[
                  formState.numberMin ?? filters.numberMin,
                  formState.numberMax ?? filters.numberMax,
                ]}
                tooltips
                connect
              />
            </div>
          )}
        </div>
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>Allele Frequency</span>
          <Clickable
            style={{ float: 'right' }}
            onClick={() => toggleExpanded('alleleFrequency')}
          >
            <ClrIcon
              shape='angle'
              dir={expanded.includes('alleleFrequency') ? 'down' : 'right'}
            />
          </Clickable>
          {expanded.includes('alleleFrequency') && (
            <div style={{ height: '2rem', margin: 'auto', width: '80%' }}>
              <Nouislider
                style={{ marginTop: '3rem' }}
                behaviour='drag'
                onEnd={(value) =>
                  sliderFn(
                    ['frequencyMin', 'frequencyMax'],
                    value.map((val) => +val)
                  )
                }
                range={{ min: filters.frequencyMin, max: filters.frequencyMax }}
                start={[
                  formState.frequencyMin ?? filters.frequencyMin,
                  formState.frequencyMax ?? filters.frequencyMax,
                ]}
                tooltips
                connect
              />
            </div>
          )}
        </div>
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>Sort by</span>
          <Dropdown
            style={{ width: '100%' }}
            value={formState.sortBy}
            options={filters.sortByList.map((option) => ({
              label: option,
              value: option,
            }))}
            onChange={(e) => sortFn(e.value)}
          />
        </div>
      </div>
      <div style={{ position: 'absolute', bottom: '0.5rem' }}>
        <Button
          type='secondary'
          style={{ marginLeft: '0.75rem' }}
          onClick={() => clearFn()}
        >
          Clear
        </Button>
        <Button style={{ marginLeft: '0.75rem' }} onClick={() => submitFn()}>
          Apply
        </Button>
      </div>
    </div>
  );
};

export const VariantSearch = withCurrentWorkspace()(
  ({ select, selectedIds, workspace: { id, namespace } }) => {
    const [first, setFirst] = useState(0);
    const [inputErrors, setInputErrors] = useState([]);
    const [loading, setLoading] = useState(false);
    const [loadingMore, setLoadingMore] = useState(false);
    const [pageToken, setPageToken] = useState(null);
    const [searching, setSearching] = useState(false);
    const [searchResults, setSearchResults] = useState<Variant[]>([]);
    const [searchTerms, setSearchTerms] = useState('');
    const [totalCount, setTotalCount] = useState(null);
    const [filtersOpen, setFiltersOpen] = useState(false);
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
        sortBy: 'Variant ID',
      });
    const [variantFilters, setVariantFilters] = useState(null);
    const [resetFilters, setResetFilters] = useState(0);

    const searchVariants = async (newSearch: boolean, firstPage?: number) => {
      try {
        const { items, nextPageToken, totalSize } =
          await cohortBuilderApi().findVariants(namespace, id, {
            ...selectedFilters,
            searchTerm: searchTerms.trim(),
            pageToken: !!firstPage ? pageToken : null,
          });
        if (newSearch && items.length > 1) {
          const filters = await cohortBuilderApi().findVariantFilters(
            namespace,
            id,
            {
              searchTerm: searchTerms.trim(),
            }
          );
          setVariantFilters(filters);
        }
        setPageToken(nextPageToken);
        setSearchResults((prevState) =>
          firstPage ? [...prevState, ...items] : items
        );
        setTotalCount(totalSize);
        setFirst(firstPage || 0);
      } catch (error) {
        console.error(error);
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    };

    useEffect(() => {
      if (resetFilters > 0) {
        setLoading(true);
        searchVariants(false);
      }
    }, [resetFilters]);

    const handleInput = (event: any) => {
      const {
        key,
        target: { value },
      } = event;
      if (key === 'Enter') {
        if (value.trim().length < searchTrigger) {
          setInputErrors([
            `Minimum criteria search length is ${searchTrigger} characters`,
          ]);
        } else {
          const newInputErrors = validateInputForMySQL(value, searchTrigger);
          if (inputErrors.length > 0) {
            setInputErrors(newInputErrors);
          } else {
            setLoading(true);
            setSearching(true);
            searchVariants(true);
          }
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
      setSearching(false);
      setSearchTerms('');
      setSearchResults([]);
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
        sortBy: 'Variant ID',
      });
    };

    const clearFilters = () => {
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
        sortBy: 'Variant ID',
      });
      setFiltersOpen(false);
      setResetFilters((prevState) => prevState++);
    };

    const getParamId = (row: Variant) => `param${row.vid}`;

    const isSelected = (row: any) => {
      const paramId = getParamId(row);
      return selectedIds.includes(paramId);
    };

    const selectItem = (row: any) => {
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

    const handleSliderChange = (filters: string[], range: number[]) =>
      setSelectedFilters((prevState) => ({
        ...prevState,
        [filters[0]]: range[0],
        [filters[1]]: range[1],
      }));

    const handleSortByChange = (value: string) =>
      setSelectedFilters((prevState) => ({
        ...prevState,
        sortBy: value,
      }));

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
        <div style={{ fontSize: '11px' }}>
          SNP/Indel Variant search is currently a proof of concept and only
          contains data for chromosome 20
        </div>
        {searchResults.length > 1 && variantFilters && (
          <div style={{ position: 'relative' }}>
            <Clickable
              style={{ color: colors.primary }}
              onClick={() => setFiltersOpen((prevState) => !prevState)}
            >
              <ClrIcon shape='filter-2' className='is-solid' size={30} />
              Filter & Sort
            </Clickable>
            {filtersOpen && (
              <VariantFilters
                filters={variantFilters}
                formState={selectedFilters}
                checkboxFn={handleCheckboxChange}
                sliderFn={handleSliderChange}
                sortFn={handleSortByChange}
                clearFn={() => clearFilters()}
                submitFn={() => {
                  setFiltersOpen(false);
                  setLoading(true);
                  searchVariants(false);
                }}
              />
            )}
          </div>
        )}
        {loading ? (
          <SpinnerOverlay />
        ) : (
          searching && (
            <DataTable
              currentPageReportTemplate={
                displayResults.length > 0
                  ? `${first + 1} - ${
                      first + displayResults.length
                    } of ${totalCount}`
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
              scrollHeight='calc(100vh - 450px)'
              style={{ fontSize: '12px' }}
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
                      <ClrIcon
                        style={styles.selectedIcon}
                        shape='check-circle'
                        size='20'
                      />
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
