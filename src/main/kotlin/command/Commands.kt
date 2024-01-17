package top.e404.slimefun.stackingmachine.command

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import top.e404.eplugin.command.AbstractDebugCommand
import top.e404.eplugin.command.ECommand
import top.e404.eplugin.command.ECommandManager
import top.e404.eplugin.config.KtxConfig
import top.e404.slimefun.stackingmachine.MachineRecipe
import top.e404.slimefun.stackingmachine.MachineRecipes
import top.e404.slimefun.stackingmachine.PL
import top.e404.slimefun.stackingmachine.config.Config
import top.e404.slimefun.stackingmachine.config.Lang
import top.e404.slimefun.stackingmachine.config.TemplateManager
import top.e404.slimefun.stackingmachine.toRecipeItem
import java.io.File

object Commands : ECommandManager(
    PL,
    "stackingmachine",
    object : AbstractDebugCommand(PL, "stackingmachine.admin") {
        override val usage get() = Lang["plugin_command.usage.debug"]
    },
    object : ECommand(PL, "reload", "(?i)reload|r", false, "stackingmachine.admin") {
        override val usage get() = Lang["plugin_command.usage.reload"]
        override fun onCommand(sender: CommandSender, args: Array<out String>) {
            plugin.runTaskAsync {
                Lang.load(sender)
                Config.load(sender)
                TemplateManager.load(sender)
                plugin.sendMsgWithPrefix(sender, Lang["plugin_command.reload_done"])
            }
        }
    },
    object : ECommand(PL, "export", "(?i)export", false, "stackingmachine.admin") {
        override val usage get() = Lang["plugin_command.usage.export"]
        override fun onCommand(sender: CommandSender, args: Array<out String>) {
            val exportDir = PL.dataFolder.resolve("export").also(File::mkdir)
            val export = Slimefun.getRegistry().allSlimefunItems.filterIsInstance<AContainer>().map { machine ->
                val recipes = machine.machineRecipes.map { machineRecipe ->
                    MachineRecipe(
                        machineRecipe.input.map(ItemStack::toRecipeItem),
                        machineRecipe.output.map(ItemStack::toRecipeItem)
                    )
                }
                MachineRecipes(machine.id, recipes)
            }.filter {
                it.recipes.isNotEmpty()
            }
            PL.runTaskAsync {
                export.forEach { machineRecipe ->
                    exportDir.resolve("${machineRecipe.machineId}.yml").writeText(
                        KtxConfig.defaultYaml.encodeToString(
                            MachineRecipes.serializer(), machineRecipe
                        )
                    )
                }
                plugin.sendMsgWithPrefix(sender, Lang["plugin_command.export_done"])
            }
        }
    },
)