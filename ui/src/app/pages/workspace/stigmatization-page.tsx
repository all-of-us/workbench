import {Component} from '@angular/core';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import * as React from 'react';

const styles = reactStyles({
  stigmaPage: {
    paddingTop: '2rem',
    paddingLeft: '3rem',
    paddingBottom: '2rem',
    maxWidth: '50rem',
    height: '100%'
  },
  header: {
    color: colors.primary,
    fontSize: 20,
    fontWeight: 600,
    lineHeight: '24px'
  },
  subHeader: {
    fontSize: 16,
    color: colors.primary,
    fontWeight: 600,
    lineHeight: '24px',
    marginTop: '0.6rem'
  },
  text: {
    color: colors.primary,
    fontSize: 14,
    fontWeight: 400,
    lineHeight: '24px'
  },
  list: {
    paddingLeft: '1rem',
    listStylePosition: 'outside',
    color: colors.primary,
    fontWeight: 600
  }
});

const ListText = [
  {
    header: 'The identification and labeling of human differences.',
    text: ' Not all differences are deemed socially relevant. Certain differences—such as ' +
    'differences in skin color, ethnicity, country of origin, ancestry, sex, gender, sexual ' +
    'orientation, socioeconomic status, educational attainment, disability status, and body ' +
    'type—are considerably more prone to stigma than others. Salience of labels in such domains ' +
    'are dependent on a process during which gross oversimplification of an attribute or ' +
    'characteristic creates artificial categories, upon which contextually-specific social ' +
    'selection occurs.'
  }, {
    header: 'The linking of labeled persons by dominant cultural belief to undesirable ' +
    'characteristics and formation of negative stereotypes.',
    text: 'A key feature in this process is the provision of characteristics that may be linked ' +
    'to one or more social labels. Scientific research can provide just such fodder, and the ' +
    'result can be arbitrary stereotypes socially perpetuated under the banner of science.'
  },
  {
    header: 'Separation of people into categories of \“us\” and \“them\” by applying said ' +
    'stereotypes.',
    text: 'The logical next step after the perpetration of a negative stereotype is the use\n' +
    ' of that stereotype to define in-groups and out-groups.'
  },
  {
    header: 'The experience of loss of status and discrimination by labeled persons.',
    text: 'This is the ultimate output of the stigma process. Here, “out-group” members are ' +
    'downwardly placed, with regards to the “in-group,” and subjected to detrimental treatment, ' +
    'consideration, or distinction based upon label(s), rather than merit.' +
    ' Discrimination can be leveraged by an individual or group towards an individual or ' +
    'collection of individuals. However, discrimination is particularly dangerous in its ' +
    'structural form, when discriminatory behavior becomes culturally accepted and accumulates ' +
    'to shape institutional practices that work to the disadvantage of labeled group(s), even in ' +
    'the absence of individual prejudices or discrimination.'
  },
  {
    header: 'The exercise of social, economic, political, or other power that reinforces the' +
    ' above processes, translating them into significant consequences for labeled individuals.',
    text: 'Without the uneven distribution of power, in-group prejudices would not carry the ' +
    'same negative consequences for out-group members.'
  }
];

export class StigmatizatonPage extends React.Component<{}, {}> {
  render() {
    return <div style={styles.stigmaPage}>
      <div style={styles.header}>Stigmatizing research</div>
      <div style={styles.subHeader}>Policy</div>
      <div style={styles.text}>Central to the mission of the <i>All of Us</i> Research Program is the
        collection of information from diverse groups of people within the U.S. population.
        However, the Program must
        recognize the potential for resources collected in good faith to be used towards social
        detriment, like stigmatization. Stigmatizing research is any research proposal, project,
        or question that has the potential to instigate or promote marginalization, discrimination,
        or loss of status by a person or group of people. Stigma may be inherent
        in the research design (i.e. the formation of a research question based on prejudicial
        biases) or a byproduct of the research findings (i.e. the interpretation of findings in
        such a way as to promote negative stereotypes) and may be intentional or unintentional.
        While complete elimination of stigmatizing research based on use of the <i>All of Us</i> data
        resources is likely impossible, the Program is taking steps in earnest to prevent resource
        use with the potential to stigmatize and to punish people who misuse the resource to the
        detriment of others as appropriate.
      </div>
      <div style={styles.subHeader}>Definition of Stigma</div>
      <div style={styles.text}>
        Stigma may be understood as convergence of the following components (adapted from
        <a href='https://www.annualreviews.org/doi/abs/10.1146/annurev.soc.27.1.363'> Link
          and Phelan, 2001
        </a>):
      </div>
      <ol style={styles.list}>
        {ListText.map(({header, text}, index) => {
          return  <li key={index} style={{marginTop: '1rem'}}>
             <span style={{...styles.text, 'fontWeight': 600, 'color': colors.primary}}>
               {header}
             </span>
             <span style={{...styles.text, marginLeft: '0.3rem'}}>
               {text}
             </span>
           </li>;
        })}
      </ol>
      <div style={{...styles.text, marginTop: '1rem'}}>
        When these components converge, they produce widespread attitudes and beliefs that
        lead people to reject, avoid, or fear those they perceive as being different,
        resulting in detrimental inequities and unfair treatment at either the individual
        or group level.
      </div>
      </div>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class StigmatizationPageComponent extends ReactWrapperBase {
  constructor() {
    super(StigmatizatonPage, []);
  }
}
