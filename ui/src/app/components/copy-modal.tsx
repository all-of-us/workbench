import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  CdrVersionTiersResponse,
  ConceptSet,
  CopyRequest,
  FileDetail,
  ResourceType,
  Workspace,
} from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { Button } from 'app/components/buttons';
import { styles as headerStyles } from 'app/components/headers';
import { Select, TextInput, ValidationError } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { Spinner } from 'app/components/spinners';
import { analysisTabPath, dataTabPath } from 'app/routing/utils';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, summarizeErrors, withCdrVersions } from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { NavigationProps } from 'app/utils/navigation';
import { toDisplay } from 'app/utils/resources';
import { WorkspacePermissions } from 'app/utils/workspace-permissions';

import { validateCopyModal } from './copy-modal-validation';
import { FlexRow } from './flex';
import { ClrIcon } from './icons';

enum RequestState {
  UNSENT,
  COPY_ERROR,
  SUCCESS,
}

export interface CopyModalProps {
  fromWorkspaceNamespace: string;
  fromWorkspaceTerraName: string;
  fromResourceName: string;
  fromCdrVersionId: string;
  fromAccessTierShortName: string;
  onClose: Function;
  onCopy: Function;
  resourceType: ResourceType;
  saveFunction: (cr: CopyRequest) => Promise<FileDetail | ConceptSet>;
}

interface HocProps extends CopyModalProps, NavigationProps {
  cdrVersionTiersResponse: CdrVersionTiersResponse;
}

interface WorkspaceOptions {
  label: string;
  options: Array<{ label: string; value: Workspace }>;
}

interface CopyModalState {
  workspaceOptions: Array<WorkspaceOptions>;
  destination: Workspace;
  newName: string;
  requestState: RequestState;
  copyErrorMsg: string;
  loading: boolean;
  cdrMismatch: string;
  accessTierMismatch: string;
}

const styles = reactStyles({
  bold: {
    fontWeight: 600,
  },
  mismatchError: {
    color: colors.danger,
    marginLeft: '0.75rem',
    marginTop: '0.375rem',
    fontFamily: 'Montserrat',
    fontSize: '12px',
    letterSpacing: 0,
    lineHeight: '22px',
  },
  mismatchWarning: {
    padding: '8px',
    fontFamily: 'Font Awesome 5 Pro',
    letterSpacing: 0,
    boxSizing: 'border-box',
    color: colors.primary,
    borderColor: colors.warning,
    backgroundColor: colorWithWhiteness(colors.danger, 0.9),
    borderWidth: '1px',
    borderStyle: 'solid',
    borderRadius: '5px',
    lineHeight: '24px',
    marginTop: '1.5rem',
  },
  restriction: {
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '14px',
  },
  warningIcon: {
    color: colors.warning,
    height: '20px',
    width: '20px',
    align: 'top',
  },
});

const AccessTierMismatch = (props: { text: string }) => (
  <div data-test-id='access-tier-mismatch-error' style={styles.mismatchError}>
    {props.text}
  </div>
);

const ConceptSetCdrMismatch = (props: { text: string }) => (
  <div
    data-test-id='concept-set-cdr-mismatch-error'
    style={styles.mismatchError}
  >
    {props.text}
  </div>
);

const NotebookCdrMismatch = (props: { text: string }) => (
  <div
    data-test-id='notebook-cdr-mismatch-warning'
    style={styles.mismatchWarning}
  >
    <FlexRow>
      <div style={{ paddingRight: '0.75rem' }}>
        <ClrIcon
          shape='warning-standard'
          class='is-solid'
          style={styles.warningIcon}
        />
      </div>
      {props.text}
    </FlexRow>
  </div>
);

const ConceptSetRestrictionText = () => (
  <div style={styles.restriction}>
    Concept sets can only be copied to workspaces using the same access tier and
    CDR version.
  </div>
);

const NotebookRestrictionText = () => (
  <div style={styles.restriction}>
    Notebooks can only be copied to workspaces in the same access tier.
  </div>
);

const CopyModal = withCdrVersions()(
  class extends React.Component<HocProps, CopyModalState> {
    constructor(props: HocProps) {
      super(props);
      this.state = {
        workspaceOptions: [],
        newName: props.fromResourceName,
        destination: null,
        requestState: RequestState.UNSENT,
        copyErrorMsg: '',
        loading: true,
        cdrMismatch: '',
        accessTierMismatch: '',
      };
    }

    isDestinationSameWorkspace(workspace: Workspace): boolean {
      const { fromWorkspaceTerraName, fromWorkspaceNamespace } = this.props;
      return (
        workspace.terraName === fromWorkspaceTerraName &&
        workspace.namespace === fromWorkspaceNamespace
      );
    }

    cdrName(cdrVersionId: string): string {
      const { cdrVersionTiersResponse } = this.props;
      const version = findCdrVersion(cdrVersionId, cdrVersionTiersResponse);
      return version ? version.name : '[CDR version not found]';
    }

    groupWorkspacesByCdrVersion(
      workspaces: Workspace[]
    ): Array<WorkspaceOptions> {
      const { fromCdrVersionId } = this.props;
      const workspacesByCdr = fp.groupBy((w) => w.cdrVersionId, workspaces);
      const cdrVersions = Array.from(
        new Set(workspaces.map((w) => w.cdrVersionId))
      );

      // list the "from" CDR version first.
      const fromCdrVersionFirst = (cdrv1: string, cdrv2: string) => {
        if (cdrv1 === fromCdrVersionId && cdrv2 !== fromCdrVersionId) {
          return -1;
        } else if (cdrv1 !== fromCdrVersionId && cdrv2 === fromCdrVersionId) {
          return 1;
        } else {
          // TODO: a meaningful ordering, possibly as part of RW-5563
          return cdrv1.localeCompare(cdrv2);
        }
      };

      return cdrVersions.sort(fromCdrVersionFirst).map((versionId) => ({
        label: this.cdrName(versionId),
        options: workspacesByCdr[versionId].map((workspace) => ({
          value: workspace,
          label: this.isDestinationSameWorkspace(workspace)
            ? `${workspace.name} (current workspace)`
            : workspace.name,
        })),
      }));
    }

    componentDidMount() {
      workspacesApi()
        .getWorkspaces()
        .then((response) => {
          const writeableWorkspaces = response.items
            .filter((item) => new WorkspacePermissions(item).canWrite)
            .map((workspaceResponse) => workspaceResponse.workspace);

          this.setState({
            workspaceOptions:
              this.groupWorkspacesByCdrVersion(writeableWorkspaces),
            loading: false,
          });
        });
    }

    setCopyError(errorMsg: string) {
      this.setState({
        copyErrorMsg: errorMsg,
        requestState: RequestState.COPY_ERROR,
        loading: false,
      });
    }

    clearCopyError() {
      this.setState({
        copyErrorMsg: '',
        requestState: RequestState.UNSENT,
      });
    }

    save() {
      this.setState({ loading: true });
      const { saveFunction, resourceType } = this.props;

      saveFunction({
        toWorkspaceNamespace: this.state.destination.namespace,
        toWorkspaceTerraName: this.state.destination.terraName,
        newName: this.state.newName,
      })
        .then((response) => {
          this.setState({ requestState: RequestState.SUCCESS, loading: false });
          this.props.onCopy(response);
        })
        .catch((response) => {
          const errorMsg =
            response.status === 409
              ? `${toDisplay(resourceType)} with the same ` +
                'name already exists in the targeted workspace.'
              : response.status === 404
              ? `${toDisplay(resourceType)} not found in the ` +
                'original workspace.'
              : 'An error occurred while copying. Please try again.';

          this.setCopyError(errorMsg);
        });
    }

    render() {
      const { resourceType } = this.props;
      const { loading, requestState } = this.state;

      return (
        <Modal onRequestClose={this.props.onClose}>
          <ModalTitle style={{ marginBottom: '0.75rem' }}>
            Copy to Workspace
          </ModalTitle>
          {resourceType === ResourceType.CONCEPT_SET && (
            <ConceptSetRestrictionText />
          )}
          {resourceType === ResourceType.NOTEBOOK && (
            <NotebookRestrictionText />
          )}
          {loading ? (
            <ModalBody style={{ textAlign: 'center' }}>
              <Spinner />
            </ModalBody>
          ) : (
            <ModalBody>
              {(requestState === RequestState.UNSENT ||
                requestState === RequestState.COPY_ERROR) &&
                this.renderFormBody()}
              {requestState === RequestState.SUCCESS &&
                this.renderSuccessBody()}
            </ModalBody>
          )}
          <ModalFooter>
            <Button type='secondary' onClick={this.props.onClose}>
              {this.getCloseButtonText()}
            </Button>
            {this.renderActionButton()}
          </ModalFooter>
        </Modal>
      );
    }

    getCloseButtonText() {
      if (
        this.state.requestState === RequestState.UNSENT ||
        this.state.requestState === RequestState.COPY_ERROR
      ) {
        return 'Close';
      } else if (this.state.requestState === RequestState.SUCCESS) {
        return 'Stay Here';
      }
    }

    renderActionButton() {
      const { resourceType } = this.props;
      const resourceString = toDisplay(resourceType);
      if (
        this.state.requestState === RequestState.UNSENT ||
        this.state.requestState === RequestState.COPY_ERROR
      ) {
        return (
          <Button
            style={{ marginLeft: '0.75rem' }}
            disabled={this.state.destination === null || this.state.loading}
            onClick={() => this.save()}
            data-test-id='copy-button'
          >
            Copy {resourceString}
          </Button>
        );
      } else if (this.state.requestState === RequestState.SUCCESS) {
        const { namespace, terraName } = this.state.destination;
        return (
          <Button
            path={
              resourceType === ResourceType.NOTEBOOK
                ? analysisTabPath(namespace, terraName)
                : dataTabPath(namespace, terraName)
            }
            style={{ marginLeft: '0.75rem' }}
          >
            Go to Copied {resourceString}
          </Button>
        );
      }
    }

    clearMismatchErrors(destination: Workspace) {
      this.setState({
        accessTierMismatch: '',
        cdrMismatch: '',
        destination: destination,
      });
    }

    // OK to copy a notebook with a mismatch, but show a warning message
    setNotebookCdrMismatchWarning(
      destination: Workspace,
      fromCdrVersionId: string
    ) {
      const warningMsg =
        'The selected destination workspace uses a different dataset version ' +
        `(${this.cdrName(
          destination.cdrVersionId
        )}) from the current workspace (${this.cdrName(fromCdrVersionId)}). ` +
        'Edits may be required to ensure your analysis is functional and accurate.';
      this.setState({ cdrMismatch: warningMsg, destination: destination });
    }

    // not OK to copy a Concept Set with a mismatch.  Show an error message and prevent copy
    setConceptSetCdrMismatchError(
      destination: Workspace,
      fromCdrVersionId: string
    ) {
      const errorMsg =
        'Can’t copy to that workspace. It uses a different dataset version ' +
        `(${this.cdrName(
          destination.cdrVersionId
        )}) from the current workspace (${this.cdrName(fromCdrVersionId)}).`;
      this.setState({ cdrMismatch: errorMsg, destination: null });
    }

    // never OK to copy anything across an access tier boundary.  Show an error message and prevent copy
    setAccessTierMismatchError(
      destination: Workspace,
      fromAccessTierShortName: string
    ) {
      const errorMsg =
        'Can’t copy to that workspace. It has a different access tier ' +
        `(${destination.accessTierShortName}) from the current workspace (${fromAccessTierShortName}).`;
      this.setState({ accessTierMismatch: errorMsg, destination: null });
    }

    validateAndSetDestination(destination: Workspace) {
      const { fromCdrVersionId, fromAccessTierShortName, resourceType } =
        this.props;

      this.clearCopyError();
      this.clearMismatchErrors(destination);

      const cdrVersionMismatch: boolean =
        fromCdrVersionId !== destination.cdrVersionId;
      const accessTierMismatch: boolean =
        fromAccessTierShortName !== destination.accessTierShortName;
      const isNotebook: boolean = resourceType === ResourceType.NOTEBOOK;
      const isConceptSet: boolean = resourceType === ResourceType.CONCEPT_SET;

      cond(
        [
          accessTierMismatch,
          () =>
            this.setAccessTierMismatchError(
              destination,
              fromAccessTierShortName
            ),
        ],
        [
          cdrVersionMismatch && isConceptSet,
          () =>
            this.setConceptSetCdrMismatchError(destination, fromCdrVersionId),
        ],
        // notebooks can be copied to different versions; warn only.
        [
          cdrVersionMismatch && isNotebook,
          () =>
            this.setNotebookCdrMismatchWarning(destination, fromCdrVersionId),
        ]
      );
    }

    showCdrMismatch(type: ResourceType) {
      const { resourceType } = this.props;
      const { accessTierMismatch, cdrMismatch } = this.state;
      return !accessTierMismatch && cdrMismatch && resourceType === type;
    }

    renderFormBody() {
      const { resourceType } = this.props;
      const {
        destination,
        workspaceOptions,
        requestState,
        accessTierMismatch,
        cdrMismatch,
        copyErrorMsg,
        newName,
      } = this.state;

      // Validate the input
      const errors = validateCopyModal({ newName }, resourceType);

      return (
        <div>
          <div style={headerStyles.formLabel}>Destination *</div>
          <Select
            value={destination}
            options={workspaceOptions}
            onChange={(destWorkspace) =>
              this.validateAndSetDestination(destWorkspace)
            }
            isOptionDisabled={(option) =>
              this.isDestinationSameWorkspace(option.value)
            }
          />
          {accessTierMismatch && (
            <AccessTierMismatch text={accessTierMismatch} />
          )}
          {this.showCdrMismatch(ResourceType.CONCEPT_SET) && (
            <ConceptSetCdrMismatch text={cdrMismatch} />
          )}
          <div style={headerStyles.formLabel}>Name *</div>
          <TextInput
            autoFocus
            value={newName}
            onChange={(v) => this.setState({ newName: v })}
          />
          <ValidationError>{summarizeErrors(errors?.newName)}</ValidationError>
          {this.showCdrMismatch(ResourceType.NOTEBOOK) && (
            <NotebookCdrMismatch text={cdrMismatch} />
          )}
          {requestState === RequestState.COPY_ERROR && (
            <ValidationError>{copyErrorMsg}</ValidationError>
          )}
        </div>
      );
    }

    renderSuccessBody() {
      const { fromResourceName, resourceType } = this.props;
      return (
        <div>
          {' '}
          Successfully copied
          <b style={styles.bold}> {fromResourceName} </b> to
          <b style={styles.bold}> {this.state.destination.name} </b>. Do you
          want to view the copied {toDisplay(resourceType)}?
        </div>
      );
    }
  }
);

export { CopyModal };
