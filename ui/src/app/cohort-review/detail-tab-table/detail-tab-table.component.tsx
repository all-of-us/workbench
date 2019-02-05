import {Component, Input} from '@angular/core';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {PageFilterRequest, PageFilterType, SortOrder} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {Paginator} from 'primereact/paginator';
import * as React from 'react';

const styles = reactStyles({
  pDatatable: {
    fontSize: '12px'
  },
  pDatatableTbody: {
    padding: 0
  }
});

export interface DetailTabTableProps {
  tabname: string;
  columns: Array<any>;
  domain: string;
  filterType: PageFilterType;
  participantId: number;
  cdrId: number;
  namespace: string;
  workspaceId: string;
  cohortId: number;
}

export interface DetailTabTableState {
  data: Array<any>;
  loading: boolean;
  totalCount: number;
  start: number;
}

export class DetailTabTable extends React.Component<DetailTabTableProps, DetailTabTableState> {
  dt: any;
  constructor(props: DetailTabTableProps) {
    super(props);
    this.state = {
      data: [],
      loading: true,
      totalCount: null,
      start: 0
    };
  }

  onPageChange = (event: any) => {
    console.log(event);
    this.setState({
      start: event.first
    });
  }

  componentDidMount() {
    console.log(this.props);
    const pageFilterRequest = {
      page: 0,
      pageSize: 10000,
      sortOrder: SortOrder.Asc,
      sortColumn: this.props.columns[0].name,
      pageFilterType: this.props.filterType,
      domain: this.props.domain,
    } as PageFilterRequest;

    cohortReviewApi().getParticipantData(
      this.props.namespace,
      this.props.workspaceId,
      this.props.cohortId,
      this.props.cdrId,
      this.props.participantId,
      pageFilterRequest
    ).then(response => {
      this.setState({data: response.items, loading: false});
    });
  }

  render() {
    const dynamicColumns = this.props.columns.map((col) => {
      return <Column style={styles.pDatatableTbody} key={col.name} field={col.name} header={col.displayName}  sortable={true} filter={true} filterMatchMode='contains' />;
    });

    const footer = <Paginator
      first={this.state.start} rows={25} totalRecords={this.state.data.length} onPageChange={this.onPageChange}
      template='FirstPageLink PrevPageLink CurrentPageReport NextPageLink LastPageLink'></Paginator>;

    return <React.Fragment>
      <DataTable
        footer={footer}
        style={styles.pDatatable}
        ref={(el) => this.dt = el}
        value={this.state.data}
        loading={this.state.loading}
        first={this.state.start}
        rows={25}
        totalRecords={this.state.data.length}
        scrollable={true}
        scrollHeight='calc(100vh - 380px)'>
        {dynamicColumns}
      </DataTable>
    </React.Fragment>;
  }
}

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
  @Input('namespace') namespace: DetailTabTableProps['namespace'];
  @Input('workspaceId') workspaceId: DetailTabTableProps['workspaceId'];
  @Input('cohortId') cohortId: DetailTabTableProps['cohortId'];
  @Input('cdrId') cdrId: DetailTabTableProps['cdrId'];

  constructor() {
    super(DetailTabTable, ['tabname', 'columns', 'domain', 'filterType', 'participantId', 'namespace', 'workspaceId', 'cohortId', 'cdrId']);
  }
}
