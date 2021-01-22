import {Button, TabButton} from 'app/components/buttons';
import {SmallHeader, styles as headerStyles} from 'app/components/headers';
import {RadioButton, Select, TextArea, TextInput} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {dataSetApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import {AnalyticsTracker} from 'app/utils/analytics';
import {
  DataSetRequest,
  FileDetail,
  KernelTypeEnum
} from 'generated/fetch';
import * as React from 'react';

interface Props {
  dataSetRequest: DataSetRequest;
  newNotebook: Function;
  notebookType?: Function;
  updateNotebookName: Function;
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

export class ExportDataSet extends React.Component<Props, State> {

  constructor(props: any) {
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
    const {dataSetRequest, workspaceNamespace, workspaceFirecloudName} = this.props;
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

  setNotebookName(notebook) {
    this.setState({notebookName: notebook});
    this.props.newNotebook(true);
    this.props.updateNotebookName(notebook);
  }

  setExistingNotebook(notebook) {
    this.setState({newNotebook: notebook === ''});
    this.setState({notebookName: notebook});
    this.props.newNotebook(notebook === '');
    this.props.updateNotebookName(notebook);
  }

  onKernelTypeChange(kernelType) {
    this.setState({kernelType: kernelType});
    this.props.notebookType(kernelType);
  }

  render() {
    const {
      existingNotebooks,
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
    return <React.Fragment>{notebooksLoading && <SpinnerOverlay />}<Button style={{marginTop: '1rem'}} data-test-id='code-preview-button'
            onClick={() => {
              if (!seePreview) {
                AnalyticsTracker.DatasetBuilder.SeeCodePreview();
              }
              this.setState({seePreview: !seePreview});
            }}>
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
              onChange={v => this.setExistingNotebook(v)}/>
    </div>
    {newNotebook && <React.Fragment>
      <SmallHeader style={{fontSize: 14, marginTop: '1rem'}}>Notebook Name</SmallHeader>
      <TextInput onChange={(v) => this.setNotebookName(v)}
                 value={notebookName} data-test-id='notebook-name-input'/>
      <div style={headerStyles.formLabel}>
        Programming Language:
      </div>
      {Object.keys(KernelTypeEnum).map(kernelTypeEnumKey => KernelTypeEnum[kernelTypeEnumKey])
        .map((kernelTypeEnum, i) =>
              <label key={i} style={{display: 'block'}}>
                <RadioButton
                    data-test-id={'kernel-type-' + kernelTypeEnum.toLowerCase()}
                    checked={this.state.kernelType === kernelTypeEnum}
                    onChange={() => this.onKernelTypeChange(kernelTypeEnum)}
                />
                &nbsp;{kernelTypeEnum}
              </label>
          )}
    </React.Fragment>}
      </React.Fragment>;
  }
}
