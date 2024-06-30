package top.e404.slimefun.stackingmachine.template.recipe

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import top.e404.eplugin.EPlugin.Companion.color
import top.e404.eplugin.adventure.display
import top.e404.eplugin.menu.Displayable
import top.e404.eplugin.table.chooseBy
import top.e404.eplugin.util.buildItemStack
import top.e404.eplugin.util.materialOf
import top.e404.slimefun.stackingmachine.hook.MiHook
import top.e404.slimefun.stackingmachine.hook.SfHook

/**
 * 配方物品
 */
@Serializable
sealed interface RecipeItem : Displayable {
    val weight: Int
    val type: String

    /**
     * 字符格式预览
     */
    fun display(magnification: Int): Component
    override var needUpdate
        get() = false
        set(_) {}

    override fun update() {}

    /**
     * 检查该物品格式是否正确
     */
    fun valid(location: RecipeLocation): List<Pair<RecipeLocation, String>>

    fun exact(): ExactRecipeItem
    
    fun match(item: ItemStack): Boolean
    fun similar(item: ItemStack): ItemStack?
}

/**
 * 确切的配方物品
 */
@Serializable
sealed interface ExactRecipeItem : RecipeItem {
    val id: String
    val amount: Int
    val display: String?
    override val weight: Int

    /**
     * 获取单个物品
     */
    fun getItemSingle(): ItemStack

    /**
     * 检测物品是否匹配
     */
    override fun match(item: ItemStack): Boolean

    override fun exact() = this
    
    fun withAmount(amount: Int): ExactRecipeItem
}

/**
 * 根据权重随机的配方物品
 */
@Serializable
@SerialName("weight")
data class WeightRecipeItem(
    val list: List<ExactRecipeItem>,
    val display: String? = null,
    override val weight: Int = 1,
) : RecipeItem {
    override val type get() = "weight"
    override fun display(magnification: Int) = display?.let { Component.text(it) } ?: Component.join(
        JoinConfiguration.separator(Component.text("/")),
        list.map { it.display(magnification) }
    )

    override val item by lazy {
        buildItemStack(Material.CHEST, name = "${list.size}种产物中随机产一个") {
            lore(list.map { it.display(1) })
            addEnchant(Enchantment.DURABILITY, 1, true)
            addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
    }

    override fun valid(location: RecipeLocation) =
        if (list.isEmpty()) listOf(location to "随机产物随机列表为空")
        else list.flatMapIndexed { index, exact -> exact.valid(location.copy(weightIndex = index)) }

    override fun exact() = list.chooseBy { it.weight }

    override fun match(item: ItemStack) = list.any { it.match(item) }

    override fun similar(item: ItemStack) = list.firstNotNullOfOrNull { it.similar(item) }
}

@Serializable
@SerialName("sf")
data class SfRecipeItem(
    override val id: String,
    override val amount: Int,
    override val display: String? = null,
    override val weight: Int = 1,
) : ExactRecipeItem {
    override val type get() = "sf"
    private val sfItem by lazy { SfHook.getItem(id) ?: error("$id 不是sf物品id") }
    private val itemTemplate by lazy { ItemStack(sfItem.item) }

    override fun getItemSingle() = itemTemplate.clone()
    override fun match(item: ItemStack) = SfHook.getId(item)?.equals(id, true) == true
    override fun display(magnification: Int) = Component.text("${display ?: sfItem.itemName}x${amount * magnification}")

    override val item get() = itemTemplate.clone()
    override fun withAmount(amount: Int) = copy(amount = amount)
    override fun valid(location: RecipeLocation) = buildList {
        val l = location
        if (amount <= 0) add(l to "amount必须大于0")
        if (weight < 1) add(l to "weight必须大于0")
        if (SfHook.getItem(id) == null) add(l to "无效slimefun物品id: $id")
    }

    override fun similar(item: ItemStack) = if (item.type == itemTemplate.type) itemTemplate.clone() else null
}

@Serializable
@SerialName("mc")
data class McRecipeItem(
    override val id: String,
    override val amount: Int,
    override val display: String? = null,
    override val weight: Int = 1,
) : ExactRecipeItem {
    override val type get() = "mc"
    private val itemTemplate by lazy {
        buildItemStack(materialOf(id) ?: error("$id 不是mc物品id"))
    }

    override fun getItemSingle() = itemTemplate.clone()
    override fun match(item: ItemStack): Boolean {
        if (SfHook.getId(item) != null) return false
        if (MiHook.enable && MiHook.getType(item) != null) return false
        return item.type.name.equals(id, true)
    }

    override fun display(magnification: Int) = (display
        ?.let { Component.text(it) }
        ?: if (itemTemplate.type == Material.AIR) Component.text("&f空气".color) else itemTemplate.display)
        .append(Component.text("x${amount * magnification}"))

    override val item get() = itemTemplate.clone()
    override fun withAmount(amount: Int) = copy(amount = amount)
    override fun valid(location: RecipeLocation) = buildList {
        if (amount <= 0) add(location to "amount必须大于0")
        if (weight < 1) add(location to "weight必须大于0")
        if (materialOf(id) == null) add(location to "无效物品id: $id")
    }

    override fun similar(item: ItemStack) = if (item.type.name.equals(id, true)) getItemSingle() else null
}