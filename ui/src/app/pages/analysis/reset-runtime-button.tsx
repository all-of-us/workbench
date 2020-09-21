import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {runtimeApi} from 'app/services/swagger-fetch-clients';
import {reportError} from 'app/utils/errors';
import {
  LeoRuntimeInitializationAbortedError,
  LeoRuntimeInitializationFailedError,
  LeoRuntimeInitializer,
} from 'app/utils/leo-runtime-initializer';
import {
  RuntimeStatus,
} from 'generated/fetch/api';

const RESTART_LABEL = 'Reset server';
const CREATE_LABEL = 'Create server';

const styles = {
  notebookSettings: {
    marginTop: '1rem'
  },
};

export interface Props {
  workspaceNamespace: string;
}

interface State {
  runtimeStatus?: RuntimeStatus;
  isPollingRuntime: boolean;
  resetRuntimePending: boolean;
  resetRuntimeModal: boolean;
  resetRuntimeFailure: boolean;
}

export class ResetRuntimeButton extends React.Component<Props, State> {
  private pollAborter = new AbortController();

  constructor(props) {
    super(props);

    this.state = {
      runtimeStatus: null,
      isPollingRuntime: true,
      resetRuntimePending: false,
      resetRuntimeModal: false,
      resetRuntimeFailure: true,
    };
  }

  componentDidMount() {
    this.createRuntimeInitializer(false);
  }

  async createRuntimeInitializer(allowRuntimeActions: boolean) {
    const maxActionCount = allowRuntimeActions ? 1 : 0;

    // Kick off an initializer which will poll for runtime status.
    try {
      this.setState({isPollingRuntime: true, runtimeStatus: null});
      await LeoRuntimeInitializer.initialize({
        workspaceNamespace: this.props.workspaceNamespace,
        onStatusUpdate: (runtimeStatus: RuntimeStatus) => {
          if (this.pollAborter.signal.aborted) {
            // IF we've been unmounted, don't try to update state.
            return;
          }
          this.setState({
            runtimeStatus: runtimeStatus,
          });
        },
        pollAbortSignal: this.pollAborter.signal,
        // For the reset button, we never want to affect the runtime state. With the maxFooCount set
        // to zero, the initializer will reject the promise when it reaches a non-transitional state.
        maxDeleteCount: maxActionCount,
        maxCreateCount: maxActionCount,
        maxResumeCount: maxActionCount,
      });
      this.setState({isPollingRuntime: false});
    } catch (e) {
      if (e instanceof LeoRuntimeInitializationAbortedError) {
        // Silently return if the init was aborted -- we've likely been unmounted and cannot call
        // setState anymore.
        return;
      } else if (e instanceof LeoRuntimeInitializationFailedError) {
        this.setState({runtimeStatus: e.runtime ? e.runtime.status : null, isPollingRuntime: false});
      } else {
        // We only expect one of the above errors, so report any other types of errors to
        // Stackdriver.
        reportError(e);
        this.setState({
          isPollingRuntime: false
        });
      }
    }
  }

  componentWillUnmount() {
    this.pollAborter.abort();
  }

  private createTooltip(content: React.ReactFragment, children: React.ReactFragment): React.ReactFragment {
    return <TooltipTrigger content={content} side='right'>
      {children}
    </TooltipTrigger>;
  }

  private createButton(label: string, enabled: boolean, callback: () => void): React.ReactFragment {
    return <Button disabled={!enabled}
                 onClick={callback}
                 data-test-id='reset-notebook-button'
                 type='secondary'>
    {label}
    </Button>;
  }

  createButtonAndLabel(): (React.ReactFragment) {
    if (this.state.isPollingRuntime) {
      const tooltipContent = <div>
        Your notebook server is still being provisioned. <br/>
        {this.state.runtimeStatus != null &&
          <span>(detailed status: {this.state.runtimeStatus})</span>
        }
      </div>;
      return this.createTooltip(
        tooltipContent,
        this.createButton(RESTART_LABEL, false, null));
    } else if (this.state.runtimeStatus === null) {
      // If the initializer has completed and the status is null, it means that
      // a runtime doesn't exist for this workspace.
      return this.createTooltip(
        'You do not currently have an active notebook server for this workspace.',
        this.createButton(CREATE_LABEL, true, () => this.createOrResetRuntime()));
    } else {
      // We usually reach this state if the runtime is at a "terminal" status and the initializer has
      // completed. This may be RuntimeStatus.Stopped, RuntimeStatus.Running, RuntimeStatus.Error,
      // etc.
      const tooltipContent = <div>
        Your notebook server is in the following state: {this.state.runtimeStatus}.
      </div>;
      return this.createTooltip(
        tooltipContent,
        this.createButton(RESTART_LABEL, true, () => this.openResetRuntimeModal()));
    }
  }

  render() {
    return <React.Fragment>
      <div style={styles.notebookSettings}>
        {this.createButtonAndLabel()}
      </div>
      {this.state.resetRuntimeModal &&
      <Modal data-test-id='reset-notebook-modal'
             loading={this.state.resetRuntimePending}>
        <ModalTitle>Reset Notebook Server?</ModalTitle>
        <ModalBody>
            <div>
              <strong>Warning:</strong> Any unsaved changes to your notebooks may be lost
              and your server will be offline for 5-10 minutes.
              <br/><br/>
              Resetting should not be necessary under normal conditions. Please help us to
              improve this experience by using "Contact Support" from the left side's hamburger
              menu and describe the reason for this reset.
            </div>
        </ModalBody>
        <ModalFooter>
          {this.state.resetRuntimeFailure ?
            <div className='error'>Could not reset your notebook server.</div> : undefined}
          <Button type='secondary'
                  onClick={() => this.setState({resetRuntimeModal: false})}
                  data-test-id='cancel-button'>Cancel</Button>
          <Button disabled={this.state.resetRuntimePending}
                  onClick={() => this.createOrResetRuntime()}
                  style={{marginLeft: '0.5rem'}}
                  data-test-id='reset-runtime-send'>Reset</Button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;
  }

  openResetRuntimeModal(): void {
    this.setState({
      resetRuntimePending: false,
      resetRuntimeModal: true,
      resetRuntimeFailure: false
    });
  }

  async createOrResetRuntime(): Promise<void> {
    try {
      this.setState({resetRuntimePending: true});
      if (this.state.runtimeStatus === null) {
        await runtimeApi().createRuntime(this.props.workspaceNamespace);
      } else {
        await runtimeApi().deleteRuntime(this.props.workspaceNamespace);
      }
      this.setState({resetRuntimePending: false, resetRuntimeModal: false});

      this.createRuntimeInitializer(true);

    } catch {
      this.setState({resetRuntimePending: false, resetRuntimeFailure: true});
    }
  }
}
