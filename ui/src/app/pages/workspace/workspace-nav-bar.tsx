import {Component, Input} from '@angular/core';

import {Button, Clickable} from 'app/components/buttons';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace, withUrlParams, withUserProfile} from 'app/utils';
import {navigate, NavStore} from 'app/utils/navigation';

import * as fp from 'lodash/fp';
import * as React from 'react';
import {StatusAlertBanner} from "app/components/status-alert-banner";
import {WorkspaceData} from "app/utils/workspace-data";
import {openZendeskWidget} from "app/utils/zendesk";
import {BillingStatus, Profile} from "generated/fetch";


const styles = reactStyles({
  container: {
    display: 'flex', alignItems: 'center', backgroundColor: colors.secondary,
    fontWeight: 500, color: 'white', textTransform: 'uppercase',
    height: 60, paddingRight: 16,
    boxShadow: 'inset rgba(0, 0, 0, 0.12) 0px 3px 2px 0px',
    width: 'calc(100% + 1.2rem)',
    marginLeft: '-0.6rem',
    paddingLeft: 80, borderBottom: `5px solid ${colors.accent}`, flex: 'none'
  },
  tab: {
    minWidth: 140, flexGrow: 0, padding: '0 20px',
    color: colors.white,
    alignSelf: 'stretch', display: 'flex', justifyContent: 'center', alignItems: 'center'
  },
  active: {
    backgroundColor: 'rgba(255,255,255,0.15)', color: 'unset',
    borderBottom: `4px solid ${colors.accent}`, fontWeight: 'bold'
  },
  separator: {
    background: 'rgba(255,255,255,0.15)', width: 1, height: 48, flexShrink: 0
  }
});

const tabs = [
  {name: 'Data', link: 'data'},
  {name: 'Analysis', link: 'notebooks'},
  {name: 'About', link: 'about'},
];

const navSeparator = <div style={styles.separator}/>;

interface Props {
  workspace: WorkspaceData;
  urlParams: any;
  tabPath: string;
  profileState: {
    profile: Profile
  }
}

interface State {
  showInvalidBillingBanner: boolean
}

export const WorkspaceNavBarReact = fp.flow(
  withCurrentWorkspace(),
  withUrlParams(),
  withUserProfile()
)(
  class extends React.Component<Props, State> {

    constructor(props) {
      super(props);
      this.state = {
        showInvalidBillingBanner: false
      }
    }

    componentDidUpdate(prevProps: Readonly<Props>, prevState: Readonly<State>, snapshot?: any): void {
      if (!prevProps.workspace && this.props.workspace && this.props.workspace.billingStatus === BillingStatus.INACTIVE) {
        this.setState({showInvalidBillingBanner: true});
      }
    }

    render() {
      const {tabPath, urlParams: {ns: namespace, wsid: id}} = this.props;
      const activeTabIndex = fp.findIndex(['link', tabPath], tabs);

      const navTab = (currentTab) => {
        const {name, link} = currentTab;
        const selected = tabPath === link;
        const hideSeparator = selected || (activeTabIndex === tabs.indexOf(currentTab) + 1);

        return <React.Fragment key={name}>
          {this.state.showInvalidBillingBanner &&
          <StatusAlertBanner
            title={'This workspace has run out of free credits'}
            message={'The free credits for the creator of this workspace have run out or expired. Please provide a valid billing account or contact support to extend free credits.'}
            footer={
              <div style={{display: 'flex', flexDirection: 'column'}}>
                <Button style={{height: '38px', width: '70%', fontWeight: 400}}
                        onClick={() => {
                          openZendeskWidget(
                            this.props.profileState.profile.givenName,
                            this.props.profileState.profile.familyName,
                            this.props.profileState.profile.username,
                            this.props.profileState.profile.contactEmail,
                          );
                        }}
                >
                  Request Extension
                </Button>
                <a style={{marginTop: '.5rem', marginLeft: '.2rem'}}
                   onClick={() => {
                     navigate(['workspaces', this.props.workspace.namespace, this.props.workspace.id, 'edit']);
                   }}
                >
                  Provide billing account
                </a>
              </div>
            }
            onClose={() => {this.setState({showInvalidBillingBanner: false})}}
          />
          }
          <Clickable
            data-test-id={name}
            aria-selected={selected}
            style={{...styles.tab, ...(selected ? styles.active : {})}}
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
        {fp.map(tab => navTab(tab), tabs)}
        <div style={{flexGrow: 1}}/>
      </div>;
    }
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
