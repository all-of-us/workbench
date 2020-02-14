import {ElementHandle, JSHandle, Page} from 'puppeteer';
import AouElement from '../driver/AouElement';
import Widget from './elements/widget';

// If element CSS/XPath selectors are used frequently, put it there.
// abbriviation SELT: selector


// Web elements on Edit page
// (required) input - Workspace Name
exports.SELT_XPATH_WORKSPACENAME = `//*[normalize-space(@placeholder)="Workspace Name"][@type="text"]`;
// (required) select - Synthetic DataSet
exports.SELT_CSS_DATASET = `input[type=text][placeholder="Workspace Name"] + div[id] > select`;
// input - required when Disease-focused research is checked
exports.SELT_XPATH_DISEASENAME = `//*[normalize-space(@placeholder)="Name of Disease"][@type="text"]`;
// textarea - Other Purpose: required when Other Purpose is checked
exports.SELT_XPATH_OTHERPURPOSE = `//*[normalize-space(text())="Other Purpose"]/following-sibling::*/textarea`;
// textarea - question #2 (required)
exports.SELT_XPATH_QUESTION2 = `//*[starts-with(normalize-space(.), "2. Provide the reason for choosing All of Us data for your investigation")]/following-sibling::*/textarea`;
// textarea - question #3 (required)
exports.SELT_XPATH_QUESTION3 = `//*[starts-with(normalize-space(.), "3. What are the specific scientific question(s) you intend to study")]/following-sibling::*/textarea`;
// textarea - question #4 (required)
exports.SELT_XPATH_QUESTION4 = `//*[starts-with(normalize-space(.), "4. What are your anticipated findings from this study")]/following-sibling::*/textarea`;
// request a review Yes or No radiobutton
exports.SELT_XPATH_YES = `//*[label[contains(normalize-space(.),"Would you like to request a review of your research purpose")]]/following-sibling::*/input[@type="radio"][following-sibling::label[1]/text()="Yes"]`;
exports.SELT_XPATH_NO = `//*[label[contains(normalize-space(.),"Would you like to request a review of your research purpose")]]/following-sibling::*/input[@type="radio"][following-sibling::label[1]/text()="No"]`;
