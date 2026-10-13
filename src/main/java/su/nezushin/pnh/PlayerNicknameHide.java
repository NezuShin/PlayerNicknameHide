package su.nezushin.pnh;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerNicknameHide extends JavaPlugin implements Listener {

    private NametagTeams teams;
    private LookTracker lookTracker;

    @Override
    public void onEnable() {
        teams = new NametagTeams();
        lookTracker = new LookTracker(this, teams);
        lookTracker.start();
        getServer().getPluginManager().registerEvents(this, this);

        for (Player player : getServer().getOnlinePlayers()) {
            teams.setup(player, getServer().getOnlinePlayers());
        }
    }

    @Override
    public void onDisable() {
        if (lookTracker != null) {
            lookTracker.stop();
        }
        if (teams != null) {
            teams.clearAll(getServer().getOnlinePlayers());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!joiner.isOnline()) {
                return;
            }
            var others = getServer().getOnlinePlayers().stream()
                    .filter(player -> !player.getUniqueId().equals(joiner.getUniqueId()))
                    .toList();
            teams.setup(joiner, others);
            for (Player viewer : others) {
                teams.addHidden(viewer, joiner);
            }
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        var remaining = getServer().getOnlinePlayers().stream()
                .filter(player -> !player.getUniqueId().equals(quitter.getUniqueId()))
                .toList();
        teams.forget(quitter, remaining);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) {
                return;
            }
            teams.reset(player, getServer().getOnlinePlayers());
        }, 1L);
    }
}
