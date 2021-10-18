import * as fp from 'lodash/fp';
import * as React from 'react';
import Iframe from 'react-iframe';

import {NavigationProps} from 'app/utils/navigation';
import {fetchAbortableRetry} from 'app/utils/retry';
import {MatchParams, RuntimeStore} from 'app/utils/stores';

import {useEffect, useState} from 'react';
import {Button} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {Spinner} from 'app/components/spinners';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {NotebookIcon} from 'app/icons/notebook-icon';
import {ReminderIcon} from 'app/icons/reminder';
import {jupyterApi, proxyApi} from 'app/services/notebooks-swagger-fetch-clients';
import {runtimeApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
    reactStyles,
    withCurrentWorkspace,
    withUserProfile
} from 'app/utils';
import {Kernels} from 'app/utils/notebook-kernels';
import {maybeInitializeRuntime, withRuntimeStore} from 'app/utils/runtime-utils';
import {withNavigation} from 'app/utils/with-navigation-hoc';
import {WorkspaceData} from 'app/utils/workspace-data';
import {environment} from 'environments/environment';
import {Profile, Runtime, RuntimeStatus} from 'generated/fetch';
import {RouteComponentProps, withRouter} from 'react-router-dom';
import {appendNotebookFileSuffix, dropNotebookFileSuffix} from './util';
import {parseQueryParams} from "app/components/app-router";

export const TerminalRedirect = fp.flow(withNavigation,
    withRouter)((spinnerProps: WithSpinnerOverlayProps) {
    const [isLoaded, setIsLoaded] = useState(false);

    if (isLoaded) {
        this.props.navigate([
            'workspaces', workspace.namespace, workspace.id,
            // navigate will encode the notebook name automatically
            'notebooks', 'preview', this.getFullNotebookName()
        ]);
    }
})
