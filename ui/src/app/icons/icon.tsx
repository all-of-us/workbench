
import colors, {colorWithWhiteness} from 'app/styles/colors';

import * as React from 'react';


const iconData = {
  times: {
    title: 'Cancel',
    path: <path d='M19.41,18l8.29-8.29a1,1,0,0,0-1.41-1.41L18,16.59,9.71,8.29A1,
      1,0,0,0,8.29,9.71L16.59,18,8.29,26.29a1,1,0,1,0,1.41,1.41L18,19.41l8.29,
      8.29a1,1,0,0,0,1.41-1.41Z'/>
  },
  check: {
    title: 'Save',
    path: <path d='M13.72,27.69,3.29,17.27a1,1,0,0,1,1.41-1.41l9,
    9L31.29,7.29a1,1,0,0,1,1.41,1.41Z'/>
  }
};

const defaultStyle = {
  height: 19,
  width: 19,
  marginLeft: '.5rem',
  fill: colors.accent,
  cursor: 'pointer'
};

const hoverStyle = {...defaultStyle, fill: colorWithWhiteness(colors.accent, 0.2)};

const disabledStyle = {...defaultStyle,
  fill: colorWithWhiteness(colors.dark, 0.6), cursor: 'not-allowed'};

export class IconComponent extends React.Component
    <{disabled: boolean, icon: string, style: object}, {style: object}> {

  constructor(props) {
    super(props);
    this.state = {
      style: {...defaultStyle, ...this.props.style}
    };
  }

  mouseOver(): void {
    this.setState({style: {...hoverStyle, ...this.props.style}});
  }

  mouseLeave(): void {
    this.setState({style: {...defaultStyle, ...this.props.style}});
  }

  render() {
    const icon = iconData[this.props.icon];
    return (
        <svg
            style={this.props.disabled ? {...this.state.style, ...disabledStyle} : this.state.style}
            version='1.1'
            viewBox='0 0 36 36'
            preserveAspectRatio='xMidYMid meet'
            xmlns='http://www.w3.org/2000/svg'
            xmlnsXlink='http://www.w3.org/1999/xlink'
            onMouseOver={() => this.mouseOver()}
            onMouseLeave={() => this.mouseLeave()}>
          <title>{icon.title}</title>
          {icon.path}
          <rect x='0' y='0'
                width={this.state.style['width']}
                height={this.state.style['height']}
                fillOpacity='0'/>
        </svg>
    );
  }
}

