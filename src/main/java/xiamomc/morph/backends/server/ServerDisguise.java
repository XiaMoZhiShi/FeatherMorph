package xiamomc.morph.backends.server;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;

import java.util.Map;

public class ServerDisguise implements Cloneable
{
    public ServerDisguise(EntityType type)
    {
        this.type = type;
        this.name = type.translationKey();

        if (type == EntityType.SLIME || type == EntityType.MAGMA_CUBE)
            this.compoundTag.putInt("Size", 4);
    }

    public EntityType type;

    public String name;

    public ChatColor glowingColor = ChatColor.WHITE;

    public final Map<String, Object> customData = new Object2ObjectOpenHashMap<>();

    public boolean saddled;

    public GameProfile profile;

    public final CompoundTag compoundTag = new CompoundTag();

    public boolean isBaby;

    @Override
    protected ServerDisguise clone()
    {
        ServerDisguise obj;

        try
        {
            obj = (ServerDisguise) super.clone();
        }
        catch (Throwable t)
        {
            obj = new ServerDisguise(this.type);
        }

        obj.type = this.type;
        obj.name = this.name;
        obj.glowingColor = this.glowingColor;

        obj.saddled = this.saddled;
        obj.profile = this.profile;

        obj.compoundTag.merge(this.compoundTag);

        obj.customData.putAll(this.customData);

        obj.isBaby = this.isBaby;

        return obj;
    }
}