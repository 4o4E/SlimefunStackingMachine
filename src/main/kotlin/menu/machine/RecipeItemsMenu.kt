package top.e404.slimefun.stackingmachine.menu.machine

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import top.e404.eplugin.EPlugin.Companion.color
import top.e404.eplugin.menu.Displayable
import top.e404.eplugin.menu.menu.ChestMenu
import top.e404.eplugin.menu.slot.BgSlot
import top.e404.eplugin.menu.slot.MenuButton
import top.e404.eplugin.menu.zone.MenuButtonZone
import top.e404.eplugin.util.buildItemStack
import top.e404.eplugin.util.emptyItem
import top.e404.slimefun.stackingmachine.PL
import top.e404.slimefun.stackingmachine.config.Lang
import top.e404.slimefun.stackingmachine.menu.MenuManager
import top.e404.slimefun.stackingmachine.template.recipe.ExactRecipeItem
import top.e404.slimefun.stackingmachine.template.recipe.WeightRecipeItem
import kotlin.math.max

/**
 * 展示合成表的菜单
 */
class RecipeItemsMenu(val last: RecipesMenu, val item: WeightRecipeItem) : ChestMenu(
    plugin = PL,
    row = 6,
    title = Lang["menu.items.title"],
    allowSelf = false
) {
    private class Display(val origin: ExactRecipeItem, val total: Int) : Displayable {
        override val item = origin.getItemSingle().apply {
            if (type != Material.AIR) lore(
                listOf(
                    Component.text(
                        "&f概率: ${origin.weight} / $total (${
                            String.format(
                                "%.2f",
                                origin.weight * 100.0 / total
                            )
                        }%)".color()
                    )
                )
            )
        }

        override fun update() {}
        override var needUpdate: Boolean
            get() = false
            set(_) {}

    }

    private var menuPage = 0
    private val zone = object : MenuButtonZone<Display>(
        menu = this,
        x = 1,
        y = 1,
        w = 7,
        h = 4,
        data = item.list.let { items ->
            val total = items.sumOf { it.weight }
            items.map { Display(it, total) }.toMutableList()
        }
    ) {
        override val inv = menu.inv
        override fun onClick(menuIndex: Int, zoneIndex: Int, itemIndex: Int, event: InventoryClickEvent) = true
    }

    private val next = object : MenuButton(this) {
        private val btn = buildItemStack(
            Material.ARROW,
            1,
            Lang["menu.recipes.next.name"],
            Lang["menu.recipes.next.lore"].lines()
        )

        override var item = if (zone.hasNext) btn else emptyItem
        override fun onClick(slot: Int, event: InventoryClickEvent): Boolean {
            if (zone.hasNext) zone.nextPage()
            return true
        }

        override fun updateItem() =
            if (zone.hasNext) item = btn.also { it.amount = max(1, menuPage + 2) }
            else item = emptyItem
    }
    private val prev = object : MenuButton(this) {
        private val btn = buildItemStack(
            Material.ARROW,
            1,
            Lang["menu.recipes.prev.name"],
            Lang["menu.recipes.prev.lore"].lines()
        )

        override var item = if (zone.hasPrev) btn else emptyItem
        override fun onClick(slot: Int, event: InventoryClickEvent): Boolean {
            if (zone.hasPrev) zone.prevPage()
            return true
        }

        override fun updateItem() =
            if (zone.hasPrev) item = btn.also { it.amount = max(1, menuPage) }
            else item = emptyItem
    }
    private val back = object : MenuButton(this) {
        override var item = buildItemStack(Material.OAK_SIGN, name = "&f返回")
        override fun updateItem() {}
        override fun onClick(slot: Int, event: InventoryClickEvent): Boolean {
            MenuManager.openMenu(last, event.whoClicked)
            return true
        }
    }

    private companion object {
        val background = buildItemStack(Material.BLACK_STAINED_GLASS_PANE, name = "")
    }

    init {
        initSlots(
            listOf(
                "b########",
                "#       #",
                "#       #",
                "#       #",
                "#       #",
                "###p#n###",
            )
        ) { _, char ->
            when (char) {
                'p' -> prev
                'n' -> next
                'b' -> back
                '#' -> BgSlot(this, background)
                else -> null
            }
        }
        zones.add(zone)
    }
}
