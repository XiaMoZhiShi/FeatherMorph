package xiamomc.morph.commands;

import xiamomc.morph.MorphPlugin;
import xiamomc.morph.commands.subcommands.MorphSubCommandHandler;
import xiamomc.morph.commands.subcommands.plugin.HelpSubCommand;
import xiamomc.morph.commands.subcommands.plugin.ReloadSubCommand;
import xiamomc.morph.commands.subcommands.plugin.ToggleSelfSubCommand;
import xiamomc.pluginbase.Command.ISubCommand;
import xiamomc.pluginbase.Command.SubCommandHandler;

import java.util.List;

public class MorphPluginCommand extends MorphSubCommandHandler {
    @Override
    public String getCommandName() {
        return "mmorph";
    }

    @Override
    public String getPermissionRequirement() {
        return null;
    }

    @Override
    public String getHelpMessage() {
        return "插件指令";
    }

    private final List<ISubCommand> subCommands = List.of(
            new ReloadSubCommand(),
            new HelpSubCommand(),
            new ToggleSelfSubCommand()
    );

    @Override
    public List<ISubCommand> getSubCommands() {
        return subCommands;
    }
}
