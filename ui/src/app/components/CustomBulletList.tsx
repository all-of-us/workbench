import * as React from 'react';

export const CustomBulletList = ({children}) => {
  return <ul style={{position: 'relative', listStyle: 'none', marginLeft: 0, paddingLeft: '1.2em'}}>{children}</ul>;
};

export const CustomBulletListItem = ({children}) => {
  return <React.Fragment>
    <div style={{position: 'absolute', left: -2}}> → </div> <li> {children} </li>
  </React.Fragment>;
};
