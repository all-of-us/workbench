import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {appendNotebookFileSuffix} from 'app/pages/analysis/util';

import {dataSetApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import {summarizeErrors, withCurrentWorkspace} from 'app/utils';
import {encodeURIComponentStrict, navigateByUrl} from 'app/utils/navigation';
import {BillingStatus, DataSet, DataSetRequest, KernelTypeEnum, } from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect, useState} from 'react';

import {validate} from 'validate.js';
import {FlexRow} from '../../../components/flex';
import {SmallHeader, styles as headerStyles} from '../../../components/headers';
import {RadioButton, Select, TextInput} from '../../../components/inputs';
import {Spinner} from '../../../components/spinners';
import colors from '../../../styles/colors';
import {AnalyticsTracker} from '../../../utils/analytics';
import {ACTION_DISABLED_INVALID_BILLING} from '../../../utils/strings';
import {WorkspaceData} from '../../../utils/workspace-data';
import {WorkspacePermissionsUtil} from '../../../utils/workspace-permissions';

// TODO eric: rename these props

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
    const [creatingNewNotebook, setCreatingNewNotebook] = useState(true);
    const [notebookName, setNotebookName] = useState('');
    const [rightPanelContent, setRightPanelContent] = useState(null);
    const [loadingNotebook, setIsLoadingNotebook] = useState(false);

    useEffect(() => {
      workspacesApi().getNoteBookList(workspace.namespace, workspace.id)
        .then(notebooks => setExistingNotebooks(notebooks))
        .catch(() => setExistingNotebooks([])); // If the request fails, at least let the user create new notebooks
    }, [workspace]);

    useEffect(() => {
      if (rightPanelContent) {
        loadCodePreview(kernelType);
      }
    }, [kernelType]);

    // TODO eric: rename
    async function saveDataSet() {
      console.log('did this run');
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
        name: dataset ? dataset.name : 'dataSet',
        ...(dataset.id ? {
          dataSetId: dataset.id
        } : {
          includesAllParticipants: dataset.includesAllParticipants,
          conceptSetIds: dataset.conceptSets.map(cs => cs.id),
          cohortIds: dataset.cohorts.map(c => c.id),
          domainValuePairs: dataset.domainValuePairs,
          prePackagedConceptSet: dataset.prePackagedConceptSet
        })
      };
      return dataSetRequest;
    }

    function loadCodePreview(kernel: KernelTypeEnum) {
      setIsLoadingNotebook(true);
      dataSetApi().previewExportToNotebook(workspace.namespace, workspace.id, {
        dataSetRequest: getDataSetRequest(),
        kernelType: kernel,
        newNotebook: false,
        notebookName: '',
      }).then(resp => {
        const placeholder = document.createElement('html');
        placeholder.innerHTML = resp.html;
        placeholder.style.overflowY = 'scroll';
        placeholder.getElementsByTagName('body')[0].style.overflowY = 'scroll';
        placeholder.querySelector<HTMLElement>('#notebook').style.paddingTop = '0';
        placeholder.querySelectorAll('.input_prompt').forEach(e => e.remove());
        const iframe = <iframe scrolling='no' style={{width: '100%', height: '100%', border: 'none'}} srcDoc={placeholder.outerHTML}/>;
        setRightPanelContent(iframe);
        setIsLoadingNotebook(false);
      });
    }

    function onCodePreviewClick() {
      if (rightPanelContent) {
        setRightPanelContent(null);
      } else {
        console.log(kernelType);
        AnalyticsTracker.DatasetBuilder.SeeCodePreview();
        loadCodePreview(kernelType);
      }
    }

    const errors = {
      ...validate({notebookName}, {
        notebookName: {
          presence: {allowEmpty: false},
          exclusion: {
            within: creatingNewNotebook && existingNotebooks ? existingNotebooks.map(fd => fd.name.slice(0, -6)) : [],
            message: 'already exists'
          }
        }
      }),
      ...(workspace.billingStatus === BillingStatus.INACTIVE ? {
        billing: [ACTION_DISABLED_INVALID_BILLING]
      } : {}),
      ...(!WorkspacePermissionsUtil.canWrite(workspace.accessLevel) ? {
        permission: ['Exporting to a notebook requires write access to the workspace']
      } : {})
    };

    const isNotebooksLoading = existingNotebooks === undefined;

    const selectOptions = [{label: '(Create a new notebook)', value: ''}];
    if (!isNotebooksLoading) {
      selectOptions.push(...existingNotebooks.map(notebook => ({
        value: notebook.name.slice(0, -6),
        label: notebook.name.slice(0, -6)
      })));
    }

    function onNotebookSelect(v) {
      setCreatingNewNotebook(v === '');
      setNotebookName(v);

      setIsLoadingNotebook(true);
      workspacesApi().getNotebookKernel(workspace.namespace, workspace.id, v)
        .then(kernel => setKernelType(kernel))
        .finally(() => setIsLoadingNotebook(false));
    }

    // TODO eric: handle export API error
    return <Modal loading={isExporting || isNotebooksLoading} width={!rightPanelContent ? 450 : 1200}>
      <FlexRow>
        <div style={{width: 'calc(450px - 2rem)'}}>
          <ModalTitle>Export Dataset</ModalTitle>
          <ModalBody>

            <div style={{marginTop: '1rem'}}>
              <Select value={creatingNewNotebook ? '' : notebookName}
                      options={selectOptions}
                      onChange={(v) => onNotebookSelect(v)}/>
            </div>

            {creatingNewNotebook && <React.Fragment>
                <SmallHeader style={{fontSize: 14, marginTop: '1rem'}}>Notebook Name</SmallHeader>
                <TextInput onChange={v => setNotebookName(v)}
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
                      disabled={loadingNotebook}
                      checked={kernelType === kernelTypeEnum}
                      onChange={() => setKernelType(kernelTypeEnum)}
                    />
                    {kernelTypeEnum}
                  </label>)}
            </React.Fragment>}

            <FlexRow style={{marginTop: '1rem', alignItems: 'center'}}>
              <Button type={'secondarySmall'}
                      disabled={loadingNotebook}
                      data-test-id='code-preview-button'
                      onClick={() => onCodePreviewClick()}>
                {rightPanelContent ? 'Hide Code Preview' : 'See Code Preview'}
              </Button>
              {loadingNotebook && <Spinner size={24} style={{marginLeft: '0.5rem'}}/>}
            </FlexRow>
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
                      disabled={!fp.isEmpty(errors)}
                      onClick={() => saveDataSet()}>
                Export
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

