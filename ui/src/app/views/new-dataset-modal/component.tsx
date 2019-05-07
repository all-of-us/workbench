import * as React from 'react';

import {validate} from 'validate.js';

import {dataSetApi, workspacesApi} from 'app/services/swagger-fetch-clients';

import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {SmallHeader} from 'app/components/headers';
import {Select, TextArea} from 'app/components/inputs';
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
  exportToNotebook: boolean;
  loading: boolean;
  missingDataSetInfo: boolean;
  name: string;
  newNotebook: boolean;
  notebookName: string;
  notebooksLoading: boolean;
  queries: Array<DataSetQuery>;
  seePreview: boolean;
}


class NewDataSetModal extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      conflictDataSetName: false,
      existingNotebooks: [],
      exportToNotebook: false,
      loading: false,
      missingDataSetInfo: false,
      name: '',
      newNotebook: true,
      notebookName: '',
      notebooksLoading: false,
      queries: [],
      seePreview: false
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
      if (!this.state.exportToNotebook) {
        this.props.closeFunction();
      } else {
        await dataSetApi().exportToNotebook(
          workspaceNamespace, workspaceId,
          {
            dataSetRequest: request,
            notebookName: this.state.notebookName,
            newNotebook: this.state.newNotebook
          });
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

  changeExportToNotebook() {
    this.setState({exportToNotebook: !this.state.exportToNotebook});
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
      exportToNotebook,
      loading,
      missingDataSetInfo,
      name,
      newNotebook,
      notebookName,
      notebooksLoading,
      existingNotebooks,
      queries,
      seePreview
    } = this.state;

    const selectOptions = [{label: '(Create a new notebook)', value: ''}]
      .concat(existingNotebooks.map(notebook => ({
        value: notebook.name.slice(0, -6),
        label: notebook.name.slice(0, -6)
      })));

    const errors = validate({name, notebookName}, {
      name: {
        presence: {allowEmpty: false}
      },
      notebookName: {
        presence: {allowEmpty: !exportToNotebook},
        exclusion: {
          within: newNotebook ? existingNotebooks.map(fd => fd.name.slice(0, -6)) : [],
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
                    onChange={() => this.changeExportToNotebook()} />
          <div style={{marginLeft: '.5rem',
            color: colors.black[0]}}>Export to Python notebook</div>
        </div>
        {exportToNotebook && <React.Fragment>
          {notebooksLoading && <SpinnerOverlay />}
          <Button style={{marginTop: '1rem'}}
                  onClick={() => this.setState({seePreview: !seePreview})}>
            {seePreview ? 'Hide Preview' : 'See Code Preview'}
          </Button>
          {seePreview && <React.Fragment>
            {queries.length === 0 && <SpinnerOverlay />}
            <TextArea disabled={true} onChange={() => {}} style={{marginTop: '1rem'}}
                      value={queries.map(query =>
                        convertQueryToText(name, query.domain, query))} />
          </React.Fragment>}
          <div style={{marginTop: '1rem'}}>
            <Select value={this.state.notebookName}
                    options={selectOptions}
                    onChange={v => this.setState({notebookName: v, newNotebook: v === ''})}/>
          </div>
          {newNotebook && <React.Fragment>
            <SmallHeader style={{fontSize: 14, marginTop: '1rem'}}>Notebook Name</SmallHeader>
            <TextInput onChange={(v) => this.setState({notebookName: v})}
                       value={notebookName}/>
          </React.Fragment>}
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
            Save{exportToNotebook && ' and Open'}
          </Button>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>;
  }
}

export {
  NewDataSetModal,
};
