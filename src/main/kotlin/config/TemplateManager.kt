package top.e404.slimefun.stackingmachine.config

import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import top.e404.eplugin.EPlugin.Companion.color
import top.e404.eplugin.config.KtxConfig.Companion.defaultYaml
import top.e404.eplugin.config.KtxMultiFileConfig
import top.e404.eplugin.menu.Displayable
import top.e404.slimefun.stackingmachine.PL
import top.e404.slimefun.stackingmachine.SfHook
import top.e404.slimefun.stackingmachine.template.TemplateRecipe
import top.e404.slimefun.stackingmachine.template.recipe.RecipeLocationBuilder

object TemplateManager : KtxMultiFileConfig<Template>(
    plugin = PL,
    dirPath = "template",
    Template.serializer(),
    format = defaultYaml
) {
    var templates = mapOf<String, Template>()
        private set

    override fun saveDefault(sender: CommandSender?) {
        if (dir.isFile) dir.delete()
        if (!dir.exists()) {
            dir.mkdir()
            TemplateManager::class.java.classLoader
                .getResource("template.yml")!!
                .readText()
                .let { dir.resolve("example.yml").writeText(it) }
        }
    }

    override fun load(sender: CommandSender?) {
        super.load(sender)
        val loadResult = mutableMapOf<String, Template>()
        for ((file, template) in this.config) {
            val valid = template.valid(RecipeLocationBuilder(file, template))
            for ((location, message) in valid) {
                PL.sendAndWarn(sender, location.warn(message))
            }
            val exists = loadResult[template.machine]
            if (exists != null) {
                loadResult[template.machine] = Template(
                    template.machine,
                    exists.empty + template.empty,
                    exists.recipes + template.recipes
                )
                continue
            }
            loadResult[template.machine] = template
        }
        templates = loadResult
    }
}

@Serializable
data class Template(
    val machine: String,
    val empty: List<TemplateRecipe>,
    val recipes: List<TemplateRecipe>,
) : Displayable {
    val machineItem by lazy {
        SfHook.getItem(machine) ?: throw IllegalArgumentException("unknown machine id: $machine")
    }

    override val item
        get() = ItemStack(machineItem.item).apply {
            lore(listOf(Component.text("&f共 &6${recipes.size} &f个配方".color())))
        }
    override var needUpdate
        get() = false
        set(_) {}

    override fun update() {}

    fun valid(location: RecipeLocationBuilder) = buildList {
        for ((i, recipe) in empty.withIndex()) {
            addAll(recipe.valid(location.copy(isEmpty = true, recipes = empty, recipeIndex = i)))
        }
        for ((i, recipe) in recipes.withIndex()) {
            addAll(recipe.valid(location.copy(isEmpty = false, recipes = recipes, recipeIndex = i)))
        }
    }
}

private fun ItemStack.stacking(magnification: Int) = buildList {
    val total = magnification * amount
    repeat(total / maxStackSize) {
        add(clone().apply { amount = maxStackSize })
    }
    val rest = total % maxStackSize
    if (rest != 0) add(clone().apply { amount = rest })
}

private fun Collection<ItemStack>.merge(): List<ItemStack> {
    val items = mutableMapOf<ItemStack, Int>()
    for (i in this) {
        val key = items.keys.firstOrNull { i.isSimilar(it) }
        if (key != null) {
            items[key] = items[key]!! + i.amount
            continue
        }
        items[i] = i.amount
    }
    return buildList {
        for ((item, count) in items) {
            repeat(count / item.maxStackSize) {
                add(item.clone().apply { amount = maxStackSize })
            }
            val i = count % item.maxStackSize
            if (i != 0) add(item.clone().apply { amount = i })
        }
    }
}