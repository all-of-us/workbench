package org.pmiops.workbench.db.model;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "user_recent_workspace")
public class UserRecentWorkspace {
    private Timestamp lastAccessDate;
    private long id;
    private Long userId;
    private Long workspaceId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    @Column(name = "lastAccessDate")
    public Timestamp getLastAccessDate() {
        return lastAccessDate;
    }

    public void setLastAccessDate(Timestamp lastAccessDate) {
        this.lastAccessDate = lastAccessDate;
    }

    public UserRecentWorkspace() {}

    public UserRecentWorkspace(
            long workspaceId, long userId, Timestamp lastAccessDate) {
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.lastAccessDate = lastAccessDate;
    }
}
