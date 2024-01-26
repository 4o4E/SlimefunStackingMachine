package top.e404.slimefun.stackingmachine.menu.machine

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import top.e404.eplugin.menu.menu.ChestMenu
import top.e404.eplugin.menu.slot.MenuButton
import top.e404.eplugin.menu.zone.MenuButtonZone
import top.e404.eplugin.util.buildItemStack
import top.e404.eplugin.util.emptyItem
import top.e404.slimefun.stackingmachine.PL
import top.e404.slimefun.stackingmachine.config.Lang
import top.e404.slimefun.stackingmachine.config.Template
import top.e404.slimefun.stackingmachine.config.TemplateManager
import top.e404.slimefun.stackingmachine.menu.MenuManager
import kotlin.math.max

class MachineMenu : ChestMenu(PL, 6, Lang["menu.machine.title"], false) {
    val data = TemplateManager.templates.values.toMutableList().apply { sortByDescending { it.recipes.size } }
    val zone = object : MenuButtonZone<Template>(this, 0, 0, 9, 5, data) {
        override val inv = menu.inv
        override fun onClick(menuIndex: Int, zoneIndex: Int, itemIndex: Int, event: InventoryClickEvent): Boolean {
            val recipes = data.getOrNull(itemIndex) ?: return true
            val player = event.whoClicked as Player
            MenuManager.openMenu(RecipesMenu(this@MachineMenu, recipes), player)
            return true
        }
    }
    private val next = object : MenuButton(this) {
        private val btn = buildItemStack(
            Material.ARROW,
            1,
            Lang["menu.machine.next.name"],
            Lang["menu.machine.next.lore"].lines()
        )

        override var item = if (zone.hasNext) btn else emptyItem
        override fun onClick(slot: Int, event: InventoryClickEvent): Boolean {
            if (zone.hasNext) {
                val player = event.whoClicked as Player
                player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F)
                zone.nextPage()
                menu.updateIcon()
            }
            return true
        }

        override fun updateItem() =
            if (!zone.hasNext) item = emptyItem
            else item = btn.also { it.amount = max(1, zone.page + 2) }
    }
    private val prev = object : MenuButton(this) {
        private val btn = buildItemStack(
            Material.ARROW,
            1,
            Lang["menu.machine.prev.name"],
            Lang["menu.machine.prev.lore"].lines()
        )

        override var item = if (zone.hasPrev) btn else emptyItem
        override fun onClick(slot: Int, event: InventoryClickEvent): Boolean {
            if (zone.hasPrev) {
                val player = event.whoClicked as Player
                player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F)
                zone.prevPage()
                menu.updateIcon()
            }
            return true
        }

        override fun updateItem() =
            if (!zone.hasPrev) item = emptyItem
            else item = btn.also { it.amount = max(1, zone.page) }
    }

    init {
        initSlots(
            listOf(
                "         ",
                "         ",
                "         ",
                "         ",
                "         ",
                "  p   n  ",
            )
        ) { _, char ->
            when (char) {
                'p' -> prev
                'n' -> next
                else -> null
            }
        }
        zones.add(zone)
    }
}
