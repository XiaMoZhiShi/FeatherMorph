package xiamomc.morph.utilities;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.commands.data.EntityDataAccessor;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NbtUtils
{
    public static CompoundTag toCompoundTag(GameProfile profile)
    {
        var compound = new CompoundTag();
        return net.minecraft.nbt.NbtUtils.writeGameProfile(compound, profile);
    }

    /**
     * 获取目标实体的 {@link CompoundTag}
     * @param entity 目标实体
     * @return 此实体的NBT数据，当实体为null或不为 {@link CraftEntity} 的实例时返回null
     */
    @NotNull
    public static CompoundTag getRawTagCompound(Entity entity)
    {
        if (entity instanceof CraftEntity craftEntity)
        {
            var nmsEntity = craftEntity.getHandle();

            var entityDataObject = new EntityDataAccessor(nmsEntity);

            return entityDataObject.getData();
        }

        return new CompoundTag();
    }

    /**
     * 将目标NBT序列化为字符串
     * @param compound 目标NBT
     * @return 由此NBT序列化的字符串，当compound为null时返回null
     */
    public static String getCompoundString(CompoundTag compound)
    {
        if (compound == null) return null;

        //StringNbtWriter
        var visitor = new StringTagVisitor();

        //StringNbtWriter#apply(NbtElement)
        return visitor.visit(compound);
    }

    /**
     *
     * @param input The input NBT string
     * @return Null if invalid of illegal
     */
    @Nullable
    public static CompoundTag toCompoundTag(@Nullable String input)
    {
        if (input == null || input.isEmpty()) return null;

        try
        {
            return TagParser.parseTag(input);
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    @Nullable
    @Contract("_, false -> !null; _, true -> _")
    public static CompoundTag toCompoundTag(@Nullable String input, boolean nullIfInvalid)
    {
        var result = toCompoundTag(input);

        if (result != null) return result;

        return nullIfInvalid ? null : new CompoundTag();
    }

    public static boolean isBabyForType(EntityType type, CompoundTag compoundTag)
    {
        var ageable = EntityTypeUtils.hasBabyVariant(type);

        if (!ageable) return false;

        if (EntityTypeUtils.isZombie(type) || type == EntityType.PIGLIN)
            return compoundTag.getBoolean("IsBaby");

        var val = compoundTag.getInt("Age");

        return val < 0;
    }

    public static List<String> defaultBlacklistedPatterns = List.of(
            //Common
            "[pP]urpur.*", "[pP]aper.*", "[sS]pigot.*", "[bB]ukkit.*",

            //Player
            "Xp.*", "food.*",

            //Misc
            "Death.*", "Spawn.*"
    );

    public static List<String> defaultBlacklistedTags = List.of(
            //Common
            "UUID", "data", "Brain", "Motion", "palette", "Attributes",
            "Invulnerable",

            //Armor stand
            "DisabledSlots", "ArmorItems", "HandItems",

            //Player
            "Tags", "recipes", "Inventory", "abilities", "recipeBook",
            "EnderItems", "warden_spawn_tracker", "previousPlayerGameType",
            "LastDeathLocation", "playerGameType", "seenCredits", "Score",

            //Villager
            "Offers", "LastRestock", "RestocksToday",

            //Misc
            "Pos", "Owner", "WorldUUIDLeast", "WorldUUIDMost",
            "Rotation", "listener", "ActiveEffects", "ArmorDropChances",
            "PersistenceRequired", "SelectedItem"
    );
}
