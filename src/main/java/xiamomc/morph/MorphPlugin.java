package xiamomc.morph;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.ApiStatus;
import xiamomc.morph.abilities.AbilityHandler;
import xiamomc.morph.commands.MorphCommandManager;
import xiamomc.morph.config.MorphConfigManager;
import xiamomc.morph.events.*;
import xiamomc.morph.interfaces.IManagePlayerData;
import xiamomc.morph.interfaces.IManageRequests;
import xiamomc.morph.messages.MessageUtils;
import xiamomc.morph.messages.MorphMessageStore;
import xiamomc.morph.messages.vanilla.VanillaMessageStore;
import xiamomc.morph.misc.NetworkingHelper;
import xiamomc.morph.misc.PlayerOperationSimulator;
import xiamomc.morph.misc.integrations.residence.ResidenceEventProcessor;
import xiamomc.morph.misc.integrations.tab.TabAdapter;
import xiamomc.morph.network.multiInstance.MultiInstanceService;
import xiamomc.morph.updates.UpdateHandler;
import xiamomc.morph.misc.integrations.placeholderapi.PlaceholderIntegration;
import xiamomc.morph.network.server.MorphClientHandler;
import xiamomc.morph.skills.MorphSkillHandler;
import xiamomc.morph.storage.skill.SkillAbilityConfigurationStore;
import xiamomc.morph.transforms.Transformer;
import xiamomc.pluginbase.Command.CommandHelper;
import xiamomc.pluginbase.Messages.MessageStore;
import xiamomc.pluginbase.XiaMoJavaPlugin;

public final class MorphPlugin extends XiaMoJavaPlugin
{
    private static MorphPlugin instance;

    /**
     * 仅当当前对象无法继承MorphPluginObject或不需要完全继承MorphPluginObject时使用
     * @return 插件的实例
     */
    @Deprecated
    public static MorphPlugin getInstance()
    {
        return instance;
    }

    public MorphPlugin()
    {
        instance = this;
    }

    public static String getMorphNameSpace()
    {
        return "morphplugin";
    }

    @Override
    public String getNameSpace()
    {
        return getMorphNameSpace();
    }

    private CommandHelper<MorphPlugin> cmdHelper;

    private MorphManager morphManager;

    private PluginManager pluginManager;

    private MorphSkillHandler skillHandler;

    private AbilityHandler abilityHandler;

    private VanillaMessageStore vanillaMessageStore;

    private MorphMessageStore messageStore;

    private PlaceholderIntegration placeholderIntegration;

    private MorphClientHandler clientHandler;

    private Metrics metrics;

    private InteractionMirrorProcessor mirrorProcessor;

    private TabAdapter tabAdapter;

    private MultiInstanceService instanceService;

    @Override
    public void onEnable()
    {
        super.onEnable();

        pluginManager = Bukkit.getPluginManager();
        var bukkitVersion = Bukkit.getMinecraftVersion();
        var targetVersion = "1.20.4";
        if (!bukkitVersion.equals(targetVersion))
        {
            logger.error("╔══════════════════════════════════════════════════════════════╗");
            logger.error("║                                                              ║");
            logger.error("║    This version of Minecraft (%s) is not supported!      ║".formatted(bukkitVersion));
            logger.error("║                Please use %s instead!                    ║".formatted(targetVersion));
            logger.error("║                                                              ║");
            //logger.error("║       https://modrinth.com/plugin/feathermorph               ║");
            //logger.error("║                                                              ║");
            logger.error("╚══════════════════════════════════════════════════════════════╝");

            pluginManager.disablePlugin(this);
            return;
        }

        this.metrics = new Metrics(this, 18062);

        this.registerListener(softDeps);

        var playerTracker = new PlayerTracker();

        softDeps.setHandle("PlaceholderAPI", p ->
        {
            logger.info("Registering Placeholders...");
            placeholderIntegration = new PlaceholderIntegration(dependencyManager);
            placeholderIntegration.register();
        }, true);

        softDeps.setHandle("Residence", r ->
        {
            logger.info("Residence detected, applying integrations...");
            this.registerListener(new ResidenceEventProcessor());
        }, true);

        softDeps.setHandle("TAB", r ->
        {
            logger.info("Applying TAB integrations...");
            this.registerListener(tabAdapter = new TabAdapter());
        }, true);

        //缓存依赖
        dependencyManager.cache(this);
        dependencyManager.cache(new NetworkingHelper());
        dependencyManager.cache(morphManager = new MorphManager());
        dependencyManager.cache(skillHandler = new MorphSkillHandler());
        dependencyManager.cache(abilityHandler = new AbilityHandler());
        dependencyManager.cache(new RevealingHandler());
        dependencyManager.cache(new Transformer());

        dependencyManager.cache(clientHandler = new MorphClientHandler());

        dependencyManager.cache(vanillaMessageStore = new VanillaMessageStore());

        dependencyManager.cacheAs(MessageStore.class, messageStore = new MorphMessageStore());
        dependencyManager.cacheAs(MiniMessage.class, MiniMessage.miniMessage());
        dependencyManager.cacheAs(IManagePlayerData.class, morphManager);
        dependencyManager.cacheAs(IManageRequests.class, new RequestManager());
        dependencyManager.cacheAs(Scoreboard.class, Bukkit.getScoreboardManager().getMainScoreboard());
        dependencyManager.cacheAs(MorphConfigManager.class, new MorphConfigManager(this));
        dependencyManager.cache(playerTracker);

        dependencyManager.cache(cmdHelper = new MorphCommandManager());

        dependencyManager.cache(new SkillAbilityConfigurationStore());

        dependencyManager.cache(new MessageUtils());

        dependencyManager.cache(new PlayerOperationSimulator());

        var updateHandler = new UpdateHandler();
        dependencyManager.cache(updateHandler);

        dependencyManager.cache(instanceService = new MultiInstanceService());

        mirrorProcessor = new InteractionMirrorProcessor();

        //注册EventProcessor
        this.schedule(() ->
        {
            registerListeners(new Listener[]
                    {
                            playerTracker,
                            mirrorProcessor,
                            new CommonEventProcessor(),
                            new RevealingEventProcessor(),
                            new DisguiseAnimationProcessor(),
                            new ForcedDisguiseProcessor(),
                            new PlayerSkinProcessor()
                    });

            clientHandler.sendReAuth(Bukkit.getOnlinePlayers());
        });
    }

    @ApiStatus.Internal
    public void crash(Throwable t)
    {
        logger.error(t.getLocalizedMessage());
        t.printStackTrace();

        this.onDisable();
    }

    @Override
    public void onDisable()
    {
        //调用super.onDisable后依赖管理器会被清空
        //需要在调用前先把一些东西处理好
        try
        {
            if (morphManager != null)
                morphManager.onPluginDisable();

            if (placeholderIntegration != null)
                placeholderIntegration.unregister();

            if (clientHandler != null)
                clientHandler.getConnectedPlayers().forEach(clientHandler::disconnect);

            if (metrics != null)
                metrics.shutdown();

            if (mirrorProcessor != null)
                mirrorProcessor.pushToLoggingBase();

            if (instanceService != null)
                instanceService.onDisable();

            var messenger = this.getServer().getMessenger();

            messenger.unregisterOutgoingPluginChannel(this);
        }
        catch (Exception e)
        {
            logger.warn("Error occurred while disabling: " + e.getMessage());
            e.printStackTrace();
        }

        super.onDisable();
    }

    private void registerListeners(Listener[] listeners)
    {
        for (Listener l : listeners)
        {
            registerListener(l);
        }
    }

    private void registerListener(Listener l)
    {
        pluginManager.registerEvents(l, this);
    }

    /**
     * Can be removed in PluginBase 0.0.30
     */
    @Override
    public boolean acceptSchedules()
    {
        return true;
    }

    @Override
    public void startMainLoop(Runnable r)
    {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, o -> r.run(), 1, 1);
    }

    @Override
    public void runAsync(Runnable r)
    {
        Bukkit.getAsyncScheduler().runNow(this, o -> r.run());
    }

    @Override
    protected int getExceptionLimit()
    {
        return 3;
    }
}
