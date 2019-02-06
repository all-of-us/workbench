import {Component, Input} from '@angular/core';
import {WorkspaceData} from 'app/resolvers/workspace';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {PageFilterRequest, PageFilterType, SortOrder} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {Paginator} from 'primereact/paginator';
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
  rowsData: Array<any>;
  loading: boolean;
  totalCount: number;
  start: number;
  rows: number;
}

export const DetailTabTable = withCurrentWorkspace()(
  class extends React.Component<DetailTabTableProps, DetailTabTableState> {
    dt: any;
    constructor(props: DetailTabTableProps) {
      super(props);
      this.state = {
        data: [],
        rowsData: [],
        loading: true,
        totalCount: null,
        start: 0,
        rows: 25
      };
    }

    onPageChange = (event: any) => {
      console.log(event);
      const startIndex = event.first;
      const endIndex = event.first + this.state.rows;
      this.setState({
        start: event.first,
        rowsData: this.state.data.slice(startIndex, endIndex)
      });
    }

    componentDidMount() {
      console.log(this.props);
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
          rowsData: response.items.slice(0, this.state.rows),
          loading: false
        });
      });
    }

    render() {
      const columns = this.props.columns.map((col) => {
        return <Column
          style={styles.pDatatableTbody}
          key={col.name} field={col.name}
          header={col.displayName}
          sortable={true}
          filter={true}
          filterMatchMode='contains' />;
      });

      const footer = <Paginator
        first={this.state.start}
        rows={this.state.rows}
        totalRecords={this.state.data.length}
        onPageChange={this.onPageChange}
        template='FirstPageLink PrevPageLink CurrentPageReport NextPageLink LastPageLink'>
      </Paginator>;

      return <React.Fragment>
        <DataTable
          footer={footer}
          style={styles.pDatatable}
          ref={(el) => this.dt = el}
          value={this.state.rowsData}
          loading={this.state.loading}
          first={this.state.start}
          rows={this.state.rows}
          totalRecords={this.state.data.length}
          scrollable={true}
          scrollHeight='calc(100vh - 380px)'>
          {columns}
        </DataTable>
      </React.Fragment>;
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
