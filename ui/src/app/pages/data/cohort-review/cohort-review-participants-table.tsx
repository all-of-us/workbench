import * as React from 'react';
import { useParams } from 'react-router';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { OverlayPanel } from 'primereact/overlaypanel';

import {
  CohortStatus,
  Filter,
  FilterColumns as Columns,
  Operator,
  PageFilterRequest as Request,
  ParticipantCohortStatus,
  SortOrder,
} from 'generated/fetch';

import { ClrIcon } from 'app/components/icons';
import { NumberInput } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import {
  getVocabOptions,
  initialFilterState,
  vocabOptions,
} from 'app/services/review-state.service';
import {
  cohortBuilderApi,
  cohortReviewApi,
} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { triggerEvent } from 'app/utils/analytics';
import { currentCohortReviewStore, useNavigation } from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';

const { useEffect, useRef, useState } = React;

const styles = reactStyles({
  backBtn: {
    padding: 0,
    border: 0,
    fontSize: '14px',
    color: colors.accent,
    background: 'transparent',
    cursor: 'pointer',
  },
  title: {
    marginTop: 0,
    fontSize: '20px',
    fontWeight: 600,
    color: colors.primary,
    overflow: 'auto',
  },
  description: {
    margin: '0 0 0.375rem',
    color: '#000000',
  },
  columnHeader: {
    background: '#f4f4f4',
    color: colors.primary,
    fontWeight: 600,
  },
  columnBody: {
    background: '#ffffff',
    padding: '0.75rem 0.75rem 0.45rem 1.125rem',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    borderBottom: 'none',
    lineHeight: '0.9rem',
    cursor: 'pointer',
    borderTop: `1px solid ${colors.tableBorder}`,
  },
  sortIcon: {
    marginTop: '4px',
    color: '#2691D0',
    fontSize: '0.75rem',
    float: 'right',
  },
  tableBody: {
    textAlign: 'left',
    lineHeight: '1.125rem',
  },
  error: {
    width: '50%',
    background: '#F7981C',
    color: '#ffffff',
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    marginTop: '0.375rem',
    padding: '8px',
  },
  filterIcon: {
    marginLeft: '0.45rem',
    padding: '2px 2px 1px 1px',
    borderRadius: '50%',
    fontWeight: 600,
    float: 'right',
  },
  filterOverlay: {
    left: '359.531px!important',
    maxHeight: 'calc(100vh - 360px)',
    overflow: 'auto',
  },
  textSearch: {
    width: '85%',
    borderRadius: '4px',
    backgroundColor: '#dae6ed',
    marginLeft: '5px',
  },
  numberInput: {
    width: '85%',
    border: 0,
    backgroundColor: 'transparent',
    outline: 'none',
  },
});
const filterIcons = {
  active: {
    ...styles.filterIcon,
    background: '#8bc990',
    color: '#ffffff',
  },
  default: {
    ...styles.filterIcon,
    color: '#262262',
  },
};
const EVENT_CATEGORY = 'Review Participant List';
const fields = [
  { field: 'participantId', name: 'Participant ID' },
  { field: 'birthDate', name: 'Date of Birth' },
  { field: 'deceased', name: 'Deceased' },
  { field: 'sexAtBirth', name: 'Sex at Birth' },
  { field: 'gender', name: 'Gender' },
  { field: 'race', name: 'Race' },
  { field: 'ethnicity', name: 'Ethnicity' },
  { field: 'status', name: 'Status' },
];
const defaultDemoFilters: any = {
  DECEASED: [
    { name: 'Yes', value: '1' },
    { name: 'No', value: '0' },
    { name: 'Select All', value: 'Select All' },
  ],
  STATUS: [
    { name: 'Included', value: CohortStatus.INCLUDED },
    { name: 'Excluded', value: CohortStatus.EXCLUDED },
    { name: 'Needs Further Review', value: CohortStatus.NEEDS_FURTHER_REVIEW },
    { name: 'Unreviewed', value: CohortStatus.NOTREVIEWED },
    { name: 'Select All', value: 'Select All' },
  ],
};
const reverseColumnEnum = {
  participantId: Columns.PARTICIPANTID,
  sexAtBirth: Columns.SEXATBIRTH,
  gender: Columns.GENDER,
  race: Columns.RACE,
  ethnicity: Columns.ETHNICITY,
  birthDate: Columns.BIRTHDATE,
  deceased: Columns.DECEASED,
  status: Columns.STATUS,
};
const numberOfRows = 25;
const formatStatusForText = (status: CohortStatus) => {
  return {
    [CohortStatus.EXCLUDED]: 'Excluded',
    [CohortStatus.INCLUDED]: 'Included',
    [CohortStatus.NEEDS_FURTHER_REVIEW]: 'Needs Further Review',
    [CohortStatus.NOTREVIEWED]: '',
  }[status];
};
const mapData = (participant: ParticipantCohortStatus) => {
  const {
    participantId,
    status,
    sexAtBirth,
    gender,
    race,
    ethnicity,
    birthDate,
    deceased,
  } = participant;
  return {
    participantId,
    status: formatStatusForText(status),
    sexAtBirth,
    gender: !!gender
      ? gender.charAt(0).toUpperCase() + gender.slice(1).toLowerCase()
      : gender,
    race,
    ethnicity,
    birthDate,
    deceased: deceased ? 'Yes' : null,
  };
};

export const CohortReviewParticipantsTable = ({ cohortReview }) => {
  const { ns, wsid, cid, crid } = useParams<MatchParams>();
  const [navigate] = useNavigation();
  const [apiError, setApiError] = useState(false);
  const [data, setData] = useState(null);
  const [demoFilters, setDemoFilters] = useState(defaultDemoFilters);
  const [filters, setFilters] = useState(
    JSON.parse(JSON.stringify(initialFilterState.participants))
  );
  const [focusedColumn, setFocusedColumn] = useState(null);
  const [loading, setLoading] = useState(false);
  const [pageState, setPageState] = useState({
    page: 0,
    pageSize: numberOfRows,
  });

  const [sortState, setSortState] = useState({
    sortField: 'participantId',
    sortOrder: 1 as 1 | 0 | -1 | null | undefined,
  });
  const [totalCount, setTotalCount] = useState(null);
  const initialRender = useRef(true);

  const mapFilters = () => {
    const filterArr = Object.keys(filters).reduce((acc, _type) => {
      const values = filters[_type];
      if (_type === Columns[Columns.PARTICIPANTID]) {
        if (values) {
          acc.push({
            property: Columns[_type],
            values: [values],
            operator: Operator.LIKE,
          } as Filter);
        }
      } else {
        if (!values.length) {
          acc.push(null);
        } else if (!values.includes('Select All')) {
          acc.push({
            property: Columns[_type],
            values: values,
            operator: Operator.IN,
          } as Filter);
        }
      }
      return acc;
    }, []);
    return filterArr.includes(null) ? null : filterArr;
  };

  const getParticipantStatuses = () => {
    const queryFilters = mapFilters();
    if (queryFilters === null) {
      setData([]);
      setLoading(false);
    } else {
      const query = {
        page: pageState.page,
        pageSize: pageState.pageSize,
        sortColumn: reverseColumnEnum[sortState.sortField],
        sortOrder: sortState.sortOrder === 1 ? SortOrder.ASC : SortOrder.Desc,
        filters: { items: queryFilters },
      } as Request;
      return cohortReviewApi().getParticipantCohortStatuses(
        ns,
        wsid,
        cohortReview.cohortReviewId,
        query
      );
    }
  };

  const initParticipantsTable = async () => {
    let updatedDemoFilters;
    const updatedFilters = filters;
    const promises = [];
    if (!cohortReview) {
      promises.push(
        getParticipantStatuses().then(
          ({ cohortReview: review, queryResultSize }) => {
            currentCohortReviewStore.next(review);
            if (!vocabOptions.getValue()) {
              getVocabOptions(ns, wsid);
            }
            setData(review.participantCohortStatuses.map(mapData));
            setTotalCount(queryResultSize);
          },
          (error) => {
            console.error(error);
            setLoading(false);
            setApiError(true);
          }
        )
      );
    } else {
      setData(cohortReview.participantCohortStatuses.map(mapData));
      setTotalCount(cohortReview.reviewSize);
    }
    promises.push(
      cohortBuilderApi()
        .findParticipantDemographics(ns, wsid)
        .then(
          ({ ethnicityList, genderList, raceList, sexAtBirthList }) => {
            const extract = (arr, _type?) =>
              fp.uniq([
                ...arr.map((i) => {
                  updatedFilters[_type].push(i.conceptId.toString());
                  return {
                    name: i.conceptName,
                    value: i.conceptId.toString(),
                  };
                }),
                { name: 'Select All', value: 'Select All' },
              ]) as string[];
            updatedDemoFilters = {
              ...demoFilters,
              RACE: extract(raceList, 'RACE'),
              GENDER: extract(genderList, 'GENDER'),
              ETHNICITY: extract(ethnicityList, 'ETHNICITY'),
              SEXATBIRTH: extract(sexAtBirthList, 'SEXATBIRTH'),
            };
            setDemoFilters(updatedDemoFilters);
            setFilters(updatedFilters);
          },
          (error) => {
            console.error(error);
            updatedDemoFilters = {
              ...demoFilters,
              RACE: [{ name: 'Select All', value: 'Select All' }],
              GENDER: [{ name: 'Select All', value: 'Select All' }],
              ETHNICITY: [{ name: 'Select All', value: 'Select All' }],
              SEXATBIRTH: [{ name: 'Select All', value: 'Select All' }],
            };
            setDemoFilters(updatedDemoFilters);
          }
        )
    );
    await Promise.all(promises);
    setLoading(false);
  };

  useEffect(() => {
    initParticipantsTable();
  }, []);

  const getTableData = async () => {
    setLoading(true);
    setApiError(false);
    const reviewResponse = await getParticipantStatuses();
    if (reviewResponse) {
      currentCohortReviewStore.next(reviewResponse.cohortReview);
      setData(
        reviewResponse.cohortReview.participantCohortStatuses.map(mapData)
      );
      setLoading(false);
      setTotalCount(reviewResponse.queryResultSize);
    }
  };

  useEffect(() => {
    if (!initialRender.current) {
      getTableData();
    }
  }, [filters, pageState, sortState]);

  useEffect(() => {
    if (initialRender.current) {
      initialRender.current = false;
    } else {
      setData(cohortReview.participantCohortStatuses.map(mapData));
      setTotalCount(cohortReview.reviewSize);
    }
  }, [cohortReview]);

  const onPage = (event: any) => {
    if (event.page !== pageState.page) {
      setLoading(true);
      setApiError(false);
      setPageState({
        ...pageState,
        page: event.page,
      });
    }
  };

  const columnSort = (sortField: string) => {
    if (sortField === 'participantId') {
      triggerEvent(
        EVENT_CATEGORY,
        'Click',
        'Sort - ID - Review Participant List'
      );
    }
    setLoading(true);
    setApiError(false);
    if (sortState.sortField === sortField) {
      setSortState({
        ...sortState,
        sortOrder: sortState.sortOrder === 1 ? -1 : 1,
      });
    } else {
      setSortState({
        sortField,
        sortOrder: 1,
      });
    }
  };

  const onInputChange = fp.debounce(300, (value) => {
    setFilters((prevFilters) => ({
      ...prevFilters,
      PARTICIPANTID: value,
    }));
  });

  const onSort = (event: any) => {
    columnSort(event.sortField);
  };

  const onCheckboxChange = (event, column) => {
    const { checked, value } = event.target;
    let columnFilter = filters[column];
    if (checked) {
      const options = demoFilters[column].map((opt) => opt.value);
      if (value === 'Select All') {
        columnFilter = options;
      } else {
        columnFilter.push(value);
        if (options.length - 1 === columnFilter.length) {
          columnFilter.push('Select All');
        }
      }
    } else {
      if (value === 'Select All') {
        columnFilter = [];
      } else {
        columnFilter.splice(columnFilter.indexOf(value), 1);
        if (columnFilter.includes('Select All')) {
          columnFilter.splice(columnFilter.indexOf('Select All'), 1);
        }
      }
    }
    setFilters((prevFilters) => ({
      ...prevFilters,
      [column]: columnFilter,
    }));
  };

  const onRowClick = (event: any) => {
    navigate([
      'workspaces',
      ns,
      wsid,
      'data',
      'cohorts',
      cid,
      'reviews',
      crid,
      'participants',
      event.data.participantId,
    ]);
  };

  const filterTemplate = (column: string) => {
    if (!data) {
      return '';
    }
    const colType = reverseColumnEnum[column];
    const options = demoFilters[colType];
    const filtered =
      (column === 'participantId' && filters.PARTICIPANTID) ||
      (column !== 'participantId' && !filters[colType].includes('Select All'));
    let fl: any, ip: any;
    return (
      <span>
        {data && (
          <i
            className='pi pi-filter'
            style={filtered ? filterIcons.active : filterIcons.default}
            onClick={(e) => {
              if (e.target instanceof Element) {
                setFocusedColumn(e.target as HTMLElement);
                fl.toggle(e);
              }
              const { name } = fields.find((it) => it.field === column);
              triggerEvent(
                EVENT_CATEGORY,
                'Click',
                `Filter - ${name} - Review Participant List`
              );
              if (column === 'participantId') {
                ip.focus();
              }
            }}
          />
        )}
        <OverlayPanel
          style={styles.filterOverlay}
          className='filterOverlay'
          ref={(el) => {
            fl = el;
          }}
          showCloseIcon={true}
          dismissable={true}
          appendTo={focusedColumn}
        >
          {column === 'participantId' && (
            <div style={styles.textSearch}>
              <i className='pi pi-search' style={{ margin: '0 5px' }} />
              <NumberInput
                ref={(i) => (ip = i)}
                style={styles.numberInput}
                onChange={(v) => onInputChange(v)}
                placeholder={'Search'}
              />
            </div>
          )}
          {!!options &&
            options.map((opt, i) => (
              <div
                key={i}
                style={{
                  borderTop:
                    opt.name === 'Select All' && options.length > 1
                      ? '1px solid #ccc'
                      : 0,
                  padding:
                    opt.name === 'Select All'
                      ? '0.75rem 0.75rem'
                      : '0.45rem 0.6rem',
                }}
              >
                <input
                  style={{ width: '1.05rem', height: '1.05rem' }}
                  type='checkbox'
                  name={opt.name}
                  checked={filters[colType].includes(opt.value)}
                  value={opt.value}
                  onChange={(e) => onCheckboxChange(e, colType)}
                  disabled={loading}
                />
                <label style={{ paddingLeft: '0.6rem' }}> {opt.name} </label>
              </div>
            ))}
        </OverlayPanel>
      </span>
    );
  };

  const paginatorTemplate = () => {
    return `CurrentPageReport${
      data && totalCount > numberOfRows
        ? ' PrevPageLink PageLinks NextPageLink'
        : ''
    }`;
  };

  const pageReportTemplate = () => {
    if (data !== null) {
      const start = pageState.page * numberOfRows;
      const lastRowOfPage =
        numberOfRows > data.length ? start + data.length : start + numberOfRows;
      return `${start + 1} - ${lastRowOfPage} of ${totalCount} records `;
    }
    return;
  };

  const errorMessage = () => {
    if (loading || data?.length || (!data && !apiError)) {
      return false;
    }
    let message: string;
    if (data && data.length === 0) {
      message =
        'Data cannot be found. Please review your filters and try again.';
    } else if (!data && apiError) {
      message = `Sorry, the request cannot be completed. Please try refreshing the page or
           contact Support in the left hand navigation.`;
    }
    return (
      <div style={styles.error}>
        <ClrIcon
          style={{ margin: '0 0.75rem 0 0.375rem' }}
          className='is-solid'
          shape='exclamation-triangle'
          size='22'
        />
        {message}
      </div>
    );
  };

  const getColumns = () => {
    return fields.map((col) => {
      const { sortField, sortOrder } = sortState;
      const bodyStyle =
        col.field === 'participantId'
          ? {
              ...styles.columnBody,
              color: '#2691D0',
              borderTop: `1px solid #c8c8c8`,
            }
          : styles.columnBody;
      const asc = sortField === col.field && sortOrder === 1;
      const desc = sortField === col.field && sortOrder === -1;
      const header = (
        <>
          <span
            onClick={() => columnSort(col.field)}
            style={styles.columnHeader}
          >
            {col.name}
          </span>
          {col.field !== 'birthDate' && filterTemplate(col.field)}
          {asc && (
            <i
              className='pi pi-arrow-up'
              style={styles.sortIcon}
              onClick={() => columnSort(col.field)}
            />
          )}
          {desc && (
            <i
              className='pi pi-arrow-down'
              style={styles.sortIcon}
              onClick={() => columnSort(col.field)}
            />
          )}
        </>
      );
      return (
        <Column
          style={styles.tableBody}
          bodyStyle={bodyStyle}
          key={col.field}
          field={col.field}
          header={header}
          sortable
        />
      );
    });
  };

  return (
    <div>
      <DataTable
        style={{ fontSize: '12px' }}
        value={loading ? null : data}
        first={pageState.page * numberOfRows}
        sortField={sortState.sortField}
        sortOrder={sortState.sortOrder}
        onSort={onSort}
        lazy
        paginator={!loading && data?.length > 0}
        onPage={onPage}
        alwaysShowPaginator={false}
        paginatorTemplate={paginatorTemplate()}
        currentPageReportTemplate={pageReportTemplate()}
        breakpoint='0px'
        rows={numberOfRows}
        totalRecords={totalCount}
        onRowClick={onRowClick}
        scrollable
        scrollHeight='calc(100vh - 350px)'
        footer={errorMessage()}
      >
        {getColumns()}
      </DataTable>
      {loading && (
        <div style={{ marginTop: '7.5rem', position: 'relative' }}>
          <SpinnerOverlay />
        </div>
      )}
    </div>
  );
};
