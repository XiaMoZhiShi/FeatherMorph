package xiamomc.morph.skills.impl;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import xiamomc.morph.messages.MessageUtils;
import xiamomc.morph.messages.SkillStrings;
import xiamomc.morph.misc.DisguiseState;
import xiamomc.morph.skills.MorphSkill;
import xiamomc.morph.skills.SkillType;
import xiamomc.morph.skills.options.EffectConfiguration;
import xiamomc.morph.storage.skill.SkillAbilityConfiguration;

public class ApplyEffectMorphSkill extends MorphSkill<EffectConfiguration>
{
    @Override
    public int executeSkill(Player player, DisguiseState state, SkillAbilityConfiguration configuration, EffectConfiguration option)
    {
        if (option == null || configuration == null)
        {
            printErrorMessage(player, configuration + "没有设置药水效果");
            return 10;
        }

        if (option.acquiresWater() && !player.isInWater())
        {
            sendDenyMessageToPlayer(player, SkillStrings.notInWaterString()
                    .withLocale(MessageUtils.getLocale(player))
                    .toComponent(null));

            return 20;
        }

        var players = findNearbyPlayers(player, option.getApplyDistance());

        var sound = Sound.sound(Key.key(option.getSoundName()), Sound.Source.PLAYER, option.getSoundDistance(), 1);

        var effect = getEffect(
                option.getName(),
                option.getDuration(),
                option.getMultiplier()
        );

        if (effect == null)
        {
            printErrorMessage(player, configuration + "设置了无效的药水效果");
            return 10;
        }

        players.forEach(p ->
        {
            p.addPotionEffect(effect);
            p.playSound(sound);

            if (option.showGuardian())
                p.spawnParticle(Particle.MOB_APPEARANCE, p.getLocation(), 1);
        });

        player.playSound(sound);

        return configuration.getCooldown();
    }

    private PotionEffect getEffect(String key, int duration, int multiplier)
    {
        if (key == null) return null;

        var type = PotionEffectType.getByKey(NamespacedKey.fromString(key));

        if (type == null) return null;

        return new PotionEffect(type, duration, multiplier, false);
    }

    @Override
    public @NotNull NamespacedKey getIdentifier()
    {
        return SkillType.APPLY_EFFECT;
    }

    private final EffectConfiguration option = new EffectConfiguration();

    @Override
    public EffectConfiguration getOptionInstance()
    {
        return option;
    }
}
