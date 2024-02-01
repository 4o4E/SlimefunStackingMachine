package top.e404.slimefun.stackingmachine.menu.machine

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import top.e404.eplugin.EPlugin.Companion.color
import top.e404.eplugin.menu.menu.ChestMenu
import top.e404.eplugin.menu.slot.BgSlot
import top.e404.eplugin.menu.slot.MenuButton
import top.e404.eplugin.menu.zone.MenuButtonZone
import top.e404.eplugin.util.buildItemStack
import top.e404.eplugin.util.emptyItem
import top.e404.slimefun.stackingmachine.PL
import top.e404.slimefun.stackingmachine.config.Lang
import top.e404.slimefun.stackingmachine.config.Template
import top.e404.slimefun.stackingmachine.menu.MenuManager
import top.e404.slimefun.stackingmachine.template.recipe.RecipeItem
import top.e404.slimefun.stackingmachine.template.recipe.RecipeType
import top.e404.slimefun.stackingmachine.template.recipe.WeightRecipeItem
import kotlin.math.max

/**
 * 展示合成表的菜单
 *
 * @property machineInfo 所有合成表
 */
class RecipesMenu(val machineInfo: Template, val type: RecipeType, val last: MachineMenu? = null) : ChestMenu(
    plugin = PL,
    row = 6,
    title = Lang["menu.recipes.title", "machine" to machineInfo.machineItem.itemName],
    allowSelf = false
) {
    var menuPage = 0
    val recipes = machineInfo.empty + machineInfo.recipes
    private val inputZone = object : MenuButtonZone<RecipeItem>(
        menu = this,
        x = 1,
        y = 1,
        w = 3,
        h = 3,
        data = recipes[menuPage].input.toMutableList()
    ) {
        override val inv = menu.inv
        override fun onClick(menuIndex: Int, zoneIndex: Int, itemIndex: Int, event: InventoryClickEvent) = true
    }
    private val outputZone = object : MenuButtonZone<RecipeItem>(
        menu = this,
        x = 5,
        y = 0,
        w = 4,
        h = 6,
        data = recipes[menuPage].output.toMutableList()
    ) {
        override val inv = menu.inv
        override fun onClick(menuIndex: Int, zoneIndex: Int, itemIndex: Int, event: InventoryClickEvent): Boolean {
            if (zoneIndex !in recipes[menuPage].output.indices) return true
            val recipeItem = recipes[menuPage].output[zoneIndex]
            if (recipeItem is WeightRecipeItem) {
                MenuManager.openMenu(RecipeItemsMenu(this@RecipesMenu, recipeItem), event.whoClicked)
            }
            return true
        }
    }

    fun refresh() {
        val templateRecipe = recipes[menuPage]

        inputZone.data.clear()
        inputZone.data.addAll(templateRecipe.input)

        outputZone.data.clear()
        outputZone.data.addAll(templateRecipe.output)

        condition.item = buildItemStack(
            Material.OAK_SIGN,
            name = "&6条件",
            lore = templateRecipe.conditions
                .map { it.display }
                .ifEmpty { emptyList() }
        )
        message.item = buildItemStack(
            Material.CRAFTING_TABLE,
            name = "&6配方",
            lore = listOf(
                "&f${if (type == RecipeType.MACHINE) "耗电" else "发电"}: ${recipes[menuPage].energy}".color(),
                "&f${if (type == RecipeType.MACHINE) "合成耗时" else "发电时长"}: ${recipes[menuPage].duration} sf tick".color(),
            )
        )
    }

    private val hasNext get() = menuPage < recipes.size - 1
    private val hasPrev get() = menuPage > 0
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
                menuPage += 1
                val player = event.whoClicked as Player
                player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F)
                refresh()
                menu.updateIcon()
            }
            return true
        }

        override fun updateItem() =
            if (hasNext) item = btn.also { it.amount = max(1, menuPage + 2) }
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
                menuPage -= 1
                val player = event.whoClicked as Player
                player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F)
                refresh()
                menu.updateIcon()
            }
            return true
        }

        override fun updateItem() =
            if (hasPrev) item = btn.also { it.amount = max(1, menuPage) }
            else item = emptyItem
    }
    private val back = object : MenuButton(this) {
        override var item = if (last == null) background else buildItemStack(Material.OAK_SIGN, name = "&f返回")
        override fun updateItem() {}
        override fun onClick(slot: Int, event: InventoryClickEvent): Boolean {
            if (last == null) return true
            MenuManager.openMenu(last, event.whoClicked)
            return true
        }
    }
    private val condition = BgSlot(this, buildItemStack(
        Material.OAK_SIGN,
        name = "&6条件",
        lore = recipes[menuPage].conditions
            .map { it.display }
            .ifEmpty { listOf("&f该配方没有条件限制".color()) }.also { println(it) }
    ))
    private val message = BgSlot(
        this, buildItemStack(
            Material.CRAFTING_TABLE,
            name = "&6配方",
            lore = listOf(
                "&f${if (type == RecipeType.MACHINE) "耗电" else "发电"}: ${recipes[menuPage].energy}J".color(),
                "&f${if (type == RecipeType.MACHINE) "合成耗时" else "发电时长"}: ${recipes[menuPage].duration} sf tick".color(),
            )
        )
    )

    private companion object {
        val background = buildItemStack(Material.BLACK_STAINED_GLASS_PANE, name = "")
    }

    init {
        initSlots(
            listOf(
                "b####oooo",
                "#iii#oooo",
                "#iii#oooo",
                "#iii#oooo",
                "#c#m#oooo",
                "#p#n#oooo",
            )
        ) { _, char ->
            when (char) {
                'p' -> prev
                'n' -> next
                'b' -> back
                'c' -> condition
                'm' -> message
                '#' -> BgSlot(this, background)
                else -> null
            }
        }
        zones.add(inputZone)
        zones.add(outputZone)
    }
}
