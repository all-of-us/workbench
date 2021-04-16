import {Component, Input} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect, useState} from 'react';

import {Clickable} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import colors from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase,
  withCdrVersions,
  withCurrentWorkspace,
  withUrlParams
} from 'app/utils';
import {
  getCdrVersion,
  getDefaultCdrVersionForTier,
  hasDefaultCdrVersion
} from 'app/utils/cdr-versions';
import {NavStore} from 'app/utils/navigation';
import {serverConfigStore} from 'app/utils/navigation';
import {CdrVersionTiersResponse, Workspace} from 'generated/fetch';
import {CdrVersionUpgradeModal} from './cdr-version-upgrade-modal';

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
    width: 'calc(100% + 1.2rem)',
    marginLeft: '-0.6rem',
    paddingLeft: 80,
    borderBottom: `5px solid ${colors.accent}`,
    flex: 'none'
  },
  tab: {
    minWidth: 140,
    flexGrow: 0,
    padding: '0 20px',
    color: colors.white,
    alignSelf: 'stretch',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center'
  },
  active: {
    backgroundColor: 'rgba(255,255,255,0.15)',
    color: 'unset',
    borderBottom: `4px solid ${colors.accent}`,
    fontWeight: 'bold'
  },
  separator: {
    background: 'rgba(255,255,255,0.15)',
    width: 1,
    height: 48,
    flexShrink: 0
  },
  disabled: {
    color: colors.disabled
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
  }
};

const USER_DISMISSED_ALERT_VALUE = 'DISMISSED';

const CdrVersion = (props: {workspace: Workspace, cdrVersionTiersResponse: CdrVersionTiersResponse}) => {
  const {workspace, cdrVersionTiersResponse} = props;
  const {namespace, id} = workspace;

  const localStorageKey = `${namespace}-${id}-user-dismissed-cdr-version-update-alert`;

  const dismissedInLocalStorage = () => localStorage.getItem(localStorageKey) === USER_DISMISSED_ALERT_VALUE;

  const [userHasDismissedAlert, setUserHasDismissedAlert] = useState(dismissedInLocalStorage());
  const [showModal, setShowModal] = useState(false);

  // check whether the user has previously dismissed the alert in localStorage, to determine icon color
  useEffect(() =>
      setUserHasDismissedAlert(dismissedInLocalStorage())
  );

  const NewVersionFlag = () => <Clickable
      data-test-id='new-version-flag'
      propagateDataTestId={true}
      onClick={() => {
        localStorage.setItem(localStorageKey, USER_DISMISSED_ALERT_VALUE);
        setUserHasDismissedAlert(true);
        setShowModal(true);
      }}>
    <span style={stylesFunction.cdrVersionFlagCircle(!userHasDismissedAlert)}>
      <ClrIcon shape='flag' class='is-solid'/>
    </span>
  </Clickable>;

  return <FlexRow data-test-id='cdr-version' style={{textTransform: 'none'}}>
    {getCdrVersion(workspace, cdrVersionTiersResponse).name}
    {!hasDefaultCdrVersion(workspace, cdrVersionTiersResponse) && <NewVersionFlag/>}
    {showModal && <CdrVersionUpgradeModal
        defaultCdrVersionName={getDefaultCdrVersionForTier(workspace, cdrVersionTiersResponse).name}
        onClose={() => setShowModal(false)}
        upgrade={() => NavStore.navigate(['/workspaces', namespace, id, 'duplicate'])}
    />}
  </FlexRow>;
};

const tabs = [
  {name: 'Data', link: 'data'},
  {name: 'Analysis', link: 'notebooks'},
  {name: 'About', link: 'about'},
];

const navSeparator = <div style={styles.separator}/>;

function restrictTab(workspace, tab) {
  return serverConfigStore.getValue().enableResearchReviewPrompt && workspace && workspace.accessLevel === 'OWNER'
      && workspace.researchPurpose.needsReviewPrompt && tab.name !== 'About';
}

export const WorkspaceNavBarReact = fp.flow(
  withCurrentWorkspace(),
  withUrlParams(),
  withCdrVersions(),
)(props => {
  const {tabPath, workspace, urlParams: {ns: namespace, wsid: id}, cdrVersionTiersResponse} = props;
  const activeTabIndex = fp.findIndex(['link', tabPath], tabs);

  const navTab = (currentTab, disabled) => {
    const {name, link} = currentTab;
    const selected = tabPath === link;
    const hideSeparator = selected || (activeTabIndex === tabs.indexOf(currentTab) + 1);
    return <React.Fragment key={name}>
      <Clickable
        data-test-id={name}
        aria-selected={selected}
        disabled={disabled}
        style={{...styles.tab, ...(selected ? styles.active : {}), ...(disabled ? styles.disabled : {})}}
        hover={{color: styles.active.color}}
        onClick={() => NavStore.navigate(fp.compact(['/workspaces', namespace, id, link]))}
      >
        {name}
      </Clickable>
      {!hideSeparator && navSeparator}
    </React.Fragment>;
  };

  return <div id='workspace-top-nav-bar' className='do-not-print' style={styles.container}>
    {activeTabIndex > 0 && navSeparator}
    {fp.map(tab => navTab(tab, restrictTab(props.workspace, tab)), tabs)}
    <div style={{flexGrow: 1}}/>
    {workspace && <CdrVersion workspace={workspace} cdrVersionTiersResponse={cdrVersionTiersResponse}/>}
  </div>;
});

@Component({
  selector: 'app-workspace-nav-bar',
  template: '<div #root></div>',
})
export class WorkspaceNavBarComponent extends ReactWrapperBase {
  @Input() tabPath;

  constructor() {
    super(WorkspaceNavBarReact, ['tabPath']);
  }
}
