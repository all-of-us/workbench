import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';
import { validate } from 'validate.js';

import {
  BillingStatus,
  DataSet,
  DataSetExportRequest,
  DataSetExportRequestGenomicsAnalysisToolEnum,
  DataSetRequest,
  KernelTypeEnum,
  PrePackagedConceptSetEnum,
  ResourceType,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { SmallHeader, styles as headerStyles } from 'app/components/headers';
import { RadioButton, Select, TextInput } from 'app/components/inputs';
import { ErrorMessage } from 'app/components/messages';
import {
  AnimatedModal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import {
  appendJupyterNotebookFileSuffix,
  dropJupyterNotebookFileSuffix,
  getExistingNotebookNames,
} from 'app/pages/analysis/util';
import { dataSetApi, notebooksApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles, summarizeErrors } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { encodeURIComponentStrict, useNavigation } from 'app/utils/navigation';
import { nameValidationFormat } from 'app/utils/resources';
import { ACTION_DISABLED_INVALID_BILLING } from 'app/utils/strings';
import { analysisTabName } from 'app/utils/user-apps-utils';
import { WorkspaceData } from 'app/utils/workspace-data';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';

interface Props {
  closeFunction: Function;
  dataset: DataSet;
  workspace: WorkspaceData;
}

const styles = reactStyles({
  radioButtonLabel: {
    display: 'inline-flex',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: '1.5rem',
    color: colors.primary,
  },
});

export const ExportDatasetModal = ({
  workspace,
  dataset,
  closeFunction,
}: Props) => {
  const [existingNotebooks, setExistingNotebooks] = useState(undefined);
  const [kernelType, setKernelType] = useState<KernelTypeEnum>(
    KernelTypeEnum.PYTHON
  );
  const [genomicsAnalysisTool, setGenomicsAnalysisTool] =
    useState<DataSetExportRequestGenomicsAnalysisToolEnum>(
      DataSetExportRequestGenomicsAnalysisToolEnum.HAIL
    );
  const [isExporting, setIsExporting] = useState(false);
  const [creatingNewNotebook, setCreatingNewNotebook] = useState(true);
  const [notebookNameWithoutSuffix, setNotebookNameWithoutSuffix] =
    useState('');
  const [codePreview, setCodePreview] = useState(null);
  const [loadingNotebook, setIsLoadingNotebook] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);
  const [, navigateByUrl] = useNavigation();

  function createDataSetRequest(): DataSetRequest {
    return {
      name: dataset ? dataset.name : 'dataset',
      ...(dataset.id
        ? { dataSetId: dataset.id }
        : {
            dataSetId: dataset.id,
            includesAllParticipants: dataset.includesAllParticipants,
            conceptSetIds: dataset.conceptSets.map((cs) => cs.id),
            cohortIds: dataset.cohorts.map((c) => c.id),
            domainValuePairs: dataset.domainValuePairs,
            prePackagedConceptSet: dataset.prePackagedConceptSet,
          }),
      domainValuePairs: [],
    };
  }

  function hasWgs() {
    return fp.includes(
      PrePackagedConceptSetEnum.WHOLE_GENOME,
      dataset.prePackagedConceptSet
    );
  }

  function createExportDatasetRequest(): DataSetExportRequest {
    return {
      dataSetRequest: createDataSetRequest(),
      kernelType,
      genomicsAnalysisTool,
      generateGenomicsAnalysisCode: hasWgs(),
      notebookName: appendJupyterNotebookFileSuffix(notebookNameWithoutSuffix),
      newNotebook: creatingNewNotebook,
    };
  }

  async function exportDataset() {
    AnalyticsTracker.DatasetBuilder.Export(kernelType);

    setErrorMsg(null);
    setIsExporting(true);
    try {
      await dataSetApi().exportToNotebook(
        workspace.namespace,
        workspace.id,
        createExportDatasetRequest()
      );
      const notebookUrl =
        `/workspaces/${workspace.namespace}/${workspace.id}/${analysisTabName}/preview/` +
        encodeURIComponentStrict(
          appendJupyterNotebookFileSuffix(notebookNameWithoutSuffix)
        );
      navigateByUrl(notebookUrl);
    } catch (e) {
      console.error(e);
      setIsExporting(false);
      setErrorMsg(
        'The request cannot be completed. Please try again or contact Support in the left hand navigation'
      );
    }
  }

  function loadHtmlStringIntoIFrame(html) {
    const placeholder = document.createElement('html');
    placeholder.innerHTML = html;

    // Remove input column from notebook cells. Also possible to strip this out in Calhoun but requires some API changes
    placeholder.querySelectorAll('.jp-InputPrompt').forEach((e) => e.remove());
    return (
      <iframe
        id='export-preview-frame'
        scrolling='yes'
        style={{ width: '100%', height: '100%', border: 'none' }}
        srcDoc={placeholder.outerHTML}
      />
    );
  }

  function loadCodePreview() {
    setIsLoadingNotebook(true);
    setErrorMsg(null);
    dataSetApi()
      .previewExportToNotebook(
        workspace.namespace,
        workspace.id,
        createExportDatasetRequest()
      )
      .then((resp) => setCodePreview(loadHtmlStringIntoIFrame(resp.html)))
      .catch(() =>
        setErrorMsg(
          'Could not load code preview. Please try again or continue exporting to a notebook.'
        )
      )
      .finally(() => setIsLoadingNotebook(false));
  }

  function onCodePreviewClick() {
    if (codePreview) {
      setCodePreview(null);
    } else {
      AnalyticsTracker.DatasetBuilder.SeeCodePreview();
      loadCodePreview();
    }
  }

  function onNotebookSelect(nameWithoutSuffix) {
    setCreatingNewNotebook(nameWithoutSuffix === '');
    setNotebookNameWithoutSuffix(nameWithoutSuffix);
    setErrorMsg(null);

    if (nameWithoutSuffix === '') {
      setCreatingNewNotebook(true);
    } else {
      setCreatingNewNotebook(false);
      setIsLoadingNotebook(true);
      notebooksApi()
        .getNotebookKernel(
          workspace.namespace,
          workspace.id,
          appendJupyterNotebookFileSuffix(nameWithoutSuffix)
        )
        .then((resp) => setKernelType(resp.kernelType))
        .catch(() =>
          setErrorMsg(
            'Could not fetch notebook metadata. Please try again or create a new notebook.'
          )
        )
        .finally(() => setIsLoadingNotebook(false));
    }
  }

  function genomicsToolRadioButton(
    displayName: string,
    genomicsTool: DataSetExportRequestGenomicsAnalysisToolEnum
  ) {
    return (
      <label
        key={'genomics-tool-' + genomicsTool}
        style={styles.radioButtonLabel}
      >
        <RadioButton
          style={{ marginRight: '0.375rem' }}
          disabled={loadingNotebook}
          data-test-id={'genomics-tool-' + genomicsTool}
          checked={genomicsAnalysisTool === genomicsTool}
          onChange={() => setGenomicsAnalysisTool(genomicsTool)}
        />
        {displayName}
      </label>
    );
  }

  useEffect(() => {
    getExistingNotebookNames(workspace)
      .then(setExistingNotebooks)
      .catch(() => setExistingNotebooks([])); // If the request fails, at least let the user create new notebooks
  }, [workspace]);

  useEffect(() => {
    if (codePreview) {
      loadCodePreview();
    }
  }, [kernelType, genomicsAnalysisTool]);

  const errors = {
    ...validate(
      // we expect the notebook name to lack the .ipynb suffix
      // but we pass it through drop-suffix to also catch the case where the user has explicitly typed it in
      {
        notebookName: dropJupyterNotebookFileSuffix(notebookNameWithoutSuffix),
      },
      {
        notebookName: nameValidationFormat(
          creatingNewNotebook ? existingNotebooks : [],
          ResourceType.NOTEBOOK
        ),
      }
    ),
    ...(workspace.billingStatus === BillingStatus.INACTIVE
      ? { billing: [ACTION_DISABLED_INVALID_BILLING] }
      : {}),
    ...(!WorkspacePermissionsUtil.canWrite(workspace.accessLevel)
      ? {
          permission: [
            'Exporting to a notebook requires write access to the workspace',
          ],
        }
      : {}),
  };

  const isNotebooksLoading = existingNotebooks === undefined;

  const selectOptions = [{ label: '(Create a new notebook)', value: '' }];
  if (!isNotebooksLoading) {
    selectOptions.push(
      ...existingNotebooks.map((notebook) => ({
        value: notebook,
        label: notebook,
      }))
    );
  }

  return (
    <AnimatedModal
      loading={isExporting || isNotebooksLoading}
      width={!codePreview ? 450 : 1200}
    >
      <FlexRow>
        <div style={{ width: 'calc(450px - 3rem)' }}>
          <ModalTitle>Export Dataset</ModalTitle>
          <ModalBody>
            <div style={{ marginTop: '1.5rem' }}>
              <Select
                value={creatingNewNotebook ? '' : notebookNameWithoutSuffix}
                data-test-id='select-notebook'
                options={selectOptions}
                onChange={(v) => onNotebookSelect(v)}
              />
            </div>

            {creatingNewNotebook && (
              <React.Fragment>
                <SmallHeader style={{ fontSize: 14, marginTop: '1.5rem' }}>
                  Notebook Name
                </SmallHeader>
                <TextInput
                  onChange={(v) => setNotebookNameWithoutSuffix(v)}
                  value={notebookNameWithoutSuffix}
                  data-test-id='notebook-name-input'
                />
              </React.Fragment>
            )}

            <div style={headerStyles.formLabel}>
              Select programming language
            </div>
            {Object.keys(KernelTypeEnum)
              .map((kernelTypeEnumKey) => KernelTypeEnum[kernelTypeEnumKey])
              .map((kernelTypeEnum, i) => (
                <label key={i} style={styles.radioButtonLabel}>
                  <RadioButton
                    style={{ marginRight: '0.375rem' }}
                    data-test-id={'kernel-type-' + kernelTypeEnum.toLowerCase()}
                    disabled={loadingNotebook || !creatingNewNotebook}
                    checked={kernelType === kernelTypeEnum}
                    onChange={() => setKernelType(kernelTypeEnum)}
                  />
                  {kernelTypeEnum}
                </label>
              ))}

            {hasWgs() && kernelType === KernelTypeEnum.PYTHON && (
              <React.Fragment>
                <div style={headerStyles.formLabel}>
                  Select analysis tool for genetic variant data
                </div>
                {genomicsToolRadioButton(
                  'Hail',
                  DataSetExportRequestGenomicsAnalysisToolEnum.HAIL
                )}
                {genomicsToolRadioButton(
                  'PLINK',
                  DataSetExportRequestGenomicsAnalysisToolEnum.PLINK
                )}
                {genomicsToolRadioButton(
                  'Other VCF-compatible tool',
                  DataSetExportRequestGenomicsAnalysisToolEnum.NONE
                )}
              </React.Fragment>
            )}

            <FlexRow style={{ marginTop: '1.5rem', alignItems: 'center' }}>
              <Button
                type={'secondarySmall'}
                disabled={loadingNotebook}
                data-test-id='code-preview-button'
                onClick={() => onCodePreviewClick()}
              >
                {codePreview ? 'Hide Code Preview' : 'See Code Preview'}
              </Button>
              {loadingNotebook && (
                <Spinner size={24} style={{ marginLeft: '0.75rem' }} />
              )}
            </FlexRow>

            {errorMsg && (
              <ErrorMessage iconSize={20}> {errorMsg} </ErrorMessage>
            )}
          </ModalBody>
          <ModalFooter>
            <Button
              type='secondary'
              data-test-id='export-dataset-modal-cancel-button'
              onClick={closeFunction}
              style={{ marginRight: '3rem' }}
            >
              Cancel
            </Button>
            <TooltipTrigger
              content={summarizeErrors(errors)}
              data-test-id='export-dataset-tooltip'
            >
              <Button
                type='primary'
                data-test-id='export-data-set'
                disabled={!fp.isEmpty(errors)}
                onClick={() => exportDataset()}
              >
                Export
              </Button>
            </TooltipTrigger>
          </ModalFooter>
        </div>

        {codePreview && (
          <div style={{ flex: 1, marginLeft: '1.5rem' }}>{codePreview}</div>
        )}
      </FlexRow>
    </AnimatedModal>
  );
};
