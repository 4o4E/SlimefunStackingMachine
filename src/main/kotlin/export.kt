package top.e404.slimefun.stackingmachine

import kotlinx.serialization.Serializable
import org.bukkit.inventory.ItemStack
import top.e404.slimefun.stackingmachine.template.recipe.McRecipeItem
import top.e404.slimefun.stackingmachine.template.recipe.RecipeItem
import top.e404.slimefun.stackingmachine.template.recipe.SfRecipeItem

@Serializable
data class MachineRecipes(
    val machineId: String,
    val recipes: List<MachineRecipe>
)

@Serializable
data class MachineRecipe(
    val input: List<RecipeItem>,
    val output: List<RecipeItem>
)

fun ItemStack.toRecipeItem() = SfHook.getId(this)?.let { SfRecipeItem(it, amount) } ?: McRecipeItem(type.name, amount)