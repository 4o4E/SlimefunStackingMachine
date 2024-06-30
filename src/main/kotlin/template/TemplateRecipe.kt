package top.e404.slimefun.stackingmachine.template

import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import top.e404.eplugin.EPlugin.Companion.color
import top.e404.slimefun.stackingmachine.template.condition.RecipeCondition
import top.e404.slimefun.stackingmachine.template.recipe.ExactRecipeItem
import top.e404.slimefun.stackingmachine.template.recipe.RecipeItem
import top.e404.slimefun.stackingmachine.template.recipe.RecipeLocation
import top.e404.slimefun.stackingmachine.template.recipe.RecipeType

/**
 * 模板配方
 *
 * @property name 名字
 * @property duration 制作时间 sf tick
 * @property energy 消耗能量 总能耗 = duration * energy
 * @property order 有序配方
 * @property input 输入
 * @property output 输出
 */
@Serializable
data class TemplateRecipe(
    val name: String,
    val duration: Int,
    val energy: Int,
    val order: Boolean = false,
    val conditions: List<RecipeCondition> = emptyList(),
    val input: List<ExactRecipeItem> = emptyList(),
    val output: List<RecipeItem> = emptyList(),
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
     * 生成产物, 按照倍率生成具体产物并合并
     *
     * @param magnification 倍率
     * @return 合并后的产物, 不会包含AIR
     */
    fun getResult(magnification: Int) = buildList {
        for (recipeItem in output) {
            // 随机产出
            repeat(magnification) {
                val exact = recipeItem.exact()
                if (exact.item.type != Material.AIR) add(exact)
            }
        }
    }.groupBy { it.type to it.id }.values.map { exacts ->
        exacts.first().withAmount(exacts.sumOf { it.amount })
    }

    fun display(magnification: Int): List<Component> {
        return buildList {
            add(Component.text("&f倍率: $magnification".color))
            add(Component.text("&f输入:".color))
            for (item in input) {
                add(item.display(magnification))
            }
            add(Component.text("&f输出:".color))
            for (item in output) {
                add(item.display(magnification))
            }
        }
    }

    fun valid(location: RecipeLocation) = buildList {
        if (input.isEmpty()) {
            if (location.isEmpty != true) add(location to "input中未包含物品")
        }
        else for ((itemIndex, item) in input.withIndex()) {
            addAll(item.valid(location.copy(isInput = true, items = input, itemIndex = itemIndex)))
        }
        if (output.isEmpty() && location.type == RecipeType.MACHINE) add(location to "output中未包含物品")
        else for ((itemIndex, item) in output.withIndex()) {
            addAll(item.valid(location.copy(isInput = false, items = output, itemIndex = itemIndex)))
        }
    }
}