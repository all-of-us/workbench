import {Component} from '@angular/core';
import {BoldHeader, Header} from 'app/components/headers';
import {TextArea, TextInput} from 'app/components/inputs';
import {statusAlertApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {ReactWrapperBase} from 'app/utils';
import {StatusAlert} from 'generated/fetch';
import * as React from 'react';
import ReactSwitch from 'react-switch';

const styles = {
  smallHeaderStyles: {
    fontSize: 14,
    fontWeight: 600
  }
};


interface AdminBannerState {
  bannerDescription: string;
  bannerEnabled: boolean;
  bannerHeadline: string;
  readMoreLink: string;
}

export class AdminBanner extends React.Component<{}, AdminBannerState> {
  constructor(props) {
    super(props);
    this.state = {
      bannerDescription: '',
      bannerEnabled: false,
      bannerHeadline: '',
      readMoreLink: ''
    }
  }

  componentDidMount(): void {
    statusAlertApi().getStatusAlert()
      .then(statusAlert => this.setState({
        bannerDescription: statusAlert.message,
        bannerEnabled: statusAlert.title !== null && statusAlert.title !== '',
        bannerHeadline: statusAlert.title,
        readMoreLink: statusAlert.link
      }));
  }

  handleBannerToggle(checked: boolean) {
    this.setState({bannerEnabled: !this.state.bannerEnabled});
    if (checked) {
      statusAlertApi().postStatusAlert({
        title: this.state.bannerHeadline,
        message: this.state.bannerDescription,
        link: this.state.readMoreLink
      });
    } else {
      statusAlertApi().postStatusAlert({
        title: '',
        message: '',
        link: ''
      });
      this.setState({
        bannerDescription: '',
        bannerHeadline: '',
        readMoreLink: ''
      })
    }
  }

  render() {
    return <div style={{width: '24rem'}}>
      <BoldHeader style={{fontSize: 18}}>Service Banners</BoldHeader>
      <Header style={{...styles.smallHeaderStyles, marginTop: '0.5rem'}}>Banner Headline</Header>
      <TextInput onChange={(v) => this.setState({bannerHeadline: v})}
                 value={this.state.bannerHeadline} data-test-id='banner-headline-input'
                 placeholder='Type headline text'/>
      <Header style={styles.smallHeaderStyles}>Banner Description</Header>
      <TextArea value={this.state.bannerDescription}
                onChange={v => this.setState({bannerDescription: v})}
                data-test-id='banner-description-input'
                placeholder='Type descriptive banner text'/>
      <Header style={styles.smallHeaderStyles}>Read More Button/CTA <span style={{fontWeight: 400,
        fontSize: 12}}>(Optional)</span></Header>
      <TextInput onChange={(v) => this.setState({readMoreLink: v})}
                 value={this.state.readMoreLink} data-test-id='read-more-link-input'
                 placeholder='Paste button link'/>
      <div style={{marginTop: '1.5rem', display: 'flex', alignItems: 'center'}}>
        <ReactSwitch checked={this.state.bannerEnabled}
                     checkedIcon={false}
                     height={17}
                     onChange={(checked) => this.handleBannerToggle(checked)}
                     uncheckedIcon={false}
                     width={34}/>
        <div style={{textTransform: 'uppercase', color: colors.primary, marginLeft: 4}}>
          {this.state.bannerEnabled ? 'On' : 'Off'}
        </div>
      </div>
    </div>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class AdminBannerComponent extends ReactWrapperBase {
  constructor() {
    super(AdminBanner, []);
  }
}
