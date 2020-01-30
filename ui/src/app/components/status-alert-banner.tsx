import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';

const styles = reactStyles({
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
    zIndex: 1,
  }
});



export interface Props {
  title: string;
  message: string;
  link: string;
  onClose: Function;
}

export class StatusAlertBanner extends React.Component<Props, {}> {
  constructor(props) {
    super(props);
  }

  navigateToLink(link) {
    window.open(link, '_blank');
  }

  renderBanner() {
    const {title, message, link} = this.props;
    return <FlexColumn style={styles.alertBanner}>
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
          onClick={() => this.props.onClose()}
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
    </FlexColumn>;
  }

  render() {
    return ReactDOM.createPortal(this.renderBanner(),
      document.getElementById('body'))
  }
}
