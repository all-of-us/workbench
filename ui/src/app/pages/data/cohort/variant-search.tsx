import * as React from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { CriteriaType, Domain, Variant } from 'generated/fetch';

import { AlertDanger } from 'app/components/alert';
import { Clickable } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { TextInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { domainToTitle } from 'app/pages/data/cohort/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { datatableStyles } from 'app/styles/datatable';
import {
  reactStyles,
  validateInputForMySQL,
  withCurrentWorkspace,
} from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';

const { useEffect, useRef, useState } = React;

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
    whiteSpace: 'nowrap',
  },
  selectIcon: {
    margin: '2px 0.75rem 2px 2px',
    color: colorWithWhiteness(colors.success, -0.5),
    cursor: 'pointer',
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
});

const columns = [
  {
    name: 'Variant Id',
    style: { ...styles.columnNameHeader, borderLeft: 0, width: '20%' },
  },
  {
    name: 'Gene',
    style: {
      ...styles.columnNameHeader,
      width: '10%',
      paddingLeft: '0',
      paddingRight: '0.75rem',
    },
  },
  {
    name: 'Consequence',
    style: {
      ...styles.columnNameHeader,
      width: '15%',
      paddingLeft: '0',
      paddingRight: '0.75rem',
    },
  },
  {
    name: 'Protein Change',
    style: { ...styles.columnNameHeader, paddingLeft: '0' },
  },
  {
    name: 'ClinVar Significance',
    style: styles.columnNameHeader,
  },
  {
    name: 'Allele Count',
    style: styles.columnNameHeader,
  },
  {
    name: 'Allele Number',
    style: styles.columnNameHeader,
  },
  {
    name: 'Allele Frequency',
    style: styles.columnNameHeader,
  },
];

const searchTrigger = 2;

export const VariantSearch = withCurrentWorkspace()(
  ({ select, selectedIds, workspace }) => {
    const [first, setFirst] = useState(0);
    const [hoverId, setHoverId] = useState(null);
    const [inputErrors, setInputErrors] = useState([]);
    const [loading, setLoading] = useState(false);
    const [loadingMore, setLoadingMore] = useState(false);
    const [pageToken, setPageToken] = useState(null);
    const [searching, setSearching] = useState(false);
    const [searchResults, setSearchResults] = useState<Variant[]>([]);
    const [searchTerms, setSearchTerms] = useState('');
    const [totalCount, setTotalCount] = useState(null);

    const searchVariants = async (searchString: string, firstPage?: number) => {
      try {
        const { id, namespace } = workspace;
        const { items, nextPageToken, totalSize } =
          await cohortBuilderApi().findVariants(
            namespace,
            id,
            searchString,
            pageToken
          );
        setPageToken(nextPageToken);
        setSearchResults((prevState) => [...prevState, ...items]);
        setTotalCount(totalSize);
        setFirst(firstPage || 0);
      } catch (error) {
        console.error(error);
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    };

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
            searchVariants(value.trim());
          }
        }
      }
    };

    const handlePage = (rows: number, firstPage: number) => {
      if (searchResults.length >= rows + firstPage - 1) {
        return;
      }
      if (searchResults && !pageToken) {
        // We've loaded at least one page, and there's no more data to load.
        return;
      }
      setLoadingMore(true);
      searchVariants(searchTerms);
    };

    const clearSearch = () => {
      setSearching(false);
      setSearchTerms('');
      setSearchResults([]);
    };

    const getParamId = (row: Variant) => `param${row.vid}`;

    const isSelected = (row: any) => {
      const paramId = getParamId(row);
      return selectedIds.includes(paramId);
    };

    const onNameHover = (el: HTMLDivElement, id: string) => {
      if (el.offsetWidth < el.scrollWidth) {
        setHoverId(id);
      }
    };

    const selectItem = (row: any) => {
      const param = {
        parameterId: getParamId(row),
        parentId: null,
        type: CriteriaType.NONE,
        name: `Variant ${row.vid}`,
        group: false,
        domainId: Domain.SNPINDELVARIANT,
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

    const renderColumnWithToolTip = (columnLabel, toolTip) => {
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
    };

    return (
      <>
        <style>{datatableStyles}</style>
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
            <TooltipTrigger side='top' content={<></>}>
              <ClrIcon
                style={styles.infoIcon}
                className='is-solid'
                shape='info-standard'
              />
            </TooltipTrigger>
          </div>
        </div>
        <div style={{ display: 'table', height: '100%', width: '100%' }}>
          BRCA2
        </div>
        {loading ? (
          <SpinnerOverlay />
        ) : (
          searching && (
            <DataTable
              first={first}
              lazy
              loading={loadingMore}
              onPage={(e) => handlePage(e.first, e.rows)}
              paginator
              paginatorTemplate='PrevPageLink CurrentPageReport NextPageLink'
              rows={25}
              scrollable
              scrollHeight='calc(100vh - 350px)'
              totalRecords={totalCount}
              value={searchResults}
            >
              <Column
                field='vid'
                header='Variant Id'
                headerStyle={{ width: '20%' }}
                body={({ vid }) => <div>{vid}</div>}
                sortable
              />
              <Column
                field='gene'
                header='Gene'
                body={({ gene }) => <div>{gene}</div>}
              />
              <Column
                field='consequence'
                header='Consequence'
                headerStyle={{ width: '15%' }}
                body={({ consequence }) => <div>{consequence}</div>}
              />
              <Column
                field='proteinChange'
                header='Protein Change'
                body={({ proteinChange }) => <div>{proteinChange || '-'}</div>}
              />
              <Column
                field='clinVarSignificance'
                header='ClinVar Significance'
                body={({ clinVarSignificance }) => (
                  <div>{clinVarSignificance || '-'}</div>
                )}
              />
              <Column
                field='alleleCount'
                header='Allele Count'
                body={({ alleleCount }) => <div>{alleleCount}</div>}
              />
              <Column
                field='alleleNumber'
                header='Allele Number'
                body={({ alleleNumber }) => <div>{alleleNumber}</div>}
              />
              <Column
                field='alleleFrequency'
                header='Allele Frequency'
                body={({ alleleFrequency }) => <div>{alleleFrequency}</div>}
              />
            </DataTable>
          )
          // <>
          //   {searching && searchResults.length === 0 && (
          //     <div>No results found</div>
          //   )}
          //   {searchResults.length > 0 && (
          //     <div
          //       ref={(el) => {
          //         scrollTable.current = el;
          //         setResultsLoaded(true);
          //       }}
          //       id='scrollArea'
          //       style={{ height: '25rem', overflow: 'auto' }}
          //     >
          //       <table
          //         ref={(el) => {
          //           scrollBody.current = el;
          //         }}
          //         className='p-datatable' style={styles.table}>
          //         <thead className='p-datatable-thead'>
          //           <tr style={{ height: '3rem', position: 'sticky', top: 0 }}>
          //             {columns.map((column, index) => (
          //               <th key={index} style={column.style as CSSProperties}>
          //                 {column.name}
          //               </th>
          //             ))}
          //           </tr>
          //         </thead>
          //         <tbody
          //           id='listItem' className='p-datatable-tbody'>
          //           {searchResults.map((row, index) => (
          //             <React.Fragment key={index}>
          //               <tr style={{ height: '2.625rem' }}>
          //                 <td
          //                   style={{
          //                     ...styles.columnBodyName,
          //                     width: '20%',
          //                     paddingRight: '0.75rem',
          //                   }}
          //                 >
          //                   {isSelected(row) ? (
          //                     <ClrIcon
          //                       style={styles.selectedIcon}
          //                       shape='check-circle'
          //                       size='20'
          //                     />
          //                   ) : (
          //                     <ClrIcon
          //                       style={styles.selectIcon}
          //                       shape='plus-circle'
          //                       size='16'
          //                       onClick={() => selectItem(row)}
          //                     />
          //                   )}
          //                   {row.vid}
          //                 </td>
          //                 <td
          //                   style={{
          //                     ...styles.columnBodyName,
          //                     width: '10%',
          //                     paddingRight: '0.75rem',
          //                   }}
          //                 >
          //                   {row.gene}
          //                 </td>
          //                 <td style={styles.columnBodyName}>
          //                   {row.consequence}
          //                 </td>
          //                 <td
          //                   style={{
          //                     ...styles.columnBodyName,
          //                     paddingLeft: '0.3rem',
          //                     paddingRight: '0.75rem',
          //                   }}
          //                 >
          //                   {row.proteinChange || '-'}
          //                 </td>
          //                 <td
          //                   style={{
          //                     ...styles.columnBodyName,
          //                     paddingLeft: '0.3rem',
          //                   }}
          //                 >
          //                   {row.clinVarSignificance || '-'}
          //                 </td>
          //                 <td
          //                   style={{
          //                     ...styles.columnBodyName,
          //                     paddingLeft: '0.3rem',
          //                   }}
          //                 >
          //                   {row.alleleCount}
          //                 </td>
          //                 <td
          //                   style={{
          //                     ...styles.columnBodyName,
          //                     paddingLeft: '0.3rem',
          //                   }}
          //                 >
          //                   {row.alleleNumber}
          //                 </td>
          //                 <td
          //                   style={{
          //                     ...styles.columnBodyName,
          //                     paddingLeft: '0.3rem',
          //                   }}
          //                 >
          //                   {row.alleleFrequency}
          //                 </td>
          //               </tr>
          //             </React.Fragment>
          //           ))}
          //         </tbody>
          //       </table>
          //     </div>
          //   )}
          // </>
        )}
      </>
    );
  }
);
