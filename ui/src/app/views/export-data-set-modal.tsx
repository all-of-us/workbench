import * as React from 'react';
import {validate} from 'validate.js';

import {Button} from 'app/components/buttons';
import {SmallHeader, styles as headerStyles} from 'app/components/headers';
import {RadioButton, Select, TextArea, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {dataSetApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import {summarizeErrors} from 'app/utils';
import {convertQueryToText} from 'app/utils/big-query-queries';
import {navigate} from 'app/utils/navigation';


import {DataSet, DataSetQuery, DataSetRequest, FileDetail, KernelTypeEnum} from 'generated/fetch';

interface Props {
  closeFunction: Function;
  dataSet: DataSet;
  workspaceNamespace: string;
  workspaceFirecloudName: string;
}

interface State {
  existingNotebooks: FileDetail[];
  kernelType: KernelTypeEnum;
  loading: boolean;
  newNotebook: boolean;
  notebookName: string;
  notebooksLoading: boolean;
  queries: Array<DataSetQuery>;
  seePreview: boolean;
}

class ExportDataSetModal extends React.Component<
  Props,
  State
  > {
  constructor(props) {
    super(props);
    this.state = {
      existingNotebooks: [],
      kernelType: KernelTypeEnum.Python,
      loading: false,
      newNotebook: true,
      notebookName: '',
      notebooksLoading: true,
      queries: [],
      seePreview: false,
    };
  }

  componentDidMount() {
    this.loadNotebooks();
    this.generateQuery();
  }

  private async loadNotebooks() {
    try {
      const {workspaceNamespace, workspaceFirecloudName} = this.props;
      this.setState({notebooksLoading: true});
      const existingNotebooks =
        await workspacesApi().getNoteBookList(workspaceNamespace, workspaceFirecloudName);
      this.setState({existingNotebooks});
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({notebooksLoading: false});
    }
  }

  async generateQuery() {
    const {workspaceNamespace, workspaceFirecloudName} = this.props;
    const dataSet: DataSetRequest = {
      name: '',
      conceptSetIds: this.props.dataSet.conceptSets.map(cs => cs.id),
      cohortIds: this.props.dataSet.cohorts.map(c => c.id),
      values: this.props.dataSet.values,
      includesAllParticipants: this.props.dataSet.includesAllParticipants,
    };
    const sqlQueries = await dataSetApi().generateQuery(
      workspaceNamespace, workspaceFirecloudName, dataSet);
    this.setState({queries: sqlQueries.queryList});
  }

  async exportDataSet() {
    this.setState({loading: true});
    const {dataSet, workspaceNamespace, workspaceFirecloudName} = this.props;
    const request = {
      name: dataSet.name,
      includesAllParticipants: dataSet.includesAllParticipants,
      description: dataSet.description,
      conceptSetIds: dataSet.conceptSets.map(cs => cs.id),
      cohortIds: dataSet.cohorts.map(c => c.id),
      values: dataSet.values
    };
    await dataSetApi().exportToNotebook(
      workspaceNamespace, workspaceFirecloudName,
      {
        dataSetRequest: request,
        notebookName: this.state.notebookName,
        newNotebook: this.state.newNotebook,
        kernelType: this.state.kernelType
      });
    navigate(['workspaces',
      workspaceNamespace,
      workspaceFirecloudName, 'notebooks', this.state.notebookName + '.ipynb']);
  }

  setKernelType(event: any) {
    console.log(event);
    this.setState({kernelType: event.value});
  }

  render() {
    const {dataSet} = this.props;
    const {
      existingNotebooks,
      kernelType,
      loading,
      newNotebook,
      notebookName,
      notebooksLoading,
      queries,
      seePreview
    } = this.state;
    const selectOptions = [{label: '(Create a new notebook)', value: ''}]
      .concat(existingNotebooks.map(notebook => ({
        value: notebook.name.slice(0, -6),
        label: notebook.name.slice(0, -6)
      })));

    const errors = validate({name, notebookName}, {
      notebookName: {
        presence: {allowEmpty: !newNotebook},
        exclusion: {
          within: newNotebook ? existingNotebooks.map(fd => fd.name.slice(0, -6)) : [],
          message: 'already exists'
        }
      }
    });
    return <Modal loading={loading || notebooksLoading}>
      <ModalTitle>Export {dataSet.name} to Notebook</ModalTitle>
      <ModalBody>
        <Button data-test-id='code-preview-button'
                onClick={() => this.setState({seePreview: !seePreview})}>
          {seePreview ? 'Hide Preview' : 'See Code Preview'}
        </Button>
        {seePreview && <React.Fragment>
          {queries.length === 0 && <SpinnerOverlay />}
          <TextArea disabled={true} onChange={() => {}} style={{marginTop: '1rem'}}
                    data-test-id='code-text-box'
                    value={queries.map(query =>
                      convertQueryToText(dataSet.name, query.domain, query))} />
        </React.Fragment>}
        <div style={{marginTop: '1rem'}}>
          <Select value={this.state.notebookName}
                  options={selectOptions}
                  onChange={v => this.setState({notebookName: v, newNotebook: v === ''})}/>
        </div>
        {newNotebook && <React.Fragment>
          <SmallHeader style={{fontSize: 14, marginTop: '1rem'}}>Notebook Name</SmallHeader>
          <TextInput onChange={(v) => this.setState({notebookName: v})}
                     value={notebookName} data-test-id='notebook-name-input'/>
          <div style={headerStyles.formLabel}>
            Programming Language:
          </div>
          <label style={{display: 'block'}}>
            <RadioButton
              checked={this.state.kernelType === KernelTypeEnum.Python}
              onChange={() => this.setState({kernelType: KernelTypeEnum.Python})}
            />
            &nbsp;Python 3
          </label>
          <label style={{display: 'block'}}>
            <RadioButton
              checked={this.state.kernelType === KernelTypeEnum.R}
              onChange={() => this.setState({kernelType: KernelTypeEnum.R})}
            />
            &nbsp;R
          </label>
         </React.Fragment>}
      </ModalBody>
      <ModalFooter>
        <Button type='secondary'
                onClick={this.props.closeFunction}
                style={{marginRight: '2rem'}}>
          Cancel
        </Button>
        <TooltipTrigger content={summarizeErrors(errors)}>
          <Button type='primary' data-test-id='save-data-set'
                  disabled={errors || loading} onClick={() => this.exportDataSet()}>
            Export and Open
          </Button>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>;
  }
}

export {ExportDataSetModal};
