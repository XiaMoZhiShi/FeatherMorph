package xiamomc.morph.backends.server.renderer.network.datawatcher.watchers.types;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import xiamomc.morph.backends.server.renderer.network.registries.ValueIndex;
import xiamomc.morph.utilities.MathUtils;

import java.util.Arrays;
import java.util.Random;

public class ZombieVillagerWatcher extends ZombieWatcher
{
    public ZombieVillagerWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.ZOMBIE_VILLAGER);
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.ZOMBIE_VILLAGER);
    }

    @Override
    protected void initValues()
    {
        super.initValues();

        var random = new Random();
        var availableTypes = Arrays.stream(VillagerWatcher.VillagerTypes.values()).toList();
        var villagerType = availableTypes.get(random.nextInt(availableTypes.size())).bindingType;

        var availableProfessions = Arrays.stream(Villager.Profession.values()).toList();
        var targetBukkit = availableProfessions.get(random.nextInt(availableProfessions.size())).getKey().asString();
        VillagerProfession villagerProfession = VillagerProfession.NONE;
        try
        {
            villagerProfession = BuiltInRegistries.VILLAGER_PROFESSION
                    .getOptional(new ResourceLocation(targetBukkit))
                    .orElse(VillagerProfession.NONE);
        }
        catch (Throwable t)
        {
            logger.error("Unable to convert bukkit type '%s' to NMS format: " + t.getMessage());
        }

        var level = random.nextInt(1, 6);
        write(ValueIndex.ZOMBIE_VILLAGER.VILLAGER_DATA, new VillagerData(villagerType, villagerProfession, level));
    }

    private void mergeFromVillagerData(CompoundTag nbt)
    {
        int level = 0;
        VillagerProfession profession = VillagerProfession.NONE;
        VillagerType type = VillagerType.PLAINS;

        if (nbt.contains("level"))
            level = MathUtils.clamp(1, 5, nbt.getInt("level"));

        if (nbt.contains("profession"))
        {
            ResourceLocation rl = BuiltInRegistries.VILLAGER_PROFESSION.getDefaultKey();

            try
            {
                rl = new ResourceLocation(nbt.getString("profession"));
            }
            catch (Throwable t)
            {
                logger.warn("Error occurred while reading nbt to villager profession: " + t.getMessage());
            }

            profession = BuiltInRegistries.VILLAGER_PROFESSION.getOptional(rl).orElse(VillagerProfession.NONE);

            logger.info("Set prof " + profession);
        }

        if (nbt.contains("type"))
        {
            ResourceLocation rl = BuiltInRegistries.VILLAGER_TYPE.getDefaultKey();

            try
            {
                rl = new ResourceLocation(nbt.getString("type"));
            }
            catch (Throwable t)
            {
                logger.warn("Error occurred while reading nbt to villager type: " + t.getMessage());
            }

            type = BuiltInRegistries.VILLAGER_TYPE.getOptional(rl).orElse(VillagerType.PLAINS);
            logger.info("Set type " + type);
        }

        write(ValueIndex.ZOMBIE_VILLAGER.VILLAGER_DATA, new VillagerData(type, profession, level));
    }

    @Override
    public void mergeFromCompound(CompoundTag nbt)
    {
        super.mergeFromCompound(nbt);

        if (nbt.contains("VillagerData"))
            mergeFromVillagerData(nbt.getCompound("VillagerData"));
    }

    @Override
    public void writeToCompound(CompoundTag nbt)
    {
        super.writeToCompound(nbt);

        var villagerData = get(ValueIndex.VILLAGER.VILLAGER_DATA);
        var profession = villagerData.getProfession();
        var type = villagerData.getType();
        var level = villagerData.getLevel();

        var compound = new CompoundTag();
        compound.putInt("level", level);
        compound.putString("profession", BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession).toString());
        compound.putString("type", BuiltInRegistries.VILLAGER_TYPE.getKey(type).toString());

        nbt.put("VillagerData", compound);
    }
}
