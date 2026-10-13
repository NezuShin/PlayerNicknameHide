package su.nezushin.pnh;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.CollisionRule;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.NameTagVisibility;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.OptionData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.ScoreBoardTeamInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.TeamMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Two client-side teams per viewer. Membership is what differs between clients.
 */
public final class NametagTeams {

    static final String SHOW_TEAM = "pnh-show";
    static final String HIDE_TEAM = "pnh-hide";

    private final Set<UUID> ready = new HashSet<>();
    private final Map<UUID, UUID> shown = new HashMap<>();

    public void setup(Player viewer, Collection<? extends Player> others) {
        if (!ready.add(viewer.getUniqueId())) {
            return;
        }
        send(viewer, team(SHOW_TEAM, TeamMode.CREATE, NameTagVisibility.ALWAYS, List.of()));
        send(viewer, team(HIDE_TEAM, TeamMode.CREATE, NameTagVisibility.NEVER, namesOf(viewer, others)));
    }

    public void addHidden(Player viewer, Player target) {
        if (!ready.contains(viewer.getUniqueId()) || viewer.getUniqueId().equals(target.getUniqueId())) {
            return;
        }
        send(viewer, members(HIDE_TEAM, TeamMode.ADD_ENTITIES, target.getName()));
    }

    public void setShown(Player viewer, Player target) {
        if (!ready.contains(viewer.getUniqueId())) {
            return;
        }
        UUID viewerId = viewer.getUniqueId();
        UUID next = target == null ? null : target.getUniqueId();
        UUID previous = shown.get(viewerId);
        if (Objects.equals(previous, next)) {
            return;
        }

        if (previous != null) {
            Player previousPlayer = Bukkit.getPlayer(previous);
            if (previousPlayer != null) {
                move(viewer, previousPlayer.getName(), SHOW_TEAM, HIDE_TEAM);
            }
        }
        if (target != null && !target.getUniqueId().equals(viewerId)) {
            move(viewer, target.getName(), HIDE_TEAM, SHOW_TEAM);
            shown.put(viewerId, target.getUniqueId());
        } else {
            shown.remove(viewerId);
        }
    }

    public void forget(Player quitter, Collection<? extends Player> remaining) {
        UUID quitterId = quitter.getUniqueId();
        String name = quitter.getName();
        for (Player viewer : remaining) {
            if (!ready.contains(viewer.getUniqueId())) {
                continue;
            }
            UUID shownId = shown.get(viewer.getUniqueId());
            String team = quitterId.equals(shownId) ? SHOW_TEAM : HIDE_TEAM;
            send(viewer, members(team, TeamMode.REMOVE_ENTITIES, name));
            if (quitterId.equals(shownId)) {
                shown.remove(viewer.getUniqueId());
            }
        }
        ready.remove(quitterId);
        shown.remove(quitterId);
    }

    public void reset(Player viewer, Collection<? extends Player> others) {
        if (ready.remove(viewer.getUniqueId())) {
            send(viewer, removeTeam(SHOW_TEAM));
            send(viewer, removeTeam(HIDE_TEAM));
        }
        shown.remove(viewer.getUniqueId());
        setup(viewer, others);
    }

    public void clearAll(Collection<? extends Player> online) {
        for (Player viewer : online) {
            if (!ready.contains(viewer.getUniqueId())) {
                continue;
            }
            send(viewer, removeTeam(SHOW_TEAM));
            send(viewer, removeTeam(HIDE_TEAM));
        }
        ready.clear();
        shown.clear();
    }

    private void move(Player viewer, String name, String from, String to) {
        send(viewer, members(from, TeamMode.REMOVE_ENTITIES, name));
        send(viewer, members(to, TeamMode.ADD_ENTITIES, name));
    }

    private static List<String> namesOf(Player viewer, Collection<? extends Player> others) {
        return others.stream()
                .filter(player -> !player.getUniqueId().equals(viewer.getUniqueId()))
                .map(Player::getName)
                .toList();
    }

    private static WrapperPlayServerTeams team(String name, TeamMode mode, NameTagVisibility visibility, Collection<String> players) {
        ScoreBoardTeamInfo info = new ScoreBoardTeamInfo(
                Component.text(name),
                Component.empty(),
                Component.empty(),
                visibility,
                CollisionRule.ALWAYS,
                NamedTextColor.WHITE,
                OptionData.NONE
        );
        return new WrapperPlayServerTeams(name, mode, info, players);
    }

    private static WrapperPlayServerTeams members(String name, TeamMode mode, String playerName) {
        return new WrapperPlayServerTeams(name, mode, (ScoreBoardTeamInfo) null, playerName);
    }

    private static WrapperPlayServerTeams removeTeam(String name) {
        return new WrapperPlayServerTeams(name, TeamMode.REMOVE, (ScoreBoardTeamInfo) null, List.of());
    }

    private static void send(Player viewer, WrapperPlayServerTeams packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }
}
