package com.github.accessreport.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a GitHub collaborator returned by the GitHub API.
 * The 'permissions' object is nested in the API response.
 */
public class CollaboratorInfo {

    private String login;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    /** Nested permissions object from GitHub API response. */
    private Permissions permissions;

    public CollaboratorInfo() {}

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Permissions getPermissions() {
        return permissions;
    }

    public void setPermissions(Permissions permissions) {
        this.permissions = permissions;
    }

    /**
     * Derives the highest effective permission level from the permissions flags.
     * GitHub returns boolean flags; we derive human-readable level.
     */
    public String getHighestPermission() {
        if (permissions == null) return "read";
        if (permissions.isAdmin()) return "admin";
        if (permissions.isMaintain()) return "maintain";
        if (permissions.isPush()) return "write";
        if (permissions.isTriage()) return "triage";
        return "read";
    }

    /**
     * Nested permissions object matching GitHub API response structure.
     */
    public static class Permissions {
        private boolean admin;
        private boolean push;
        private boolean pull;
        private boolean maintain;
        private boolean triage;

        public boolean isAdmin() { return admin; }
        public void setAdmin(boolean admin) { this.admin = admin; }
        public boolean isPush() { return push; }
        public void setPush(boolean push) { this.push = push; }
        public boolean isPull() { return pull; }
        public void setPull(boolean pull) { this.pull = pull; }
        public boolean isMaintain() { return maintain; }
        public void setMaintain(boolean maintain) { this.maintain = maintain; }
        public boolean isTriage() { return triage; }
        public void setTriage(boolean triage) { this.triage = triage; }
    }
}
