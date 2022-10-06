package xiamomc.morph;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import xiamomc.morph.misc.MessageUtils;
import xiamomc.pluginbase.Annotations.Resolved;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MorphAbilityHandler extends MorphPluginObject
{
    @Resolved
    private MorphManager manager;

    private final PotionEffect miningFatigueEffect = new PotionEffect(PotionEffectType.SLOW_DIGGING, 6000, 2);

    private final PotionEffect dolphinsGraceEffect = new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 100, 0);

    //who, when
    private final Map<UUID, Long> elderGuardianCoolDownTraceMap = new ConcurrentHashMap<>();

    public void executeDisguiseAbility(Player player)
    {
        var state = manager.getDisguiseStateFor(player);

        if (state == null) return;

        switch (state.getDisguise().getType().getEntityType())
        {
            case ENDERMAN ->
            {
                var targetBlock = player.getTargetBlock(32);
                if (targetBlock == null || targetBlock.getBlockData().getMaterial().isAir())
                {
                    sendDenyMessageToPlayer(player, Component.text("目标太远或不合适"));
                    state.resetCooldown();
                    return;
                }

                //获取位置
                var loc = targetBlock.getLocation();
                loc.setY(loc.getY() + 1);
                loc.setDirection(player.getEyeLocation().getDirection());

                playSoundToNearbyPlayers(player, 10,
                        Key.key("minecraft", "entity.enderman.teleport"), Sound.Source.HOSTILE);

                player.teleport(loc);

                playSoundToNearbyPlayers(player, 10,
                        Key.key("minecraft", "entity.enderman.teleport"), Sound.Source.HOSTILE);

                state.resetCooldown();
            }

            case ENDER_DRAGON ->
            {
                shootFireBall(player, DragonFireball.class);

                playSoundToNearbyPlayers(player, 50,
                        Key.key("minecraft", "entity.ender_dragon.shoot"), Sound.Source.HOSTILE);

                state.resetCooldown();
            }

            case GHAST ->
            {
                shootFireBall(player, Fireball.class);

                playSoundToNearbyPlayers(player, 25,
                        Key.key("minecraft", "entity.ghast.shoot"), Sound.Source.HOSTILE);

                state.resetCooldown();
            }

            case BLAZE ->
            {
                shootFireBall(player, SmallFireball.class);

                playSoundToNearbyPlayers(player, 15,
                        Key.key("minecraft", "entity.blaze.shoot"), Sound.Source.HOSTILE);

                state.resetCooldown();
            }

            case CREEPER ->
            {
                player.getWorld().createExplosion(player, 3,
                        false, Boolean.TRUE.equals(player.getWorld().getGameRuleValue(GameRule.MOB_GRIEFING)));

                if (!(player.getGameMode() == GameMode.CREATIVE))
                    player.setHealth(0d);

                state.resetCooldown();
            }

            case SHULKER ->
            {
                var distance = 15;
                var target = player.getTargetEntity(distance);

                if (player.getWorld().getDifficulty() == Difficulty.PEACEFUL)
                {
                    sendDenyMessageToPlayer(player, Component.text("世界难度为和平"));
                    state.setAbilityCooldown(5);
                    return;
                }

                if (target != null)
                {
                    var loc = player.getEyeLocation().clone();

                    var bullet = player.getWorld().spawn(loc, ShulkerBullet.class);

                    bullet.setTarget(target);
                    bullet.setShooter(player);

                    playSoundToNearbyPlayers(player, 15,
                            Key.key("minecraft", "entity.shulker.shoot"), Sound.Source.HOSTILE);
                }
                else
                {
                    sendDenyMessageToPlayer(player, Component.text("视线" + distance + "格以内没有实体").color(NamedTextColor.RED));
                    state.setAbilityCooldown(10);
                    return;
                }

                state.resetCooldown();
            }

            case ELDER_GUARDIAN ->
            {
                if (!player.isInWater())
                {
                    state.setAbilityCooldown(20);
                    sendDenyMessageToPlayer(player, Component.text("你需要在水里才能使用此技能"));
                    return;
                }

                //对远古守卫者的技能进行额外限制
                var lastActivate = elderGuardianCoolDownTraceMap.get(player.getUniqueId());

                if (Objects.isNull(lastActivate)) lastActivate = -32767L;

                if (Plugin.getCurrentTick() - lastActivate < state.getDefaultCooldown())
                {
                    sendDenyMessageToPlayer(player, Component.text("距离上次使用远古守卫者的技能不足1分钟"));
                    return;
                }

                var players = new ArrayList<Player>();

                player.getNearbyEntities(50, 50, 50).forEach(e ->
                {
                    if (e instanceof Player) players.add((Player) e);
                });

                var sound = Sound.sound(Key.key("entity.elder_guardian.curse"), Sound.Source.HOSTILE, 50, 1);
                players.forEach(p ->
                {
                    p.addPotionEffect(miningFatigueEffect);
                    p.playSound(sound);
                    p.spawnParticle(Particle.MOB_APPEARANCE, p.getLocation(), 1);
                });

                player.playSound(sound);

                state.resetCooldown();
                elderGuardianCoolDownTraceMap.put(player.getUniqueId(), Plugin.getCurrentTick());
            }

            case DOLPHIN ->
            {
                state.resetCooldown();

                if (!player.isInWater())
                {
                    sendDenyMessageToPlayer(player, Component.text("你需要在水里才能使用此技能"));
                    return;
                }

                var players = new ArrayList<Player>();

                player.getNearbyEntities(9, 9, 9).forEach(e ->
                {
                    if (e instanceof Player p && p.isInWater()) players.add(p);
                });

                players.forEach(p -> p.addPotionEffect(dolphinsGraceEffect));
            }

            default ->
            {
                state.setAbilityCooldown(20);

                sendDenyMessageToPlayer(player, Component.text("此伪装暂时没有技能"));
            }
        }
    }

    private void sendDenyMessageToPlayer(Player player, Component text)
    {
        player.sendMessage(MessageUtils.prefixes(player, text.color(NamedTextColor.RED)));

        player.playSound(Sound.sound(Key.key("minecraft", "entity.villager.no"),
                Sound.Source.PLAYER, 1f, 1f));
    }

    private <T extends Fireball> T shootFireBall(Player player, Class<T> fireball)
    {
        var fireBall = player.getWorld()
                .spawn(player.getEyeLocation(), fireball);

        fireBall.setShooter(player);

        fireBall.setVelocity(player.getEyeLocation().getDirection().multiply(2));

        return fireBall;
    }

    private void playSoundToNearbyPlayers(Player player, int distance, Key key, Sound.Source source)
    {
        var players = player.getNearbyEntities(distance, distance, distance).stream()
                .filter(e -> e instanceof Player).toList();

        var sound = Sound.sound(key, source, distance * 2, 1f);

        player.playSound(sound);

        for (var e : players)
            e.playSound(sound);
    }

}