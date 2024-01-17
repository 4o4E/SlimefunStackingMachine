@file:Suppress("DEPRECATION", "UNUSED")

package top.e404.slimefun.stackingmachine

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

data class MenuTemplateCache(
    val char: Char,
    val template: Pair<ItemStack, ChestMenu.MenuClickHandler>? = null,
    val slots: MutableList<Int> = mutableListOf()
)

data class MenuCache(val rows: Int, val title: String, val map: MutableMap<Char, MenuTemplateCache>) :
    Map<Char, MenuTemplateCache> by map {
    fun forEach(template: Char, block: (slot: Int, item: ItemStack?, handler: ChestMenu.MenuClickHandler?) -> Unit) {
        val cache = map[template] ?: return
        val (item, handler) = cache.template ?: return
        for (slot in cache.slots) block(slot, item, handler)
    }

    fun forEach(block: (char: Char, slot: Int, item: ItemStack?, handler: ChestMenu.MenuClickHandler?) -> Unit) {
        for ((char, cache) in map) {
            val (item, handler) = cache.template ?: continue
            for (slot in cache.slots) block(char, slot, item, handler)
        }
    }

    fun getSlots(template: Char) = get(template)?.slots ?: Collections.emptyList()
    fun getSlot(template: Char) = get(template)!!.slots.first()
    fun getSlotOrNull(template: Char) = get(template)?.slots?.firstOrNull()

    fun updateSlots(inv: Inventory, template: Char, item: ItemStack?) {
        forEach(template) { slot, _, _ -> inv.setItem(slot, item) }
    }

    fun create(item: SlimefunItem, block: InventoryBlock) = apply {
        block.createPreset(item, title) {
            it.setSize(rows * 9)
            forEach { _, index, item, handler ->
                it.addItem(index, item, handler)
            }
        }
    }
}

val DO_NOTHING_HANDLER = ChestMenu.MenuClickHandler { _, _, _, _ -> true }
val DENY_TOUCH inline get() = ChestMenuUtils.getEmptyClickHandler()

fun buildMenu(
    title: String,
    vararg template: String,
    convert: (index: Int, char: Char) -> Pair<ItemStack, ChestMenu.MenuClickHandler>?
): MenuCache {
    require(template.size < 6 && template.isNotEmpty()) { "模板数量必须在[1,6]之内" }
    require(template.all { it.length == 9 }) { "模板每行的长度必须是9" }
    val menuItems = mutableMapOf<Char, MenuTemplateCache>()
    listOf(*template).joinToString("").forEachIndexed { index, char ->
        menuItems.getOrPut(char) {
            val result = convert(index, char)
            MenuTemplateCache(char, result)
        }.slots.add(index)
    }
    return MenuCache(template.size, title, menuItems)
}