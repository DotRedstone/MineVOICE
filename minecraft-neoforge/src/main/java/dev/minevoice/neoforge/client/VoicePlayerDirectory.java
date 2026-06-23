package dev.minevoice.neoforge.client;

import dev.minevoice.neoforge.network.VoiceRosterEntry;
import dev.minevoice.neoforge.network.VoiceRosterPayload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VoicePlayerDirectory {
    private final Map<UUID, VoiceRosterEntry> entries = new LinkedHashMap<>();

    public synchronized void update(VoiceRosterPayload roster) {
        entries.clear();
        for (VoiceRosterEntry entry : roster.entries()) {
            entries.put(entry.playerId(), entry);
        }
    }

    public synchronized VoiceRosterEntry get(UUID playerId) {
        return entries.get(playerId);
    }

    public synchronized List<VoiceRosterEntry> entries() {
        return entries.values().stream()
                .sorted(Comparator.comparing(VoiceRosterEntry::playerName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized List<VoiceRosterEntry> groupMembers(UUID groupId) {
        if (groupId == null) {
            return List.of();
        }
        return entries.values().stream()
                .filter(entry -> groupId.equals(entry.groupId()))
                .sorted(Comparator.comparing(VoiceRosterEntry::playerName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized List<VoiceGroupSummary> groups() {
        Map<UUID, VoiceGroupSummary> groups = new LinkedHashMap<>();
        for (VoiceRosterEntry entry : entries.values()) {
            if (entry.groupId() == null) {
                continue;
            }
            VoiceGroupSummary previous = groups.get(entry.groupId());
            groups.put(entry.groupId(), new VoiceGroupSummary(
                    entry.groupId(),
                    entry.groupName(),
                    previous == null ? 1 : previous.memberCount() + 1
            ));
        }
        return groups.values().stream()
                .sorted(Comparator.comparing(VoiceGroupSummary::groupName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized Collection<VoiceRosterEntry> snapshot() {
        return new ArrayList<>(entries.values());
    }
}
