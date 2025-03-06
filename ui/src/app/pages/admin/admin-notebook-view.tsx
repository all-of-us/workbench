import * as React from 'react';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router';

import { useQuery } from 'app/components/app-router';
import { StyledRouterLink } from 'app/components/buttons';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { MatchParams } from 'app/utils/stores';

const styles = reactStyles({
  heading: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: 500,
  },
  notebook: {
    width: '100%',
    height: 'calc(100% - 40px)',
    position: 'absolute',
    border: 0,
  },
  error: {
    marginLeft: 'auto',
    marginRight: 'auto',
    height: '40px',
    maxWidth: '550px',
    paddingLeft: '8px',
    fontFamily: 'Font Awesome 5 Pro',
    fontSize: '16px',
    letterSpacing: 0,
    lineHeight: '40px',
    boxSizing: 'border-box',
    color: colors.primary,
    borderColor: colors.warning,
    backgroundColor: colorWithWhiteness(colors.danger, 0.9),
    borderWidth: '1px',
    borderStyle: 'solid',
    borderRadius: '5px',
  },
});

interface HeaderProps {
  workspaceDisplayName: string;
  workspaceNamespace: string;
  notebookNameWithSuffix: string;
  accessReason: string;
}
const Header = ({
  workspaceDisplayName,
  workspaceNamespace,
  notebookNameWithSuffix,
  accessReason,
}: HeaderProps) => {
  const workspace = workspaceDisplayName
    ? `Workspace ${workspaceDisplayName}`
    : 'Workspace';
  const link = (
    <StyledRouterLink path={`/admin/workspaces/${workspaceNamespace}`}>
      {workspace} with namespace {workspaceNamespace}
    </StyledRouterLink>
  );
  return (
    <div style={styles.heading}>
      Viewing {notebookNameWithSuffix} in {link} for reason:
      <div>{accessReason}</div>
    </div>
  );
};

interface MainProps {
  notebookHtml: string;
  errorMessage: string;
}
const Main = ({ notebookHtml, errorMessage }: MainProps) => {
  if (notebookHtml) {
    return (
      <iframe
        id='notebook-frame'
        style={styles.notebook}
        srcDoc={notebookHtml}
      />
    );
  } else if (errorMessage) {
    return <div style={styles.error}>{errorMessage}</div>;
  } else {
    return <SpinnerOverlay />;
  }
};

interface Props {
  workspaceNamespace: string;
  notebookNameWithSuffix: string;
  accessReason: string;
}
const AdminNotebookViewComponent = (props: Props) => {
  const { workspaceNamespace, notebookNameWithSuffix, accessReason } = props;
  const [notebookHtml, setNotebookHtml] = useState('');
  const [workspaceDisplayName, setWorkspaceDisplayName] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    workspaceAdminApi()
      .getWorkspaceAdminView(workspaceNamespace)
      .then((workspaceAdminView) =>
        setWorkspaceDisplayName(workspaceAdminView.workspace.name)
      );
  }, []);

  useEffect(() => {
    if (!accessReason?.trim()) {
      setErrorMessage(
        'Error: must include accessReason query parameter in URL'
      );
      return;
    }

    workspaceAdminApi()
      .adminReadOnlyNotebook(workspaceNamespace, notebookNameWithSuffix, {
        reason: accessReason,
      })
      .then((response) => setNotebookHtml(response.html))
      .catch((e) => {
        if (e.status === 404) {
          setErrorMessage(`Notebook ${notebookNameWithSuffix} was not found`);
        } else if (e.status === 412) {
          setErrorMessage('Notebook is too large to display in preview mode');
        } else {
          setErrorMessage(
            'Failed to render notebook preview due to unknown error'
          );
        }
      });
  }, []);

  return (
    <React.Fragment>
      <Header
        {...{
          workspaceDisplayName,
          workspaceNamespace,
          notebookNameWithSuffix,
          accessReason,
        }}
      />
      <Main {...{ notebookHtml, errorMessage }} />
    </React.Fragment>
  );
};

const AdminNotebookView = (spinnerProps: WithSpinnerOverlayProps) => {
  useEffect(() => spinnerProps.hideSpinner(), []);

  const { ns, nbName } = useParams<MatchParams>();
  const accessReason = useQuery().get('accessReason');

  // react-router does not handle decoding of URL parameters, they must be decoded here.
  return (
    <AdminNotebookViewComponent
      workspaceNamespace={ns}
      notebookNameWithSuffix={decodeURIComponent(nbName)}
      accessReason={accessReason}
    />
  );
};

export { AdminNotebookView };
