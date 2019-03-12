import {Clickable} from 'app/components/buttons';
import {ClrIcon, InfoIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {reactStyles} from 'app/utils';
import {UnderservedPopulationEnum} from 'generated/model/underservedPopulationEnum';
import * as fp from 'lodash/fp';
import * as React from 'react';

const styles = reactStyles({
  section: {
    width: '60%',
    minWidth: '835px',
    marginTop: '1rem'
  },
  header: {
    fontWeight: 600,
    lineHeight: '24px',
    color: '#262262',
    fontSize: '16px'
  },
  infoIcon: {
    height: 16,
    marginTop: '0.1rem',
    marginLeft: '0.2rem',
    width: 16
  },
  focusLabel: {
    position: 'relative',
    display: 'inline-block',
    minHeight: '1rem',
    cursor: 'pointer',
    lineHeight: '1rem'
  },
  title: {
    paddingTop: '1rem',
    paddingBottom: '0.5rem',
    borderBottom: '1px solid #4356a7',
    marginBottom: '0.6rem',
    color: '#262262',
    fontSize: '16px',
    fontWeight: 600,
    lineHeight: '19px'
  },
  text: {
    fontSize: '13px',
    color: '#4A4A4A',
    fontWeight: 400,
    lineHeight: '24px'
  },
  checkBox: {
    color: '#333333',
    fontSize: 14,
    fontWeight: 400,
    lineHeight: '30px', paddingRight: '0.3rem'
  },
  row: {paddingLeft: '12px', paddingRight: '12px',
    width: '33.3%', display: 'flex', flexDirection: 'row', height: '3rem'},
  checkbox: {
    color: '#333333',
    fontSize: 14,
    fontWeight: 400,
    lineHeight: '30px', paddingRight: '0.3rem'},
  label: {
    position: 'relative',
    display: 'inline-block',
    minHeight: '1rem',
    paddingLeft: '.31667rem',
    cursor: 'pointer',
    lineHeight: '1rem',
    color: 'black',
    marginTop: '-0.3rem'
  },
  focusHeader: {
    paddingTop: '1rem',
    paddingBottom: '0.5rem',
    borderBottom: '1px solid #4356a7',
    marginBottom: '0.6rem',
    color: '#262262',
    fontSize: '16px',
    fontWeight: 600,
    lineHeight: '19px'
  },
  icon: {width: '18px', height: '18px', fill: '#302C71'}

});

const FocusCategories = [
  {
    title: 'Race',
    categories: [
      {
        id: UnderservedPopulationEnum.RACEAMERICANINDIANORALASKANATIVE,
        label: ' American Indian or Alaska Native '
      },
      {id: UnderservedPopulationEnum.RACEHISPANICORLATINO, label: ' Hispanic or Latino'},
      {id: UnderservedPopulationEnum.RACEMORETHANONERACE, label: ' More than one race '},
      {id: UnderservedPopulationEnum.RACEASIAN, label: 'Asian'},
      {
        id: UnderservedPopulationEnum.RACEMIDDLEEASTERNORNORTHAFRICAN,
        label: ' Middle Eastern or North African '
      },
      {
        id: UnderservedPopulationEnum.RACEBLACKAFRICANORAFRICANAMERICAN,
        label: ' Black, African or African American '
      },
      {
        id: UnderservedPopulationEnum.RACENATIVEHAWAIIANORPACIFICISLANDER,
        label: ' Native Hawaiian or Pacific Islander '
      }
    ]
  },
  {
    title: 'Age',
    categories: [
      {id: UnderservedPopulationEnum.AGECHILDREN, label: 'Children (0-11)'},
      {id: UnderservedPopulationEnum.AGEADOLESCENTS, label: 'Adolescents (12-17)'},
      {id: UnderservedPopulationEnum.AGEOLDERADULTS, label: ' Older Adults (65-74)'},
      {id: UnderservedPopulationEnum.AGEELDERLY, label: 'Elderly (75+)'}
    ]
  },
  {
    title: 'Sex',
    categories: [
      {id: UnderservedPopulationEnum.SEXFEMALE, label: 'Female'},
      {id: UnderservedPopulationEnum.SEXINTERSEX, label: 'Intersex'}
    ]
  },
  {
    title: 'Gender Identity',
    categories: [
      {id: UnderservedPopulationEnum.GENDERIDENTITYWOMAN, label: 'Woman'},
      {id: UnderservedPopulationEnum.GENDERIDENTITYNONBINARY, label: 'Non-Binary'},
      {
        id: UnderservedPopulationEnum.GENDERIDENTITYTRANSMAN,
        label: 'Trans man/Transgender Man/FTM'
      },
      {
        id: UnderservedPopulationEnum.GENDERIDENTITYTRANSWOMAN,
        label: 'Trans woman/Transgender Woman/MTF'
      },
      {id: UnderservedPopulationEnum.GENDERIDENTITYGENDERQUEER, label: 'Genderqueer'},
      {id: UnderservedPopulationEnum.GENDERIDENTITYGENDERFLUID, label: 'Genderfluid'},
      {id: UnderservedPopulationEnum.GENDERIDENTITYGENDERVARIANT, label: 'Gender Variant'},
      {
        id: UnderservedPopulationEnum.GENDERIDENTITYQUESTIONING,
        label: 'Questioning or unsure of their identity'
      }
    ]
  },
  {
    title: 'Sexual Orientation',
    categories: [
      {id: UnderservedPopulationEnum.SEXUALORIENTATIONGAY, label: 'Gay'},
      {id: UnderservedPopulationEnum.SEXUALORIENTATIONLESBIAN, label: 'Lesbian'},
      {id: UnderservedPopulationEnum.SEXUALORIENTATIONBISEXUAL, label: 'Bisexual'},
      {
        id: UnderservedPopulationEnum.SEXUALORIENTATIONPOLYSEXUALOMNISEXUALSAPIOSEXUALORPANSEXUAL,
        label: 'Polysexual, omnisexual, sapiosexual or pansexual'
      },
      {id: UnderservedPopulationEnum.SEXUALORIENTATIONASEXUAL, label: 'Asexual'},
      {id: UnderservedPopulationEnum.SEXUALORIENTATIONTWOSPIRIT, label: 'Two-Spirit'},
      {
        id: UnderservedPopulationEnum.SEXUALORIENTATIONFIGURINGOUTSEXUALITY,
        label: 'Have not figured out or are in the process of figuring out their sexuality'
      },
      {
        id: UnderservedPopulationEnum.SEXUALORIENTATIONMOSTLYSTRAIGHT,
        label: 'Mostly straight, but sometimes attracted to people of their own sex'
      },
      {
        id: UnderservedPopulationEnum.SEXUALORIENTATIONDOESNOTTHINKOFHAVINGSEXUALITY,
        label: 'Does not think of themselves as having sexuality'
      },
      {
        id: UnderservedPopulationEnum.SEXUALORIENTATIONDOESNOTUSELABELS,
        label: 'Does not use labels to identify themselves'
      },
      {
        id: UnderservedPopulationEnum.SEXUALORIENTATIONDOESNOTKNOWANSWER,
        label: 'Does not know the answer'
      }
    ]
  },
  {
    title: 'Geography',
    categories: [
      {
        id: UnderservedPopulationEnum.GEOGRAPHYURBANCLUSTERS,
        label: 'Urban clusters (2,500-50,000 people)'
      },
      {
        id: UnderservedPopulationEnum.GEOGRAPHYRURAL,
        label: 'Rural (All population, housing and territory not included within an urban area)'
      }
    ]
  },
  {
    title: 'Disability',
    categories: [
      {id: UnderservedPopulationEnum.DISABILITYPHYSICAL, label: 'Physical Disability'},
      {id: UnderservedPopulationEnum.DISABILITYMENTAL, label: 'Mental Disability'}
    ]
  },
  {
    title: 'Access to care',
    categories: [
      {
        id: UnderservedPopulationEnum.ACCESSTOCARENOTPASTTWELVEMONTHS,
        label: 'Have not had a clinic visit in the past 12 months'
      },
      {
        id: UnderservedPopulationEnum.ACCESSTOCARECANNOTOBTAINORPAYFOR,
        label: 'Cannot easily obtain or pay for medical care'
      }
    ]
  },
  {
    title: 'Education',
    categories: [
      {
        id: UnderservedPopulationEnum.EDUCATIONINCOMELESSTHANHIGHSCHOOLGRADUATE,
        label: 'Less than a high school graduate'
      },
      {
        id: UnderservedPopulationEnum.EDUCATIONINCOMELESSTHANTWENTYFIVETHOUSANDFORFOURPEOPLE,
        label: 'Less than $25,000 for 4 people'
      }
    ]
  }
];

export class WorkspaceUnderservedPopulation extends
    React.Component<{value: Array<UnderservedPopulationEnum>, onChange: Function},
    {show: boolean, value: Array<UnderservedPopulationEnum>}> {
  constructor(props: any) {
    super(props);
    this.state = {show: false, value: !props.value ? [] : props.value};
  }

  flipShow() {
    this.setState({
      show: !this.state.show
    });
  }

  focusCategoryChange(subCategory, value) {
    if (!value ) {
      const index = this.state.value.findIndex(item => item === subCategory.id);
      this.state.value.splice(index , 1);
    } else if (value) {
      this.state.value.push(subCategory);
    }
    this.props.onChange(this.state.value);
  }


  render() {
    return <div style={styles.section}>
      <div style={{display: 'flex', flexDirection: 'row'}}>
        <div>
          <Clickable onClick={() => this.flipShow()}>
            {!this.state.show &&
            <ClrIcon shape='plus-circle' style={styles.icon}/>
            }
            {this.state.show &&
            <ClrIcon shape='minus-circle' style={styles.icon}/>
            }
          </Clickable>
        </div>
      <div>
        <div style={styles.header}>
          Focus on an underserved population
          <TooltipTrigger content='A primary mission of the All of Us Research Program is to include
          populations that are medically underserved and/or historically underrepresented in
          biomedical research or who, because of systematic social disadvantage, experience
          disparities in health. As a way to understand how much research is being conducted on
          these populations, All of Us requests that you mark all options for underserved
          populations that will be included in your research.'>
            <InfoIcon style={styles.infoIcon}/>
          </TooltipTrigger>
        </div>
        <label style={styles.focusLabel}>
          This research will focus on, or include findings on, distinguishing characteristics
          related to one or more underserved populations
        </label>
      </div>

    </div>
      {this.state.show && <div style={{paddingTop: '1rem'}}>
        <h5>Additional information for underserved population</h5>
        <label>Select all that apply. Only options for underserved populations are provided.
        </label>
        <div>
          { FocusCategories.map (({title, categories} ) => {
            return <div key={title}>
              <h4 style={styles.title}>
                {title}
              </h4>
              <div style={{display: 'flex', flexWrap: 'wrap'}}>
                {
                  categories.map(({id, label}) => {
                    const isIncluded = fp.includes(id, this.state.value);
                    return <div style={styles.row}>
                      <input type='checkbox' style={styles.checkBox} checked={isIncluded}
                             onChange = {e => (this.focusCategoryChange(id, e.target.checked))}/>
                      <label style={styles.label}>{label}</label>
                    </div>;
                  })
                }
              </div>
            </div>;
          })}
         </div>
      </div>
      }
    </div>;
  }
}

export default WorkspaceUnderservedPopulation;
