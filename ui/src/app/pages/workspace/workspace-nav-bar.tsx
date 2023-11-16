import * as React from 'react';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { CdrVersionTiersResponse, Workspace } from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { analysisTabName } from 'app/routing/utils';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCdrVersions, withCurrentWorkspace } from 'app/utils';
import {
  getCdrVersion,
  getDefaultCdrVersionForTier,
  hasDefaultCdrVersion,
} from 'app/utils/cdr-versions';
import { useNavigation } from 'app/utils/navigation';
import { MatchParams, serverConfigStore } from 'app/utils/stores';

import { CdrVersionUpgradeModal } from './cdr-version-upgrade-modal';

const styles = reactStyles({
  container: {
    display: 'flex',
    alignItems: 'center',
    backgroundColor: colors.secondary,
    fontWeight: 500,
    color: 'white',
    textTransform: 'uppercase',
    height: 60,
    paddingRight: 16,
    boxShadow: 'inset rgba(0, 0, 0, 0.12) 0px 3px 2px 0px',
    width: 'calc(100% + 1.8rem)',
    marginLeft: '-0.9rem',
    paddingLeft: 80,
    borderBottom: `5px solid ${colors.accent}`,
    flex: 'none',
  },
  tab: {
    minWidth: 140,
    flexGrow: 0,
    padding: '0 20px',
    color: colors.white,
    alignSelf: 'stretch',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
  },
  active: {
    backgroundColor: 'rgba(255,255,255,0.15)',
    color: 'unset',
    borderBottom: `4px solid ${colors.accent}`,
    fontWeight: 'bold',
  },
  separator: {
    background: 'rgba(255,255,255,0.15)',
    width: 1,
    height: 48,
    flexShrink: 0,
  },
  disabled: {
    color: colors.disabled,
  },
});

const stylesFunction = {
  cdrVersionFlagCircle: (alert: boolean): React.CSSProperties => {
    return {
      border: 'solid 1px',
      borderRadius: '50%',
      height: '50px',
      width: '50px',
      marginLeft: '12px',
      padding: '4px',
      backgroundColor: alert ? colors.danger : colors.secondary,
    };
  },
};

const USER_DISMISSED_ALERT_VALUE = 'DISMISSED';

const CdrVersion = (props: {
  workspace: Workspace;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
}) => {
  const { workspace, cdrVersionTiersResponse } = props;
  const { namespace, id } = workspace;

  const localStorageKey = `${namespace}-${id}-user-dismissed-cdr-version-update-alert`;

  const dismissedInLocalStorage = () =>
    localStorage.getItem(localStorageKey) === USER_DISMISSED_ALERT_VALUE;

  const [userHasDismissedAlert, setUserHasDismissedAlert] = useState(
    dismissedInLocalStorage()
  );
  const [showModal, setShowModal] = useState(false);
  const [navigate] = useNavigation();

  // check whether the user has previously dismissed the alert in localStorage, to determine icon color
  useEffect(() => setUserHasDismissedAlert(dismissedInLocalStorage()));

  const NewVersionFlag = () => (
    <Clickable
      data-test-id='new-version-flag'
      propagateDataTestId={true}
      onClick={() => {
        localStorage.setItem(localStorageKey, USER_DISMISSED_ALERT_VALUE);
        setUserHasDismissedAlert(true);
        setShowModal(true);
      }}
    >
      <span style={stylesFunction.cdrVersionFlagCircle(!userHasDismissedAlert)}>
        <ClrIcon shape='flag' class='is-solid' />
      </span>
    </Clickable>
  );

  return (
    <FlexRow data-test-id='cdr-version' style={{ textTransform: 'none' }}>
      {getCdrVersion(workspace, cdrVersionTiersResponse).name}
      {!hasDefaultCdrVersion(workspace, cdrVersionTiersResponse) && (
        <NewVersionFlag />
      )}
      {showModal && (
        <CdrVersionUpgradeModal
          defaultCdrVersionName={
            getDefaultCdrVersionForTier(
              workspace.accessTierShortName,
              cdrVersionTiersResponse
            ).name
          }
          onClose={() => setShowModal(false)}
          upgrade={() => navigate(['workspaces', namespace, id, 'duplicate'])}
        />
      )}
    </FlexRow>
  );
};

interface Tab {
  name: string;
  link: string;
}
const tabs: Tab[] = [
  { name: 'Data', link: 'data' },
  {
    name: 'Analysis',
    link: analysisTabName,
  },
  { name: 'About', link: 'about' },
];

const navSeparator = <div style={styles.separator} />;

const restrictTab = (workspace: Workspace, tab: Tab) =>
  workspace?.adminLocked && tab.name !== 'About';

export const WorkspaceNavBar = fp.flow(
  withCurrentWorkspace(),
  withCdrVersions()
)((props) => {
  const { tabPath, workspace, cdrVersionTiersResponse } = props;
  // default to Data tab if the tabPath is not set
  const activeTabIndex = fp.findIndex(['link', tabPath ?? 'data'], tabs);
  const [navigate] = useNavigation();
  const { ns, wsid } = useParams<MatchParams>();

  useEffect(() => {
    if (
      serverConfigStore.get().config.enableDataExplorer &&
      !tabs.find((tab) => tab.name === 'Data Explorer')
    ) {
      tabs.push({ name: 'Data Explorer', link: 'data-explorer' });
    }
  }, []);

  const experimentalTabStyle = (selected) => {
    return selected
      ? { backgroundColor: colorWithWhiteness(colors.danger, 0.3) }
      : { backgroundColor: colors.danger };
  };

  const navTab = (currentTab, disabled) => {
    const { name, link } = currentTab;
    const currentTabIndex = tabs.indexOf(currentTab);
    const selected = activeTabIndex === currentTabIndex;
    const hideSeparator = selected || activeTabIndex === currentTabIndex + 1;

    return (
      <React.Fragment key={name}>
        <Clickable
          data-test-id={name}
          aria-label={name}
          aria-selected={selected}
          disabled={disabled}
          style={{
            ...styles.tab,
            ...(selected ? styles.active : {}),
            ...(disabled ? styles.disabled : {}),
            ...(['data-explorer', 'tanagra'].includes(link)
              ? experimentalTabStyle(selected)
              : {}),
          }}
          onClick={() => navigate(['workspaces', ns, wsid, link])}
        >
          {name}
        </Clickable>
        {!hideSeparator && navSeparator}
      </React.Fragment>
    );
  };

  return (
    <div
      id='workspace-top-nav-bar'
      className='do-not-print'
      style={styles.container}
    >
      {activeTabIndex > 0 && navSeparator}
      {fp.map((tab) => navTab(tab, restrictTab(props.workspace, tab)), tabs)}
      <div style={{ flexGrow: 1 }} />
      {workspace && (
        <CdrVersion
          workspace={workspace}
          cdrVersionTiersResponse={cdrVersionTiersResponse}
        />
      )}
    </div>
  );
});
