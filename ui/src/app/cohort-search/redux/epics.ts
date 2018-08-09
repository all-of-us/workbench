import {Injectable} from '@angular/core';
import {DomainType} from 'generated';
import {Map} from 'immutable';
import {Epic} from 'redux-observable';
import {Observable} from 'rxjs/Observable';

import {CohortSearchActions} from './actions';

/* tslint:disable:ordered-imports */
import {
  BEGIN_CRITERIA_REQUEST,
  BEGIN_ALL_CRITERIA_REQUEST,
  BEGIN_DRUG_CRITERIA_REQUEST,
  BEGIN_AUTOCOMPLETE_REQUEST,
  BEGIN_INGREDIENT_REQUEST,
  CANCEL_CRITERIA_REQUEST,

  BEGIN_COUNT_REQUEST,
  CANCEL_COUNT_REQUEST,

  BEGIN_CHARTS_REQUEST,
  CANCEL_CHARTS_REQUEST,

  BEGIN_PREVIEW_REQUEST,
  BEGIN_ATTR_PREVIEW_REQUEST,

  RootAction,
  ActionTypes, BEGIN_CRITERIA_SUBTREE_REQUEST,
} from './actions/types';

import {
  loadCriteriaRequestResults,
  criteriaRequestError,

  loadCountRequestResults,
  countRequestError,

  loadChartsRequestResults,
  chartsRequestError,

  loadPreviewRequestResults,
  loadAttributePreviewRequestResults,
  previewRequestError,

  loadAutocompleteOptions,
  autocompleteRequestError,

  loadIngredients,
  loadCriteriaSubtree,

  loadSubtreeItems,
} from './actions/creators';

import {CohortSearchState} from './store';
/* tslint:enable:ordered-imports */

import {CohortBuilderService} from 'generated';

type CSEpic = Epic<RootAction, CohortSearchState>;
type CritRequestAction = ActionTypes[typeof BEGIN_CRITERIA_REQUEST];
type CritSubRequestAction = ActionTypes[typeof BEGIN_CRITERIA_SUBTREE_REQUEST];
type DrugCritRequestAction = ActionTypes[typeof BEGIN_DRUG_CRITERIA_REQUEST];
type AutocompleteRequestAction = ActionTypes[typeof BEGIN_AUTOCOMPLETE_REQUEST];
type IngredientRequestAction = ActionTypes[typeof BEGIN_INGREDIENT_REQUEST];
type CountRequestAction = ActionTypes[typeof BEGIN_COUNT_REQUEST];
type ChartRequestAction = ActionTypes[typeof BEGIN_CHARTS_REQUEST];
type PreviewRequestAction = ActionTypes[typeof BEGIN_ATTR_PREVIEW_REQUEST];
type AttributePreviewRequestAction = ActionTypes[typeof BEGIN_PREVIEW_REQUEST];
const compare = (obj) => (action) => Map(obj).isSubset(Map(action));

const mockSubtree = [
  {
    conceptId: 40772948,
    parentId: 0,
    name: 'Allergy',
    code: 'LP31625-4',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 340864,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40779591,
    parentId: 340864,
    name: 'Animal',
    code: 'LP31627-0',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 340888,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40776224,
    parentId: 340864,
    name: 'Drug allergens',
    code: 'LP30682-6',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 340889,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40789320,
    parentId: 340864,
    name: 'House dust',
    code: 'LP16999-2',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 340890,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40792769,
    parentId: 340864,
    name: 'Miscellaneous allergens',
    code: 'LP30681-8',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 340891,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40772922,
    parentId: 340864,
    name: 'Molds and Other Microorganisms',
    code: 'LP30672-7',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 340892,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40779557,
    parentId: 340864,
    name: 'Occupational allergens',
    code: 'LP30689-1',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 340893,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40776267,
    parentId: 340864,
    name: 'Others',
    code: 'LP32758-2',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 340894,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40782937,
    parentId: 340864,
    name: 'Plant',
    code: 'LP31628-8',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 340895,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 21496039,
    parentId: 340864,
    name: 'XXX allergen | Bld-Ser-Plas',
    code: 'LP199002-9',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 340896,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40782473,
    parentId: 340888,
    name: 'Fish',
    code: 'LP13942-5',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 344332,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40776225,
    parentId: 340888,
    name: 'Fowl',
    code: 'LP30699-0',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 344333,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40789541,
    parentId: 340888,
    name: 'Insects and venoms',
    code: 'LP31632-0',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 344334,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40786239,
    parentId: 340888,
    name: 'Mammals',
    code: 'LP31630-4',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 344335,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40796089,
    parentId: 340888,
    name: 'Mixes',
    code: 'LP30712-1',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 344336,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40789523,
    parentId: 340888,
    name: 'Mollusks',
    code: 'LP30694-1',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 344337,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40796665,
    parentId: 344332,
    name: 'Abalone (Haliotis spp) | Bld-Ser-Plas',
    code: 'LP44133-4',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352058,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 42530533,
    parentId: 344332,
    name: 'Alaska pollock roe | Bld-Ser-Plas',
    code: 'LP243314-4',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352059,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40780202,
    parentId: 344332,
    name: 'Anchovy (Engraulis encrasicolus) | Bld-Ser-Plas',
    code: 'LP44138-3',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352060,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40795046,
    parentId: 344332,
    name: 'Angler (Lophius piscatorius) | Bld-Ser-Plas',
    code: 'LP90854-8',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352061,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40776539,
    parentId: 344332,
    name: 'Artemia salina (fish feed) | Bld-Ser-Plas',
    code: 'LP41182-4',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352062,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40791264,
    parentId: 344332,
    name: 'Bass, Black (Micropterus salmoides) | Bld-Ser-Plas',
    code: 'LP62812-0',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352063,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 21497221,
    parentId: 344332,
    name: 'Carp recombinant (rCyp c) 1 | Bld-Ser-Plas',
    code: 'LP214722-3',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352064,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40777139,
    parentId: 344332,
    name: 'Carp | Bld-Ser-Plas',
    code: 'LP46763-6',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352065,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40778684,
    parentId: 344332,
    name: 'Catfish (Ictalurus punctatus) | Bld-Ser-Plas',
    code: 'LP97423-5',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352066,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 42530281,
    parentId: 344332,
    name: 'Chum salmon roe | Bld-Ser-Plas',
    code: 'LP243394-6',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352067,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40782476,
    parentId: 344332,
    name: 'Codfish (Gadus morhua)',
    code: 'LP13956-5',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352068,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40772702,
    parentId: 344332,
    name: 'Crab (Cancer pagurus)',
    code: 'LP16960-4',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352069,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40783517,
    parentId: 344332,
    name: 'Crawfish (Astacus astacus) | Bld-Ser-Plas',
    code: 'LP44246-4',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352070,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40786552,
    parentId: 344332,
    name: 'Daphnia (fish feed) | Bld-Ser-Plas',
    code: 'LP41183-2',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352071,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40797210,
    parentId: 344332,
    name: 'Eel (Anguilla anguilla) | Bld-Ser-Plas',
    code: 'LP49117-2',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352072,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40777840,
    parentId: 344332,
    name: 'Fish | Bld-Ser-Plas',
    code: 'LP54555-5',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352073,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40783519,
    parentId: 344332,
    name: 'Flounder | Bld-Ser-Plas',
    code: 'LP44284-5',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352074,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40778166,
    parentId: 344332,
    name: 'Giant Japanese spider crab (Macrocheira kaempferi) | Bld-Ser-Plas',
    code: 'LP65631-1',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352075,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40798344,
    parentId: 344332,
    name: 'Giant grouper (Epinephelus lanceolatus) | Bld-Ser-Plas',
    code: 'LP90850-6',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352076,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40794375,
    parentId: 344332,
    name: 'Grayfish | Bld-Ser-Plas',
    code: 'LP54585-2',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352077,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40793356,
    parentId: 344332,
    name: 'Haddock (Melanogrammus aeglefinus) | Bld-Ser-Plas',
    code: 'LP44321-5',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352078,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40787072,
    parentId: 344332,
    name: 'Hake (Merluccius merluccius) | Bld-Ser-Plas',
    code: 'LP46237-1',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352079,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40796689,
    parentId: 344332,
    name: 'Halibut (Hippoglossus hippoglossus) | Bld-Ser-Plas',
    code: 'LP44322-3',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352080,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40796690,
    parentId: 344332,
    name: 'Herring (Clupea harengus) | Bld-Ser-Plas',
    code: 'LP44330-6',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352081,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40773528,
    parentId: 344332,
    name: 'Lobster (Homarus gammarus) | Bld-Ser-Plas',
    code: 'LP44372-8',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352082,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40776971,
    parentId: 344332,
    name: 'Lumpfish roe | Bld-Ser-Plas',
    code: 'LP45142-4',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352083,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40782725,
    parentId: 344332,
    name: 'Mackerel (Scomber scombrus)',
    code: 'LP17504-9',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352084,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 42530331,
    parentId: 344332,
    name: 'Mahi mahi | Bld-Ser-Plas',
    code: 'LP243377-1',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352085,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40793568,
    parentId: 344332,
    name: 'Mealworm (Tenebrio mollitor) | Bld-Ser-Plas',
    code: 'LP46242-1',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352086,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40786579,
    parentId: 344332,
    name: 'Orange roughy (Hoplostethus atlanticus) | Bld-Ser-Plas',
    code: 'LP41442-2',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352087,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40790098,
    parentId: 344332,
    name: 'Perch (Perca spp) | Bld-Ser-Plas',
    code: 'LP44447-8',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352088,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40783235,
    parentId: 344332,
    name: 'Plaice (Pleuronectes platessa) | Bld-Ser-Plas',
    code: 'LP41216-0',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352089,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40775744,
    parentId: 344332,
    name: 'Pollock (Pollachius virens) | Bld-Ser-Plas',
    code: 'LP133871-6',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352090,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40773545,
    parentId: 344332,
    name: 'Red Snapper | Bld-Ser-Plas',
    code: 'LP44518-6',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352091,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40793475,
    parentId: 344332,
    name: 'Redfish | Bld-Ser-Plas',
    code: 'LP45445-1',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352092,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40783544,
    parentId: 344332,
    name: 'Saline fish feed | Bld-Ser-Plas',
    code: 'LP44501-2',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352093,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40772495,
    parentId: 344332,
    name: 'Salmon (Salmo salar)',
    code: 'LP13958-1',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352094,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40775852,
    parentId: 344332,
    name: 'Sardine',
    code: 'LP15139-6',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352095,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40783253,
    parentId: 344332,
    name: 'Scad (Trachurus japonicus) | Bld-Ser-Plas',
    code: 'LP41430-7',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352096,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40786580,
    parentId: 344332,
    name: 'Shark | Bld-Ser-Plas',
    code: 'LP41446-3',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352097,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 42870893,
    parentId: 344332,
    name: 'Shrimp',
    code: 'LP149596-1',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352098,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40777858,
    parentId: 344332,
    name: 'Smelt | Bld-Ser-Plas',
    code: 'LP54716-3',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352099,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40790108,
    parentId: 344332,
    name: 'Sole (Solea solea) | Bld-Ser-Plas',
    code: 'LP44519-4',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352100,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40786872,
    parentId: 344332,
    name: 'Spiny lobster (Palinurus spp) | Bld-Ser-Plas',
    code: 'LP44373-6',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352101,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40790110,
    parentId: 344332,
    name: 'Swordfish (Xiphias gladius) | Bld-Ser-Plas',
    code: 'LP44533-5',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352102,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40786553,
    parentId: 344332,
    name: 'Tetramin (fish feed) | Bld-Ser-Plas',
    code: 'LP41184-0',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352103,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40790681,
    parentId: 344332,
    name: 'Tilapia | Bld-Ser-Plas',
    code: 'LP50092-3',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352104,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40796706,
    parentId: 344332,
    name: 'Trout (Oncorhynchus mykiss) | Bld-Ser-Plas',
    code: 'LP44549-1',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352105,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40790114,
    parentId: 344332,
    name: 'Tuna (Thunnus albacares) | Bld-Ser-Plas',
    code: 'LP44550-9',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352106,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40790378,
    parentId: 344332,
    name: 'Walleye Pike (Stizostedion vitreum) | Bld-Ser-Plas',
    code: 'LP46891-5',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352107,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40776541,
    parentId: 344332,
    name: 'Whiff (Lepidorhombus whiffiagonis) | Bld-Ser-Plas',
    code: 'LP41192-3',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352108,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 40772747,
    parentId: 344332,
    name: 'Whitefish',
    code: 'LP17666-6',
    count: null,
    hasAttributes: false,
    selectable: false,
    subtype: 'LAB',
    type: 'MEAS',
    id: 352109,
    predefinedAttributes: null,
    domainId: '',
    group: true
  },
  {
    conceptId: 3014376,
    parentId: 352058,
    name: 'Abalone IgE Ab RAST class [Presence] in Serum',
    code: '15518-4',
    count: null,
    hasAttributes: false,
    selectable: true,
    subtype: 'LAB',
    type: 'MEAS',
    id: 377265,
    predefinedAttributes: null,
    domainId: '',
    group: false
  },
  {
    conceptId: 3013210,
    parentId: 352058,
    name: 'Abalone IgE Ab [Units/volume] in Serum',
    code: '7060-7',
    count: null,
    hasAttributes: false,
    selectable: true,
    subtype: 'LAB',
    type: 'MEAS',
    id: 377266,
    predefinedAttributes: null,
    domainId: '',
    group: false
  }
];

/**
 * CohortSearchEpics
 *
 * Exposes functions (called `epics` by redux-observable) that listen in on the
 * stream of dispatched actions (exposed as an Observable) and attach handlers
 * to certain of them; this allows us to dispatch actions asynchronously.  This is
 * the interface between the application state and the backend API.
 *
 * TODO: clean up these funcs using the new lettable operators
 */
@Injectable()
export class CohortSearchEpics {
  constructor(
    private service: CohortBuilderService,
    private actions: CohortSearchActions
  ) {}

  fetchCriteria: CSEpic = (action$) => (
    action$.ofType(BEGIN_CRITERIA_REQUEST).mergeMap(
      ({cdrVersionId, kind, parentId}: CritRequestAction) => {
        return this.service.getCriteriaByTypeAndParentId(cdrVersionId, kind, parentId)
          .map(result => loadCriteriaRequestResults(kind, parentId, result.items))
          .race(action$
            .ofType(CANCEL_CRITERIA_REQUEST)
            .filter(compare({kind, parentId}))
            .first())
          .catch(e => Observable.of(criteriaRequestError(kind, parentId, e)));
      }
    )
  )

  fetchCriteriaSubtree: CSEpic = (action$) => (
    action$.ofType(BEGIN_CRITERIA_SUBTREE_REQUEST).mergeMap(
      ({cdrVersionId, kind, id}: CritSubRequestAction) => {
        return this.service.getCriteriaById(cdrVersionId, id)
          .map(result => loadSubtreeItems(kind, id, result.items))
          .race(action$
            .ofType(CANCEL_CRITERIA_REQUEST)
            .filter(compare({kind, id}))
            .first())
          .catch(e => Observable.of(criteriaRequestError(kind, id, e)));
      }
    )
  )

  fetchAllCriteria: CSEpic = (action$) => (
    action$.ofType(BEGIN_ALL_CRITERIA_REQUEST).mergeMap(
      ({cdrVersionId, kind, parentId}: CritRequestAction) => {
        return this.service.getCriteriaByType(cdrVersionId, kind)
          .map(result => loadCriteriaRequestResults(kind, parentId, result.items))
          .race(action$
            .ofType(CANCEL_CRITERIA_REQUEST)
            .filter(compare({kind, parentId}))
            .first())
          .catch(e => Observable.of(criteriaRequestError(kind, parentId, e)));
      }
    )
  )

  fetchDrugCriteria: CSEpic = (action$) => (
    action$.ofType(BEGIN_DRUG_CRITERIA_REQUEST).mergeMap(
      ({cdrVersionId, kind, parentId, subtype}: DrugCritRequestAction) => {
        return this.service.getCriteriaByTypeAndSubtype(cdrVersionId, kind, subtype)
          .map(result => loadCriteriaRequestResults(kind, parentId, result.items))
          .race(action$
            .ofType(CANCEL_CRITERIA_REQUEST)
            .filter(compare({kind, parentId}))
            .first())
          .catch(e => Observable.of(criteriaRequestError(kind, parentId, e)));
      }
    )
  )

  fetchAutocompleteOptions: CSEpic = (action$) => (
    action$.ofType(BEGIN_AUTOCOMPLETE_REQUEST).mergeMap(
      ({cdrVersionId, kind, searchTerms}: AutocompleteRequestAction) => {
        if (kind === DomainType[DomainType.DRUG]) {
          return this.service.getDrugBrandOrIngredientByName(cdrVersionId, searchTerms)
            .map(result => loadAutocompleteOptions(result.items))
            .catch(e => Observable.of(autocompleteRequestError(e)));
        } else {
          return this.service.getCriteriaByTypeForCodeOrName(cdrVersionId, kind, searchTerms)
            .map(result => loadAutocompleteOptions(result.items))
            .catch(e => Observable.of(autocompleteRequestError(e)));
        }
      }
    )
  )

  fetchIngredientsForBrand: CSEpic = (action$) => (
    action$.ofType(BEGIN_INGREDIENT_REQUEST).mergeMap(
      ({cdrVersionId, conceptId}: IngredientRequestAction) => {
        return this.service.getDrugIngredientByConceptId(cdrVersionId, conceptId)
          .map(result => loadIngredients(result.items))
          .catch(e => Observable.of(autocompleteRequestError(e)));
      }
    )
  )

  fetchCount: CSEpic = (action$) => (
    action$.ofType(BEGIN_COUNT_REQUEST).mergeMap(
      ({cdrVersionId, entityType, entityId, request}: CountRequestAction) =>
      this.service.countParticipants(cdrVersionId, request)
        .map(response => typeof response === 'number' ? response : 0)
        .map(count => loadCountRequestResults(entityType, entityId, count))
        .race(action$
          .ofType(CANCEL_COUNT_REQUEST)
          .filter(compare({entityType, entityId}))
          .first())
        .catch(e => Observable.of(countRequestError(entityType, entityId, e)))
    )
  )

  previewCount: CSEpic = (action$) => (
    action$.ofType(BEGIN_PREVIEW_REQUEST).switchMap(
      ({cdrVersionId, request}: PreviewRequestAction) =>
      this.service.countParticipants(cdrVersionId, request)
        .map(response => typeof response === 'number' ? response : 0)
        .map(count => loadPreviewRequestResults(count))
        .catch(e => Observable.of(previewRequestError(e)))
    )
  )

  attributePreviewCount: CSEpic = (action$) => (
    action$.ofType(BEGIN_ATTR_PREVIEW_REQUEST).switchMap(
      ({cdrVersionId, request}: AttributePreviewRequestAction) =>
      this.service.countParticipants(cdrVersionId, request)
        .map(response => typeof response === 'number' ? response : 0)
        .map(count => loadAttributePreviewRequestResults(count))
        .catch(e => Observable.of(previewRequestError(e)))
    )
  )

  fetchChartData: CSEpic = (action$) => (
    action$.ofType(BEGIN_CHARTS_REQUEST).mergeMap(
      ({cdrVersionId, entityType, entityId, request}: ChartRequestAction) =>
      this.service.getChartInfo(cdrVersionId, request)
        .map(result => loadChartsRequestResults(entityType, entityId, result.items))
        .race(action$
          .ofType(CANCEL_CHARTS_REQUEST)
          .filter(compare({entityType, entityId}))
          .first())
        .catch(e => Observable.of(chartsRequestError(entityType, entityId, e)))
    )
  )
}
