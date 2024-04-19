import * as React from 'react';
import { Dropdown } from 'primereact/dropdown';
import Nouislider from 'nouislider-react';

import { VariantFilterRequest, VariantFilterResponse } from 'generated/fetch';

import { Button, Clickable } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import colors, { colorWithWhiteness } from 'app/styles/colors';

const { useEffect, useState } = React;

export const VariantSearchFilters = ({
  filters,
  formState,
  checkboxFn,
  sliderFn,
  sortFn,
  clearFn,
  submitFn,
}: {
  filters: VariantFilterResponse;
  formState: VariantFilterRequest;
  checkboxFn: Function;
  sliderFn: Function;
  sortFn: Function;
  clearFn: Function;
  submitFn: Function;
}) => {
  const [expanded, setExpanded] = useState([]);
  const [sliderInputState, setSliderInputState] = useState({
    countMin: formState.countMin ?? filters.countMin,
    countMax: formState.countMax ?? filters.countMax,
    numberMin: formState.numberMin ?? filters.numberMin,
    numberMax: formState.numberMax ?? filters.numberMax,
    frequencyMin: formState.frequencyMin ?? filters.frequencyMin,
    frequencyMax: formState.frequencyMax ?? filters.frequencyMax,
  });

  useEffect(() => {
    setSliderInputState({
      countMin: formState.countMin ?? filters.countMin,
      countMax: formState.countMax ?? filters.countMax,
      numberMin: formState.numberMin ?? filters.numberMin,
      numberMax: formState.numberMax ?? filters.numberMax,
      frequencyMin: formState.frequencyMin ?? filters.frequencyMin,
      frequencyMax: formState.frequencyMax ?? filters.frequencyMax,
    });
  }, [
    formState.countMin,
    formState.countMax,
    formState.numberMin,
    formState.numberMax,
    formState.frequencyMin,
    formState.frequencyMax,
  ]);

  const toggleExpanded = (section: string) =>
    setExpanded((prevState) =>
      prevState.includes(section)
        ? prevState.filter((sec) => sec !== section)
        : [...prevState, section]
    );

  const handleMinInputChange = (
    sliderName: string,
    range: number[],
    value: string,
    blur?: boolean
  ) => {
    let newValue = +value;
    if ((blur && value === '') || newValue < range[0]) {
      newValue = range[0];
    } else if (newValue > range[1]) {
      newValue = range[1];
    }
    setSliderInputState((prevState) => ({
      ...prevState,
      [`${sliderName}Min`]: blur ? newValue : value,
    }));
    sliderFn(sliderName, [newValue, range[1]]);
  };

  const handleMaxInputChange = (
    sliderName: string,
    range: number[],
    value: string,
    blur?: boolean
  ) => {
    let newValue = +value;
    if ((blur && value === '') || newValue > range[1]) {
      newValue = range[1];
    } else if (newValue < range[0]) {
      newValue = range[0];
    }
    setSliderInputState((prevState) => ({
      ...prevState,
      [`${sliderName}Max`]: blur ? newValue : value,
    }));
    sliderFn(sliderName, [range[0], newValue]);
  };

  return (
    <div
      style={{
        background: 'white',
        border: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
        borderRadius: '2px',
        paddingBottom: '4rem',
        position: 'absolute',
        top: '100%',
        width: '15rem',
        zIndex: 2,
      }}
    >
      <div
        style={{
          height: '20rem',
          overflow: 'auto',
          padding: '0.5rem',
        }}
      >
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>Gene</span>
          <Clickable
            style={{ float: 'right' }}
            onClick={() => toggleExpanded('geneList')}
          >
            <ClrIcon
              shape='angle'
              dir={expanded.includes('geneList') ? 'down' : 'right'}
            />
          </Clickable>
          {expanded.includes('geneList') && (
            <div>
              {filters.geneList?.map((checkboxName, index) => (
                <div key={index} style={{ display: 'flex' }}>
                  <input
                    style={{ marginRight: '0.25rem' }}
                    type='checkbox'
                    name={checkboxName}
                    checked={formState.geneList.includes(checkboxName)}
                    onChange={(e) =>
                      checkboxFn('geneList', checkboxName, e.target.checked)
                    }
                  />
                  {checkboxName}
                </div>
              ))}
            </div>
          )}
        </div>
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>Consequence</span>
          <Clickable
            style={{ float: 'right' }}
            onClick={() => toggleExpanded('consequenceList')}
          >
            <ClrIcon
              shape='angle'
              dir={expanded.includes('consequenceList') ? 'down' : 'right'}
            />
          </Clickable>
          {expanded.includes('consequenceList') && (
            <div>
              {filters.consequenceList?.map((checkboxName, index) => (
                <div key={index} style={{ display: 'flex' }}>
                  <input
                    style={{ marginRight: '0.25rem' }}
                    type='checkbox'
                    name={checkboxName}
                    checked={formState.consequenceList.includes(checkboxName)}
                    onChange={(e) =>
                      checkboxFn(
                        'consequenceList',
                        checkboxName,
                        e.target.checked
                      )
                    }
                  />
                  {checkboxName}
                </div>
              ))}
            </div>
          )}
        </div>
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>ClinVar Significance</span>
          <Clickable
            style={{ float: 'right' }}
            onClick={() => toggleExpanded('clinicalSignificanceList')}
          >
            <ClrIcon
              shape='angle'
              dir={
                expanded.includes('clinicalSignificanceList') ? 'down' : 'right'
              }
            />
          </Clickable>
          {expanded.includes('clinicalSignificanceList') && (
            <div>
              {filters.clinicalSignificanceList?.map((checkboxName, index) => (
                <div key={index} style={{ display: 'flex' }}>
                  <input
                    style={{ marginRight: '0.25rem' }}
                    type='checkbox'
                    name={checkboxName}
                    checked={formState.clinicalSignificanceList.includes(
                      checkboxName
                    )}
                    onChange={(e) =>
                      checkboxFn(
                        'clinicalSignificanceList',
                        checkboxName,
                        e.target.checked
                      )
                    }
                  />
                  {checkboxName}
                </div>
              ))}
            </div>
          )}
        </div>
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>Allele Count</span>
          <Clickable
            style={{ float: 'right' }}
            onClick={() => toggleExpanded('alleleCount')}
          >
            <ClrIcon
              shape='angle'
              dir={expanded.includes('alleleCount') ? 'down' : 'right'}
            />
          </Clickable>
          {expanded.includes('alleleCount') && (
            <>
              <div style={{ marginBottom: '1rem' }}>
                <input
                  style={{ width: '5rem' }}
                  type='number'
                  value={sliderInputState.countMin}
                  min={filters.countMin}
                  max={sliderInputState.countMax}
                  onChange={(e) =>
                    handleMinInputChange(
                      'count',
                      [filters.countMin, sliderInputState.countMax],
                      e.target.value
                    )
                  }
                  onBlur={(e) =>
                    handleMinInputChange(
                      'count',
                      [filters.countMin, sliderInputState.countMax],
                      e.target.value,
                      true
                    )
                  }
                />
                <input
                  style={{ float: 'right', width: '5rem' }}
                  type='number'
                  value={sliderInputState.countMax}
                  min={sliderInputState.countMin}
                  max={filters.countMax}
                  onChange={(e) =>
                    handleMaxInputChange(
                      'count',
                      [sliderInputState.countMin, filters.countMax],
                      e.target.value
                    )
                  }
                  onBlur={(e) =>
                    handleMaxInputChange(
                      'count',
                      [sliderInputState.countMin, filters.countMax],
                      e.target.value,
                      true
                    )
                  }
                />
              </div>
              <div style={{ height: '2rem', margin: 'auto', width: '85%' }}>
                <Nouislider
                  behaviour='drag'
                  onSlide={(value) =>
                    sliderFn(
                      'count',
                      value.map((val) => +val)
                    )
                  }
                  range={{
                    min: filters.countMin,
                    max:
                      // Prevent Nouislider slider error if min/max are the same
                      filters.countMax === filters.countMin
                        ? filters.countMax + 1
                        : filters.countMax,
                  }}
                  start={[
                    formState.countMin ?? filters.countMin,
                    formState.countMax ?? filters.countMax,
                  ]}
                  connect
                />
              </div>
            </>
          )}
        </div>
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>Allele Number</span>
          <Clickable
            style={{ float: 'right' }}
            onClick={() => toggleExpanded('alleleNumber')}
          >
            <ClrIcon
              shape='angle'
              dir={expanded.includes('alleleNumber') ? 'down' : 'right'}
            />
          </Clickable>
          {expanded.includes('alleleNumber') && (
            <>
              <div style={{ marginBottom: '1rem' }}>
                <input
                  style={{ width: '5rem' }}
                  type='number'
                  value={sliderInputState.numberMin}
                  min={filters.numberMin}
                  max={sliderInputState.numberMax}
                  onChange={(e) =>
                    handleMinInputChange(
                      'number',
                      [filters.numberMin, sliderInputState.numberMax],
                      e.target.value
                    )
                  }
                  onBlur={(e) =>
                    handleMinInputChange(
                      'number',
                      [filters.numberMin, sliderInputState.numberMax],
                      e.target.value,
                      true
                    )
                  }
                />
                <input
                  style={{ float: 'right', width: '5rem' }}
                  type='number'
                  value={sliderInputState.numberMax}
                  min={sliderInputState.numberMin}
                  max={filters.numberMax}
                  onChange={(e) =>
                    handleMaxInputChange(
                      'number',
                      [sliderInputState.numberMin, filters.numberMax],
                      e.target.value
                    )
                  }
                  onBlur={(e) =>
                    handleMaxInputChange(
                      'number',
                      [sliderInputState.numberMin, filters.numberMax],
                      e.target.value,
                      true
                    )
                  }
                />
              </div>
              <div style={{ height: '2rem', margin: 'auto', width: '85%' }}>
                <Nouislider
                  behaviour='drag'
                  onSlide={(value) =>
                    sliderFn(
                      'number',
                      value.map((val) => +val)
                    )
                  }
                  range={{
                    min: filters.numberMin,
                    max:
                      // Prevent Nouislider slider error if min/max are the same
                      filters.numberMax === filters.numberMin
                        ? filters.numberMax + 1
                        : filters.numberMax,
                  }}
                  start={[
                    formState.numberMin ?? filters.numberMin,
                    formState.numberMax ?? filters.numberMax,
                  ]}
                  connect
                />
              </div>
            </>
          )}
        </div>
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>Allele Frequency</span>
          <Clickable
            style={{ float: 'right' }}
            onClick={() => toggleExpanded('alleleFrequency')}
          >
            <ClrIcon
              shape='angle'
              dir={expanded.includes('alleleFrequency') ? 'down' : 'right'}
            />
          </Clickable>
          {expanded.includes('alleleFrequency') && (
            <>
              <div style={{ marginBottom: '1rem' }}>
                <input
                  style={{ width: '5rem' }}
                  type='number'
                  value={sliderInputState.frequencyMin}
                  min={filters.frequencyMin}
                  max={sliderInputState.frequencyMax}
                  onChange={(e) =>
                    handleMinInputChange(
                      'frequency',
                      [filters.frequencyMin, sliderInputState.frequencyMax],
                      e.target.value
                    )
                  }
                  onBlur={(e) =>
                    handleMinInputChange(
                      'frequency',
                      [filters.frequencyMin, sliderInputState.frequencyMax],
                      e.target.value,
                      true
                    )
                  }
                />
                <input
                  style={{ float: 'right', width: '5rem' }}
                  type='number'
                  value={sliderInputState.frequencyMax}
                  min={sliderInputState.frequencyMin}
                  max={filters.frequencyMax}
                  onChange={(e) =>
                    handleMaxInputChange(
                      'frequency',
                      [sliderInputState.frequencyMin, filters.frequencyMax],
                      e.target.value
                    )
                  }
                  onBlur={(e) =>
                    handleMaxInputChange(
                      'frequency',
                      [sliderInputState.frequencyMin, filters.frequencyMax],
                      e.target.value,
                      true
                    )
                  }
                />
              </div>
              <div style={{ height: '2rem', margin: 'auto', width: '85%' }}>
                <Nouislider
                  behaviour='drag'
                  onSlide={(value) =>
                    sliderFn(
                      'frequency',
                      value.map((val) => +val)
                    )
                  }
                  range={{
                    min: filters.frequencyMin,
                    max:
                      // Prevent Nouislider slider error if min/max are the same
                      filters.frequencyMax === filters.frequencyMin
                        ? filters.frequencyMax + 1
                        : filters.frequencyMax,
                  }}
                  start={[
                    formState.frequencyMin ?? filters.frequencyMin,
                    formState.frequencyMax ?? filters.frequencyMax,
                  ]}
                  connect
                />
              </div>
            </>
          )}
        </div>
        <div style={{ color: colors.primary, fontSize: '12px' }}>
          <span style={{ fontWeight: 500 }}>Sort by</span>
          <Dropdown
            style={{ width: '100%' }}
            value={formState.sortBy}
            options={filters.sortByList.map((option) => ({
              label: option,
              value: option,
            }))}
            onChange={(e) => sortFn(e.value)}
          />
        </div>
      </div>
      <div style={{ position: 'absolute', bottom: '0.5rem' }}>
        <Button
          type='secondary'
          style={{ marginLeft: '0.75rem' }}
          onClick={() => clearFn()}
        >
          Clear
        </Button>
        <Button style={{ marginLeft: '0.75rem' }} onClick={() => submitFn()}>
          Apply
        </Button>
      </div>
    </div>
  );
};
