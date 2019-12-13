import * as React from "react";
import colors, {colorWithWhiteness} from "app/styles/colors";
import {statusAlertApi} from "app/services/swagger-fetch-clients";
import {FlexColumn, FlexRow} from "app/components/flex";
import {ClrIcon} from "app/components/icons";
import {Button} from "app/components/buttons";
import {cookiesEnabled} from 'app/utils';

const styles = {
  alertBanner: {
    backgroundColor: colorWithWhiteness(colors.highlight, .5),
    color: colors.primary,
    marginRight: '1rem',
    marginTop: '1rem',
    padding: '0.5rem',
    width: '300px',
    borderRadius: '0.5em',
    position: 'absolute',
    top: '0',
    right: '0',
    zIndex: '1',
  }
};

const cookieKey = "status-alert-banner-dismissed";

export interface State {
  statusAlertId: number;
  title: string;
  message: string;
  link: string;
  isVisible: boolean;
}

export class StatusAlertBanner extends React.Component<{}, State> {
  constructor(props) {
    super(props);
    this.state = {
      statusAlertId: 0,
      title: '',
      message: '',
      link: '',
      isVisible: false
    }
  }

  async componentDidMount() {
    const statusAlerts = await statusAlertApi().getStatusAlert();
    if (statusAlerts.length > 0) {
      // There should be either one or zero alerts.
      const statusAlert = statusAlerts[0];
      this.setState({
        statusAlertId: statusAlert.statusAlertId,
        title: statusAlert.title,
        message: statusAlert.message,
        link: statusAlert.link
      });
      // This needs to happen separately so that the other state is already set in isVisible()
      this.setState({
        isVisible: this.isVisible()
      });
    }
  }

  getStyles(isVisible) {
    if (isVisible) {
      return {...styles.alertBanner, height: '200px'}
    }
    else {
      return {...styles.alertBanner, height: '0px'}
    }
  }

  navigateToLink(link) {
    window.open(link, '_blank');
  }

  dismiss() {
    if(cookiesEnabled()) {
      localStorage.setItem(cookieKey, `${this.state.statusAlertId}`)
    }
    this.setState({isVisible: false})
  }

  isVisible() {
    debugger;
    if(cookiesEnabled()) {
      const cookie = localStorage.getItem(cookieKey);
      return (cookie && cookie !== `${this.state.statusAlertId}`) || !!this.state.message
    }
    else {
      return !!this.state.message
    }
  }

  render() {
    const {title, message, link, isVisible} = this.state;
    return isVisible && <FlexColumn style={this.getStyles(isVisible)}>
      <FlexRow style={{width: '100%'}}>
        <ClrIcon
            shape={'warning-standard'}
            class={'is-solid'}
            size={20}
            style={{
              color: colors.warning,
              flex: '0 0 auto'
            }}
        />
        <div style={{
          fontWeight: 'bold',
          marginLeft: '.2rem',
          // These next two styles fake the appearance of only having spacing below the line
          // whereas line-height is automatically distributed both above and below
          position: 'relative',
          top: '-0.2rem'
        }}>{title}</div>
        <ClrIcon
            shape={'times'}
            size={20}
            style={{marginLeft: 'auto', flex: '0 0 auto'}}
            onClick={() => this.dismiss()}
        />
      </FlexRow>
      <div>{message}</div>
      {
        link && <Button
            style={{marginTop: 'auto', width: '125px'}}
            onClick={() => this.navigateToLink(link)}
            data-test-id='status-banner-read-more-button'
        >
          READ MORE
        </Button>
      }
    </FlexColumn>
  }
}