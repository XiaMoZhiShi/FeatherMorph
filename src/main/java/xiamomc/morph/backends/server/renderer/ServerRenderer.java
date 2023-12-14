package xiamomc.morph.backends.server.renderer;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xiamomc.morph.MorphPluginObject;
import xiamomc.morph.backends.server.renderer.network.*;
import xiamomc.morph.backends.server.renderer.network.datawatcher.WatcherIndex;
import xiamomc.morph.backends.server.renderer.network.queue.PacketQueue;
import xiamomc.morph.backends.server.renderer.skins.SkinStore;

public class ServerRenderer extends MorphPluginObject
{
    private final ProtocolHandler protocolHandler;

    private final RenderRegistry registry = new RenderRegistry();

    private final SkinStore skinStore = new SkinStore();

    private final PacketFactory packetFactory = new PacketFactory();

    private final PacketQueue packetQueue = new PacketQueue();

    public ServerRenderer()
    {
        dependencies.cache(packetFactory);
        dependencies.cache(packetQueue);

        dependencies.cache(registry);
        dependencies.cache(skinStore);
        dependencies.cache(protocolHandler = new ProtocolHandler());
    }

    public void renderEntity(Player player, EntityType entityType, String name)
    {
        registry.register(player.getUniqueId(),
                new RegistryParameters(entityType, name, WatcherIndex.getInstance().getWatcherForType(player, entityType)));
    }

    public void unRenderEntity(Player player)
    {
        registry.unregister(player.getUniqueId());
    }

    public void dispose()
    {
        registry.reset();
        protocolHandler.dispose();
    }
}
