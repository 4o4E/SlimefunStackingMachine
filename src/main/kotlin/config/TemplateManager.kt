package top.e404.slimefun.stackingmachine.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import top.e404.eplugin.EPlugin.Companion.color
import top.e404.eplugin.config.KtxConfig.Companion.defaultYaml
import top.e404.eplugin.config.KtxMultiFileConfig
import top.e404.eplugin.menu.Displayable
import top.e404.eplugin.table.Tableable
import top.e404.eplugin.table.choose
import top.e404.eplugin.util.buildItemStack
import top.e404.eplugin.util.editItemMeta
import top.e404.eplugin.util.materialOf
import top.e404.eplugin.util.name
import top.e404.slimefun.stackingmachine.PL
import top.e404.slimefun.stackingmachine.SfHook

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
        for (template in this.config.values) {
            val exists = loadResult[template.machine]
            if (exists != null) {
                loadResult[template.machine] = Template(
                    template.machine,
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
    val recipes: List<TemplateRecipe>
) : Displayable {
    val machineItem by lazy {
        SfHook.getItem(machine) ?: throw IllegalArgumentException("unknown machine id: $machine")
    }

    override val item get() = ItemStack(machineItem.item).editItemMeta {
        lore = listOf("&f共 &6${recipes.size} &f个配方".color())
    }
    override var needUpdate
        get() = false
        set(_) {}

    override fun update() {}
}

/**
 * 模板配方
 *
 * @property name 名字
 * @property duration 制作时间 sf tick
 * @property energy 消耗能量 总能耗 = duration * energy
 * @property randomOut 产物随机模式 产物会按照权重进行随机抽取 只有一种产物
 * @property order 有序配方
 * @property input 输入
 * @property output 输出
 */
@Serializable
data class TemplateRecipe(
    val name: String,
    val duration: Int,
    val energy: Int,
    @SerialName("random_out")
    val randomOut: Boolean = false,
    val order: Boolean = false,
    val input: List<RecipeItem>,
    val output: List<RecipeItem>,
) {
    /**
     * 检查机器中的物品模板是否对应该模板
     *
     * @param template 物品模板
     */
    fun match(template: List<ItemStack>): Boolean {
        // 有序
        if (order) {
            for (i in 1..9) {
                if (!input[i].match(template[i])) {
                    return false
                }
            }
            return true
        }
        // 无序
        val temp = ArrayList(input)
        template@ for (item in template) {
            val iterator = temp.iterator()
            while (iterator.hasNext()) {
                val recipeItem = iterator.next()
                if (recipeItem.match(item)) {
                    iterator.remove()
                    continue@template
                }
            }
            // 该配方没有找到对应的物品
            return false
        }
        // 若为空则全匹配
        return temp.isEmpty()
    }

    /**
     * 生成产物 若randomOut则通过权重随机
     *
     * @param magnification 倍率
     */
    fun getResult(magnification: Int) =
        if (randomOut) (1..magnification).map { output.choose() }.merge()
        else output.flatMap { it.getItems(magnification) }

    fun display(magnification: Int): List<String> {
        return buildList {
            add("&f倍率: $magnification")
            add("&f输入:")
            for (recipeItem in input) {
                add("&f${recipeItem.display()}x${recipeItem.amount * magnification}")
            }
            add("&f输出:")
            for (recipeItem in output) {
                add("&f${recipeItem.display()}x${recipeItem.amount * magnification}")
            }
        }
    }
}

@Serializable
sealed interface RecipeItem : Tableable<ItemStack>, Displayable {
    val display: String?
    val id: String
    val amount: Int
    override val weight: Int
    override fun generator(): ItemStack
    fun getItems(magnification: Int) = this.generator().stacking(magnification)
    fun match(item: ItemStack): Boolean

    /**
     * 字符格式预览
     */
    fun display(): String
    override var needUpdate
        get() = false
        set(_) {}

    override fun update() {}
}

@Serializable
@SerialName("sf")
data class SfRecipeItem(
    override val id: String,
    override val amount: Int,
    override val weight: Int = 1,
    override val display: String? = null,
) : RecipeItem {
    private val itemTemplate by lazy {
        SfHook.getItem(id)?.item?.let(::ItemStack)?.also { it.amount = amount }
            ?: throw IllegalArgumentException("$id 不是sf物品id")
    }

    override fun generator() = itemTemplate.clone()
    override fun match(item: ItemStack) = SfHook.getId(item)?.equals(id, true) == true
    override fun display() = display ?: itemTemplate.name ?: itemTemplate.type.name

    override val item get() = itemTemplate.clone()
}

@Serializable
@SerialName("mc")
data class McRecipeItem(
    override val id: String,
    override val amount: Int,
    override val weight: Int = 1,
    override val display: String? = null,
) : RecipeItem {
    private val itemTemplate by lazy {
        buildItemStack(
            materialOf(id)
                ?: throw IllegalArgumentException("$id 不是mc物品id"),
            amount = amount
        )
    }

    override fun generator() = itemTemplate.clone()
    override fun match(item: ItemStack) = item.type.name.equals(id, true)
    override fun display() = display ?: itemTemplate.type.name

    override val item get() = itemTemplate.clone()
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