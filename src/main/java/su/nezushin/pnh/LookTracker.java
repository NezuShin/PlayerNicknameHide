package su.nezushin.pnh;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * Shows the nametag of the single player a viewer is looking at.
 */
public final class LookTracker implements Runnable {

    private static final double RANGE = 40.0;

    private final JavaPlugin plugin;
    private final NametagTeams teams;
    private BukkitTask task;

    public LookTracker(JavaPlugin plugin, NametagTeams teams) {
        this.plugin = plugin;
        this.teams = teams;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, 2L, 2L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void run() {
        var online = plugin.getServer().getOnlinePlayers();
        for (Player viewer : online) {
            teams.setShown(viewer, findLookedAt(viewer, online));
        }
    }

    private static Player findLookedAt(Player viewer, Collection<? extends Player> online) {
        Location eye = viewer.getEyeLocation();
        Vector start = eye.toVector();
        Vector direction = eye.getDirection();
        if (!anyoneOnCrosshair(viewer, online, start, direction)) {
            return null;
        }
        RayTraceResult hit = viewer.getWorld().rayTrace(
                eye,
                direction,
                RANGE,
                FluidCollisionMode.NEVER,
                true,
                0.0,
                entity -> isOtherPlayer(viewer, entity)
        );
        if (hit != null && hit.getHitEntity() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static boolean anyoneOnCrosshair(Player viewer, Collection<? extends Player> online, Vector start, Vector direction) {
        for (Player other : online) {
            if (other == viewer || other.getWorld() != viewer.getWorld()) {
                continue;
            }
            if (other.getBoundingBox().rayTrace(start, direction, RANGE) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOtherPlayer(Player viewer, Entity entity) {
        return entity instanceof Player player && !player.getUniqueId().equals(viewer.getUniqueId());
    }
}
