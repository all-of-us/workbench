import {Component, Input} from '@angular/core';
import {Clickable} from 'app/components/buttons';
import {SpinnerOverlay} from 'app/components/spinners';
import {WorkspaceData} from 'app/resolvers/workspace';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {PageFilterRequest, PageFilterType, SortOrder} from 'generated/fetch';
import {Column} from 'primereact/column';
import {Inplace, InplaceContent, InplaceDisplay} from 'primereact/components/inplace/Inplace';
import {DataTable} from 'primereact/datatable';
import {InputText} from 'primereact/inputtext';
import * as React from 'react';

const styles = reactStyles({
  pDatatable: {
    fontSize: '12px',
    border: '1px solid #ccc'
  },
  pDatatableTbody: {
    padding: '5px',
    verticalAlign: 'top',
    borderLeft: 0,
    borderRight: 0,
    lineHeight: '0.6rem'
  },
  sortIcons: {
    color: '#2691D0',
    fontSize: '0.5rem'
  }
});

export interface DetailTabTableProps {
  tabname: string;
  columns: Array<any>;
  domain: string;
  filterType: PageFilterType;
  participantId: number;
  cohortId: number;
  workspace: WorkspaceData;
}

export interface DetailTabTableState {
  data: Array<any>;
  loading: boolean;
  filters: any;
  start: number;
  rows: number;
  sortField: string;
  sortOrder: number;
}

export const DetailTabTable = withCurrentWorkspace()(
  class extends React.Component<DetailTabTableProps, DetailTabTableState> {
    dt: any;
    constructor(props: DetailTabTableProps) {
      super(props);
      this.state = {
        data: null,
        loading: true,
        filters: {},
        start: 0,
        rows: 25,
        sortField: null,
        sortOrder: 1
      };
    }

    componentDidMount() {
      this.getParticipantData();
    }

    componentDidUpdate(prevProps) {
      if (prevProps.participantId !== this.props.participantId) {
        this.setState({
          data: null,
          loading: true,
          filters: {}
        });
        this.getParticipantData();
      }
    }

    getParticipantData() {
      const {cdrVersionId, id, namespace} = this.props.workspace;
      const pageFilterRequest = {
        page: 0,
        pageSize: 10000,
        sortOrder: SortOrder.Asc,
        sortColumn: this.props.columns[0].name,
        pageFilterType: this.props.filterType,
        domain: this.props.domain,
      } as PageFilterRequest;

      cohortReviewApi().getParticipantData(
        namespace,
        id,
        this.props.cohortId,
        +cdrVersionId,
        this.props.participantId,
        pageFilterRequest
      ).then(response => {
        this.setState({
          data: response.items,
          loading: false,
        });
      });
    }

    onFilter = (event) => {
      this.setState({filters: event.filters});
    }

    onSort = (event) => {
      this.setState({sortField: event.sortField, sortOrder: event.sortOrder});
    }

    columnFilter = (event) => {
      const {id, value} = event.target;
      this.dt.filter(value, id, 'contains');
      console.log(this.dt);
    }

    render() {
      const {filters, loading, rows, start, sortField, sortOrder} = this.state;
      const data = this.state.data || [];

      const columns = this.props.columns.map((col) => {
        const filter = <Inplace
          closable={true}>
          <InplaceDisplay>
            <i className='pi pi-filter' />
          </InplaceDisplay>
          <InplaceContent>
            <InputText
              value={filters[col.name] ? filters[col.name].value : ''}
              className='p-inputtext p-column-filter'
              id={col.name}
              onChange={this.columnFilter} />
          </InplaceContent>
        </Inplace>;
        const asc = sortField === col.name && sortOrder === 1;
        const desc = sortField === col.name && sortOrder === -1;
        const header = <div>
          {col.displayName}
          {asc && <i className='pi pi-arrow-up' style={styles.sortIcons} />}
          {desc && <i className='pi pi-arrow-down' style={styles.sortIcons} />}
        </div>;

        return <Column
          style={styles.pDatatableTbody}
          key={col.name}
          field={col.name}
          header={header}
          sortable={true}
          filter={true}
          filterElement={filter} />;
      });

      const style = `
        .pi.pi-sort,
        .pi.pi-sort-up,
        .pi.pi-sort-down{
          display: none;
        }
      `;

      return <div style={{position: 'relative'}}>
        <style>{style}</style>
        {data && <DataTable
          style={styles.pDatatable}
          ref={(el) => this.dt = el}
          value={data}
          filters={filters}
          onFilter={this.onFilter}
          sortField={sortField}
          sortOrder={sortOrder}
          onSort={this.onSort}
          paginator={true}
          paginatorTemplate='FirstPageLink PrevPageLink CurrentPageReport NextPageLink LastPageLink'
          first={start}
          rows={rows}
          totalRecords={data.length}
          scrollable={true}
          scrollHeight='calc(100vh - 380px)'>
          {columns}
        </DataTable>}
        {loading && <SpinnerOverlay />}
      </div>;
    }
  }
);

@Component ({
  selector : 'app-detail-tab-table',
  template: '<div #root></div>'
})
export class DetailTabTableComponent extends ReactWrapperBase {
  @Input('tabname') tabname: DetailTabTableProps['tabname'];
  @Input('columns') columns: DetailTabTableProps['columns'];
  @Input('domain') domain: DetailTabTableProps['domain'];
  @Input('filterType') filterType: DetailTabTableProps['filterType'];
  @Input('participantId') participantId: DetailTabTableProps['participantId'];
  @Input('cohortId') cohortId: DetailTabTableProps['cohortId'];

  constructor() {
    super(DetailTabTable, [
      'tabname',
      'columns',
      'domain',
      'filterType',
      'participantId',
      'cohortId',
    ]);
  }
}
