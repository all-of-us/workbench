import {Button} from 'app/components/buttons';
import {AnimatedModal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {appendNotebookFileSuffix} from 'app/pages/analysis/util';

import {dataSetApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import {summarizeErrors, withCurrentWorkspace} from 'app/utils';
import {encodeURIComponentStrict, navigateByUrl} from 'app/utils/navigation';
import {BillingStatus, DataSet, DataSetRequest, KernelTypeEnum, } from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect, useState} from 'react';

import {FlexRow} from 'app/components/flex';
import {SmallHeader, styles as headerStyles} from 'app/components/headers';
import {RadioButton, Select, TextInput} from 'app/components/inputs';
import {ErrorMessage} from 'app/components/messages';
import {Spinner} from 'app/components/spinners';
import colors from 'app/styles/colors';
import {AnalyticsTracker} from 'app/utils/analytics';
import {ACTION_DISABLED_INVALID_BILLING} from 'app/utils/strings';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {validate} from 'validate.js';

interface Props {
  closeFunction: Function;
  dataset: DataSet;
}

// HocProps includes all props that are inherited through HOC
interface HocProps extends Props {
  workspace: WorkspaceData;
}

export const ExportDatasetModal: (props: Props) => JSX.Element = fp.flow(withCurrentWorkspace())(
  ({workspace, dataset, closeFunction}: HocProps) => {
    const [existingNotebooks, setExistingNotebooks] = useState(undefined);
    const [kernelType, setKernelType] = useState(KernelTypeEnum.Python);
    const [isExporting, setIsExporting] = useState(false);
    const [creatingNewNotebook, setCreatingNewNotebook] = useState(true);
    const [notebookName, setNotebookName] = useState('');
    const [codePreview, setCodePreview] = useState(null);
    const [loadingNotebook, setIsLoadingNotebook] = useState(false);
    const [errorMsg, setErrorMsg] = useState(null);

    async function exportDataset() {
      AnalyticsTracker.DatasetBuilder.Export(kernelType);

      setErrorMsg(null);
      setIsExporting(true);
      try {
        await dataSetApi().exportToNotebook(
          workspace.namespace, workspace.id,
          {
            dataSetRequest: createDataSetRequest(),
            kernelType: kernelType,
            notebookName: notebookName,
            newNotebook: creatingNewNotebook
          });
        // Open notebook in a new tab and return back to the Data tab
        const notebookUrl = `/workspaces/${workspace.namespace}/${workspace.id}/notebooks/preview/`
          + appendNotebookFileSuffix(encodeURIComponentStrict(notebookName));
        navigateByUrl(notebookUrl);
      } catch (e) {
        console.error(e);
        setIsExporting(false);
        setErrorMsg('The request cannot be completed. Please try again or contact Support in the left hand navigation');
      }
    }

    function createDataSetRequest(): DataSetRequest {
      return {
        name: dataset ? dataset.name : 'dataset',
        ...(dataset.id
          ? { dataSetId: dataset.id }
          : {
            dataSetId: dataset.id,
            includesAllParticipants: dataset.includesAllParticipants,
            conceptSetIds: dataset.conceptSets.map(cs => cs.id),
            cohortIds: dataset.cohorts.map(c => c.id),
            domainValuePairs: dataset.domainValuePairs,
            prePackagedConceptSet: dataset.prePackagedConceptSet
          })
      };
    }

    function loadHtmlStringIntoIFrame(html) {
      const placeholder = document.createElement('html');
      placeholder.innerHTML = html;
      // Some styling changes to the jupyter notebook to make it easier to view
      placeholder.style.overflowY = 'scroll';
      placeholder.getElementsByTagName('body')[0].style.overflowY = 'scroll';
      placeholder.querySelector<HTMLElement>('#notebook').style.paddingTop = '0';

      // Remove input column from notebook cells. Also possible to strip this out in Calhoun but requires some API changes
      placeholder.querySelectorAll('.input_prompt').forEach(e => e.remove());
      return <iframe scrolling='no' style={{width: '100%', height: '100%', border: 'none'}} srcDoc={placeholder.outerHTML}/>;
    }

    function loadCodePreview(kernel: KernelTypeEnum) {
      setIsLoadingNotebook(true);
      setErrorMsg(null);
      dataSetApi().previewExportToNotebook(workspace.namespace, workspace.id, {
        dataSetRequest: createDataSetRequest(),
        kernelType: kernel,
        newNotebook: false,
        notebookName: '',
      }).then(resp => setCodePreview(loadHtmlStringIntoIFrame(resp.html)))
        .catch(() => setErrorMsg('Could not load code preview. Please try again or continue exporting to a notebook.'))
        .finally(() => setIsLoadingNotebook(false));
    }

    function onCodePreviewClick() {
      if (codePreview) {
        setCodePreview(null);
      } else {
        AnalyticsTracker.DatasetBuilder.SeeCodePreview();
        loadCodePreview(kernelType);
      }
    }

    function onNotebookSelect(value) {
      setCreatingNewNotebook(value === '');
      setNotebookName(value);
      setErrorMsg(null);

      if (value === '') {
        setCreatingNewNotebook(true);
      } else {
        setCreatingNewNotebook(false);
        setIsLoadingNotebook(true);
        workspacesApi().getNotebookKernel(workspace.namespace, workspace.id, value)
          .then(resp => setKernelType(resp.kernelType))
          .catch(() => setErrorMsg('Could not fetch notebook metadata. Please try again or create a new notebook.'))
          .finally(() => setIsLoadingNotebook(false));
      }
    }

    useEffect(() => {
      workspacesApi().getNoteBookList(workspace.namespace, workspace.id)
        .then(notebooks => setExistingNotebooks(notebooks.map(fileDetail => fileDetail.name.slice(0, -6))))
        .catch(() => setExistingNotebooks([])); // If the request fails, at least let the user create new notebooks
    }, [workspace]);

    useEffect(() => {
      if (codePreview) {
        loadCodePreview(kernelType);
      }
    }, [kernelType]);

    const errors = {
      ...validate({notebookName}, {
        notebookName: {
          presence: {allowEmpty: false},
          exclusion: {
            within: existingNotebooks,
            message: 'already exists'
          }
        }
      }),
      ...(workspace.billingStatus === BillingStatus.INACTIVE
        ? { billing: [ACTION_DISABLED_INVALID_BILLING]}
        : {}),
      ...(!WorkspacePermissionsUtil.canWrite(workspace.accessLevel)
        ? { permission: ['Exporting to a notebook requires write access to the workspace']}
        : {})
    };

    const isNotebooksLoading = existingNotebooks === undefined;

    const selectOptions = [{label: '(Create a new notebook)', value: ''}];
    if (!isNotebooksLoading) {
      selectOptions.push(...existingNotebooks.map(notebook => ({
        value: notebook,
        label: notebook,
      })));
    }

    return <AnimatedModal loading={isExporting || isNotebooksLoading} width={!codePreview ? 450 : 1200}>
      <FlexRow>
        <div style={{width: 'calc(450px - 2rem)'}}>
          <ModalTitle>Export Dataset</ModalTitle>
          <ModalBody>

            <div style={{marginTop: '1rem'}}>
              <Select value={creatingNewNotebook ? '' : notebookName}
                      data-test-id='select-notebook'
                      options={selectOptions}
                      onChange={(v) => onNotebookSelect(v)}/>
            </div>

            {creatingNewNotebook && <React.Fragment>
                <SmallHeader style={{fontSize: 14, marginTop: '1rem'}}>Notebook Name</SmallHeader>
                <TextInput onChange={v => setNotebookName(v)}
                           value={notebookName} data-test-id='notebook-name-input'/>
            </React.Fragment>}

            <div style={headerStyles.formLabel}>
              Select programming language
            </div>
            {Object.keys(KernelTypeEnum).map(kernelTypeEnumKey => KernelTypeEnum[kernelTypeEnumKey])
              .map((kernelTypeEnum, i) =>
                <label key={i} style={
                  {display: 'inline-flex', justifyContent: 'center', alignItems: 'center', marginRight: '1rem', color: colors.primary}}>
                  <RadioButton
                    style={{marginRight: '0.25rem'}}
                    data-test-id={'kernel-type-' + kernelTypeEnum.toLowerCase()}
                    disabled={loadingNotebook || !creatingNewNotebook}
                    checked={kernelType === kernelTypeEnum}
                    onChange={() => setKernelType(kernelTypeEnum)}
                  />
                  {kernelTypeEnum}
                </label>)}

            <FlexRow style={{marginTop: '1rem', alignItems: 'center'}}>
              <Button type={'secondarySmall'}
                      disabled={loadingNotebook}
                      data-test-id='code-preview-button'
                      onClick={() => onCodePreviewClick()}>
                {codePreview ? 'Hide Code Preview' : 'See Code Preview'}
              </Button>
              {loadingNotebook && <Spinner size={24} style={{marginLeft: '0.5rem'}}/>}
            </FlexRow>

            {errorMsg && <ErrorMessage iconSize={20}> {errorMsg} </ErrorMessage>}
          </ModalBody>
          <ModalFooter>
            <Button type='secondary'
                    onClick={closeFunction}
                    style={{marginRight: '2rem'}}>
              Cancel
            </Button>
            <TooltipTrigger content={summarizeErrors(errors)} data-test-id='export-dataset-tooltip'>
              <Button type='primary'
                      data-test-id='export-data-set'
                      disabled={!fp.isEmpty(errors)}
                      onClick={() => exportDataset()}>
                Export
              </Button>
            </TooltipTrigger>
          </ModalFooter>
        </div>

        {codePreview &&
        <div style={{flex: 1, marginLeft: '1rem'}}>
          {codePreview}
        </div>}
      </FlexRow>
    </AnimatedModal>;
  });

