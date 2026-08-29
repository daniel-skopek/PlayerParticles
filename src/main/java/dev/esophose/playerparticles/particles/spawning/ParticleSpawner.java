package dev.esophose.playerparticles.particles.spawning;

import dev.esophose.playerparticles.PlayerParticles;
import dev.esophose.playerparticles.config.Settings;
import dev.esophose.playerparticles.manager.ParticleManager;
import dev.esophose.playerparticles.particles.PPlayer;
import dev.esophose.playerparticles.particles.ParticleEffect;
import dev.esophose.playerparticles.particles.data.ColorTransition;
import dev.esophose.playerparticles.particles.data.ParticleColor;
import dev.esophose.playerparticles.particles.data.Vibration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public abstract class ParticleSpawner {

    /**
     * Displays a particle effect
     *
     * @param particleEffect The particle type to display
     * @param offsetX Maximum distance particles can fly away from the center on the x-axis
     * @param offsetY Maximum distance particles can fly away from the center on the y-axis
     * @param offsetZ Maximum distance particles can fly away from the center on the z-axis
     * @param speed Display speed of the particles
     * @param amount Amount of particles
     * @param center Center location of the effect
     * @param isLongRange If the particle can be viewed from long range
     * @param owner The player that owns the particles
     * @throws ParticleDataException If the particle effect requires additional data
     */
    public abstract void display(ParticleEffect particleEffect, double offsetX, double offsetY, double offsetZ, double speed, int amount, Location center, boolean isLongRange, Player owner);

    /**
     * Displays a single particle which is colored
     *
     * @param particleEffect The particle type to display
     * @param color Color of the particle
     * @param center Center location of the effect
     * @param isLongRange If the particle can be viewed from long range
     * @param owner The player that owns the particles
     * @param size The size of the particle
     * @throws ParticleColorException If the particle effect is not colorable or the color type is incorrect
     */
    public abstract void display(ParticleEffect particleEffect, ParticleColor color, Location center, boolean isLongRange, Player owner, float size);

    /**
     * Displays a particle effect which requires additional data and is only
     * visible for all players within a certain range in the world of @param
     * center
     *
     * @param particleEffect The particle type to display
     * @param spawnMaterial Material of the effect
     * @param offsetX Maximum distance particles can fly away from the center on the x-axis
     * @param offsetY Maximum distance particles can fly away from the center on the y-axis
     * @param offsetZ Maximum distance particles can fly away from the center on the z-axis
     * @param speed Display speed of the particles
     * @param amount Amount of particles
     * @param center Center location of the effect
     * @param isLongRange If the particle can be viewed from long range
     * @param owner The player that owns the particles
     * @throws ParticleDataException If the particle effect does not require additional data or if the data type is incorrect
     */
    public abstract void display(ParticleEffect particleEffect, Material spawnMaterial, double offsetX, double offsetY, double offsetZ, double speed, int amount, Location center, boolean isLongRange, Player owner);

    /**
     * Displays a particle effect which requires additional data and is only
     * visible for all players within a certain range in the world of @param
     * center
     *
     * @param particleEffect The particle type to display
     * @param colorTransition Color transition of the effect
     * @param offsetX Maximum distance particles can fly away from the center on the x-axis
     * @param offsetY Maximum distance particles can fly away from the center on the y-axis
     * @param offsetZ Maximum distance particles can fly away from the center on the z-axis
     * @param amount Amount of particles
     * @param center Center location of the effect
     * @param isLongRange If the particle can be viewed from long range
     * @param owner The player that owns the particles
     * @param size The size of the particles
     * @throws ParticleDataException If the particle effect does not require additional data or if the data type is incorrect
     */
    public abstract void display(ParticleEffect particleEffect, ColorTransition colorTransition, double offsetX, double offsetY, double offsetZ, int amount, Location center, boolean isLongRange, Player owner, float size);

    /**
     * Displays a particle effect which requires additional data and is only
     * visible for all players within a certain range in the world of @param
     * center
     *
     * @param particleEffect The particle type to display
     * @param vibration Vibration of the effect
     * @param offsetX Maximum distance particles can fly away from the center on the x-axis
     * @param offsetY Maximum distance particles can fly away from the center on the y-axis
     * @param offsetZ Maximum distance particles can fly away from the center on the z-axis
     * @param amount Amount of particles
     * @param center Center location of the effect
     * @param isLongRange If the particle can be viewed from long range
     * @param owner The player that owns the particles
     * @throws ParticleDataException If the particle effect does not require additional data or if the data type is incorrect
     */
    public abstract void display(ParticleEffect particleEffect, Vibration vibration, double offsetX, double offsetY, double offsetZ, int amount, Location center, boolean isLongRange, Player owner);

    /**
     * Gets a List of Players within the particle display range
     *
     * @param center The center of the radius to check around
     * @param isLongRange If the particle can be viewed from long range
     * @param owner The player that owns the particles
     * @return A List of Players within the particle display range
     */
    public static List<Player> getPlayersInRange(Location center, boolean isLongRange, Player owner) {
        ViewerSnapshot snapshot = getViewerSnapshot();
        List<Viewer> viewers = snapshot.byWorld.get(center.getWorld());
        if (viewers == null || viewers.isEmpty())
            return Collections.emptyList();

        double range = isLongRange ? snapshot.fixedRangeSq : snapshot.playerRangeSq;
        double centerX = center.getX();
        double centerY = center.getY();
        double centerZ = center.getZ();

        List<Player> players = new ArrayList<>(viewers.size());
        for (Viewer viewer : viewers) {
            Player player = viewer.player;
            if (!canSee(player, owner))
                continue;
            if (!viewer.pplayer.canSeeOwnParticles() && player == owner && !isLongRange)
                continue;
            if (!viewer.pplayer.canSeeParticles())
                continue;

            double dx = centerX - viewer.x;
            double dy = centerY - viewer.y;
            double dz = centerZ - viewer.z;
            if (dx * dx + dy * dy + dz * dz <= range)
                players.add(player);
        }

        return players;
    }

    /**
     * Gets the current tick's viewer snapshot, rebuilding it if it has become stale.
     * Positions are only sampled once per tick per thread instead of once per particle.
     *
     * @return The current ViewerSnapshot
     */
    private static ViewerSnapshot getViewerSnapshot() {
        ViewerSnapshot snapshot = VIEWER_SNAPSHOT.get();
        long now = System.currentTimeMillis();
        if (snapshot == null || now - snapshot.timestamp >= SNAPSHOT_TTL_MS) {
            snapshot = ViewerSnapshot.create();
            VIEWER_SNAPSHOT.set(snapshot);
        }
        return snapshot;
    }

    private static final long SNAPSHOT_TTL_MS = 45L;
    private static final ThreadLocal<ViewerSnapshot> VIEWER_SNAPSHOT = new ThreadLocal<>();

    /**
     * A snapshot of all online players and their positions, grouped by world.
     */
    private static final class ViewerSnapshot {

        private final Map<World, List<Viewer>> byWorld;
        private final long timestamp;
        private final double playerRangeSq;
        private final double fixedRangeSq;

        private ViewerSnapshot(Map<World, List<Viewer>> byWorld, long timestamp, double playerRangeSq, double fixedRangeSq) {
            this.byWorld = byWorld;
            this.timestamp = timestamp;
            this.playerRangeSq = playerRangeSq;
            this.fixedRangeSq = fixedRangeSq;
        }

        private static ViewerSnapshot create() {
            Map<World, List<Viewer>> byWorld = new HashMap<>();
            Map<UUID, PPlayer> pplayers = PlayerParticles.getInstance().getManager(ParticleManager.class).getPPlayers();

            for (Player player : Bukkit.getOnlinePlayers()) {
                PPlayer pplayer = pplayers.get(player.getUniqueId());
                if (pplayer == null)
                    continue;

                Location location = player.getLocation();
                World world = location.getWorld();
                if (world == null)
                    continue;

                byWorld.computeIfAbsent(world, k -> new ArrayList<>()).add(new Viewer(player, pplayer, location.getX(), location.getY(), location.getZ()));
            }

            double playerRange = Settings.PARTICLE_RENDER_RANGE_PLAYER.get();
            double fixedRange = Settings.PARTICLE_RENDER_RANGE_FIXED_EFFECT.get();
            return new ViewerSnapshot(byWorld, System.currentTimeMillis(), playerRange * playerRange, fixedRange * fixedRange);
        }

    }

    /**
     * A cached viewer holding an online player, their PPlayer, and their sampled position.
     */
    private static final class Viewer {

        private final Player player;
        private final PPlayer pplayer;
        private final double x, y, z;

        private Viewer(Player player, PPlayer pplayer, double x, double y, double z) {
            this.player = player;
            this.pplayer = pplayer;
            this.x = x;
            this.y = y;
            this.z = z;
        }

    }

    /**
     * Checks if a player can see another player
     *
     * @param player The player
     * @param target The target
     * @return True if player can see target, otherwise false
     */
    public static boolean canSee(Player player, Player target) {
        if (player == null || target == null)
            return true;

        return player.canSee(target);
    }

    /**
     * Represents a runtime exception that is thrown either if the displayed
     * particle effect requires data and has none or vice-versa or if the data
     * type is incorrect
     * <p>
     * This class is part of the <b>ParticleEffect Library</b> and follows the
     * same usage conditions
     *
     * @author DarkBlade12
     * @since 1.6
     */
    public static final class ParticleDataException extends RuntimeException {
        private static final long serialVersionUID = 3203085387160737484L;

        /**
         * Construct a new particle data exception
         *
         * @param message Message that will be logged
         */
        public ParticleDataException(String message) {
            super(message);
        }
    }

    /**
     * Represents a runtime exception that is thrown either if the displayed
     * particle effect is not colorable or if the particle color type is
     * incorrect
     * <p>
     * This class is part of the <b>ParticleEffect Library</b> and follows the
     * same usage conditions
     *
     * @author DarkBlade12
     * @since 1.7
     */
    public static final class ParticleColorException extends RuntimeException {
        private static final long serialVersionUID = 3203085387160737485L;

        /**
         * Construct a new particle color exception
         *
         * @param message Message that will be logged
         */
        public ParticleColorException(String message) {
            super(message);
        }
    }

}
