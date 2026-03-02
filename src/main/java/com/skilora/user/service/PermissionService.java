package com.skilora.user.service;

import com.skilora.user.enums.Capability;
import com.skilora.user.enums.Role;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class PermissionService {
    private static final PermissionService INSTANCE = new PermissionService();
    private final Map<Role, Set<Capability>> permissions;

    private PermissionService() {
        EnumMap<Role, Set<Capability>> map = new EnumMap<>(Role.class);

        map.put(Role.ADMIN, EnumSet.of(
            Capability.VIEW_DASHBOARD,
            Capability.MANAGE_USERS,
            Capability.VIEW_REPORTS,
            Capability.VIEW_ACTIVE_OFFERS,
            Capability.VIEW_COMMUNITY,
            Capability.ADMIN_FORMATIONS,
            Capability.VIEW_FINANCE,
            Capability.ADMIN_FINANCE,
            Capability.ADMIN_SUPPORT
        ));

        map.put(Role.EMPLOYER, EnumSet.of(
            Capability.VIEW_DASHBOARD,
            Capability.POST_PROJECT,
            Capability.MANAGE_OWN_OFFERS,
            Capability.VIEW_APPLICATION_INBOX,
            Capability.MANAGE_INTERVIEWS,
            Capability.VIEW_COMMUNITY,
            Capability.VIEW_FINANCE,
            Capability.VIEW_SUPPORT
        ));

        map.put(Role.USER, EnumSet.of(
            Capability.VIEW_DASHBOARD,
            Capability.BROWSE_FEED,
            Capability.MANAGE_APPLICATIONS,
            Capability.VIEW_COMMUNITY,
            Capability.BROWSE_FORMATIONS,
            Capability.VIEW_FINANCE,
            Capability.VIEW_SUPPORT
        ));

        map.put(Role.TRAINER, EnumSet.of(
            Capability.VIEW_DASHBOARD,
            Capability.ADMIN_FORMATIONS,
            Capability.VIEW_MENTORSHIP,
            Capability.VIEW_COMMUNITY,
            Capability.VIEW_SUPPORT
        ));

        permissions = Collections.unmodifiableMap(map);
    }

    public static PermissionService getInstance() { return INSTANCE; }

    public boolean can(Role role, Capability capability) {
        Set<Capability> caps = permissions.get(role);
        return caps != null && caps.contains(capability);
    }

    public Set<Capability> getCapabilities(Role role) {
        return permissions.getOrDefault(role, EnumSet.noneOf(Capability.class));
    }
}
