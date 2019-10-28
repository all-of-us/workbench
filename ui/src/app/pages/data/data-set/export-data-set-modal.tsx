import * as React from 'react';
import {validate} from 'validate.js';

import {Button, TabButton} from 'app/components/buttons';
import {SmallHeader, styles as headerStyles} from 'app/components/headers';
import {RadioButton, Select, TextArea, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {dataSetApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import {summarizeErrors} from 'app/utils';
import {encodeURIComponentStrict, navigateByUrl} from 'app/utils/navigation';


import {appendNotebookFileSuffix} from 'app/pages/analysis/util';
import {DataSet, DataSetRequest, FileDetail, KernelTypeEnum} from 'generated/fetch';

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
  previewedKernelType: KernelTypeEnum;
  queries: Map<KernelTypeEnum, String>;
  seePreview: boolean;
}

const styles = {
  codePreviewSelector: {
    display: 'flex',
    marginTop: '1rem'
  },
  codePreviewSelectorTab: {
    width: '2.6rem'
  }
};

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
      previewedKernelType: KernelTypeEnum.Python,
      queries: new Map([[KernelTypeEnum.Python, undefined], [KernelTypeEnum.R, undefined]]),
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
    const {dataSet, workspaceNamespace, workspaceFirecloudName} = this.props;
    const dataSetRequest: DataSetRequest = {
      name: dataSet.name,
      conceptSetIds: dataSet.conceptSets.map(cs => cs.id),
      cohortIds: dataSet.cohorts.map(c => c.id),
      domainValuePairs: dataSet.domainValuePairs,
      includesAllParticipants: dataSet.includesAllParticipants,
    };
    dataSetApi().generateCode(
      workspaceNamespace,
      workspaceFirecloudName,
      KernelTypeEnum.Python.toString(),
      dataSetRequest).then(pythonCode => {
        this.setState(({queries}) => ({
          queries: queries.set(KernelTypeEnum.Python, pythonCode.code)}));
      });
    dataSetApi().generateCode(
      workspaceNamespace,
      workspaceFirecloudName,
      KernelTypeEnum.R.toString(),
      dataSetRequest).then(rCode => {
        this.setState(({queries}) => ({queries: queries.set(KernelTypeEnum.R, rCode.code)}));
      });
  }

  async exportDataSet() {
    this.setState({loading: true});
    const {dataSet, workspaceNamespace, workspaceFirecloudName} = this.props;
    const request: DataSetRequest = {
      name: dataSet.name,
      includesAllParticipants: dataSet.includesAllParticipants,
      description: dataSet.description,
      conceptSetIds: dataSet.conceptSets.map(cs => cs.id),
      cohortIds: dataSet.cohorts.map(c => c.id),
      domainValuePairs: dataSet.domainValuePairs
    };
    try {
      await dataSetApi().exportToNotebook(
        workspaceNamespace, workspaceFirecloudName,
        {
          dataSetRequest: request,
          notebookName: this.state.notebookName,
          newNotebook: this.state.newNotebook,
          kernelType: this.state.kernelType
        });
      // Open notebook in a new tab and close the modal
      const notebookUrl = `/workspaces/${workspaceNamespace}/${workspaceFirecloudName}/notebooks/preview/` +
        appendNotebookFileSuffix(encodeURIComponentStrict(this.state.notebookName));
      navigateByUrl(notebookUrl);
      this.props.closeFunction();
    } catch (error) {
      // https://precisionmedicineinitiative.atlassian.net/browse/RW-3782
      // The exportToNotebook call can fail with a 400.  Catch that error to ensure the user can exit the modal.
      // TODO: better failure UX
      console.error(error);
      this.setState({loading: false});
    }
  }

  render() {
    const {dataSet} = this.props;
    const {
      existingNotebooks,
      loading,
      newNotebook,
      notebookName,
      notebooksLoading,
      previewedKernelType,
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
          {Array.from(queries.values())
            .filter(query => query !== undefined).length === 0 && <SpinnerOverlay />}
          <div style={styles.codePreviewSelector}>
            {Object.keys(KernelTypeEnum).map(kernelTypeEnumKey => KernelTypeEnum[kernelTypeEnumKey])
              .map((kernelTypeEnum, i) =>
              <TabButton onClick={() => this.setState({previewedKernelType: kernelTypeEnum})}
                key={i}
                active={previewedKernelType === kernelTypeEnum}
                style={styles.codePreviewSelectorTab}
                disabled={queries.get(kernelTypeEnum) === undefined}>
              {kernelTypeEnum}
            </TabButton>)}
          </div>
          <TextArea disabled={true} onChange={() => {}}
                    data-test-id='code-text-box'
                    value={queries.get(previewedKernelType)} />
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
          {Object.keys(KernelTypeEnum).map(kernelTypeEnumKey => KernelTypeEnum[kernelTypeEnumKey])
            .map((kernelTypeEnum, i) =>
              <label key={i} style={{display: 'block'}}>
                <RadioButton
                  checked={this.state.kernelType === kernelTypeEnum}
                  onChange={() => this.setState({kernelType: kernelTypeEnum})}
                />
                &nbsp;{kernelTypeEnum}
              </label>
            )}
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
