import {Component} from '@angular/core';
import * as fp from 'lodash/fp';

import {
  cohortReviewStore,
  multiOptions,
  vocabOptions
} from 'app/cohort-review/review-state.service';
import {css} from 'app/cohort-review/review-utils/primeReactCss.utils';
import {TextInput} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi, cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore, navigate, urlParamsStore} from 'app/utils/navigation';

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
    color: '#2691D0',
    fontSize: '0.4rem',
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
    constructor(props: any) {
      super(props);
      this.state = {
        data: null,
        loading: true,
        page: 0,
        sortField: 'participantId',
        sortOrder: 1,
        total: null,
        filters: {RACE: ['Select All'], GENDER: ['Select All'], ETHNICITY: ['Select All'], STATUS: ['Select All']}
      };
    }

    componentDidMount() {
      const {cdrVersionId, id, namespace} = this.props.workspace;
      const {cid} = urlParamsStore.getValue();
      this.getTableData();
      if (!multiOptions.getValue()) {
        cohortBuilderApi().getParticipantDemographics(+cdrVersionId).then(data => {
          const extract = (arr, _type?) => fp.uniq([
            ...arr.map(i => {
              return {
                name: _type === Columns.GENDER
                  ? this.formatGenderForText(i.conceptName) : i.conceptName,
                value: i.conceptName
              };
            }),
            {name: 'Select All', value: 'Select All'}
          ]) as string[];
          multiFilters = {
            RACE: extract(data.raceList),
            GENDER: extract(data.genderList, Columns.GENDER),
            ETHNICITY: extract(data.ethnicityList),
            STATUS: [
              {name: 'Included', value: CohortStatus.INCLUDED},
              {name: 'Excluded', value: CohortStatus.EXCLUDED},
              {name: 'Needs Further Review', value: CohortStatus.NEEDSFURTHERREVIEW},
              {name: 'Unreviewed', value: CohortStatus.NOTREVIEWED},
              {name: 'Select All', value: 'Select All'}
            ]
          };
          multiOptions.next(multiFilters);
        });
      } else {
        multiFilters = multiOptions.getValue();
      }
      if (!vocabOptions.getValue()) {
        cohortReviewApi().getVocabularies(namespace, id, cid, +cdrVersionId)
          .then(response => {
            const vocabFilters = {Source: {}, Standard: {}};
            response.items.forEach(item => {
              vocabFilters[item.type][item.domain] = [
                ...(vocabFilters[item.type][item.domain] || []),
                item.vocabulary
              ];
            });
            vocabOptions.next(vocabFilters);
          });
      }
    }

    getTableData(): void {
      const {filters, page, sortField, sortOrder} = this.state;
      const {cdrVersionId, id, namespace} = this.props.workspace;
      const {cid} = urlParamsStore.getValue();
      const query = {
        page: page,
        pageSize: rows,
        sortColumn: reverseColumnEnum[sortField],
        sortOrder: sortOrder === 1 ? SortOrder.Asc : SortOrder.Desc,
        filters: {items: this.mapFilters()},
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

    mapFilters = () => {
      const {filters} = this.state;
      return Object.keys(filters).reduce((acc, _type) => {
        const values = filters[_type].filter(val => val !== 'Select All');
        if (values.length) {
          const filter = {
            property: Columns[_type],
            values: values,
            operator: Operator.IN
          } as Filter;
          acc.push(filter);
        }
        return acc;
      }, []);
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
      navigate(['/workspaces', namespace, id, 'cohorts']);
    }

    onRowClick = (event: any) => {
      const {id, namespace} = this.props.workspace;
      const {cid} = urlParamsStore.getValue();
      console.log(event);
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
      const colType = reverseColumnEnum[column]
      const options = multiFilters[colType];
      // const counts = {total: 0};
      // data.forEach(item => {
      //   counts[item[colName]] = !!counts[item[colName]] ? counts[item[colName]] + 1 : 1;
      //   counts.total++;
      // });
      // let options: Array<any>;
      // if (colName === 'domain') {
      //   options = domains.map(option => {
      //     return {name: option, count: counts[option] || 0};
      //   });
      // } else {
      //   const vocabs = colName === 'standardVocabulary'
      //     ? vocabOptions.getValue().Standard
      //     : vocabOptions.getValue().Source;
      //   options = vocabs[domain] ? vocabs[domain].map(option => {
      //     return {name: option, count: counts[option] || 0};
      //   }) : [];
      // }
      // options.push({name: 'Select All', count: counts.total});
      // if (checkedItems[colName].find(i => i === 'Select All')) {
      //   checkedItems[colName] = options.map(opt => opt.name);
      // }
      let fl: any;

      return <span>
        {data && <i className='pi pi-filter' onClick={(e) => fl.toggle(e)}/>}
        <OverlayPanel style={{left: '359.531px!important'}} className='filterOverlay'
                      ref={(el) => {fl = el; }} showCloseIcon={true} dismissable={true}>
          {column === 'participantId' && <TextInput />}
          {column !== 'participantId' && options.map((opt, i) => (
            <div key={i} style={{borderTop: opt.name === 'Select All' ? '1px solid #ccc' : 'none',
              padding: opt.name === 'Select All' ? '0.5rem 0.5rem' : '0.3rem 0.4rem'}} >
              <input style={{width: '0.7rem',  height: '0.7rem'}} type='checkbox' name={opt.name}
                     checked={filters[colType].includes(opt.value)} value={opt.value}
                     onChange={(e) => this.updateData(e, colType)}/>
              <label style={{paddingLeft: '0.4rem'}}> {opt.name} </label>
            </div>
          ))}
        </OverlayPanel>
      </span>;
    }

    updateData = (event, column) => {
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
        filters[column].splice(filters[column].indexOf(value), 1);
        if (filters[column].includes('Select All')) {
          filters[column].splice(filters[column].indexOf('Select All'), 1);
        }
      }
      this.setState({loading: true, filters: filters});
      setTimeout(() => this.getTableData());
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
          {asc && <i className='pi pi-arrow-up' style={styles.sortIcon} />}
          {desc && <i className='pi pi-arrow-down' style={styles.sortIcon} />}
          {!['deceased', 'birthDate'].includes(col.field) && this.filterTemplate(col.field)}
        </React.Fragment>;
        return <Column
          style={styles.tableBody}
          bodyStyle={bodyStyle}
          key={col.field}
          field={col.field}
          header={header}
          sortable/>;
      })
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
