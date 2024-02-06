package top.e404.slimefun.stackingmachine.command

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import kotlinx.serialization.Serializable
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer
import me.mrCookieSlime.Slimefun.api.BlockStorage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.e404.eplugin.command.ECommand
import top.e404.eplugin.command.ECommandManager
import top.e404.eplugin.config.KtxConfig
import top.e404.eplugin.util.forEachOnline
import top.e404.slimefun.stackingmachine.PL
import top.e404.slimefun.stackingmachine.config.Config
import top.e404.slimefun.stackingmachine.config.GeneratorManager
import top.e404.slimefun.stackingmachine.config.Lang
import top.e404.slimefun.stackingmachine.config.TemplateManager
import top.e404.slimefun.stackingmachine.debug.MachineLogger
import top.e404.slimefun.stackingmachine.hook.SfHook
import top.e404.slimefun.stackingmachine.menu.MenuManager
import top.e404.slimefun.stackingmachine.menu.machine.MachineMenu
import top.e404.slimefun.stackingmachine.template.recipe.McRecipeItem
import top.e404.slimefun.stackingmachine.template.recipe.RecipeItem
import top.e404.slimefun.stackingmachine.template.recipe.RecipeType
import top.e404.slimefun.stackingmachine.template.recipe.SfRecipeItem
import java.io.File

object Commands : ECommandManager(
    PL,
    "stackingmachine",
    object : ECommand(PL, "debug", "(?i)debug|d", false, "stackingmachine.admin") {
        override val usage get() = Lang["plugin_command.usage.debug"]
        override fun onCommand(sender: CommandSender, args: Array<out String>) {
            if (args.size != 5) {
                PL.sendMsgWithPrefix(sender, usage)
                return
            }
            val (_, worlds, xs, ys, zs) = args
            val world = Bukkit.getWorld(worlds) ?: run { PL.sendMsgWithPrefix(sender, "&c不存在的世界"); return }
            val x = xs.toDoubleOrNull() ?: run { PL.sendMsgWithPrefix(sender, usage); return }
            val y = ys.toDoubleOrNull() ?: run { PL.sendMsgWithPrefix(sender, usage); return }
            val z = zs.toDoubleOrNull() ?: run { PL.sendMsgWithPrefix(sender, usage); return }
            val location = Location(world, x, y, z)
            val state = MachineLogger.switchDebug(location, sender)

            @Suppress("DEPRECATION")
            val itemName = BlockStorage.checkID(location)?.let { SfHook.getItem(it) }?.itemName ?: "无粘液方块"
            PL.sendMsgWithPrefix(sender, "已${if (state) "开启" else "关闭"}对应位置(${itemName}&7)的debug监听")
        }

        override fun onTabComplete(sender: CommandSender, args: Array<out String>, complete: MutableList<String>) {
            if (sender !is Player) return
            when (args.size) {
                2 -> complete.add(sender.world.name)
                3 -> complete.add(sender.location.blockX.toString())
                4 -> complete.add((sender.location.blockY - 1).toString())
                5 -> complete.add(sender.location.blockZ.toString())
                else -> {}
            }
        }
    },
    object : ECommand(PL, "reload", "(?i)reload|r", false, "stackingmachine.admin") {
        override val usage get() = Lang["plugin_command.usage.reload"]
        override fun onCommand(sender: CommandSender, args: Array<out String>) {
            plugin.runTaskAsync {
                Lang.load(sender)
                Config.load(sender)
                TemplateManager.load(sender)
                GeneratorManager.load(sender)
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
    object : ECommand(PL, "recipes", "(?i)recipes", false, "stackingmachine.admin") {
        override val usage get() = Lang["plugin_command.usage.recipes"]
        override fun onCommand(sender: CommandSender, args: Array<out String>) {
            if (args.size != 2) {
                PL.sendMsgWithPrefix(sender, usage)
                return
            }
            val player = Bukkit.getPlayer(args[1])
            if (player == null) {
                PL.sendNotPlayer(sender)
                return
            }
            MenuManager.openMenu(MachineMenu(TemplateManager.templates.values
                .sortedByDescending { it.recipes.size }
                .toMutableList(), RecipeType.MACHINE), player)
        }

        override fun onTabComplete(
            sender: CommandSender,
            args: Array<out String>,
            complete: MutableList<String>,
        ) {
            if (args.size == 2) forEachOnline { complete.add(it.name) }
        }
    },
    object : ECommand(PL, "generator", "(?i)generator", false, "stackingmachine.admin") {
        override val usage get() = Lang["plugin_command.usage.generator"]
        override fun onCommand(sender: CommandSender, args: Array<out String>) {
            if (args.size != 2) {
                PL.sendMsgWithPrefix(sender, usage)
                return
            }
            val player = Bukkit.getPlayer(args[1])
            if (player == null) {
                PL.sendNotPlayer(sender)
                return
            }
            MenuManager.openMenu(MachineMenu(GeneratorManager.templates.values
                .sortedByDescending { it.recipes.size }
                .toMutableList(), RecipeType.GENERATOR), player)
        }

        override fun onTabComplete(
            sender: CommandSender,
            args: Array<out String>,
            complete: MutableList<String>,
        ) {
            if (args.size == 2) forEachOnline { complete.add(it.name) }
        }
    },
)


@Serializable
private data class MachineRecipes(
    val machineId: String,
    val recipes: List<MachineRecipe>
)

@Serializable
private data class MachineRecipe(
    val input: List<RecipeItem>,
    val output: List<RecipeItem>
)

private fun ItemStack.toRecipeItem() = SfHook.getId(this)?.let { SfRecipeItem(it, amount) } ?: McRecipeItem(type.name, amount)