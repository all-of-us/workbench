import colors from 'app/styles/colors';
import {nextSort} from 'app/utils';
import * as fp from 'lodash/fp';
import * as React from 'react';
import Pagination from 'react-paginating';
import {ClrIcon} from './icons';

const rowHeight = '1.5rem';

const styles = {
  flexCell: ({basis = 0, grow = 1, shrink = 1, min: minWidth = 0, max} = {} as any) => ({
    flexGrow: grow,
    flexShrink: shrink,
    flexBasis: basis,
    minWidth,
    maxWidth: max
  }),
  headerRow: {
    display: 'flex', height: rowHeight,
    borderBottom: '1px solid #ccc', backgroundColor: '#fafafa'
  },
  bodyRow: {
    display: 'flex', height: rowHeight,
    borderTop: '1px solid #ddd', backgroundColor: 'white'
  },
  cell: {
    display: 'flex', alignItems: 'center',
    paddingLeft: '0.5rem', paddingRight: '0.5rem'
  },
  paginationRow: {
    display: 'flex', alignItems: 'center', height: rowHeight,
    border: '1px solid #ccc', backgroundColor: '#fafafa', fontSize: 13
  },
  paddedTable: {
    color: colors.primary, fontSize: 13, lineHeight: '24px',
    background: colors.white, padding: '0.2rem 0.5rem', display: 'flex'
  }
};

export const SimpleTable = ({columns, rowCount, rowProps}) => {
  return <div style={{border: '1px solid #ccc'}}>
    <div style={styles.headerRow}>
      {fp.map(([i, {headerRenderer, size}]: any) => {
        return <div key={i} style={{...styles.cell, ...styles.flexCell(size)}}>
          {headerRenderer()}
        </div>;
      }, fp.toPairs(columns))}
    </div>
    {fp.map(rowIndex => {
      const {style = {}, ...props} = rowProps ? rowProps({rowIndex}) : {};
      return <div key={rowIndex} style={{...styles.bodyRow, ...style}} {...props}>
        {fp.map(([i, {cellRenderer, size}]: any) => {
          return <div key={i} style={{...styles.cell, ...styles.flexCell(size)}}>
            {cellRenderer({rowIndex})}
          </div>;
        }, fp.toPairs(columns))}
      </div>;
    }, fp.range(0, rowCount))}
  </div>;
};

const PageButton = ({active = false, onClick, children}) => {
  return <div
    style={{
      cursor: 'pointer', padding: '0.1666rem',
      fontWeight: active ? 'bold' : undefined,
      textDecoration: active ? 'underline' : undefined
    }}
    {...{onClick, children}}
  />;
};

export const SimplePagination = ({total, limit, currentPage, onPageChange}) => {
  return <Pagination {...{total, limit, currentPage}} pageCount={5}>
    {({pages, hasPreviousPage, previousPage, hasNextPage, nextPage, totalPages}) => {
      return <div style={styles.paginationRow}>
        <div style={{marginLeft: 'auto', marginRight: '1rem'}}>
          {(currentPage - 1) * limit + 1} - {fp.min([total, currentPage * limit])} of {total}
        </div>
        <PageButton onClick={hasPreviousPage ? (() => onPageChange(1)) : undefined}>
          <ClrIcon shape='angle-double left'/>
        </PageButton>
        <PageButton onClick={hasPreviousPage ? (() => onPageChange(previousPage)) : undefined}>
          <ClrIcon shape='angle left'/>
        </PageButton>
        {fp.map(page => {
          return <PageButton
            key={page}
            active={page === currentPage}
            onClick={() => onPageChange(page)}
          >{page}</PageButton>;
        }, pages)}
        <PageButton onClick={hasNextPage ? (() => onPageChange(nextPage)) : undefined}>
          <ClrIcon shape='angle right'/>
        </PageButton>
        <PageButton onClick={hasNextPage ? (() => onPageChange(totalPages)) : undefined}>
          <ClrIcon shape='angle-double right'/>
        </PageButton>
      </div>;
    }}
  </Pagination>;
};

export const SortableHeader = ({sort, field, onSort, children}) => {
  return <div
    style={{flex: 1, display: 'flex', alignItems: 'center', cursor: 'pointer', minWidth: 0}}
    onClick={() => onSort(nextSort(sort, field))}
  >
    {children}
    {sort.field === field && <div style={{color: '#0079b8', marginLeft: 'auto'}}>
        <ClrIcon shape={sort.direction === 'asc' ? 'arrow down' : 'arrow'}/>
    </div>}
  </div>;
};

export const TextCell = ({style = {}, ...props}) => {
  return <div
    style={{overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis', ...style}}
    {...props}
  />;
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
