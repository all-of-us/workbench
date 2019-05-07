import * as React from 'react';

import {validate} from 'validate.js';

import {dataSetApi, workspacesApi} from 'app/services/swagger-fetch-clients';

import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {SmallHeader} from 'app/components/headers';
import {TextArea} from 'app/components/inputs';
import {CheckBox, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import colors from 'app/styles/colors';
import {convertQueryToText} from 'app/utils/big-query-queries';
import {summarizeErrors} from 'app/utils/index';
import {navigate} from 'app/utils/navigation';
import {
  DataSetQuery,
  DataSetRequest,
  DomainValuePair,
  FileDetail
} from 'generated/fetch';

interface Props {
  closeFunction: Function;
  includesAllParticipants: boolean;
  selectedConceptSetIds: number[];
  selectedCohortIds: number[];
  selectedValues: DomainValuePair[];
  workspaceNamespace: string;
  workspaceId: string;
}

interface State {
  conflictDataSetName: boolean;
  existingNotebooks: FileDetail[];
  exportToNewNotebook: boolean;
  loading: boolean;
  missingDataSetInfo: boolean;
  name: string;
  notebookName: string;
  notebooksLoading: boolean;
  queries: Array<DataSetQuery>;
}


class NewDataSetModal extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      conflictDataSetName: false,
      exportToNewNotebook: false,
      loading: false,
      missingDataSetInfo: false,
      name: '',
      notebookName: '',
      existingNotebooks: [],
      notebooksLoading: false,
      queries: [],
    };
  }

  componentDidMount() {
    this.generateQuery();
    this.loadNotebooks();
  }

  private async loadNotebooks() {
    try {
      const {workspaceNamespace, workspaceId} = this.props;
      this.setState({notebooksLoading: true});
      const existingNotebooks =
        await workspacesApi().getNoteBookList(workspaceNamespace, workspaceId);
      this.setState({existingNotebooks});
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({notebooksLoading: false});
    }
  }

  async saveDataSet() {
    const {workspaceNamespace, workspaceId} = this.props;
    if (!this.state.name) {
      return;
    }
    this.setState({conflictDataSetName: false, missingDataSetInfo: false, loading: true});
    const request = {
      name: this.state.name,
      includesAllParticipants: this.props.includesAllParticipants,
      description: '',
      conceptSetIds: this.props.selectedConceptSetIds,
      cohortIds: this.props.selectedCohortIds,
      values: this.props.selectedValues
    };
    try {
      await dataSetApi().createDataSet(
        workspaceNamespace, workspaceId, request);
      if (!this.state.exportToNewNotebook) {
        this.props.closeFunction();
      } else {
        await dataSetApi().exportToNotebook(
          workspaceNamespace, workspaceId,
          {dataSetRequest: request, notebookName: this.state.notebookName, newNotebook: true});
        navigate(['workspaces',
          workspaceNamespace,
          workspaceId, 'notebooks', this.state.notebookName + '.ipynb']);
      }
    } catch (e) {
      if (e.status === 409) {
        this.setState({conflictDataSetName: true, loading: false});
      } else if (e.status === 400) {
        this.setState({missingDataSetInfo: true, loading: false});
      }
    }
  }

  changeExportToNewNotebook() {
    this.setState({exportToNewNotebook: !this.state.exportToNewNotebook});
  }

  async generateQuery() {
    const {workspaceNamespace, workspaceId} = this.props;
    const dataSet: DataSetRequest = {
      name: '',
      conceptSetIds: this.props.selectedConceptSetIds,
      cohortIds: this.props.selectedCohortIds,
      values: this.props.selectedValues,
      includesAllParticipants: this.props.includesAllParticipants,
    };
    const sqlQueries = await dataSetApi().generateQuery(workspaceNamespace, workspaceId, dataSet);
    this.setState({queries: sqlQueries.queryList});
  }

  render() {
    const {
      conflictDataSetName,
      exportToNewNotebook,
      loading,
      missingDataSetInfo,
      name,
      notebookName,
      notebooksLoading,
      existingNotebooks,
      queries,
    } = this.state;

    const errors = validate({name, notebookName}, {
      name: {
        presence: {allowEmpty: false}
      },
      notebookName: {
        presence: {allowEmpty: !exportToNewNotebook},
        exclusion: {
          within: existingNotebooks.map(fd => fd.name.slice(0, -6)),
          message: 'already exists'
        }
      }
    });
    return <Modal loading={loading}>
      <ModalTitle>Save Dataset</ModalTitle>
      <ModalBody>
        <div>
          {conflictDataSetName &&
          <AlertDanger>DataSet with same name exist</AlertDanger>
          }
          {missingDataSetInfo &&
          <AlertDanger> Data state cannot save as some information is missing</AlertDanger>
          }
          <TextInput type='text' autoFocus placeholder='Data Set Name'
                     value={name}
                     onChange={v => this.setState({
                       name: v, conflictDataSetName: false
                     })}/>
        </div>
        <div style={{display: 'flex', alignItems: 'center', marginTop: '1rem'}}>
          <CheckBox style={{height: 17, width: 17}}
                    onChange={() => this.changeExportToNewNotebook()} />
          <div style={{marginLeft: '.5rem',
            color: colors.black}}>Export to new Python notebook</div>
        </div>
        {exportToNewNotebook && <React.Fragment>
          {(queries.length === 0 || notebooksLoading) && <SpinnerOverlay />}
          <TextArea disabled={true} onChange={() => {}} style={{marginTop: '1rem'}}
                    value={queries.map(query =>
                      convertQueryToText(name, query.domain, query))} />
          <SmallHeader style={{fontSize: 14}}>Notebook Name</SmallHeader>
          <TextInput onChange={(v) => this.setState({notebookName: v})}
                     value={notebookName}/>
        </React.Fragment>}
      </ModalBody>
      <ModalFooter>
        <Button type='secondary'
                onClick={this.props.closeFunction}
                style={{marginRight: '2rem'}}>
          Cancel
        </Button>
        <TooltipTrigger content={summarizeErrors(errors)}>
          <Button type='primary' disabled={errors} onClick={() => this.saveDataSet()}>
            Save{exportToNewNotebook && ' and Open'}
          </Button>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>;
  }
}

export {
  NewDataSetModal,
};
