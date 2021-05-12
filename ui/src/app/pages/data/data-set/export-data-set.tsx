import {Button} from 'app/components/buttons';
import {SmallHeader, styles as headerStyles} from 'app/components/headers';
import {RadioButton, Select, TextInput} from 'app/components/inputs';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {dataSetApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import {AnalyticsTracker} from 'app/utils/analytics';
import {DataSetExportRequest, DataSetRequest, FileDetail, KernelTypeEnum} from 'generated/fetch';
import * as React from 'react';
import { useSpring, animated } from "react-spring";
import colors from 'app/styles/colors';
import {FlexRow} from '../../../components/flex';
import GenomicsAnalysisToolEnum = DataSetExportRequest.GenomicsAnalysisToolEnum;

interface Props {
  dataSetRequest: DataSetRequest;
  newNotebook: Function;
  notebookType?: Function;
  updateNotebookName: Function;
  workspaceNamespace: string;
  workspaceFirecloudName: string;
  onSeeCodePreview: Function;
  onHideCodePreview: Function;
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
  loadingPreview: boolean;
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
      loadingPreview: false
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
    return <React.Fragment>{notebooksLoading && <SpinnerOverlay />}
    {/*{seePreview && <React.Fragment>*/}
    {/*  {Array.from(queries.values())*/}
    {/*    .filter(query => query !== undefined).length === 0 && <SpinnerOverlay />}*/}
    {/*  <div style={styles.codePreviewSelector}>*/}
    {/*    {Object.keys(KernelTypeEnum).map(kernelTypeEnumKey => KernelTypeEnum[kernelTypeEnumKey])*/}
    {/*      .map((kernelTypeEnum, i) =>*/}
    {/*            <TabButton onClick={() => this.setState({previewedKernelType: kernelTypeEnum})}*/}
    {/*                       key={i}*/}
    {/*                       active={previewedKernelType === kernelTypeEnum}*/}
    {/*                       style={styles.codePreviewSelectorTab}*/}
    {/*                       disabled={queries.get(kernelTypeEnum) === undefined}>*/}
    {/*              {kernelTypeEnum}*/}
    {/*            </TabButton>)}*/}
    {/*  </div>*/}
    {/*  <TextArea disabled={true} onChange={() => {}}*/}
    {/*            data-test-id='code-text-box'*/}
    {/*            value={queries.get(previewedKernelType)} />*/}
    {/*</React.Fragment>}*/}
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
        Select programming language
      </div>
      {Object.keys(KernelTypeEnum).map(kernelTypeEnumKey => KernelTypeEnum[kernelTypeEnumKey])
        .map((kernelTypeEnum, i) =>
              <label key={i} style={{display: 'inline-flex', justifyContent: 'center', alignItems: 'center', marginRight: '1rem', color: colors.primary}}>
                <RadioButton
                    style={{marginRight: '0.25rem'}}
                    data-test-id={'kernel-type-' + kernelTypeEnum.toLowerCase()}
                    checked={this.state.kernelType === kernelTypeEnum}
                    onChange={() => this.onKernelTypeChange(kernelTypeEnum)}
                />
                {kernelTypeEnum}
              </label>
          )}

      <div style={headerStyles.formLabel}>
          Select analysis tool for genetic variant data
      </div>
      {this.genomicsToolInput('Hail', GenomicsAnalysisToolEnum.HAIL)}
      {this.genomicsToolInput('PLINK', GenomicsAnalysisToolEnum.PLINK)}
      {this.genomicsToolInput('Other VCF-compatible tool', GenomicsAnalysisToolEnum.NONE)}
    </React.Fragment>
    }

    <FlexRow style={{marginTop: '1rem', alignItems: 'center'}}>
      <Button type={'secondarySmall'}
              disabled={this.state.loadingPreview}
              data-test-id='code-preview-button'
              onClick={() => {
                if (!seePreview) {
                  AnalyticsTracker.DatasetBuilder.SeeCodePreview();
                }
                this.setState({seePreview: !seePreview});
                if (seePreview) {
                  // if it's currently seePreview, the user want's to hide
                  this.props.onHideCodePreview();
                } else {
                  this.setState({loadingPreview: true});
                  workspacesApi().readOnlyNotebook("aou-rw-local1-44385e06", "genomicsextractionworkspace", "qq.ipynb").then(html => {
                    const placeholder = document.createElement('html');
                    placeholder.innerHTML = html.html;
                    placeholder.style.overflowY = 'scroll';
                    placeholder.getElementsByTagName('body')[0].style.overflowY = 'scroll';
                    placeholder.querySelector('#notebook').style.paddingTop = 0;
                    placeholder.querySelectorAll('.input_prompt').forEach(e => e.remove());
                    const iframe = <iframe scrolling="no" style={{width: '100%', height: '100%', border: 'none'}} srcDoc={placeholder.outerHTML}/>;
                    this.props.onSeeCodePreview(iframe);
                    this.setState({loadingPreview: false});
                  });
                }
              }}>
        {seePreview ? 'Hide Code Preview' : 'See Code Preview'}
      </Button>

      {this.state.loadingPreview && <Spinner size={24} style={{marginLeft: '0.5rem'}}/>}
    </FlexRow>


      </React.Fragment>;
  }

  genomicsToolInput(displayName: string, genomicsTool: GenomicsAnalysisToolEnum) {
    return <label key={'genomics-tool-' + genomicsTool} style={{display: 'inline-flex', justifyContent: 'center', alignItems: 'center', marginRight: '1rem', color: colors.primary}}>
      <RadioButton
        style={{marginRight: '0.25rem'}}
        data-test-id={'genomics-tool-' + genomicsTool}
        checked={this.state.kernelType === genomicsTool}
        onChange={() => this.onKernelTypeChange(genomicsTool)}
      />
      {displayName}
    </label>;
  }
}
