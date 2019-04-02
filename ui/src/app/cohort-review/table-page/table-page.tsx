import {Component} from '@angular/core';
import * as fp from 'lodash/fp';

import {
  cohortReviewStore,
  filterStateStore,
  multiOptions,
  vocabOptions
} from 'app/cohort-review/review-state.service';
import {css} from 'app/cohort-review/review-utils/primeReactCss.utils';
import {TextInput} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi, cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore, navigate, navigateByUrl, urlParamsStore} from 'app/utils/navigation';

import {
  CohortStatus,
  Filter,
  Operator,
  PageFilterType,
  ParticipantCohortStatus,
  ParticipantCohortStatusColumns as Columns,
  ParticipantCohortStatuses as Request,
  SortOrder,
} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {OverlayPanel} from 'primereact/overlaypanel';
import * as React from 'react';

const fields = [
  {field: 'participantId', name: 'Participant ID'},
  {field: 'birthDate', name: 'Date of Birth'},
  {field: 'deceased', name: 'Deceased'},
  {field: 'gender', name: 'Sex'},
  {field: 'race', name: 'Race'},
  {field: 'ethnicity', name: 'Ethnicity'},
  {field: 'status', name: 'Status'},
];
const rows = 25;
const styles = reactStyles({
  backBtn: {
    padding: 0,
    border: 0,
    fontSize: '14px',
    color: '#2691d0',
    background: 'transparent',
    cursor: 'pointer',
  },
  title: {
    marginTop: 0,
    fontSize: '20px',
    fontWeight: 600,
    color: '#262262',
  },
  description: {
    margin: '0.5rem 0',
    color: '#000000',
  },
  container: {
    position: 'relative',
    minHeight: '15rem'
  },
  table: {
    fontSize: '12px',
  },
  tableBody: {
    textAlign: 'left',
    lineHeight: '0.6rem'
  },
  columnHeader: {
    background: '#f4f4f4',
    color: '#262262',
    fontWeight: 600,
  },
  columnBody: {
    background: '#ffffff',
    padding: '0.5rem 0.5rem 0.3rem 0.75rem',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    borderBottom: 'none',
    lineHeight: '0.6rem',
    cursor: 'pointer',
  },
  graphColumnBody: {
    background: '#ffffff',
    padding: '5px',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    lineHeight: '0.6rem',
    width: '2rem',
  },
  sortIcon: {
    marginTop: '3px',
    color: '#2691D0',
    fontSize: '0.4rem',
    float: 'right'
  },
  filterIcon: {
    float: 'right'
  },
  overlayHeader: {
    padding: '0.3rem',
  },
  caretIcon: {
    fontSize: '0.6rem',
    paddingLeft: '0.4rem',
    color: '#0086C1',
    cursor: 'pointer',
  },
  filterBorder: {
    paddingTop: '0.5rem',
    borderTop: '1px solid #ccc',
  },
  graphStyle: {
    borderLeft: 'none',
    width: '2rem',
  },
  headerStyle: {
    color: '#2691D0',
    fontSize: '14px',
    fontWeight: 'bold',
    width: '20rem',
    textOverflow: 'ellipsis',
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    margin: 'auto',
    paddingTop: '0.5rem',
    textAlign: 'center',
  },
  filterOverlay: {
    left: '359.531px!important',
    maxHeight: 'calc(100vh - 360px)',
    overflow: 'auto'
  }
});

const idColumn = {
  ...styles.columnBody,
  color: '#2691D0'
};
let multiFilters: any;
const reverseColumnEnum = {
  participantId: Columns.PARTICIPANTID,
  gender: Columns.GENDER,
  race: Columns.RACE,
  ethnicity: Columns.ETHNICITY,
  birthDate: Columns.BIRTHDATE,
  deceased: Columns.DECEASED,
  status: Columns.STATUS
};

export interface ParticipantsTableState {
  data: Array<any>;
  loading: boolean;
  page: number;
  sortField: string;
  sortOrder: number;
  total: number;
  filters: any;
}

export const ParticipantsTable = withCurrentWorkspace()(
  class extends React.Component<{workspace: WorkspaceData}, ParticipantsTableState> {
    filterInput: Function;
    constructor(props: any) {
      super(props);
      this.state = {
        data: null,
        loading: true,
        page: 0,
        sortField: 'participantId',
        sortOrder: 1,
        total: null,
        filters: filterStateStore.getValue().participants
      };
      this.filterInput = fp.debounce(300, () => this.getTableData());
    }

    componentDidMount() {
      const {filters} = this.state;
      const {cdrVersionId, id, namespace} = this.props.workspace;
      const {cid} = urlParamsStore.getValue();
      if (!multiOptions.getValue()) {
        cohortBuilderApi().getParticipantDemographics(+cdrVersionId).then(data => {
          const extract = (arr, _type?) => fp.uniq([
            ...arr.map(i => {
              filters[_type].push(i.conceptName);
              return {
                name: _type === Columns.GENDER
                  ? this.formatGenderForText(i.conceptName) : i.conceptName,
                value: i.conceptName
              };
            }),
            {name: 'Select All', value: 'Select All'}
          ]) as string[];
          multiFilters = {
            RACE: extract(data.raceList, Columns.RACE),
            GENDER: extract(data.genderList, Columns.GENDER),
            ETHNICITY: extract(data.ethnicityList, Columns.ETHNICITY),
            DECEASED: [
              {name: 'Yes', value: '1'},
              {name: 'No', value: '0'},
              {name: 'Select All', value: 'Select All'}
            ],
            STATUS: [
              {name: 'Included', value: CohortStatus.INCLUDED},
              {name: 'Excluded', value: CohortStatus.EXCLUDED},
              {name: 'Needs Further Review', value: CohortStatus.NEEDSFURTHERREVIEW},
              {name: 'Unreviewed', value: CohortStatus.NOTREVIEWED},
              {name: 'Select All', value: 'Select All'}
            ]
          };
          multiOptions.next(multiFilters);
          this.setState({filters});
          setTimeout(() => this.getTableData());
        });
      } else {
        multiFilters = multiOptions.getValue();
        const review = cohortReviewStore.getValue();
        if (review) {
          this.setState({
            data: review.participantCohortStatuses.map(this.mapData),
            loading: false,
            page: review.page,
            total: review.queryResultSize
          });
        } else {
          this.getTableData();
        }
      }
      if (!vocabOptions.getValue()) {
        cohortReviewApi().getVocabularies(namespace, id, cid, +cdrVersionId)
          .then(response => {
            const vocabFilters = {source: {}, standard: {}};
            response.items.forEach(item => {
              const type = item.type.toLowerCase();
              vocabFilters[type][item.domain] = [
                ...(vocabFilters[type][item.domain] || []),
                item.vocabulary
              ];
            });
            vocabOptions.next(vocabFilters);
          });
      }
    }

    componentWillUnmount(): void {
      const {filters} = this.state;
      const filterState = filterStateStore.getValue();
      filterState.participants = filters;
      filterStateStore.next(filterState);
    }

    getTableData(): void {
      try {
        const {page, sortField, sortOrder} = this.state;
        const {cdrVersionId, id, namespace} = this.props.workspace;
        const {cid} = urlParamsStore.getValue();
        const filters = this.mapFilters();
        if (filters === null) {
          this.setState({
            data: [],
            loading: false,
          });
        } else {
          const query = {
            page: page,
            pageSize: rows,
            sortColumn: reverseColumnEnum[sortField],
            sortOrder: sortOrder === 1 ? SortOrder.Asc : SortOrder.Desc,
            filters: {items: filters},
            pageFilterType: PageFilterType.ParticipantCohortStatuses,
          } as Request;
          cohortReviewApi().getParticipantCohortStatuses(namespace, id, cid, +cdrVersionId, query)
            .then(review => {
              cohortReviewStore.next(review);
              this.setState({
                data: review.participantCohortStatuses.map(this.mapData),
                loading: false,
                total: review.queryResultSize
              });
            });
        }
      } catch (error) {
        console.log(error);
        this.setState({
          loading: false
        });
      }
    }

    mapFilters = () => {
      const {filters} = this.state;
      const filterArr =  Object.keys(filters).reduce((acc, _type) => {
        const values = filters[_type];
        if (_type === Columns[Columns.PARTICIPANTID]) {
          if (values) {
            acc.push({
              property: Columns[_type],
              values: [values],
              operator: Operator.LIKE
            } as Filter);
          }
        } else {
          if (!values.length) {
            acc.push(null);
          } else if (!values.includes('Select All')) {
            acc.push({
              property: Columns[_type],
              values: values,
              operator: Operator.IN
            } as Filter);
          }
        }
        return acc;
      }, []);
      return filterArr.includes(null) ? null : filterArr;
    }

    mapData = (participant: ParticipantCohortStatus) => {
      const {participantId, status, gender, race, ethnicity, birthDate, deceased} = participant;
      return {
        participantId,
        status: this.formatStatusForText(status),
        gender: !!gender ? gender.charAt(0).toUpperCase() + gender.slice(1).toLowerCase() : gender,
        race,
        ethnicity,
        birthDate,
        deceased: deceased ? 'Yes' : null
      };
    }

    formatStatusForText(status: CohortStatus): string {
      return {
        [CohortStatus.EXCLUDED]: 'Excluded',
        [CohortStatus.INCLUDED]: 'Included',
        [CohortStatus.NEEDSFURTHERREVIEW]: 'Needs Further Review',
        [CohortStatus.NOTREVIEWED]: '',
      }[status];
    }

    formatGenderForText(gender: string): string {
      return {
        AMBIGUOUS: 'Ambiguous',
        FEMALE: 'Female',
        MALE: 'Male',
        OTHER: 'Other',
        UNKNOWN: 'Unknown'
      }[gender];
    }

    backToCohort() {
      const {id, namespace} = this.props.workspace;
      const {cid} = urlParamsStore.getValue();
      navigateByUrl( `/workspaces/${namespace}/${id}/cohorts/build?cohortId=${cid}`);
    }

    onRowClick = (event: any) => {
      const {id, namespace} = this.props.workspace;
      const {cid} = urlParamsStore.getValue();
      navigate([
        'workspaces',
        namespace,
        id,
        'cohorts',
        cid,
        'review',
        'participants',
        event.data.participantId
      ]);
    }

    onPage = (event: any) => {
      const {page} = this.state;
      if (event.page !== page) {
        this.setState({loading: true, page: event.page});
        setTimeout(() => this.getTableData());
      }
    }

    columnSort = (sortField: string) => {
      if (this.state.sortField === sortField) {
        const sortOrder = this.state.sortOrder === 1 ? -1 : 1;
        this.setState({loading: true, sortOrder});
      } else {
        this.setState({loading: true, sortField, sortOrder: 1});
      }
      setTimeout(() => this.getTableData());
    }

    filterTemplate(column: string) {
      const {data, filters} = this.state;
      if (!data) {
        return {};
      }
      const colType = reverseColumnEnum[column];
      const options = multiFilters[colType];
      let fl: any;

      return <span>
        {data &&
          <i className='pi pi-filter' style={styles.filterIcon} onClick={(e) => fl.toggle(e)}/>}
        <OverlayPanel style={styles.filterOverlay} className='filterOverlay'
                      ref={(el) => {fl = el; }} showCloseIcon={true} dismissable={true}>
          {column === 'participantId' &&
            <TextInput
              style={{width: '90%', marginLeft: '5%'}}
              value={filters.PARTICIPANTID}
              onChange={this.onInputChange} />}
          {column !== 'participantId' && options.map((opt, i) => (
            <div key={i} style={{borderTop: opt.name === 'Select All' ? '1px solid #ccc' : 'none',
              padding: opt.name === 'Select All' ? '0.5rem 0.5rem' : '0.3rem 0.4rem'}} >
              <input style={{width: '0.7rem',  height: '0.7rem'}} type='checkbox' name={opt.name}
                     checked={filters[colType].includes(opt.value)} value={opt.value}
                     onChange={(e) => this.onCheckboxChange(e, colType)}/>
              <label style={{paddingLeft: '0.4rem'}}> {opt.name} </label>
            </div>
          ))}
        </OverlayPanel>
      </span>;
    }

    onCheckboxChange = (event, column) => {
      const {filters} = this.state;
      const {checked, value} = event.target;
      if (checked) {
        const options = multiFilters[column].map(opt => opt.value);
        if (value === 'Select All') {
          filters[column] = options;
        } else {
          filters[column].push(value);
          if (options.length - 1 === filters[column].length) {
            filters[column].push('Select All');
          }
        }
      } else {
        if (value === 'Select All') {
          filters[column] = [];
        } else {
          filters[column].splice(filters[column].indexOf(value), 1);
          if (filters[column].includes('Select All')) {
            filters[column].splice(filters[column].indexOf('Select All'), 1);
          }
        }
      }
      this.setState({loading: true, filters});
      setTimeout(() => this.getTableData());
    }

    onInputChange = (value: any) => {
      const {filters} = this.state;
      filters.PARTICIPANTID = value;
      this.setState({loading: true, filters});
      this.filterInput(value);
    }

    render() {
      const {data, loading, page, sortField, sortOrder, total} = this.state;
      const cohort = currentCohortStore.getValue();
      const start = page * rows;
      let pageReportTemplate;
      if (data !== null) {
        const lastRowOfPage = rows > data.length ? start + data.length : start + rows;
        pageReportTemplate = `${start + 1} - ${lastRowOfPage} of ${total} records `;
      }
      let paginatorTemplate = 'CurrentPageReport';
      if (data && total > rows) {
        paginatorTemplate += ' PrevPageLink PageLinks NextPageLink';
      }
      const columns = fields.map(col => {
        const bodyStyle = col.field === 'participantId' ? idColumn : styles.columnBody;
        const asc = sortField === col.field && sortOrder === 1;
        const desc = sortField === col.field && sortOrder === -1;
        const header = <React.Fragment>
          <span
            onClick={() => this.columnSort(col.field)}
            style={styles.columnHeader}>
            {col.name}
          </span>
          {col.field !== 'birthDate' && this.filterTemplate(col.field)}
          {asc && <i className='pi pi-arrow-up' style={styles.sortIcon} />}
          {desc && <i className='pi pi-arrow-down' style={styles.sortIcon} />}
        </React.Fragment>;
        return <Column
          style={styles.tableBody}
          bodyStyle={bodyStyle}
          key={col.field}
          field={col.field}
          header={header}
          sortable/>;
      });
      return <div>
        <style>{css}</style>
        <button
          style={styles.backBtn}
          type='button'
          title='Go back to cohort'
          onClick={() => this.backToCohort()}>
          Back to cohort
        </button>
        <h4 style={styles.title}>Review Sets for {cohort.name}</h4>
        <div style={styles.description}>{cohort.description}</div>
        {data && <DataTable
          style={styles.table}
          value={data}
          first={start}
          sortField={sortField}
          sortOrder={sortOrder}
          lazy
          paginator
          onPage={this.onPage}
          paginatorTemplate={data.length ? paginatorTemplate : ''}
          currentPageReportTemplate={data.length ? pageReportTemplate : ''}
          rows={rows}
          totalRecords={total}
          onRowClick={this.onRowClick}
          scrollable
          scrollHeight='calc(100vh - 350px)'>
          {columns}
        </DataTable>}
        {loading && <SpinnerOverlay />}
      </div>;
    }
  }
);

@Component ({
  template: '<div #root></div>'
})
export class TablePage extends ReactWrapperBase {
  constructor() {
    super(ParticipantsTable, []);
  }
}
