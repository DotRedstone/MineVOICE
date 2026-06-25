package dev.minevoice.neoforge.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VoiceGroupManager {
    private static final int MAX_GROUP_NAME_LENGTH = 32;
    private static final int MAX_GROUP_PASSWORD_LENGTH = 64;

    private final Map<UUID, MutableVoiceGroup> groups = new LinkedHashMap<>();
    private final Map<UUID, UUID> playerGroups = new LinkedHashMap<>();

    public VoiceGroup create(UUID ownerId, String requestedName, String requestedPassword) {
        String name = normalizeName(requestedName);
        String password = normalizePassword(requestedPassword);
        leave(ownerId);
        UUID groupId = UUID.randomUUID();
        MutableVoiceGroup group = new MutableVoiceGroup(groupId, name, ownerId, password);
        group.members.add(ownerId);
        groups.put(groupId, group);
        playerGroups.put(ownerId, groupId);
        return group.snapshot();
    }

    public boolean join(UUID playerId, UUID groupId, String requestedPassword) {
        MutableVoiceGroup group = groups.get(groupId);
        if (group == null) {
            return false;
        }
        String password = normalizePassword(requestedPassword);
        if (!group.password.isEmpty() && !group.password.equals(password)) {
            throw new IllegalArgumentException("group password is incorrect");
        }
        leave(playerId);
        group.members.add(playerId);
        playerGroups.put(playerId, groupId);
        return true;
    }

    public boolean leave(UUID playerId) {
        UUID groupId = playerGroups.remove(playerId);
        if (groupId == null) {
            return false;
        }
        MutableVoiceGroup group = groups.get(groupId);
        if (group == null) {
            return true;
        }
        group.members.remove(playerId);
        if (group.members.isEmpty()) {
            groups.remove(groupId);
        } else if (group.ownerId.equals(playerId)) {
            group.ownerId = group.members.iterator().next();
        }
        return true;
    }

    public VoiceGroup groupFor(UUID playerId) {
        UUID groupId = playerGroups.get(playerId);
        MutableVoiceGroup group = groupId == null ? null : groups.get(groupId);
        return group == null ? null : group.snapshot();
    }

    public List<VoiceGroup> groups() {
        return groups.values().stream().map(MutableVoiceGroup::snapshot).toList();
    }

    public void clear() {
        groups.clear();
        playerGroups.clear();
    }

    private static String normalizeName(String requestedName) {
        String normalized = requestedName == null ? "" : requestedName.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("group name cannot be empty");
        }
        if (normalized.length() > MAX_GROUP_NAME_LENGTH) {
            throw new IllegalArgumentException("group name is too long");
        }
        return normalized;
    }

    private static String normalizePassword(String requestedPassword) {
        String normalized = requestedPassword == null ? "" : requestedPassword.trim();
        if (normalized.length() > MAX_GROUP_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("group password is too long");
        }
        return normalized;
    }

    private static final class MutableVoiceGroup {
        private final UUID id;
        private final String name;
        private final String password;
        private UUID ownerId;
        private final Set<UUID> members = new LinkedHashSet<>();

        private MutableVoiceGroup(UUID id, String name, UUID ownerId, String password) {
            this.id = id;
            this.name = name;
            this.ownerId = ownerId;
            this.password = password;
        }

        private VoiceGroup snapshot() {
            return new VoiceGroup(id, name, ownerId, members, !password.isEmpty());
        }
    }
}
