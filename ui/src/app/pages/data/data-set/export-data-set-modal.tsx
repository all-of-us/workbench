import * as React from 'react';
import {validate} from 'validate.js';

import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {dataSetApi} from 'app/services/swagger-fetch-clients';
import {summarizeErrors} from 'app/utils';
import {encodeURIComponentStrict, navigateByUrl} from 'app/utils/navigation';


import {appendNotebookFileSuffix} from 'app/pages/analysis/util';
import {AnalyticsTracker} from 'app/utils/analytics';
import {DataSet, DataSetRequest, FileDetail, KernelTypeEnum} from 'generated/fetch';
import {ExportDataSet} from './export-data-set';

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

  get datasetRequest() {
    const {dataSet} = this.props;
    return {
      dataSetId: dataSet.id,
      name: dataSet.name,
      includesAllParticipants: dataSet.includesAllParticipants,
      description: dataSet.description,
      conceptSetIds: [],
      cohortIds: [],
      domainValuePairs: dataSet.domainValuePairs,
      prePackagedConceptSet: dataSet.prePackagedConceptSet
    } as DataSetRequest;
  }

  async exportDataSet() {
    this.setState({loading: true});
    const {workspaceNamespace, workspaceFirecloudName} = this.props;
    const request = this.datasetRequest;
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
      notebookName
    } = this.state;

    const errors = validate({name, notebookName}, {
      notebookName: {
        presence: {allowEmpty: !newNotebook},
        exclusion: {
          within: newNotebook ? existingNotebooks.map(fd => fd.name.slice(0, -6)) : [],
          message: 'already exists'
        }
      }
    });
    return <Modal loading={loading}>
      <ModalTitle>Export {dataSet.name} to Notebook</ModalTitle>
      <ModalBody>
        <ExportDataSet dataSetRequest={this.datasetRequest}
                       notebookType={(kernelTypeEnum) => this.setState({kernelType: kernelTypeEnum})}
                       newNotebook={(newNotebookName) => this.setState({newNotebook: newNotebookName})}
                       updateNotebookName={(name) => this.setState({notebookName: name})}
                       workspaceNamespace={this.props.workspaceNamespace} workspaceFirecloudName={this.props.workspaceFirecloudName}/>
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
                  disabled={errors || loading}
                  onClick={() => {
                    AnalyticsTracker.DatasetBuilder.Export(this.state.kernelType);
                    this.exportDataSet();
                  }}>
            Export and Open
          </Button>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>;
  }
}

export {ExportDataSetModal};
