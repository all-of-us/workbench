import {AlertDanger} from 'app/components/alert';
import {Button, Link} from 'app/components/buttons';
import {styles as headerStyles} from 'app/components/headers';
import {CheckBox, RadioButton, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {TextColumn} from 'app/components/text-column';
import {appendNotebookFileSuffix} from 'app/pages/analysis/util';

import {dataSetApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {summarizeErrors} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {encodeURIComponentStrict, navigateByUrl} from 'app/utils/navigation';
import {ACTION_DISABLED_INVALID_BILLING} from 'app/utils/strings';
import {
  DataSet,
  DataSetExportRequest,
  DataSetRequest,
  DomainValuePair,
  FileDetail,
  KernelTypeEnum,
  PrePackagedConceptSetEnum
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {validate} from 'validate.js';
import {FlexRow} from '../../../components/flex';
import GenomicsAnalysisToolEnum = DataSetExportRequest.GenomicsAnalysisToolEnum;
import GenomicsDataTypeEnum = DataSetExportRequest.GenomicsDataTypeEnum;
import {ExportDataSet} from './export-data-set';

interface Props {
  closeFunction: Function;
  dataSet: DataSet;
  includesAllParticipants: boolean;
  prePackagedConceptSet: Array<PrePackagedConceptSetEnum>;
  selectedConceptSetIds: number[];
  selectedCohortIds: number[];
  selectedDomainValuePairs: DomainValuePair[];
  workspaceNamespace: string;
  workspaceId: string;
  billingLocked: boolean;
  displayMicroarrayOptions: boolean;
}

interface State {
  conflictDataSetName: boolean;
  existingNotebooks: FileDetail[];
  exportToNotebook: boolean;
  kernelType: KernelTypeEnum;
  loading: boolean;
  name: string;
  newNotebook: boolean;
  notebookName: string;
  notebooksLoading: boolean;
  previewedKernelType: KernelTypeEnum;
  queries: Map<KernelTypeEnum, String>;
  seePreview: boolean;
  includeRawMicroarrayData: boolean;
  genomicsAnalysisTool: GenomicsAnalysisToolEnum;
  saveError: boolean;
  exportError: boolean;
  showRightPanel: boolean;
  rightPanelContent: JSX.Element;
}

class NewDataSetModal extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      conflictDataSetName: false,
      existingNotebooks: [],
      exportToNotebook: !props.billingLocked,
      kernelType: KernelTypeEnum.Python,
      loading: false,
      name: '',
      newNotebook: true,
      notebookName: '',
      notebooksLoading: false,
      previewedKernelType: KernelTypeEnum.Python,
      queries: new Map([[KernelTypeEnum.Python, undefined], [KernelTypeEnum.R, undefined]]),
      seePreview: false,
      includeRawMicroarrayData: false,
      genomicsAnalysisTool: GenomicsAnalysisToolEnum.NONE,
      saveError: false,
      exportError: false,
      showRightPanel: false,
      rightPanelContent: null
    };
  }

  componentDidMount() {
    if (this.props.dataSet) {
      this.setState({name: this.props.dataSet.name});
    }
  }

  async updateDataSet() {
    const {dataSet, workspaceNamespace, workspaceId} = this.props;
    const {name} = this.state;
    const request: DataSetRequest = {
      name: name,
      includesAllParticipants: dataSet.includesAllParticipants,
      conceptSetIds: this.props.selectedConceptSetIds,
      cohortIds: this.props.selectedCohortIds,
      domainValuePairs: this.props.selectedDomainValuePairs,
      etag: dataSet.etag
    };
    await dataSetApi().updateDataSet(workspaceNamespace, workspaceId, dataSet.id, request);
  }

  async saveDataSet() {

    const {dataSet, workspaceNamespace, workspaceId} = this.props;
    if (!this.state.name) {
      return;
    }
    this.setState({conflictDataSetName: false, loading: true, saveError: false, exportError: false});
    const {name} = this.state;
    const request: DataSetRequest = {
      name: name,
      description: '',
      includesAllParticipants: this.props.includesAllParticipants,
      conceptSetIds: this.props.selectedConceptSetIds,
      cohortIds: this.props.selectedCohortIds,
      domainValuePairs: this.props.selectedDomainValuePairs,
      prePackagedConceptSet: this.props.prePackagedConceptSet
    };
    try {
      // If dataset exist it is an update
      if (this.props.dataSet) {
        const updateReq = {
          ...request,
          description: dataSet.description,
          etag: dataSet.etag
        };
        await dataSetApi().updateDataSet(workspaceNamespace, workspaceId, dataSet.id, updateReq);
      } else {
        await dataSetApi().createDataSet(workspaceNamespace, workspaceId, request);
      }
    } catch (e) {
      console.error(e);
      if (e.status === 409) {
        this.setState({conflictDataSetName: true, loading: false});
      } else {
        this.setState({saveError: true, loading: false});
      }
      return;
    }
    try {
      if (this.state.exportToNotebook) {
        await dataSetApi().exportToNotebook(
          workspaceNamespace, workspaceId,
          {
            dataSetRequest: request,
            kernelType: this.state.kernelType,
            notebookName: this.state.notebookName,
            newNotebook: this.state.newNotebook,
            genomicsDataType: this.state.includeRawMicroarrayData ? GenomicsDataTypeEnum.MICROARRAY : GenomicsDataTypeEnum.NONE,
            genomicsAnalysisTool: this.state.genomicsAnalysisTool
          });
        // Open notebook in a new tab and return back to the Data tab
        const notebookUrl = `/workspaces/${workspaceNamespace}/${workspaceId}/notebooks/preview/` +
          appendNotebookFileSuffix(encodeURIComponentStrict(this.state.notebookName));
        navigateByUrl(notebookUrl);
      } else {
        window.history.back();
      }
    } catch (e) {
      console.error(e);
      this.setState({exportError: true, loading: false});
    }
  }

  changeExportToNotebook(checked: boolean) {
    this.setState({exportToNotebook: checked});
  }

  onSaveClick() {
    if (this.props.dataSet) {
      if (this.state.exportToNotebook) {
        AnalyticsTracker.DatasetBuilder.UpdateAndAnalyze(this.state.kernelType);
      } else {
        AnalyticsTracker.DatasetBuilder.Update();
      }
    } else {
      if (this.state.exportToNotebook) {
        AnalyticsTracker.DatasetBuilder.SaveAndAnalyze(this.state.kernelType);
      } else {
        AnalyticsTracker.DatasetBuilder.Save();
      }
    }

    this.saveDataSet();
  }

  getDataSetRequest() {
    const dataSetRequest: DataSetRequest = {
      name: 'dataSet',
      conceptSetIds: this.props.selectedConceptSetIds,
      cohortIds: this.props.selectedCohortIds,
      domainValuePairs: this.props.selectedDomainValuePairs,
      includesAllParticipants: this.props.includesAllParticipants,
      prePackagedConceptSet: this.props.prePackagedConceptSet
    };
    return dataSetRequest;
  }

  navigateToDataPage() {
    const {workspaceNamespace, workspaceId} = this.props;
    navigateByUrl(`/workspaces/${workspaceNamespace}/${workspaceId}/data`);
  }

  renderRightPanel(children: JSX.Element) {
    this.setState({showRightPanel: true, rightPanelContent: children});
  }

  hideRightPanel() {
    this.setState({showRightPanel: false, rightPanelContent: null});
  }

  render() {
    const {
      conflictDataSetName,
      exportToNotebook,
      loading,
      name,
      newNotebook,
      notebookName,
      existingNotebooks,
      saveError,
      exportError
    } = this.state;

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
    const isApiError = conflictDataSetName || saveError || exportError;
    return <Modal loading={loading} width={!this.state.rightPanelContent ? 450 : 1200}>
      <FlexRow>
        <div style={{maxWidth: 'calc(450px - 2rem)'}}>
          <ModalTitle>{this.props.dataSet ? 'Update' : 'Save'} Dataset</ModalTitle>
          <ModalBody>
            <div>
              {isApiError && <AlertDanger style={{marginBottom: '0.25rem', padding: '0 0.25rem'}}>
                {conflictDataSetName && <span>A Dataset with the same name exists</span>}
                {saveError && <span>
              The request cannot be completed. Please try again or contact Support in the left hand navigation
            </span>}
                {exportError && <span>
              The Dataset was saved but there was an error exporting to the notebook. Please try exporting from the Dataset card on the
                    &nbsp;<Link style={{display: 'inline-block'}} onClick={() => this.navigateToDataPage()}>Data Page</Link>
            </span>}
              </AlertDanger>}
              <TextInput type='text' autoFocus placeholder='Dataset Name'
                         value={name} data-test-id='data-set-name-input'
                         onChange={v => this.setState({
                           name: v, conflictDataSetName: false
                         })}/>
            </div>
            <TooltipTrigger content={this.props.billingLocked && ACTION_DISABLED_INVALID_BILLING}>
              <div style={{display: 'flex', alignItems: 'center', marginTop: '1rem', ...(this.props.billingLocked && {opacity: 0.5})}}>
                <CheckBox style={{height: 17, width: 17}}
                          disabled={this.props.billingLocked}
                          data-test-id='export-to-notebook'
                          onChange={(checked) => this.changeExportToNotebook(checked)}
                          checked={this.state.exportToNotebook} />
                <div style={{marginLeft: '.5rem',
                  color: colors.primary}}>Export to notebook</div>
              </div>
            </TooltipTrigger>
            <React.Fragment>  {exportToNotebook && <ExportDataSet
                dataSetRequest={this.getDataSetRequest()}
                notebookType={(kernelTypeEnum) => this.setState({
                  kernelType: kernelTypeEnum,
                  includeRawMicroarrayData: kernelTypeEnum === KernelTypeEnum.R ? false : this.state.includeRawMicroarrayData
                })}
                newNotebook={(v) => this.setState({newNotebook: v})}
                updateNotebookName={(v) => this.setState({notebookName: v})}
                workspaceNamespace={this.props.workspaceNamespace} workspaceFirecloudName={this.props.workspaceId}
                onSeeCodePreview={(children) => this.renderRightPanel(children)}
                onHideCodePreview={() => this.hideRightPanel()}
            />}
            </React.Fragment>
          </ModalBody>
          <ModalFooter>
            <Button type='secondary'
                    onClick={this.props.closeFunction}
                    style={{marginRight: '2rem'}}>
              Cancel
            </Button>
            <TooltipTrigger content={summarizeErrors(errors)}>
              <Button type='primary'
                      data-test-id='save-data-set'
                      disabled={errors}
                      onClick={() => this.onSaveClick()}>
                {!this.props.dataSet ? 'Save' : 'Update' }{exportToNotebook && ' and Analyze'}
              </Button>
            </TooltipTrigger>
          </ModalFooter>
        </div>

        {this.state.rightPanelContent &&
          <div style={{flex: 1, marginLeft: '1rem'}}>
            {this.state.rightPanelContent}
          </div>
        }
      </FlexRow>
    </Modal>;
  }
}

export {
  NewDataSetModal
};
