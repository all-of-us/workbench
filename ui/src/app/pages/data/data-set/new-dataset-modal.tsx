import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
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
  missingDataSetInfo: boolean;
  name: string;
  newNotebook: boolean;
  notebookName: string;
  notebooksLoading: boolean;
  previewedKernelType: KernelTypeEnum;
  queries: Map<KernelTypeEnum, String>;
  seePreview: boolean;
  includeRawMicroarrayData: boolean;
  genomicsAnalysisTool: GenomicsAnalysisToolEnum;
  saveOrExportError: boolean;
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
      missingDataSetInfo: false,
      name: '',
      newNotebook: true,
      notebookName: '',
      notebooksLoading: false,
      previewedKernelType: KernelTypeEnum.Python,
      queries: new Map([[KernelTypeEnum.Python, undefined], [KernelTypeEnum.R, undefined]]),
      seePreview: false,
      includeRawMicroarrayData: false,
      genomicsAnalysisTool: GenomicsAnalysisToolEnum.NONE,
      saveOrExportError: false
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
    this.setState({conflictDataSetName: false, missingDataSetInfo: false, loading: true, saveOrExportError: false});
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
      if (e.status === 409) {
        this.setState({conflictDataSetName: true, loading: false});
      } else if (e.status === 400) {
        this.setState({missingDataSetInfo: true, loading: false});
      } else {
        this.setState({saveOrExportError: true, loading: false});
      }
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
  render() {
    const {
      conflictDataSetName,
      exportToNotebook,
      loading,
      missingDataSetInfo,
      name,
      newNotebook,
      notebookName,
      existingNotebooks,
      saveOrExportError
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
    const isApiError = conflictDataSetName || missingDataSetInfo || saveOrExportError;
    return <Modal loading={loading}>
      <ModalTitle>{this.props.dataSet ? 'Update' : 'Save'} Dataset</ModalTitle>
      <ModalBody>
        <div>
          {isApiError && <AlertDanger style={{marginBottom: '0.25rem', padding: '0 0.25rem'}}>
            {conflictDataSetName && <span>A Dataset with the same name exists</span>}
            {missingDataSetInfo && <span>Cannot save the Dataset because some information is missing</span>}
            {saveOrExportError && <span>
              The request cannot be completed. Please try again or contact Support in the left hand navigation
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
            workspaceNamespace={this.props.workspaceNamespace} workspaceFirecloudName={this.props.workspaceId}/>}
          {this.props.displayMicroarrayOptions && this.state.kernelType === KernelTypeEnum.Python &&
          <div style={{border: '1px solid grey', padding: '.5rem', paddingTop: 0, marginTop: '.5rem'}}>
            <TextColumn>
              <div style={headerStyles.formLabel}>Genomics Pre-alpha</div>
              <div>(non-production only; synthetic data)</div>
            </TextColumn>

            <div style={{display: 'flex', alignItems: 'center'}}>
              <CheckBox style={{height: 17, width: 17}}
                        data-test-id='include-raw-microarray-data'
                        onChange={(checked) => this.setState({includeRawMicroarrayData: checked})}
                        checked={this.state.includeRawMicroarrayData} />
              <div style={{marginLeft: '.5rem', color: colors.primary}}>
                Include raw microarray data
              </div>
            </div>

            {this.state.includeRawMicroarrayData && <div style={{marginTop: '.3rem'}}>
              <p style={{color: colors.primary}}>
                Extract genomics data for analysis using:
              </p>

              {Object.keys(GenomicsAnalysisToolEnum).map((enumKey, i) => {
                return <React.Fragment>
                  <label key={i} style={{display: 'block'}}>
                    <RadioButton
                      data-test-id={'genomics-analysis-tool-' + enumKey.toLowerCase()}
                      checked={this.state.genomicsAnalysisTool === GenomicsAnalysisToolEnum[enumKey]}
                      onChange={() => this.setState({genomicsAnalysisTool: GenomicsAnalysisToolEnum[enumKey]})}
                    />
                    &nbsp; {fp.startCase(enumKey.toLowerCase())}
                  </label>
                </React.Fragment>;
              })}
            </div>}
          </div> }
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
    </Modal>;
  }
}

export {
  NewDataSetModal
};
