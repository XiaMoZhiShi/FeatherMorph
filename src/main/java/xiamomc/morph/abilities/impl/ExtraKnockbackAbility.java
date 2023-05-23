package xiamomc.morph.abilities.impl;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;
import xiamomc.morph.MorphManager;
import xiamomc.morph.abilities.AbilityType;
import xiamomc.morph.abilities.MorphAbility;
import xiamomc.morph.abilities.options.ExtraKnockbackOption;
import xiamomc.morph.misc.DisguiseState;
import xiamomc.pluginbase.Annotations.Resolved;

public class ExtraKnockbackAbility extends MorphAbility<ExtraKnockbackOption>
{
    /**
     * 获取此被动技能的ID
     *
     * @return {@link NamespacedKey}
     */
    @Override
    public @NotNull NamespacedKey getIdentifier()
    {
        return AbilityType.EXTRA_KNOCKBACK;
    }

    @Override
    public void onClientInit(DisguiseState state)
    {
        super.onClientInit(state);
    }

    /*
    @EventHandler
    private void onGolemDmg(EntityDamageByEntityEvent e)
    {
        if (!(e.getDamager() instanceof CraftIronGolem craftIronGolem)) return;
        if (!(e.getEntity() instanceof CraftPlayer craftPlayer)) return;

        logger.info("VECTOR: %s".formatted(craftPlayer.getHandle().getDeltaMovement()));
        this.addSchedule(() ->
        {
            logger.info("SCHED VECTOR: %s".formatted(craftPlayer.getHandle().getDeltaMovement()));
        });
    }
    */

    @Resolved(shouldSolveImmediately = true)
    private MorphManager manager;

    @EventHandler(ignoreCancelled = true)
    private void onPlayerDamage(EntityDamageByEntityEvent e)
    {
        if (!(e.getDamager() instanceof Player player)) return;

        if (!this.appliedPlayers.contains(player)) return;

        var damaged = e.getEntity();
        var nmsDamaged = ((CraftEntity)damaged).getHandle();

        var state = manager.getDisguiseStateFor(player);

        assert state != null;
        var option = this.getOptionFor(state);

        if (option == null) option = defaultOption;

        //var yDelta = 0.745584025D;
        var yDelta = option.yMotion;
        var baseYDelta = 0.345584025D;

        if (nmsDamaged instanceof LivingEntity livingEntity)
        {
            var knockbackResistance = livingEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            yDelta *= Math.max(0D, 1D - knockbackResistance);
            baseYDelta *= Math.max(0D, 1D - knockbackResistance);
        }

        yDelta = baseYDelta + yDelta;

        //workaround: 需要让实体离地再设置Motion，否则不会起效
        var movement = nmsDamaged.getDeltaMovement().add(option.xMotion, yDelta, option.zMotion);
        nmsDamaged.setPos(nmsDamaged.position().add(0, 0.01D, 0));
        nmsDamaged.setOnGround(false);
        nmsDamaged.setDeltaMovement(movement);
        nmsDamaged.hasImpulse = true;

        /*
        logger.info("VECTOR: %s".formatted(nmsDamaged.getDeltaMovement()));
        this.addSchedule(() ->
        {
            logger.info("SCHED VECTOR: %s".formatted(nmsDamaged.getDeltaMovement()));
        });
        */
    }

    private static final ExtraKnockbackOption defaultOption = ExtraKnockbackOption.from(0, 0.4D, 0);

    @Override
    protected @NotNull ExtraKnockbackOption createOption()
    {
        return defaultOption;
    }
}
