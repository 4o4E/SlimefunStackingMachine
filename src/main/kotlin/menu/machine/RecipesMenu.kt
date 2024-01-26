package top.e404.slimefun.stackingmachine.menu.machine

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import top.e404.eplugin.menu.menu.ChestMenu
import top.e404.eplugin.menu.slot.BgSlot
import top.e404.eplugin.menu.slot.MenuButton
import top.e404.eplugin.menu.zone.MenuButtonZone
import top.e404.eplugin.util.buildItemStack
import top.e404.eplugin.util.emptyItem
import top.e404.slimefun.stackingmachine.PL
import top.e404.slimefun.stackingmachine.config.Lang
import top.e404.slimefun.stackingmachine.template.recipe.RecipeItem
import top.e404.slimefun.stackingmachine.config.Template
import top.e404.slimefun.stackingmachine.menu.MenuManager
import kotlin.math.max

/**
 * 展示合成表的菜单
 *
 * @property machineInfo 所有合成表
 */
class RecipesMenu(val machineInfo: Template, val lastPage: Int) : ChestMenu(
    plugin = PL,
    row = 6,
    title = Lang["menu.recipes.title", "machine" to machineInfo.machineItem.itemName],
    allowSelf = false
) {
    var page = 0
    val inputZone = object : MenuButtonZone<RecipeItem>(
        menu = this,
        x = 1,
        y = 1,
        w = 3,
        h = 3,
        data = machineInfo.recipes[page].input.toMutableList()
    ) {
        override val inv = menu.inv
        override fun onClick(menuIndex: Int, zoneIndex: Int, itemIndex: Int, event: InventoryClickEvent) = true
    }
    val outputZone = object : MenuButtonZone<RecipeItem>(
        menu = this,
        x = 5,
        y = 0,
        w = 4,
        h = 6,
        data = machineInfo.recipes[page].output.toMutableList()
    ) {
        override val inv = menu.inv
        override fun onClick(menuIndex: Int, zoneIndex: Int, itemIndex: Int, event: InventoryClickEvent) = true
    }

    fun refresh() {
        inputZone.data.clear()
        val templateRecipe = machineInfo.recipes[page]
        inputZone.data.addAll(templateRecipe.input)

        outputZone.data.clear()
        outputZone.data.addAll(templateRecipe.output)
    }

    val hasNext get() = page < machineInfo.recipes.size - 1
    val hasPrev get() = page > 0
    private val next = object : MenuButton(this) {
        private val btn = buildItemStack(
            Material.ARROW,
            1,
            Lang["menu.recipes.next.name"],
            Lang["menu.recipes.next.lore"].lines()
        )

        override var item = if (hasNext) btn else emptyItem
        override fun onClick(slot: Int, event: InventoryClickEvent): Boolean {
            if (hasNext) {
                page += 1
                val player = event.whoClicked as Player
                player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F)
                refresh()
                menu.updateIcon()
            }
            return true
        }

        override fun updateItem() =
            if (hasNext) item = btn.also { it.amount = max(1, page + 2) }
            else item = emptyItem
    }
    private val prev = object : MenuButton(this) {
        private val btn = buildItemStack(
            Material.ARROW,
            1,
            Lang["menu.recipes.prev.name"],
            Lang["menu.recipes.prev.lore"].lines()
        )

        override var item = if (hasPrev) btn else emptyItem
        override fun onClick(slot: Int, event: InventoryClickEvent): Boolean {
            if (hasPrev) {
                page -= 1
                val player = event.whoClicked as Player
                player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F)
                refresh()
                menu.updateIcon()
            }
            return true
        }

        override fun updateItem() =
            if (hasPrev) item = btn.also { it.amount = max(1, page) }
            else item = emptyItem
    }
    private val back = object : MenuButton(this) {
        override var item = buildItemStack(Material.OAK_SIGN, name = "&f返回")
        override fun updateItem() {}
        override fun onClick(slot: Int, event: InventoryClickEvent): Boolean {
            MenuManager.openMenu(MachineMenu(lastPage), event.whoClicked)
            return true
        }
    }

    companion object {
        val background = buildItemStack(Material.BLACK_STAINED_GLASS_PANE, name = "")
    }

    init {
        initSlots(
            listOf(
                "b####oooo",
                "#iii#oooo",
                "#iii#oooo",
                "#iii#oooo",
                "#####oooo",
                "#p#n#oooo",
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
        zones.add(inputZone)
        zones.add(outputZone)
    }
}
