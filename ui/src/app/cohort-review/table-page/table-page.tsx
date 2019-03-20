import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from 'app/cohort-review/participant.model';
import {cohortReviewStore, vocabOptions} from 'app/cohort-review/review-state.service';
import {css} from 'app/cohort-review/review-utils/primeReactCss.utils';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi, cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore, navigate, urlParamsStore} from 'app/utils/navigation';

import {
  CohortReview,
  PageFilterType,
  ParticipantCohortStatusColumns as Columns,
  ParticipantCohortStatuses as Request,
  SortOrder,
} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';

const fields = [
  {field: 'participantId', name: 'Participant ID'},
  {field: 'birthDate', name: 'Date of Birth'},
  {field: 'formattedDeceasedText', name: 'Deceased'},
  {field: 'formattedGenderText', name: 'Sex'},
  {field: 'race', name: 'Race'},
  {field: 'ethnicity', name: 'Ethnicity'},
  {field: 'formattedStatusText', name: 'Status'}
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
}

export interface ParticipantsTableState {
  data: Array<any>;
  loading: boolean;
  page: number;
  sortField: string;
  sortOrder: number;
  total: number;
}

export const ParticipantsTable = withCurrentWorkspace()(
  class extends React.Component<{workspace: WorkspaceData}, ParticipantsTableState> {

    readonly ReverseColumnEnum = {
      participantId: Columns.PARTICIPANTID,
      gender: Columns.GENDER,
      race: Columns.RACE,
      ethnicity: Columns.ETHNICITY,
      birthDate: Columns.BIRTHDATE,
      status: Columns.STATUS
    };

    participants: Participant[];

    review: CohortReview;
    loading: boolean;
    subscription: Subscription;
    genders: string[] = [];
    races: string[] = [];
    ethnicities: string[] = [];
    cohortName: string;
    tab = 'participants';

    constructor(props: any) {
      super(props);
      this.state = {
        data: null,
        loading: true,
        page: 0,
        sortField: 'participantId',
        sortOrder: 1,
        total: null
      };
    }

    componentDidMount() {
      const {cdrVersionId, id, namespace} = this.props.workspace;
      const {cid} = urlParamsStore.getValue();
      this.getTableData();
      cohortBuilderApi().getParticipantDemographics(+cdrVersionId).then(data => {
        const extract = arr => fp.uniq(arr.map(i => i.conceptName)) as string[];
        this.races = extract(data.raceList);
        this.genders = extract(data.genderList);
        this.ethnicities = extract(data.ethnicityList);
      });
      if (!vocabOptions.getValue()) {
        cohortReviewApi().getVocabularies(namespace, id, cid, +cdrVersionId)
          .then(response => {
            const filters = {Source: {}, Standard: {}};
            response.items.forEach(item => {
              filters[item.type][item.domain] = [
                ...(filters[item.type][item.domain] || []),
                item.vocabulary
              ];
            });
            vocabOptions.next(filters);
          });
      }
    }

    getTableData(): void {
      const {page, sortField, sortOrder} = this.state;
      const {cdrVersionId, id, namespace} = this.props.workspace;
      const {cid} = urlParamsStore.getValue();
      const query = {
        page: page,
        pageSize: rows,
        sortColumn: this.ReverseColumnEnum[sortField],
        sortOrder: sortOrder === 1 ? SortOrder.Asc : SortOrder.Desc,
        filters: {items: []},
        pageFilterType: PageFilterType.ParticipantCohortStatuses,
      } as Request;
      cohortReviewApi().getParticipantCohortStatuses(namespace, id, cid, +cdrVersionId, query)
        .then(review => {
          cohortReviewStore.next(review);
          this.setState({
            data: review.participantCohortStatuses.map(Participant.fromStatus),
            loading: false,
            total: review.queryResultSize
          });
        });
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

    render() {
      const {data, loading, page, sortField, sortOrder, total} = this.state;
      const cohort = currentCohortStore.getValue();
      const start = page * rows;
      let pageReportTemplate;
      if (data !== null) {
        const lastRowOfPage = rows > data.length ? rows - data.length : start + rows;
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
