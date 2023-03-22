package xiamomc.morph.backends.fallback;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.backends.DisguiseWrapper;
import xiamomc.morph.misc.DisguiseEquipment;
import xiamomc.morph.misc.DisguiseState;

public class NilWrapper extends DisguiseWrapper<NilDisguise>
{
    public NilWrapper(@NotNull NilDisguise instance)
    {
        super(instance);
    }

    private final DisguiseEquipment equipment = new DisguiseEquipment();

    @Override
    public EntityEquipment getDisplayingEquipments()
    {
        return equipment;
    }

    @Override
    public void setDisplayingEquipments(EntityEquipment newEquipment)
    {
        this.equipment.setArmorContents(newEquipment.getArmorContents());

        this.equipment.setHandItems(newEquipment.getItemInMainHand(), newEquipment.getItemInOffHand());
    }

    @Override
    public void toggleServerSelfView(boolean enabled)
    {
    }

    @Override
    public EntityType getEntityType()
    {
        return instance.type;
    }

    @Override
    public NilDisguise copyInstance()
    {
        return instance.clone();
    }

    @Override
    public DisguiseWrapper<NilDisguise> clone()
    {
        return new NilWrapper(this.copyInstance());
    }

    /**
     * 返回此伪装的名称
     *
     * @return 伪装名称
     */
    @Override
    public String getDisguiseName()
    {
        return instance.name;
    }

    @Override
    public void setDisguiseName(String name)
    {
        this.instance.name = name;
    }

    @Override
    public BoundingBox getBoundingBox()
    {
        return new BoundingBox();
    }

    @Override
    public void setGlowingColor(ChatColor glowingColor)
    {
        instance.glowingColor = glowingColor;
    }

    @Override
    public void setGlowing(boolean glowing)
    {
    }

    @Override
    public ChatColor getGlowingColor()
    {
        return instance.glowingColor;
    }

    @Override
    public void addCustomData(String key, Object data)
    {
        instance.customData.put(key, data);
    }

    @Override
    public Object getCustomData(String key)
    {
        return instance.customData.getOrDefault(key, null);
    }

    @Override
    public void applySkin(GameProfile profile)
    {
        System.out.println(this + " Appling Skin " + instance + " :: " +  profile);
        this.instance.profile = profile;
    }

    @Override
    public @Nullable GameProfile getSkin()
    {
        System.out.println(this + " Returning " + instance + " :: " + instance.profile);
        return instance.profile;
    }

    @Override
    public void onPostConstructDisguise(DisguiseState state, @Nullable Entity targetEntity)
    {

    }

    @Override
    public String serializeDisguiseData()
    {
        return "";
    }

    @Override
    public void update(boolean isClone, DisguiseState state, Player player)
    {

    }

    @Override
    public void initializeCompound(CompoundTag rootCompound)
    {
    }

    @Override
    public void setSaddled(boolean saddled)
    {
        instance.saddled = saddled;
    }

    @Override
    public boolean isSaddled()
    {
        return instance.saddled;
    }
}
