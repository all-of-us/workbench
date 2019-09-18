package org.pmiops.workbench.db.model;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "user_recent_workspace")
public class UserRecentWorkspace {
    private Timestamp lastUpdateTime;
    private int id;
    private Long userId;
    private Long workspaceId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "user_id")
    public long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Column(name = "workspace_id")
    public long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Column(name = "lastUpdateTime")
    public Timestamp getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Timestamp lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public UserRecentWorkspace() {}

    public UserRecentWorkspace(
            long workspaceId, long userId, Timestamp lastUpdateTime) {
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.lastUpdateTime = lastUpdateTime;
    }
}
