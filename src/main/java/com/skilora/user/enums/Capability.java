package com.skilora.user.enums;

public enum Capability {
    // Dashboard (all roles)
    VIEW_DASHBOARD,

    // User management (admin)
    MANAGE_USERS,
    VIEW_REPORTS,
    VIEW_ACTIVE_OFFERS,

    // Recruitment - Client
    POST_PROJECT,
    MANAGE_OWN_OFFERS,
    VIEW_APPLICATION_INBOX,
    MANAGE_INTERVIEWS,

    // Recruitment - Freelancer
    BROWSE_FEED,
    MANAGE_APPLICATIONS,

    // Community (all roles)
    VIEW_COMMUNITY,

    // Formations
    BROWSE_FORMATIONS,
    ADMIN_FORMATIONS,
    VIEW_MENTORSHIP,

    // Finance
    VIEW_FINANCE,
    ADMIN_FINANCE,

    // Support
    VIEW_SUPPORT,
    ADMIN_SUPPORT
}
