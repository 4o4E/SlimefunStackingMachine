package top.e404.slimefun.stackingmachine

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon
import org.bukkit.Location
import top.e404.eplugin.EPlugin
import top.e404.slimefun.stackingmachine.command.Commands
import top.e404.slimefun.stackingmachine.config.Config
import top.e404.slimefun.stackingmachine.config.Data
import top.e404.slimefun.stackingmachine.config.GeneratorManager
import top.e404.slimefun.stackingmachine.config.Lang
import top.e404.slimefun.stackingmachine.config.TemplateManager
import top.e404.slimefun.stackingmachine.debug.MachineLogger
import top.e404.slimefun.stackingmachine.hook.HookManager
import top.e404.slimefun.stackingmachine.machine.StackingGenerator
import top.e404.slimefun.stackingmachine.machine.StackingMachine
import top.e404.slimefun.stackingmachine.menu.MenuManager

class SlimefunStackingMachine : EPlugin() {
    override val debugPrefix get() = langManager.getOrElse("debug_prefix") { "&7[&bStackingMachineDebug&7]" }
    override val prefix get() = langManager.getOrElse("prefix") { "&7[&aStackingMachine&7]" }

    override var debug: Boolean
        get() = Config.debug
        set(value) {
            Config.debug = value
        }
    override val langManager by lazy { Lang }
    fun debug(location: Location, msg: () -> String) = MachineLogger.debug(location, msg)

    override fun onEnable() {
        PL = this
        Config.load(null)
        Lang.load(null)
        Data.load(null)
        TemplateManager.load(null)
        GeneratorManager.load(null)
        Commands.register()
        HookManager.register()
        SlimefunStackingMachineAddon.register()
        MenuManager.register()
        MachineLogger.register()
        info("&a加载完成")
    }

    override fun onDisable() {
        Data.saveImmediately()
        MenuManager.shutdown()
        cancelAllTask()
        info("&a卸载完成")
    }
}

object SlimefunStackingMachineAddon : SlimefunAddon {
    override fun getJavaPlugin() = PL
    override fun getBugTrackerURL() = null
    fun register() {
        StackingMachine.register(this)
        StackingGenerator.register(this)
    }
}

lateinit var PL: SlimefunStackingMachine
    private set