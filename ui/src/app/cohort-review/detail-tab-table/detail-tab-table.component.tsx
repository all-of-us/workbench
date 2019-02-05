import {Component, Input} from '@angular/core';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase} from 'app/utils';
import {PageFilterRequest, PageFilterType, SortOrder} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';

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
}

export class DetailTabTable extends React.Component<DetailTabTableProps, DetailTabTableState> {
  dt: any;
  constructor(props: DetailTabTableProps) {
    super(props);
    this.state = {
      data: [],
      loading: true,
      totalCount: null
    };
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
      return <Column key={col.name} field={col.name} header={col.displayName}  sortable={true} filter={true} filterMatchMode='contains' />;
    });

    return <React.Fragment>
      <DataTable ref={(el) => this.dt = el} value={this.state.data} loading={this.state.loading}
                 paginator={true} rows={25} totalRecords={this.state.data.length}>
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
