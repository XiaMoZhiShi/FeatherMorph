package xiamomc.morph.backends.server.renderer.network.listeners;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import io.papermc.paper.util.TickThread;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.MorphPlugin;
import xiamomc.morph.MorphPluginObject;
import xiamomc.morph.backends.server.renderer.network.PacketFactory;
import xiamomc.morph.config.ConfigOption;
import xiamomc.morph.config.MorphConfigManager;
import xiamomc.morph.utilities.NmsUtils;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.Optional;
import java.util.function.Function;

public abstract class ProtocolListener extends MorphPluginObject implements PacketListener
{
    @Resolved(shouldSolveImmediately = true)
    private PacketFactory packetFactory;

    public abstract String getIdentifier();

    protected PacketFactory getFactory() { return packetFactory; }

    protected ProtocolManager protocolManager()
    {
        return ProtocolLibrary.getProtocolManager();
    }

    @Override
    public org.bukkit.plugin.Plugin getPlugin()
    {
        return MorphPlugin.getInstance();
    }

    private final Bindable<Boolean> debugOutput = new Bindable<>(false);

    protected boolean isDebugEnabled()
    {
        return debugOutput.get();
    }

    @Initializer
    private void load(MorphConfigManager configManager)
    {
        configManager.bind(debugOutput, ConfigOption.DEBUG_OUTPUT);
    }

    @Nullable
    protected Player getNmsPlayerEntityFrom(PacketEvent event, int id)
    {
        //if (!TickThread.isTickThread())
        //    logger.warn("Not on a tick thread! Caution for exceptions!");

        return Bukkit.getOnlinePlayers()
                .stream()
                .filter(p -> p.getEntityId() == id)
                .map(bukkit -> ((CraftPlayer)bukkit).getHandle())
                .map(Optional::ofNullable)
                .findFirst().flatMap(Function.identity())
                .orElse(null);
    }

    @Nullable
    protected Entity getNmsEntityFrom(PacketEvent event, int id)
    {
        var packetTarget = event.getPlayer();
        var sourceNmsEntity = NmsUtils.getNmsLevel(packetTarget.getWorld()).getEntity(id);
        if (sourceNmsEntity == null)
        {
            if (debugOutput.get())
            {
                logger.warn("A packet from a player that doesn't exist in its world?!");
                logger.warn("Packet: " + event.getPacketType());
            }

            return null;
        }

        return sourceNmsEntity;
    }
}
