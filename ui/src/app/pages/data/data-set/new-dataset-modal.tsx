import {Button, Link} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {appendNotebookFileSuffix} from 'app/pages/analysis/util';

import {dataSetApi} from 'app/services/swagger-fetch-clients';
import {summarizeErrors} from 'app/utils';
import {encodeURIComponentStrict, navigateByUrl} from 'app/utils/navigation';
import {
  DataSet,
  DataSetRequest,
  DomainValuePair,
  FileDetail,
  KernelTypeEnum,
  PrePackagedConceptSetEnum
} from 'generated/fetch';
import * as React from 'react';

import {validate} from 'validate.js';
import {FlexRow} from '../../../components/flex';
import {ExportDataSet} from './export-data-set';

interface Props {
  closeFunction: Function;
  dataSet: DataSet;

  workspaceNamespace: string;
  workspaceId: string;
  billingLocked: boolean;
}

interface State {
  existingNotebooks: FileDetail[];
  kernelType: KernelTypeEnum;
  loading: boolean;
  newNotebook: boolean;
  notebookName: string;
  seePreview: boolean;
  exportError: boolean;
  rightPanelContent: JSX.Element;
}

class NewDataSetModal extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      existingNotebooks: [],
      kernelType: KernelTypeEnum.Python,
      loading: false,
      newNotebook: true,
      notebookName: '',
      seePreview: false,
      exportError: false,
      rightPanelContent: null
    };
  }

  async saveDataSet() {
    const {workspaceNamespace, workspaceId} = this.props;

    this.setState({loading: true, exportError: false});
    try {
      await dataSetApi().exportToNotebook(
        workspaceNamespace, workspaceId,
        {
          dataSetRequest: this.getDataSetRequest(),
          kernelType: this.state.kernelType,
          notebookName: this.state.notebookName,
          newNotebook: this.state.newNotebook
        });
      // Open notebook in a new tab and return back to the Data tab
      const notebookUrl = `/workspaces/${workspaceNamespace}/${workspaceId}/notebooks/preview/` +
        appendNotebookFileSuffix(encodeURIComponentStrict(this.state.notebookName));
      navigateByUrl(notebookUrl);
    } catch (e) {
      console.error(e);
      this.setState({exportError: true, loading: false});
    }
  }

  // TODO eric: update export to notebook analytics trackers. AnalyticsTracker.DatasetBuilder.Analyze(this.state.kernelType);
  getDataSetRequest(): DataSetRequest {
    const dataSetRequest: DataSetRequest = {
      name: '',
      includesAllParticipants: this.props.dataSet.includesAllParticipants,
      conceptSetIds: this.props.dataSet.conceptSets.map(cs => cs.id),
      cohortIds: this.props.dataSet.cohorts.map(c => c.id),
      domainValuePairs: this.props.dataSet.domainValuePairs,
      prePackagedConceptSet: this.props.dataSet.prePackagedConceptSet
    };
    return dataSetRequest;
  }

  navigateToDataPage() {
    const {workspaceNamespace, workspaceId} = this.props;
    navigateByUrl(`/workspaces/${workspaceNamespace}/${workspaceId}/data`);
  }

  renderRightPanel(children: JSX.Element) {
    this.setState({rightPanelContent: children});
  }

  hideRightPanel() {
    this.setState({rightPanelContent: null});
  }

  render() {
    const {
      loading,
      newNotebook,
      notebookName,
      existingNotebooks,
      exportError
    } = this.state;

    const errors = validate({notebookName}, {
      notebookName: {
        exclusion: {
          within: newNotebook ? existingNotebooks.map(fd => fd.name.slice(0, -6)) : [],
          message: 'already exists'
        }
      }
    });

    // TODO eric: handle export API error
    return <Modal loading={loading} width={!this.state.rightPanelContent ? 450 : 1200}>
      <FlexRow>
        <div style={{width: 'calc(450px - 2rem)'}}>
          <ModalTitle>{this.props.dataSet ? 'Update' : 'Save'} Dataset</ModalTitle>
          <ModalBody>

            <ExportDataSet
                dataSetRequest={this.getDataSetRequest()}
                notebookType={(kernelTypeEnum) => this.setState({
                  kernelType: kernelTypeEnum
                })}
                newNotebook={(v) => this.setState({newNotebook: v})}
                updateNotebookName={(v) => this.setState({notebookName: v})}
                workspaceNamespace={this.props.workspaceNamespace} workspaceFirecloudName={this.props.workspaceId}
                onSeeCodePreview={(children) => this.renderRightPanel(children)}
                onHideCodePreview={() => this.hideRightPanel()}
            />

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
                      onClick={() => this.saveDataSet()}>
                'Analyze'}
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
