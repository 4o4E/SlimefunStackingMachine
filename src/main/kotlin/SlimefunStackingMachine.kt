package top.e404.slimefun.stackingmachine

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon
import top.e404.eplugin.EPlugin
import top.e404.slimefun.stackingmachine.command.Commands
import top.e404.slimefun.stackingmachine.config.Config
import top.e404.slimefun.stackingmachine.config.Lang
import top.e404.slimefun.stackingmachine.config.TemplateManager
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

    override fun onEnable() {
        PL = this
        Config.load(null)
        Lang.load(null)
        TemplateManager.load(null)
        Commands.register()
        HookManager.register()
        SlimefunStackingMachineAddon.register()
        MenuManager.register()
        info("&a加载完成")
    }

    override fun onDisable() {
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
    }
}

lateinit var PL: SlimefunStackingMachine
    private set