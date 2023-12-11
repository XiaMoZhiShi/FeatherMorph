package xiamomc.morph.backends.server;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xiamomc.morph.backends.DisguiseBackend;
import xiamomc.morph.backends.DisguiseWrapper;
import xiamomc.morph.backends.server.renderer.ServerRenderer;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

public class ServerBackend extends DisguiseBackend<ServerDisguise, ServerDisguiseWrapper>
{
    @Nullable
    public static ServerBackend getInstance()
    {
        return instance;
    }

    private static ServerBackend instance;

    private final ServerRenderer serverRenderer;

    public ServerBackend()
    {
        instance = this;
        serverRenderer = new ServerRenderer();
    }

    @Override
    public void dispose()
    {
        serverRenderer.dispose();
    }

    /**
     * Gets the identifier of this backend.
     *
     * @return An identifier of this backend.
     */
    @Override
    public String getIdentifier()
    {
        return "server";
    }

    /**
     * Creates a disguise from the giving entity
     *
     * @param targetEntity The entity used to construct disguise
     * @return A wrapper that handles the constructed disguise
     */
    @Override
    public DisguiseWrapper<ServerDisguise> createInstance(@NotNull Entity targetEntity)
    {
        var wrapper = new ServerDisguiseWrapper(new ServerDisguise(targetEntity.getType()), this);
        wrapper.setDisguiseName(targetEntity.getName());

        return wrapper;
    }

    /**
     * Creates a disguise by the giving type
     *
     * @param entityType Target entity type
     * @return A wrapper that handles the constructed disguise
     */
    @Override
    public DisguiseWrapper<ServerDisguise> createInstance(EntityType entityType)
    {
        return new ServerDisguiseWrapper(new ServerDisguise(entityType), this);
    }

    /**
     * Creates a player disguise by the giving name
     *
     * @param targetPlayerName Target player name
     * @return A wrapper that handles the constructed disguise
     */
    @Override
    public DisguiseWrapper<ServerDisguise> createPlayerInstance(String targetPlayerName)
    {
        var wrapper = new ServerDisguiseWrapper(new ServerDisguise(EntityType.PLAYER), this);
        wrapper.setDisguiseName(targetPlayerName);

        return wrapper;
    }

    /**
     * Creates a disguise instance directly from the entity
     *
     * @param entity The entity used to construct disguise
     * @return The constructed instance
     */
    @Override
    public ServerDisguise createRawInstance(Entity entity)
    {
        return new ServerDisguise(entity.getType());
    }

    private final Map<UUID, ServerDisguiseWrapper> disguiseWrapperMap = new Object2ObjectOpenHashMap<>();

    /**
     * Checks whether an entity is disguised by this backend
     *
     * @param target The entity to check
     * @return Whether this entity is disguised by this backend
     */
    @Override
    public boolean isDisguised(@org.jetbrains.annotations.Nullable Entity target)
    {
        if (target == null) return false;
        return disguiseWrapperMap.containsKey(target.getUniqueId());
    }

    /**
     * Gets the wrapper that handles the target entity's disguise instance
     *
     * @param target The entity to lookup
     * @return The wrapper that handles the entity's disguise. Null if it's not disguised.
     */
    @Override
    public @Nullable ServerDisguiseWrapper getWrapper(Entity target)
    {
        return disguiseWrapperMap.getOrDefault(target.getUniqueId(), null);
    }

    /**
     * 将某一玩家伪装成给定Wrapper中的实例
     *
     * @param player  目标玩家
     * @param wrapper 目标Wrapper
     * @return 操作是否成功
     * @apiNote 传入的wrapper可能不是此后端产出的Wrapper，需要对其进行验证
     */
    @Override
    public boolean disguise(Player player, DisguiseWrapper<?> wrapper)
    {
        if (!(wrapper instanceof ServerDisguiseWrapper serverDisguiseWrapper)) return false;
        if (disguiseWrapperMap.containsKey(player.getUniqueId())) return false;

        disguiseWrapperMap.put(player.getUniqueId(), serverDisguiseWrapper);
        serverRenderer.renderEntity(player, wrapper.getEntityType(), wrapper.getDisguiseName());
        return true;
    }

    /**
     * Undisguise a player
     *
     * @param player The player to undisguise
     * @return Whether the operation was successful
     */
    @Override
    public boolean unDisguise(Player player)
    {
        serverRenderer.unRenderEntity(player);

        var uuid = player.getUniqueId();
        var wrapper = disguiseWrapperMap.getOrDefault(uuid, null);
        if (wrapper != null)
            wrapper.dispose();

        disguiseWrapperMap.remove(uuid);
        return true;
    }

    /**
     * Deserialize a wrapper instance from the giving parameter
     *
     * @param offlineParameter The parameter to deserialize
     * @return A wrapper that presents the giving parameter.
     * null if invalid or illegal
     * @apiNote The format for the input string is undefined and may looks like one of these three formats: "id|content", "id|*empty*", "*empty*"
     */
    @Override
    public @Nullable ServerDisguiseWrapper fromOfflineSave(String offlineParameter)
    {
        return null;
    }

    /**
     * Serialize a wrapper instance to a string that can be saved in the Offline Storage
     *
     * @param wrapper The target wrapper to save
     * @return A serialized string that can be deserialized to a wrapper in the future.
     * Null if the giving wrapper is not supported by this backend.
     */
    @Override
    public @Nullable String toOfflineSave(DisguiseWrapper<?> wrapper)
    {
        return null;
    }
}