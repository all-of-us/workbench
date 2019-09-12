import colors from 'app/styles/colors';
import * as React from 'react';

const styles = {
  paddedTable: {
    color: colors.primary, fontSize: 13, lineHeight: '24px',
    background: colors.white, padding: '0.2rem 0.5rem', display: 'flex'
  }
};

export const PaddedTableCell = ({left, content, leftWidth = '50%', rightWidth = '50%'}) => {
  return <div style={{padding: '.2rem', width: left ? leftWidth :
      rightWidth, display: 'flex'}}>
    <div style={{...styles.paddedTable, width: '100%'}}>{content}</div>
  </div>;
};

export const TwoColPaddedTable = ({style = {}, header = false, headerLeft = '',
  headerRight = '', cellWidth = {left: '50%', right: '50%'}, contentLeft, contentRight}) => {
  return <div style={{display: 'flex', flexDirection: 'column', ...style}}>
    {header &&
      <div style={{display: 'flex', flexDirection: 'row', height: '100%'}}>
        <PaddedTableCell left={true} leftWidth={cellWidth.left}
                         content={<strong>{headerLeft}</strong>}/>
        <PaddedTableCell left={false} rightWidth={cellWidth.right}
                         content={<strong>{headerRight}</strong>}/>
      </div>}
    {contentLeft.map((c, i) =>
      <div style={{display: 'flex', flexDirection: 'row'}}>
        <PaddedTableCell key={i + '_l'} left={true} content={c}
                         leftWidth={cellWidth.left}/>
        {contentRight.length >= i + 1 &&
        <PaddedTableCell key={i + '_r'} left={false} content={contentRight[i]}
                         rightWidth={cellWidth.right}/>}
      </div>
    )}
  </div>;
};
