import * as React from 'react';
import ReactSwitch from 'react-switch';
import { Dropdown } from 'primereact/dropdown';
import validate from 'validate.js';

import { StatusAlertLocation } from 'generated/fetch';
const { BEFORE_LOGIN, AFTER_LOGIN } = StatusAlertLocation;

import { cond } from '@terra-ui-packages/core-utils';
import { BoldHeader, Header } from 'app/components/headers';
import { TextArea, TextInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { statusAlertApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { notTooLong, required } from 'app/utils/validators';

const styles = {
  smallHeaderStyles: {
    fontSize: 14,
    fontWeight: 600,
  },
};
const NO_CHANGES_WHILE_ENABLED =
  'The banner cannot be changed while it is enabled.';

interface AdminBannerState {
  bannerDescription: string;
  bannerEnabled: boolean;
  bannerHeadline: string;
  readMoreLink: string;
  alertLocation: StatusAlertLocation;
}
const validators = {
  bannerDescription: { ...required, ...notTooLong(4000) },
  bannerHeadline: { ...required, ...notTooLong(200) },
  readMoreLink: { ...notTooLong(200) },
};

const getHeadlineTooltip = (bannerEnabled: boolean, isBeforeLogin: boolean) =>
  cond(
    [bannerEnabled, () => NO_CHANGES_WHILE_ENABLED],
    [isBeforeLogin, () => 'Before Login banner has a fixed headline.'],
    ''
  );

export class AdminBanner extends React.Component<
  WithSpinnerOverlayProps,
  AdminBannerState
> {
  constructor(props) {
    super(props);
    this.state = {
      bannerDescription: '',
      bannerEnabled: false,
      bannerHeadline: '',
      readMoreLink: '',
      alertLocation: AFTER_LOGIN,
    };
  }

  componentDidMount(): void {
    this.props.hideSpinner();
    statusAlertApi()
      .getStatusAlert()
      .then((statusAlert) =>
        this.setState({
          bannerDescription: statusAlert.message,
          bannerEnabled: statusAlert.title !== null && statusAlert.title !== '',
          bannerHeadline: statusAlert.title,
          readMoreLink: statusAlert.link,
          alertLocation: statusAlert.alertLocation || AFTER_LOGIN,
        })
      );
  }

  handleBannerToggle(checked: boolean) {
    this.setState({ bannerEnabled: !this.state.bannerEnabled });
    if (checked) {
      statusAlertApi().postStatusAlert({
        title: this.state.bannerHeadline,
        message: this.state.bannerDescription,
        link: this.state.readMoreLink,
        alertLocation: this.state.alertLocation,
      });
    } else {
      statusAlertApi().postStatusAlert({
        title: '',
        message: '',
        link: '',
        alertLocation: AFTER_LOGIN,
      });
      this.setState({
        bannerDescription: '',
        bannerHeadline: '',
        readMoreLink: '',
        alertLocation: AFTER_LOGIN,
      });
    }
  }

  render() {
    const {
      bannerDescription,
      bannerEnabled,
      bannerHeadline,
      readMoreLink,
      alertLocation,
    } = this.state;
    const errors = validate(
      {
        bannerDescription,
        bannerHeadline,
        readMoreLink,
      },
      validators,
      {
        prettify: (v) =>
          ({
            bannerDescription: 'Banner Description',
            bannerHeadline: 'Banner Headline',
            readMoreLink: 'Read More Button',
          }[v] || validate.prettify(v)),
      }
    );
    const isBeforeLogin = alertLocation === BEFORE_LOGIN;
    return (
      <div style={{ width: '36rem', margin: '1.5rem' }}>
        <BoldHeader style={{ fontSize: 18 }}>Service Banners</BoldHeader>
        <Header style={{ ...styles.smallHeaderStyles, marginTop: '0.75rem' }}>
          Banner Headline
        </Header>
        <TooltipTrigger
          content={getHeadlineTooltip(bannerEnabled, isBeforeLogin)}
          side='right'
        >
          <div>
            <TextInput
              disabled={isBeforeLogin || bannerEnabled}
              onChange={(v) => this.setState({ bannerHeadline: v })}
              value={bannerHeadline}
              placeholder='Type headline text'
            />
          </div>
        </TooltipTrigger>
        <Header style={styles.smallHeaderStyles}>Banner Description</Header>
        <TooltipTrigger
          content={bannerEnabled && NO_CHANGES_WHILE_ENABLED}
          side='right'
        >
          <div>
            <TextArea
              disabled={bannerEnabled}
              value={bannerDescription}
              onChange={(v) => this.setState({ bannerDescription: v })}
              placeholder='Type descriptive banner text'
            />
          </div>
        </TooltipTrigger>
        <Header style={styles.smallHeaderStyles}>
          Read More Button/CTA{' '}
          <span style={{ fontWeight: 400, fontSize: 12 }}>(Optional)</span>
        </Header>
        <TooltipTrigger
          content={bannerEnabled && NO_CHANGES_WHILE_ENABLED}
          side='right'
        >
          <div>
            <TextInput
              disabled={bannerEnabled}
              onChange={(v) => this.setState({ readMoreLink: v })}
              value={readMoreLink}
              placeholder='Paste button link'
            />
          </div>
        </TooltipTrigger>
        <Header style={styles.smallHeaderStyles}>Banner Type</Header>
        <TooltipTrigger
          content={bannerEnabled && NO_CHANGES_WHILE_ENABLED}
          side='right'
        >
          <div style={{ display: 'inline-block' }}>
            <Dropdown
              disabled={bannerEnabled}
              value={alertLocation}
              options={[
                { value: BEFORE_LOGIN, label: 'Before Login' },
                { value: AFTER_LOGIN, label: 'After Login' },
              ]}
              // BEFORE_LOGIN has a fixed headline, so toggling should clear the headline
              // to help avoid confusion
              onChange={(e) =>
                this.setState({
                  alertLocation: e.value,
                  bannerHeadline:
                    e.value === BEFORE_LOGIN
                      ? 'Scheduled Downtime Notice for the Researcher Workbench'
                      : '',
                })
              }
            />
          </div>
        </TooltipTrigger>
        <TooltipTrigger
          content={
            !bannerEnabled &&
            !!errors &&
            'Headline and description are required.'
          }
          side='right'
        >
          <div
            style={{
              marginTop: '2.25rem',
              display: 'flex',
              alignItems: 'center',
              width: '4.5rem',
            }}
          >
            <ReactSwitch
              checked={bannerEnabled}
              disabled={!bannerEnabled && errors}
              checkedIcon={false}
              height={17}
              onChange={(checked) => this.handleBannerToggle(checked)}
              uncheckedIcon={false}
              width={34}
            />
            <div
              style={{
                textTransform: 'uppercase',
                color: colors.primary,
                marginLeft: 4,
              }}
            >
              {bannerEnabled ? 'On' : 'Off'}
            </div>
          </div>
        </TooltipTrigger>
      </div>
    );
  }
}
