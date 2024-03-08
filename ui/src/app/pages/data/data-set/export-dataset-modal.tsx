import * as React from 'react';
import { useEffect, useState } from 'react';
import fp from 'lodash/fp';
import { Toast } from 'primereact/toast';

import {
  AnalysisLanguage,
  BillingStatus,
  DataSet,
  DataSetExportRequest,
  DataSetExportRequestGenomicsAnalysisToolEnum,
  DataSetRequest,
  PrePackagedConceptSetEnum,
} from 'generated/fetch';

import { Button, IconButton } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { SmallHeader, styles as headerStyles } from 'app/components/headers';
import { Copy } from 'app/components/icons';
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
  getExistingJupyterNotebookNames,
} from 'app/pages/analysis/util';
import { analysisTabPath } from 'app/routing/utils';
import { dataSetApi, notebooksApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { isEmpty, reactStyles, summarizeErrors } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { encodeURIComponentStrict, useNavigation } from 'app/utils/navigation';
import { validateNewNotebookName } from 'app/utils/resources';
import { ACTION_DISABLED_INVALID_BILLING } from 'app/utils/strings';
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
  const [existingJupyterNotebooks, setExistingJupyterNotebooks] =
    useState<string[]>(undefined);
  const [analysisLanguage, setAnalysisLanguage] = useState<AnalysisLanguage>(
    AnalysisLanguage.PYTHON
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
  const [showCodePreview, setShowCodePreview] = useState(false);
  const [codeText, setCodeText] = useState(null);
  const [loadingCode, setLoadingCode] = useState(false);
  const [loadingClipboard, setLoadingClipboard] = useState(false);
  const [loadingNotebookKernel, setLoadingNotebookKernel] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);
  const [, navigateByUrl] = useNavigation();
  const toast = React.useRef(null);

  const createDataSetRequest = (): DataSetRequest => {
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
  };

  const hasWgs = () => {
    return fp.includes(
      PrePackagedConceptSetEnum.WHOLE_GENOME,
      dataset.prePackagedConceptSet
    );
  };

  const createExportDatasetRequest = (
    language: AnalysisLanguage
  ): DataSetExportRequest => {
    return {
      dataSetRequest: createDataSetRequest(),
      analysisLanguage: language,
      genomicsAnalysisTool,
      generateGenomicsAnalysisCode: hasWgs(),
      notebookName: appendJupyterNotebookFileSuffix(notebookNameWithoutSuffix),
      newNotebook: creatingNewNotebook,
    };
  };

  const exportDataset = async (language: AnalysisLanguage) => {
    AnalyticsTracker.DatasetBuilder.Export(analysisLanguage);

    setErrorMsg(null);
    setIsExporting(true);
    try {
      await dataSetApi().exportToNotebook(
        workspace.namespace,
        workspace.id,
        createExportDatasetRequest(language)
      );
      const notebookUrl =
        `${analysisTabPath(workspace.namespace, workspace.id)}/preview/` +
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
  };

  const isNotebooksLoading = existingJupyterNotebooks === undefined;
  const isCodePreviewLoading = showCodePreview && !codePreview;

  // Any state that necessitates disabling all modal controls.
  const shouldDisable =
    isNotebooksLoading ||
    loadingNotebookKernel ||
    loadingClipboard ||
    loadingCode ||
    isExporting;

  const resetCode = () => {
    setCodePreview(null);
    setCodeText(null);
  };

  const onChangeGenomicsAnalysisTool = (
    genomicsTool: DataSetExportRequestGenomicsAnalysisToolEnum
  ) => {
    resetCode();
    setGenomicsAnalysisTool(genomicsTool);
  };
  const genomicsToolRadioButton = (
    displayName: string,
    genomicsTool: DataSetExportRequestGenomicsAnalysisToolEnum
  ) => {
    return (
      <label
        key={'genomics-tool-' + genomicsTool}
        style={styles.radioButtonLabel}
      >
        <RadioButton
          name='genomics-tool'
          style={{ marginRight: '0.375rem' }}
          disabled={shouldDisable}
          data-test-id={'genomics-tool-' + genomicsTool}
          checked={genomicsAnalysisTool === genomicsTool}
          onChange={() => onChangeGenomicsAnalysisTool(genomicsTool)}
        />
        {displayName}
      </label>
    );
  };

  const loadHtmlStringIntoIFrame = (html: string) => {
    const placeholder = document.createElement('html');
    placeholder.innerHTML = html;

    // Remove top padding and margin, because it looks odd with
    // the copy button above it
    placeholder.getElementsByTagName('body')[0].style.marginTop = '0';
    placeholder.getElementsByTagName('body')[0].style.paddingTop = '0';
    // Remove input column from notebook cells. Also possible to strip this out in Calhoun but requires some API changes
    placeholder.querySelectorAll('.jp-InputPrompt').forEach((e) => e.remove());
    return (
      <iframe
        id='export-preview-frame'
        data-testid='export-preview-frame'
        scrolling='yes'
        style={{
          width: '100%',
          height: '100%',
          border: '1px solid',
          borderRadius: '0.5rem',
        }}
        srcDoc={placeholder.outerHTML}
      />
    );
  };

  const getCode = (language: AnalysisLanguage) => {
    setLoadingCode(true);
    setErrorMsg(null);
    dataSetApi()
      .previewExportToNotebook(
        workspace.namespace,
        workspace.id,
        createExportDatasetRequest(language)
      )
      .then((resp) => {
        setCodePreview(loadHtmlStringIntoIFrame(resp.html));
        setCodeText(resp.text);
      })
      .catch(() =>
        setErrorMsg(
          'Could not load code. Please try again or continue exporting to a notebook.'
        )
      )
      .finally(() => setLoadingCode(false));
  };

  const onCodePreviewClick = () => {
    if (!codePreview) {
      AnalyticsTracker.DatasetBuilder.SeeCodePreview();
    }
    setShowCodePreview(!showCodePreview);
  };
  const onCopyCodeClick = () => {
    if (!codeText) {
      getCode(analysisLanguage);
    }
    setLoadingClipboard(true);
  };

  const onChangeAnalysisLanguage = (language: AnalysisLanguage) => {
    resetCode();
    setAnalysisLanguage(language);
  };

  const onJupyterNotebookSelect = (nameWithoutSuffix: string) => {
    setCreatingNewNotebook(nameWithoutSuffix === '');
    setNotebookNameWithoutSuffix(nameWithoutSuffix);
    setErrorMsg(null);

    if (nameWithoutSuffix !== '') {
      setLoadingNotebookKernel(true);
      notebooksApi()
        .getNotebookKernel(
          workspace.namespace,
          workspace.id,
          appendJupyterNotebookFileSuffix(nameWithoutSuffix)
        )
        .then((resp) => {
          // can compare directly for now, because they exactly match
          if (resp.kernelType !== analysisLanguage) {
            setAnalysisLanguage(resp.kernelType);
            // code preview will be stale if the language changed
            getCode(resp.kernelType);
          }
        })
        .catch(() =>
          setErrorMsg(
            'Could not fetch notebook metadata. Please try again or create a new notebook.'
          )
        )
        .finally(() => setLoadingNotebookKernel(false));
    }
  };

  useEffect(() => {
    getExistingJupyterNotebookNames(workspace)
      .then(setExistingJupyterNotebooks)
      .catch(() => setExistingJupyterNotebooks([])); // If the request fails, at least let the user create new notebooks
  }, [workspace]);

  // When code preview is showing, but codePreview does not
  // have a value, update code.
  useEffect(() => {
    if (showCodePreview && !codePreview) {
      getCode(analysisLanguage);
    }
  }, [codePreview, showCodePreview]);

  // If the user is waiting for the code to be copied to the
  // clipboard, and the code has been loaded, copy the code to the
  // clipboard and stop waiting.
  useEffect(() => {
    if (loadingClipboard && codeText) {
      setLoadingClipboard(false);
      navigator.clipboard.writeText(codeText);
      toast.current.show({
        severity: 'success',
        summary: 'Copied to clipboard',
        detail: 'Dataset query copied to clipboard',
      });
    }
  }, [codeText, loadingClipboard]);

  const errors = {
    ...validateNewNotebookName(
      notebookNameWithoutSuffix,
      creatingNewNotebook ? existingJupyterNotebooks : [],
      'notebookName'
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

  const selectOptions = [{ label: '(Create a new notebook)', value: '' }];
  if (!isNotebooksLoading) {
    selectOptions.push(
      ...existingJupyterNotebooks.map((notebook) => ({
        value: notebook,
        label: notebook,
      }))
    );
  }

  return (
    <AnimatedModal
      loading={isExporting || isNotebooksLoading || loadingNotebookKernel}
      width={showCodePreview && codePreview ? 1200 : 450}
    >
      <FlexRow>
        <div style={{ width: 'calc(450px - 3rem)' }}>
          <ModalTitle>Export Dataset</ModalTitle>
          <ModalBody>
            <div style={{ marginTop: '1.5rem' }}>
              <Select
                isDisabled={shouldDisable}
                value={creatingNewNotebook ? '' : notebookNameWithoutSuffix}
                data-test-id='select-notebook'
                options={selectOptions}
                onChange={onJupyterNotebookSelect}
              />
            </div>

            {creatingNewNotebook && (
              <label>
                <SmallHeader style={{ fontSize: 14, marginTop: '1.5rem' }}>
                  Notebook Name
                </SmallHeader>
                <TextInput
                  onChange={(v) => setNotebookNameWithoutSuffix(v)}
                  value={notebookNameWithoutSuffix}
                  data-test-id='notebook-name-input'
                  disabled={shouldDisable}
                />
              </label>
            )}

            <div style={headerStyles.formLabel}>
              Select programming language
            </div>
            {Object.keys(AnalysisLanguage)
              .map(
                (analysisLanguageKey) => AnalysisLanguage[analysisLanguageKey]
              )
              .map((analysisLanguageValue) => (
                <label
                  key={analysisLanguageValue}
                  style={styles.radioButtonLabel}
                >
                  <RadioButton
                    style={{ marginRight: '0.375rem' }}
                    name='Analysis Language'
                    disabled={shouldDisable || !creatingNewNotebook}
                    checked={analysisLanguage === analysisLanguageValue}
                    onChange={() =>
                      onChangeAnalysisLanguage(analysisLanguageValue)
                    }
                  />
                  {analysisLanguageValue}
                </label>
              ))}

            {hasWgs() && analysisLanguage === AnalysisLanguage.PYTHON && (
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

            <FlexRow style={{ alignItems: 'center' }}>
              <Button
                type='link'
                disabled={isCodePreviewLoading || shouldDisable}
                data-test-id='code-preview-button'
                onClick={() => onCodePreviewClick()}
                style={{ padding: 0, margin: 0 }}
              >
                {showCodePreview ? 'Hide Code Preview' : 'See Code Preview'}
              </Button>
              {isCodePreviewLoading && (
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
            <Toast ref={toast} />
            <Button
              id='copyCodeButton'
              disabled={isCodePreviewLoading || shouldDisable}
              type='secondaryOutline'
              onClick={onCopyCodeClick}
              style={{ marginRight: '3rem', minWidth: '120px' }}
            >
              {loadingClipboard ? <Spinner size={18} /> : 'Copy Code'}
            </Button>
            <TooltipTrigger
              content={summarizeErrors(errors)}
              data-test-id='export-dataset-tooltip'
            >
              <Button
                type='primary'
                data-test-id='export-data-set'
                disabled={!isEmpty(errors) || shouldDisable}
                onClick={() => exportDataset(analysisLanguage)}
              >
                Export
              </Button>
            </TooltipTrigger>
          </ModalFooter>
        </div>

        {showCodePreview && codePreview && (
          <FlexColumn
            style={{ display: 'flex', flex: 1, marginLeft: '1.5rem' }}
          >
            <div
              style={{
                alignSelf: 'flex-end',
                padding: '0 0.5rem',
                marginBottom: '0.5rem',
              }}
            >
              <IconButton
                icon={Copy}
                onClick={onCopyCodeClick}
                style={{ height: '24px', width: '24px' }}
                title='Dataset query copy icon button'
              />
            </div>
            {codePreview}
          </FlexColumn>
        )}
      </FlexRow>
    </AnimatedModal>
  );
};
