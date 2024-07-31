import '@testing-library/jest-dom';

import React from 'react';

import { Autopilot } from 'generated/fetch';

import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { AutopilotMachineSelector } from './autopilot-machine-selector';

describe('AutopilotMachineSelector', () => {
  const mockOnChange = jest.fn();
  const selectedMachine: Autopilot = {
    cpuInMillicores: 2000,
    memoryInGb: 8,
  };

  beforeEach(() => {
    mockOnChange.mockClear();
  });

  it('renders CPU and memory input fields with correct initial values', () => {
    render(
      <AutopilotMachineSelector
        selectedMachine={selectedMachine}
        idPrefix='test'
        onChange={mockOnChange}
        disabled={false}
      />
    );

    const cpuInput = screen.getByRole('spinbutton', { name: 'CPUs' });
    const memoryInput = screen.getByRole('spinbutton', { name: 'RAM (GB)' });
    expect(cpuInput).toHaveValue('2');
    expect(memoryInput).toHaveValue('8');
  });

  it('calls onChange with correct values when CPU input is changed', async () => {
    render(
      <AutopilotMachineSelector
        selectedMachine={selectedMachine}
        idPrefix='test'
        onChange={mockOnChange}
        disabled={false}
      />
    );

    const cpuInput = screen.getByRole('spinbutton', { name: 'CPUs' });
    fireEvent.change(cpuInput, { target: { value: '4' } });
    fireEvent.blur(cpuInput);

    expect(cpuInput).toHaveValue('4');
    await waitFor(() =>
      expect(mockOnChange).toHaveBeenCalledWith({
        ...selectedMachine,
        cpuInMillicores: 4000,
        memoryInGb: 8,
      })
    );
  });

  it('calls onChange with correct values when memory input is changed', () => {
    render(
      <AutopilotMachineSelector
        selectedMachine={selectedMachine}
        idPrefix='test'
        onChange={mockOnChange}
        disabled={false}
      />
    );

    const memoryInput = screen.getByRole('spinbutton', { name: 'RAM (GB)' });
    fireEvent.change(memoryInput, { target: { value: 12 } });
    fireEvent.blur(memoryInput);

    expect(mockOnChange).toHaveBeenCalledWith({
      ...selectedMachine,
      cpuInMillicores: 2000,
      memoryInGb: 12,
    });
  });

  it('auto-adjusts memory when CPU input is changed to maintain ratio constraints', () => {
    render(
      <AutopilotMachineSelector
        selectedMachine={selectedMachine}
        idPrefix='test'
        onChange={mockOnChange}
        disabled={false}
      />
    );

    const cpuInput = screen.getByRole('spinbutton', { name: 'CPUs' });
    fireEvent.change(cpuInput, { target: { value: 0.5 } });
    fireEvent.blur(cpuInput);

    expect(mockOnChange).toHaveBeenCalledWith({
      ...selectedMachine,
      cpuInMillicores: 500,
      memoryInGb: 3.25,
    });
  });

  it('auto-adjusts CPU when memory input is changed to maintain ratio constraints', () => {
    render(
      <AutopilotMachineSelector
        selectedMachine={selectedMachine}
        idPrefix='test'
        onChange={mockOnChange}
        disabled={false}
      />
    );

    const memoryInput = screen.getByRole('spinbutton', { name: 'RAM (GB)' });
    fireEvent.change(memoryInput, { target: { value: 65 } });
    fireEvent.blur(memoryInput);

    expect(mockOnChange).toHaveBeenCalledWith({
      ...selectedMachine,
      cpuInMillicores: 10000,
      memoryInGb: 65,
    });
  });
});
