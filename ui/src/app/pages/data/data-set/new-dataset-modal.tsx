import {Button, Link} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {appendNotebookFileSuffix} from 'app/pages/analysis/util';

import {dataSetApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import {summarizeErrors, withCurrentWorkspace} from 'app/utils';
import {encodeURIComponentStrict, navigateByUrl} from 'app/utils/navigation';
import {
  DataSet,
  DataSetRequest,
  KernelTypeEnum,
} from 'generated/fetch';
import {useEffect, useState} from 'react';
import * as React from 'react';
import * as fp from 'lodash/fp';

import {validate} from 'validate.js';
import {FlexRow} from '../../../components/flex';
import {WorkspaceData} from '../../../utils/workspace-data';
import {ExportDataSet} from './export-data-set';

interface MyProps {
  closeFunction: Function;
  dataset: DataSet;
}

interface Props extends MyProps {
  workspace: WorkspaceData;
}

// TODO eric: add billing locked to export button / functionality

export const NewDataSetModal: (props: MyProps) => JSX.Element = fp.flow(withCurrentWorkspace())(
  ({workspace, dataset, closeFunction}: Props) => {
    const [existingNotebooks, setExistingNotebooks] = useState(undefined);
    const [kernelType, setKernelType] = useState(KernelTypeEnum.Python);
    const [isExporting, setIsExporting] = useState(false); // replace w/ undefined notebooks list? // test case - no notebooks in workspace
    const [creatingNewNotebook, setCreatingNewNotebook] = useState(true); // replace w/ name inside notebooks list?
    const [notebookName, setNotebookName] = useState('');
    const [rightPanelContent, setRightPanelContent] = useState(null);

    useEffect(() => {
      workspacesApi().getNoteBookList(workspace.namespace, workspace.id)
        .then(notebooks => setExistingNotebooks(notebooks))
        .catch(() => setExistingNotebooks([])); // If the request fails, at least let the user create new notebooks
    }, [workspace]);

    async function saveDataSet() {
      setIsExporting(true);
      try {
        await dataSetApi().exportToNotebook(
          workspace.namespace, workspace.id,
          {
            dataSetRequest: getDataSetRequest(),
            kernelType: kernelType,
            notebookName: notebookName,
            newNotebook: creatingNewNotebook
          });
        // Open notebook in a new tab and return back to the Data tab
        const notebookUrl = `/workspaces/${workspace.namespace}/${workspace.id}/notebooks/preview/` +
          appendNotebookFileSuffix(encodeURIComponentStrict(notebookName));
        navigateByUrl(notebookUrl);
      } catch (e) {
        console.error(e);
        setIsExporting(false);
      }
    }

    // TODO eric: update export to notebook analytics trackers. AnalyticsTracker.DatasetBuilder.Analyze(this.state.kernelType);
    function getDataSetRequest(): DataSetRequest {
      const dataSetRequest: DataSetRequest = {
        name: '',
        includesAllParticipants: dataset.includesAllParticipants,
        conceptSetIds: dataset.conceptSets.map(cs => cs.id),
        cohortIds: dataset.cohorts.map(c => c.id),
        domainValuePairs: dataset.domainValuePairs,
        prePackagedConceptSet: dataset.prePackagedConceptSet
      };
      return dataSetRequest;
    }

    const errors = validate({notebookName}, {
      notebookName: {
        exclusion: {
          within: creatingNewNotebook && existingNotebooks ? existingNotebooks.map(fd => fd.name.slice(0, -6)) : [],
          message: 'already exists'
        }
      }
    });

    // TODO eric: handle export API error
    return <Modal loading={isExporting} width={!rightPanelContent ? 450 : 1200}>
      <FlexRow>
        <div style={{width: 'calc(450px - 2rem)'}}>
          <ModalTitle>Export Dataset</ModalTitle>
          <ModalBody>

            <ExportDataSet
              dataSetRequest={getDataSetRequest()}
              notebookType={setKernelType}
              newNotebook={setCreatingNewNotebook}
              updateNotebookName={setNotebookName}
              workspaceNamespace={workspace.namespace}
              workspaceFirecloudName={workspace.id}
              onSeeCodePreview={setRightPanelContent}
              onHideCodePreview={() => setRightPanelContent(null)}
            />

          </ModalBody>
          <ModalFooter>
            <Button type='secondary'
                    onClick={closeFunction}
                    style={{marginRight: '2rem'}}>
              Cancel
            </Button>
            <TooltipTrigger content={summarizeErrors(errors)}>
              <Button type='primary'
                      data-test-id='save-data-set'
                      disabled={errors}
                      onClick={() => saveDataSet()}>
                'Analyze'
              </Button>
            </TooltipTrigger>
          </ModalFooter>
        </div>

        {rightPanelContent &&
        <div style={{flex: 1, marginLeft: '1rem'}}>
          {rightPanelContent}
        </div>}
      </FlexRow>
    </Modal>;
  });

