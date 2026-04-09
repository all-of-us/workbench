import { useState } from 'react';

import { Button } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import { withCurrentWorkspace } from 'app/utils';
import { WorkspaceData } from 'app/utils/workspace-data';

interface Props {
  workspace: WorkspaceData;
}

const pods = ['Pod A', 'Pod B', 'Pod C'];

export const MigrationPage = withCurrentWorkspace()(({ workspace }: Props) => {
  const [selectedPod, setSelectedPod] = useState('');

  if (!workspace) return null;

  return (
    <div style={{ padding: '' }}>
      {/* 🔶 TOP BANNER */}
      <div
        style={{
          background: '#FBE3C2',
          padding: '12px 16px',
          borderRadius: '6px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '20px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <ClrIcon shape='exclamation-triangle' size={18} />
          <div style={{ marginLeft: '10px', fontSize: '14px' }}>
            <strong>Important</strong> — For migration to be successful you need
            to agree to the terms of service in Verily Workbench before starting
            migration.
          </div>
        </div>

        <div
          style={{
            color: '#2F2C7A',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          Open Verily Workbench ✕
        </div>
      </div>

      {/* 📦 READY CARD */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          marginBottom: '20px',
        }}
      >
        <div
          style={{
            border: '1px solid #D3DAE6',
            borderRadius: '8px',
            padding: '16px',
            width: '60%',
            background: '#F7F9FC',
          }}
        >
          <h3 style={{ marginBottom: '8px' }}>Ready to migrate?</h3>

          <div style={{ fontSize: '14px', lineHeight: '20px' }}>
            Once you start migration you will no longer be able to access this
            workspace on AoU Researcher workbench.
          </div>

          <div style={{ marginTop: '12px', fontSize: '14px' }}>
            <strong>What will be migrated:</strong>
            <div>• Your workspace and files in your cloud bucket</div>

            <div style={{ marginTop: '8px' }}>
              <strong>What won’t be migrated:</strong>
              <div>• Any files on your persistent disks</div>
            </div>
          </div>
        </div>

        {/* Support Button */}
        <div>
          <Button variant='secondary'>Open user support hub</Button>
        </div>
      </div>

      {/* 🧾 WORKSPACE ROW */}
      <div
        style={{
          border: '1px solid #D3DAE6',
          borderRadius: '8px',
          padding: '16px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        {/* Left Info */}
        <div>
          <div style={{ fontWeight: 600 }}>{workspace.terraName}</div>
          <div style={{ fontSize: '13px', color: '#6B7280' }}>
            Controlled Tier Dataset V8
          </div>
          <div style={{ fontSize: '12px', marginTop: '4px' }}>
            Last changed: —
          </div>
        </div>

        {/* Middle Pod Select */}
        <div style={{ width: '300px' }}>
          <select
            value={selectedPod}
            onChange={(e) => setSelectedPod(e.target.value)}
            style={{
              width: '100%',
              padding: '8px',
              borderRadius: '6px',
              border: '1px solid #D3DAE6',
            }}
          >
            <option value=''>Select a pod</option>
            {pods.map((pod) => (
              <option key={pod} value={pod}>
                {pod}
              </option>
            ))}
          </select>
        </div>

        {/* CTA */}
        <Button disabled={!selectedPod}>Start migration</Button>
      </div>
    </div>
  );
});
