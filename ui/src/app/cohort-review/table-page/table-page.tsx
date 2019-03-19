import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ClrDatagridStateInterface} from '@clr/angular';
import * as fp from 'lodash/fp';
import {Subscription} from 'rxjs/Subscription';

import {ClearButtonFilterComponent} from 'app/cohort-review/clearbutton-filter/clearbutton-filter.component';
import {MultiSelectFilterComponent} from 'app/cohort-review/multiselect-filter/multiselect-filter.component';
import {Participant} from 'app/cohort-review/participant.model';
import {cohortReviewStore, vocabOptions} from 'app/cohort-review/review-state.service';
import {css} from 'app/cohort-review/review-utils/primeReactCss.utils';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi, cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore, currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';

import {
  CohortReview,
  Filter,
  Operator,
  PageFilterType,
  ParticipantCohortStatusColumns,
  ParticipantCohortStatusColumns as Columns,
  ParticipantCohortStatuses as Request,
  SortOrder,
} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';
import {from} from 'rxjs/observable/from';

function isMultiSelectFilter(filter): filter is MultiSelectFilterComponent {
  return (filter instanceof MultiSelectFilterComponent);
}

function isClearButtonFilter(filter): filter is ClearButtonFilterComponent {
  return (filter instanceof ClearButtonFilterComponent);
}
const fields = [
  {
    field: 'participantId',
    name: 'Participant ID',
  },
  {
    field: 'birthDate',
    name: 'DOB',
  },
  {
    field: 'gender',
    name: 'Sex',
  },
  {
    field: 'race',
    name: 'Race',
  },
  {
    field: 'ethnicity',
    name: 'Ethnicity',
  },
  {
    field: 'formattedStatusText',
    name: 'Status',
  }
];
const rows = 25;
const styles = reactStyles({
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
    padding: '5px',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    borderBottom: 'none',
    lineHeight: '0.6rem',
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

  filterIcon: {
    color: '#0086C1',
    fontSize: '0.5rem',
    float: 'right'
  },
  sortIcon: {
    color: '#2691D0',
    fontSize: '0.4rem'
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

export interface ParticipantsTableState {
  data: Array<any>;
  filteredData: Array<any>;
  loading: boolean;
  start: number;
  sortField: string;
  sortOrder: number;
}

export const ParticipantsTable = withCurrentWorkspace()(
  class extends React.Component<{}, ParticipantsTableState> {

    readonly ColumnEnum = Columns;
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
    isFiltered = [];
    cohortName: string;
    totalParticipantCount: number;
    tab = 'participants';
    reportInit = false;

    constructor(props: any) {
      super(props);
      this.state = {
        data: null,
        filteredData: null,
        loading: true,
        start: 0,
        sortField: null,
        sortOrder: 1,
      };
    }

    componentDidMount() {
      this.loading = false;
      const cid = currentCohortStore.getValue().id;
      this.cohortName = currentCohortStore.getValue().name;
      this.subscription = cohortReviewStore.subscribe(review => {
        this.review = review;
        this.participants = review.participantCohortStatuses.map(Participant.fromStatus);
        this.setState({data: this.participants, loading: false});
        this.totalParticipantCount = review.matchedParticipantCount;
      });
      const {cdrVersionId, id, namespace} = currentWorkspaceStore.getValue();
      const cdrid = +cdrVersionId;
      cohortBuilderApi().getParticipantDemographics(cdrid).then(data => {
        const extract = arr => fp.uniq(arr.map(i => i.conceptName)) as string[];
        this.races = extract(data.raceList);
        this.genders = extract(data.genderList);
        this.ethnicities = extract(data.ethnicityList);
      });
      if (!vocabOptions.getValue()) {
        cohortReviewApi().getVocabularies(namespace, id, cid, cdrid)
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

    refresh(state: ClrDatagridStateInterface) {
      setTimeout(() => this.loading = true, 0);
      console.log('Datagrid state: ');
      console.dir(state);

      /* Populate the query with page / pagesize and then defaults */
      const query = {
        page: Math.floor(state.page.from / state.page.size),
        pageSize: state.page.size,
        sortColumn: Columns.PARTICIPANTID,
        sortOrder: SortOrder.Asc,
        filters: {items: []},
        pageFilterType: PageFilterType.ParticipantCohortStatuses,
      } as Request;

      if (state.sort) {
        const sortby = (state.sort.by) as string;
        query.sortColumn = this.ReverseColumnEnum[sortby];
        query.sortOrder = state.sort.reverse
          ? SortOrder.Desc
          : SortOrder.Asc;
      }

      this.isFiltered = [];
      if (state.filters) {
        for (const filter of state.filters) {
          if (isMultiSelectFilter(filter)) {
            const property = filter.property;
            this.isFiltered.push(property);

            const operator = Operator.IN;
            query.filters.items.push({
              property,
              values: filter.selection.value,
              operator
            } as Filter);
          } else if (isClearButtonFilter(filter)) {
            const property = filter.property;
            this.isFiltered.push(property);
            let operator = Operator.EQUAL;
            if (filter.property === ParticipantCohortStatusColumns.PARTICIPANTID ||
              filter.property === ParticipantCohortStatusColumns.BIRTHDATE) {
              operator = Operator.LIKE;
            }
            query.filters.items.push({
              property,
              values: [filter.selection.value],
              operator
            } as Filter);
          } else {
            const {property, value} = filter as any;
            const operator = Operator.EQUAL;
            query.filters.items.push({property, values: [value], operator} as Filter);
          }
        }
      }

      const {ns, wsid, cid} = urlParamsStore.getValue();
      const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);

      console.log('Participant page request parameters:');
      console.dir(query);

      return from(cohortReviewApi()
        .getParticipantCohortStatuses(ns, wsid, cid, cdrid, query))
        .do(_ => this.loading = false)
        .subscribe(review => {
          cohortReviewStore.next(review);
        });
    }

    isSelected(column: string) {
      return this.isFiltered.indexOf(column) > -1;
    }

    ngOnDestroy() {
      this.subscription.unsubscribe();
    }

    setTab(tab: string) {
      this.tab = tab;
      if (tab === 'report' && !this.reportInit) {
        this.reportInit = true;
      }
    }

    onSort = (event: any) => {
      this.setState({sortField: event.sortField, sortOrder: event.sortOrder});
    }

    columnSort = (sortField: string) => {
      if (this.state.sortField === sortField) {
        const sortOrder = this.state.sortOrder === 1 ? -1 : 1;
        this.setState({sortOrder});
      } else {
        this.setState({sortField, sortOrder: 1});
      }
    }

    render() {
      const {data, loading, sortField, sortOrder, start} = this.state;
      console.log(sortOrder);
      console.log(sortField);
      const columns = fields.map(col => {
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
          key={col.field}
          field={col.field}
          header={header}
          sortable/>;
      })
      return <div>
        <style>{css}</style>
        {data && <DataTable
          value={data}
          sortField={sortField}
          sortOrder={sortOrder}
          onSort={this.onSort}
          first={start}
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
