package xiamomc.morph.abilities.impl;

import org.bukkit.NamespacedKey;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import xiamomc.morph.abilities.AbilityType;
import xiamomc.morph.abilities.options.ReduceDamageOption;

public class ReduceMagicDamageAbility extends DamageReducingAbility<ReduceDamageOption>
{
    @Override
    public @NotNull NamespacedKey getIdentifier()
    {
        return AbilityType.REDUCES_MAGIC_DAMAGE;
    }

    @Override
    protected ReduceDamageOption createOption()
    {
        return new ReduceDamageOption();
    }

    @Override
    protected EntityDamageEvent.DamageCause getTargetCause()
    {
        return EntityDamageEvent.DamageCause.MAGIC;
    }
}
