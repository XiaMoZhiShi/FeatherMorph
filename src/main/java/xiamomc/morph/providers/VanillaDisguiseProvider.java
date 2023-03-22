package xiamomc.morph.providers;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.backends.DisguiseWrapper;
import xiamomc.morph.config.ConfigOption;
import xiamomc.morph.config.MorphConfigManager;
import xiamomc.morph.messages.vanilla.VanillaMessageStore;
import xiamomc.morph.misc.DisguiseInfo;
import xiamomc.morph.misc.DisguiseState;
import xiamomc.morph.misc.DisguiseTypes;
import xiamomc.morph.utilities.EntityTypeUtils;
import xiamomc.morph.utilities.NbtUtils;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.List;
import java.util.stream.Collectors;

public class VanillaDisguiseProvider extends DefaultDisguiseProvider
{
    @Override
    public @NotNull String getNameSpace()
    {
        return DisguiseTypes.VANILLA.getNameSpace();
    }

    @Override
    public boolean isValid(String rawIdentifier)
    {
        return getAllAvailableDisguises().contains(rawIdentifier);
    }

    public VanillaDisguiseProvider()
    {
        var list = new ObjectArrayList<String>();

        for (var eT : EntityType.values())
        {
            if (eT == EntityType.UNKNOWN || !eT.isAlive()) continue;

            list.add(eT.getKey().asString());
        }

        list.removeIf(s -> s.equals("minecraft:player"));

        vanillaIdentifiers = list;
    }

    private final List<String> vanillaIdentifiers;

    @Override
    public List<String> getAllAvailableDisguises()
    {
        return vanillaIdentifiers;
    }

    @Override
    @NotNull
    public DisguiseResult morph(Player player, DisguiseInfo disguiseInfo, @Nullable Entity targetEntity)
    {
        var identifier = disguiseInfo.getIdentifier();

        DisguiseWrapper<?> constructedDisguise;
        var backend = getMorphManager().getCurrentBackend();

        var entityType = EntityTypeUtils.fromString(identifier, true);

        if (entityType == null || entityType == EntityType.PLAYER || !entityType.isAlive())
        {
            logger.error("Illegal mob type: " + identifier + "(" + entityType + ")");
            return DisguiseResult.fail();
        }

        var copyResult = getCopy(disguiseInfo, targetEntity);

        constructedDisguise = copyResult.success()
                ? copyResult.disguise()
                : backend.createInstance(null, entityType);

        backend.disguise(player, constructedDisguise);

        return DisguiseResult.success(constructedDisguise, copyResult.isCopy());
    }

    private final Bindable<Boolean> armorStandShowArms = new Bindable<>(false);
    private final Bindable<Boolean> doHealthScale = new Bindable<>(true);
    private final Bindable<Integer> healthCap = new Bindable<>(60);

    @Initializer
    private void load(MorphConfigManager configManager)
    {
        configManager.bind(armorStandShowArms, ConfigOption.ARMORSTAND_SHOW_ARMS);
        configManager.bind(doHealthScale, ConfigOption.HEALTH_SCALE);
        configManager.bind(healthCap, ConfigOption.HEALTH_SCALE_MAX_HEALTH);
    }

    @Override
    public void postConstructDisguise(DisguiseState state, @Nullable Entity targetEntity)
    {
        super.postConstructDisguise(state, targetEntity);

        var disguise = state.getDisguise();
        var backend = getMorphManager().getCurrentBackend();

        if (!backend.isDisguised(targetEntity))
        {
            //盔甲架加上手臂
            if (disguise.getEntityType().equals(EntityType.ARMOR_STAND) && armorStandShowArms.get())
                disguise.showArms(true);
        }

        if (doHealthScale.get())
        {
            var player = state.getPlayer();
            var loc = player.getLocation();
            loc.setY(-8192);

            removeAllHealthModifiers(player);

            var entityClazz = state.getEntityType().getEntityClass();
            if (entityClazz != null)
            {
                var entity = state.getPlayer().getWorld().spawn(loc, entityClazz, CreatureSpawnEvent.SpawnReason.CUSTOM);

                if (entity instanceof LivingEntity living)
                {
                    var mobMaxHealth = living.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                    var playerAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

                    assert playerAttribute != null;
                    var diff = mobMaxHealth - playerAttribute.getBaseValue();

                    //确保血量不会超过上限
                    if (playerAttribute.getBaseValue() + diff > healthCap.get())
                        diff = healthCap.get() - playerAttribute.getBaseValue();

                    //缩放生命值
                    double finalDiff = diff;
                    this.executeThenScaleHealth(player, playerAttribute, () ->
                    {
                        var modifier = new AttributeModifier(modifierName, finalDiff, AttributeModifier.Operation.ADD_NUMBER);
                        playerAttribute.addModifier(modifier);
                    });
                }

                entity.remove();
            }
        }
    }

    private final String modifierName = "FeatherMorphVDP_Modifier";

    private void executeThenScaleHealth(Player player, AttributeInstance attributeInstance, Runnable runnable)
    {
        var currentPercent = player.getHealth() / attributeInstance.getValue();

        runnable.run();

        if (player.getHealth() > 0)
            player.setHealth(Math.min(player.getMaxHealth(), attributeInstance.getValue() * currentPercent));
    }

    private void removeAllHealthModifiers(Player player)
    {
        var attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        assert attribute != null;

        this.executeThenScaleHealth(player, attribute, () ->
        {
            attribute.getModifiers().stream()
                    .filter(m -> m.getName().equals(modifierName)).collect(Collectors.toSet())
                    .forEach(attribute::removeModifier);
        });
    }

    @Override
    public boolean unMorph(Player player, DisguiseState state)
    {
        if (super.unMorph(player, state))
        {
            removeAllHealthModifiers(player);
            return true;
        }
        else
            return false;
    }

    @Override
    public @Nullable CompoundTag getNbtCompound(DisguiseState state, Entity targetEntity)
    {
        var info = getMorphManager().getDisguiseInfo(state.getDisguiseIdentifier());

        var rawCompound = targetEntity != null && canConstruct(info, targetEntity, null)
                ? NbtUtils.getRawTagCompound(targetEntity)
                : new CompoundTag();

        if (rawCompound == null) rawCompound = new CompoundTag();

        var theirDisguise = getMorphManager().getDisguiseStateFor(targetEntity);

        if (theirDisguise != null)
        {
            var theirNbtString = theirDisguise.getCachedNbtString();

            try
            {
                rawCompound = TagParser.parseTag(theirNbtString);
            }
            catch (Throwable t)
            {
                logger.error("Unable to copy NBT Tag from disguise: " + t.getMessage());
                t.printStackTrace();
            }
        }

        if (state.getEntityType().equals(EntityType.ARMOR_STAND)
                && rawCompound.get("ShowArms") == null)
        {
            rawCompound.putBoolean("ShowArms", armorStandShowArms.get());
        }

        if (targetEntity == null || targetEntity.getType() != state.getEntityType())
            state.getDisguise().initializeCompound(rawCompound);

        return cullNBT(rawCompound);
    }

    @Override
    public boolean validForClient(DisguiseState state)
    {
        return true;
    }

    @Override
    public boolean canConstruct(DisguiseInfo info, Entity targetEntity, DisguiseState theirState)
    {
        return theirState != null
                ? theirState.getDisguise().getEntityType().equals(info.getEntityType())
                : targetEntity.getType().equals(info.getEntityType());
    }

    @Override
    protected boolean canCopyDisguise(DisguiseInfo info, Entity targetEntity,
                                      @Nullable DisguiseState theirDisguiseState, @NotNull DisguiseWrapper<?> theirDisguise)
    {
        return theirDisguise.getEntityType().equals(info.getEntityType());
    }

    @Resolved
    private VanillaMessageStore vanillaMessageStore;

    @Override
    public Component getDisplayName(String disguiseIdentifier, String locale)
    {
        var type = EntityTypeUtils.fromString(disguiseIdentifier, true);

        if (type == null)
            return Component.text("???");
        else
            return vanillaMessageStore.getComponent(type.translationKey(), null, locale);
    }
}
