import {Component, Input} from '@angular/core';
import {SpinnerOverlay} from 'app/components/spinners';
import {WorkspaceData} from 'app/resolvers/workspace';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {urlParamsStore} from 'app/utils/navigation';
import {PageFilterRequest, PageFilterType, SortOrder} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {InputText} from 'primereact/inputtext';
import {OverlayPanel} from 'primereact/overlaypanel';
import * as React from 'react';

const styles = reactStyles({
  table: {
    fontSize: '12px',
    border: '1px solid #ccc'
  },
  tableBody: {
    padding: '5px',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    lineHeight: '0.6rem'
  },
  columnHeader: {
    background: '#f4f4f4',
    color: '#262262',
    fontWeight: 600
  },
  columnBody: {
    background: '#ffffff',
    padding: '5px',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    lineHeight: '0.6rem'
  },
  filterIcon: {
    color: '#0086C1',
    fontSize: '0.5rem',
    float: 'right'
  },
  sortIcon: {
    color: '#2691D0',
    fontSize: '0.4rem'
  }
});

export interface DetailTabTableProps {
  tabname: string;
  columns: Array<any>;
  domain: string;
  filterType: PageFilterType;
  participantId: number;
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
    overlays = {};
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

    componentDidUpdate(prevProps: any) {
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
      const {cid} = urlParamsStore.getValue();
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
        +cid,
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

    onFilter = (event: any) => {
      this.setState({filters: event.filters});
    }

    onSort = (event: any) => {
      this.setState({sortField: event.sortField, sortOrder: event.sortOrder});
    }

    columnFilter = (event: any) => {
      const {id, value} = event.target;
      this.dt.filter(value, id, 'contains');
      console.log(this.dt);
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
      const {filters, loading, rows, start, sortField, sortOrder} = this.state;
      const data = this.state.data || [];

      const columns = this.props.columns.map((col) => {
        const filter = <React.Fragment>
          <i
            id={col.name + '-filter'}
            className='pi pi-filter'
            style={styles.filterIcon}
            onClick={(e) => this.overlays[col.name].toggle(e)} />
          <OverlayPanel ref={(el) => this.overlays[col.name] = el} appendTo={document.body}>
            <InputText
              value={filters[col.name] ? filters[col.name].value : ''}
              className='p-inputtext p-column-filter'
              id={col.name}
              onChange={this.columnFilter} />
          </OverlayPanel>
        </React.Fragment>;

        const asc = sortField === col.name && sortOrder === 1;
        const desc = sortField === col.name && sortOrder === -1;
        const header = <span
          onClick={() => this.columnSort(col.name)}
          style={styles.columnHeader}>
          {col.displayName}
          {asc && <i className='pi pi-arrow-up' style={styles.sortIcon} />}
          {desc && <i className='pi pi-arrow-down' style={styles.sortIcon} />}
        </span>;

        return <Column
          style={styles.tableBody}
          bodyStyle={styles.columnBody}
          key={col.name}
          field={col.name}
          header={header}
          sortable={true}
          filter={true}
          filterElement={filter} />;
      });

      const style = `
        body .p-datatable .p-sortable-column:not(.p-highlight):hover,
        body .p-datatable .p-sortable-column.p-highlight {
          color: #333333;
          background-color: #f4f4f4;
        }
        .pi.pi-sort,
        .pi.pi-sort-up,
        .pi.pi-sort-down {
          display: none;
        }
      `;

      return <div style={{position: 'relative'}}>
        <style>{style}</style>
        {data && <DataTable
          style={styles.table}
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

  constructor() {
    super(DetailTabTable, [
      'tabname',
      'columns',
      'domain',
      'filterType',
      'participantId',
    ]);
  }
}
