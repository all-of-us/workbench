import * as React from 'react';

export const buttonStyles = {
  btn: {
   fontWeight: 500,
   fontSize: '12px'
  } as React.CSSProperties,
  primary: {
    backgroundColor: '#262262',
    border: 'none',
    padding: '0rem 0.77rem',
    borderRadius: '0.3rem',
    cursor: 'hand',
    textTransform: 'uppercase',
    color: '#fff',
    letterSpacing: '0.02rem',
    lineHeight: '0.77rem'
  } as React.CSSProperties
};

interface ButtonProps {
  onClick: Function;
  text: string;
  styles: Object;
}

export class PrimaryButton extends React.Component<ButtonProps, {}> {
  props: ButtonProps;

  constructor(props: ButtonProps) {
    super(props);
  }

  render() {
    return <button style={{...buttonStyles.btn, ...buttonStyles.primary, ...this.props.styles}}
                   type='button'
                   onClick={() => this.props.onClick()}>{this.props.text}</button>;
  }
}

export default PrimaryButton;
