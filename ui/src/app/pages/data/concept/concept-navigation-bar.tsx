import {Clickable} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import * as React from 'react';

const styles = reactStyles({
  headerLinks: {
    color: colors.accent,
    fontSize: '20px',
    lineHeight: '24px',
    height: '1.5rem',
    padding: '0 .5rem',
    textAlign: 'center',
    letterSpacing: '0.12rem',
    textDecoration: 'none'
  },
  infoIcon: {
    color: colors.accent,
    cursor: 'pointer',
    marginLeft: '0.2rem',
    height: '20px',
    width: '20px'
  }
});

const activatedStyles = reactStyles({
  headerActivated: {
    ...styles.headerLinks,
    fontWeight: 700,
    borderBottom: 'solid 3px'
  }
});

const tooltipContent = [
  <ul><u>What is a concept?</u><ul/>
    Concepts describe information in a patient’s medical record,
    such as a condition they have, a  prescription they are
    taking or their physical measurements. <p/>
  <u>What is a concept set?</u><ul/>
    You can search for and save collections of concepts from a particular domain as a
    “Concept set”. For example, you can search for height, weight and blood pressure
    concepts from “Measurements” domain and call it “biometrics” concept set.<p/>
  <u>How to use a concept set </u><ul/>
    You can use Notebooks to extract data defined in your “concept set” from your “cohort”.
    For example, you can launch a Notebook to import your “diabetes cases” cohort
    and then select your “biometrics” concept set, to get biometrics data for
    the participants in your cohort.</ul>
];

export const ConceptNavigationBar: React.FunctionComponent<
  {ns: string, wsId: string, showConcepts: boolean}> =
  ({ns, wsId, showConcepts}) => {
    return <FlexRow>
      <Clickable style={showConcepts ? activatedStyles.headerActivated : styles.headerLinks}
                 onClick={() => navigate(['workspaces', ns, wsId, 'data', 'concepts'])}
                 data-test-id='concepts-link'>
        Concepts
      </Clickable>
      <Clickable
        style={{...(showConcepts ? styles.headerLinks : activatedStyles.headerActivated),
          marginLeft: '1rem'}}
        onClick={() => navigate(['workspaces', ns, wsId, 'data', 'concepts', 'sets'])}
        data-test-id='concept-sets-link'>
        Concept Sets
      </Clickable>
      <TooltipTrigger content={tooltipContent}>
        <ClrIcon shape='info' className='is-solid' style={styles.infoIcon}/>
      </TooltipTrigger>
    </FlexRow>;
  };
