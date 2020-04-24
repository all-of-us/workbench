import * as React from 'react';
import {reactStyles} from "../utils";

export const CustomBulletList = ({children}) => {
  return <ul style={{position: 'relative', listStyle: 'none', marginLeft: 0}}>{children}</ul>;
};
export const CustomBulletListItem = ({bullet, children}) => {
  return <React.Fragment>
    <div style={{position: 'absolute', left: '-.2em'}}> {bullet} </div>
    <li style={{marginLeft: '1.2em'}}> {children} </li>
  </React.Fragment>;
};

const styles = reactStyles({
  bulletAlignedList: {
    listStylePosition: 'outside',
    marginLeft: '1rem',
  }
});

export const BulletAlignedUnorderedList = ({style = {}, children}) => {
  return <ul style={{...styles.bulletAlignedList, ...style}}>{children}</ul>;
};
