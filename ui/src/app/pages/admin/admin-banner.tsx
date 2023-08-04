import * as React from 'react';
import ReactSwitch from 'react-switch';
import validate from 'validate.js';

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

interface AdminBannerState {
  bannerDescription: string;
  bannerEnabled: boolean;
  bannerHeadline: string;
  readMoreLink: string;
}
const validators = {
  bannerDescription: { ...required, ...notTooLong(4000) },
  bannerHeadline: { ...required, ...notTooLong(200) },
  readMoreLink: { ...notTooLong(200) },
};
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
      });
    } else {
      statusAlertApi().postStatusAlert({
        title: '',
        message: '',
        link: '',
      });
      this.setState({
        bannerDescription: '',
        bannerHeadline: '',
        readMoreLink: '',
      });
    }
  }

  render() {
    const { bannerDescription, bannerEnabled, bannerHeadline, readMoreLink } =
      this.state;
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
    return (
      <div style={{ width: '36rem', margin: '1.5rem' }}>
        <BoldHeader style={{ fontSize: 18 }}>Service Banners</BoldHeader>
        <Header style={{ ...styles.smallHeaderStyles, marginTop: '0.75rem' }}>
          Banner Headline
        </Header>
        <TextInput
          onChange={(v) => this.setState({ bannerHeadline: v })}
          value={bannerHeadline}
          data-test-id='banner-headline-input'
          placeholder='Type headline text'
        />
        <Header style={styles.smallHeaderStyles}>Banner Description</Header>
        <TextArea
          value={bannerDescription}
          onChange={(v) => this.setState({ bannerDescription: v })}
          data-test-id='banner-description-input'
          placeholder='Type descriptive banner text'
        />
        <Header style={styles.smallHeaderStyles}>
          Read More Button/CTA{' '}
          <span style={{ fontWeight: 400, fontSize: 12 }}>(Optional)</span>
        </Header>
        <TextInput
          onChange={(v) => this.setState({ readMoreLink: v })}
          value={readMoreLink}
          data-test-id='read-more-link-input'
          placeholder='Paste button link'
        />
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
