package xiamomc.morph.abilities.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import xiamomc.morph.abilities.MorphAbility;
import xiamomc.morph.storage.skill.ISkillOption;

public abstract class OnAttackAbility<T extends ISkillOption> extends MorphAbility<T>
{
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamagedByEntity(EntityDamageByEntityEvent e)
    {
        if (!(e.getDamager() instanceof Player player)) return;
        if (!(e.getEntity() instanceof LivingEntity livingEntity)) return;
        if (!appliedPlayers.contains(player)) return;
        this.apply(livingEntity, player);
    }

    protected abstract void apply(LivingEntity hurt, Player source);
}
