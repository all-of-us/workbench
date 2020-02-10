import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';

const styles = reactStyles({
  alertBanner: {
    backgroundColor: colorWithWhiteness(colors.highlight, .5),
    color: colors.primary,
    fontSize: '12px',
    marginRight: '1rem',
    marginTop: '1rem',
    padding: '0.5rem',
    width: '300px',
    borderRadius: '0.5em',
    position: 'absolute',
    top: '0',
    right: '0',
    zIndex: 101,
  }
});

export interface Props {
  title: string;
  message: string;
  footer: JSX.Element;
  onClose: Function;
}

export class StatusAlertBanner extends React.Component<Props, {}> {
  constructor(props) {
    super(props);
  }

  renderBanner() {
    const {title, message} = this.props;
    return <FlexColumn style={styles.alertBanner}>
      <FlexRow style={{alignItems: 'center', marginTop: '.1rem'}}>
        <ClrIcon
          shape={'warning-standard'}
          class={'is-solid'}
          size={26}
          style={{
            color: colors.warning,
            flex: '0 0 auto'
          }}
        />
        <div style={{
          fontWeight: 600,
          width: '80%',
          marginLeft: '.4rem',
          lineHeight: '18px'
          // These next two styles fake the appearance of only having spacing below the line
          // whereas line-height is automatically distributed both above and below
        }}>{title}</div>
      </FlexRow>
      <div style={{
        lineHeight: '20px',
        marginTop: '.3rem',
        paddingLeft: '.2rem',
        paddingRight: '.2rem'}
      }>
        {message}
      </div>
      <div style={{marginTop: '.5rem'}}>
        {this.props.footer}
      </div>
      <ClrIcon
        shape={'times'}
        size={20}
        style={{
          position: 'absolute',
          top: '.3rem',
          right: '.3rem',
          colors: colors.accent
        }}
        onClick={() => this.props.onClose()}
      />
    </FlexColumn>;
  }

  render() {
    return ReactDOM.createPortal(this.renderBanner(),
      document.getElementsByTagName('body')[0]);
  }
}
