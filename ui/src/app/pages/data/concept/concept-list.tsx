import {Clickable} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import colors from 'app/styles/colors';
import {
  reactStyles,
  withCurrentConcept
} from 'app/utils';
import {currentConceptStore, setSidebarActiveIconStore} from 'app/utils/navigation';
import {Concept} from 'generated';
import * as fp from 'lodash/fp';
import * as React from 'react';
const styles = reactStyles({
  sectionTitle: {
    marginTop: '0',
    fontWeight: 600,
    color: colors.primary,
    marginBottom: '1rem'
  },
  selectionContainer: {
    background: colors.white,
    border: `2px solid ${colors.primary}`,
    borderRadius: '5px',
    lineHeight: '0.75rem',
    minHeight: 'calc(100vh - 15rem)',
    padding: '0.5rem',
    overflowX: 'hidden',
    overflowY: 'auto',
  },
  removeSelection: {
    background: 'none',
    border: 0,
    color: colors.danger,
    cursor: 'pointer',
    marginRight: '0.25rem',
    padding: 0
  }
});
export const  ConceptListPage = fp.flow(withCurrentConcept())(
  class extends React.Component<{concept: Array<Concept>} , {}> {
    constructor(props) {
      super(props);
    }

    removeSelection(conceptToDel) {
      const newArr = this.props.concept.filter((concept) => concept !== conceptToDel);
      currentConceptStore.next(newArr);
    }

    render() {
      return <div>
        <FlexRow><h3 style={styles.sectionTitle}>Selected Concepts</h3>
          <Clickable style={{marginRight: '1rem', position: 'absolute', right: '0px'}}
                     onClick={() => setSidebarActiveIconStore.next(undefined)}>
            <img src={'/assets/icons/times-light.svg'}
                 style={{height: '27px', width: '17px'}}
                 alt='Close'/>
          </Clickable></FlexRow>

        <div style={styles.selectionContainer}>
          {this.props.concept.map((con, index) => <FlexRow style={{lineHeight: '1.25rem'}}>
            <button style={styles.removeSelection} onClick={() => this.removeSelection(con)}>
              <ClrIcon shape='times-circle'/>
            </button>
            <b style={{paddingRight: '0.25rem'}}>{con.conceptCode}</b>
            {con.conceptName}
          </FlexRow>)}
        </div>
        </div>;
    }
  });
