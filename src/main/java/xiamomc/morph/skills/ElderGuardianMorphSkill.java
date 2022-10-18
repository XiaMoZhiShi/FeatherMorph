package xiamomc.morph.skills;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import xiamomc.morph.messages.SkillStrings;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ElderGuardianMorphSkill extends MorphSkill
{
    private final PotionEffect miningFatigueEffect = new PotionEffect(PotionEffectType.SLOW_DIGGING, 6000, 2);

    @Override
    public int executeSkill(Player player)
    {
        if (!player.isInWater())
        {
            sendDenyMessageToPlayer(player, SkillStrings.notInWaterString().toComponent());
            return 20;
        }

        var players = findNearbyPlayers(player, 50);

        var sound = Sound.sound(Key.key("entity.elder_guardian.curse"), Sound.Source.HOSTILE, 50, 1);
        players.forEach(p ->
        {
            p.addPotionEffect(miningFatigueEffect);
            p.playSound(sound);
            p.spawnParticle(Particle.MOB_APPEARANCE, p.getLocation(), 1);
        });

        player.playSound(sound);

        int defaultCooldown = 1200;
        return defaultCooldown;
    }

    @Override
    public EntityType getType()
    {
        return EntityType.ELDER_GUARDIAN;
    }
}
