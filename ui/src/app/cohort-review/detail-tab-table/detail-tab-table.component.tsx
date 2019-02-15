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

const css = `
  body .p-datatable .p-sortable-column:not(.p-highlight):hover,
  body .p-datatable .p-sortable-column.p-highlight {
    color: #333333;
    background-color: #f4f4f4;
  }
  body .p-datatable .p-datatable-thead > tr > th {
    border: 0;
    border-bottom: 1px solid #c8c8c8;
    border-left: 1px solid #c8c8c8;
  }
  body .p-datatable .p-datatable-thead > tr > th:first-of-typr {
    border-left: 0;
  }
  body .p-datatable .p-column-title {
    display: flex;
  }
  .pi.pi-sort,
  .pi.pi-sort-up,
  .pi.pi-sort-down {
    display: none;
  }
  .p-datatable .p-datatable-scrollable-wrapper {
    border: 1px solid #c8c8c8;
  }
  .p-datatable .p-paginator.p-paginator-bottom {
    border: 0;
    margin-top: 20px;
    background: none;
    font-size: 12px;
    text-align: right;
  }
  body .p-paginator .p-paginator-prev,
  body .p-paginator .p-paginator-next,
  body .p-paginator .p-paginator-pages .p-paginator-page {
    border: 1px solid #cccccc;
    border-radius: 3px;
    background: #fafafa;
    color: #2691D0;
    height: auto;
    width: auto;
    min-width: 0;
    padding: 7px;
    margin: 0 2px;
    line-height: 0.5rem;
  }
  body .p-paginator .p-paginator-prev,
  body .p-paginator .p-paginator-next {
    height: 28px;
    width: 24px;
  }
  body .p-paginator .p-paginator-pages .p-paginator-page:focus {
    box-shadow: 0;
  }
  body .p-paginator .p-paginator-pages .p-paginator-page.p-highlight {
    background: #fafafa;
    color: rgba(0, 0, 0, .5);
  }
  `;

const styles = reactStyles({
  table: {
    fontSize: '12px',
  },
  tableBody: {
    padding: '5px',
    verticalAlign: 'top',
    textAlign: 'left',
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
      this.dt.filter(event.target.value, event.target.id, 'contains');
    }

    columnSort = (sortField: string) => {
      if (this.state.sortField === sortField) {
        const sortOrder = this.state.sortOrder === 1 ? -1 : 1;
        this.setState({sortOrder});
      } else {
        this.setState({sortField, sortOrder: 1});
      }
    }

    onPage = (event: any) => {
      this.setState({start: event.first});
    }

    render() {
      const {filters, loading, rows, start, sortField, sortOrder} = this.state;
      const data = this.state.data || [];
      const lastRowOfPage = (start + rows) > data.length
        ? start + rows - (start + rows - data.length) : start + rows;
      const pageReportTemplate = (start + 1) + ' - ' + lastRowOfPage + ' of ' + data.length
        + ' records ';
      const paginatorTemplate = 'CurrentPageReport PrevPageLink PageLinks NextPageLink';

      const columns = this.props.columns.map((col) => {
        const asc = sortField === col.name && sortOrder === 1;
        const desc = sortField === col.name && sortOrder === -1;
        const header = <React.Fragment>
          <span
            onClick={() => this.columnSort(col.name)}
            style={styles.columnHeader}>
            {col.displayName}
          </span>
          {asc && <i className='pi pi-arrow-up' style={styles.sortIcon} />}
          {desc && <i className='pi pi-arrow-down' style={styles.sortIcon} />}
          <i
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

        return <Column
          style={styles.tableBody}
          bodyStyle={styles.columnBody}
          key={col.name}
          field={col.name}
          header={header}
          sortable={true} />;
      });

      return <div style={{position: 'relative'}}>
        <style>{css}</style>
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
          paginatorTemplate={data.length ? paginatorTemplate : ''}
          currentPageReportTemplate={data.length ? pageReportTemplate : ''}
          onPage={this.onPage}
          first={start}
          rows={rows}
          totalRecords={data.length}
          scrollable={true}
          scrollHeight='calc(100vh - 350px)'
          autoLayout={true}
          emptyMessage=''>
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
