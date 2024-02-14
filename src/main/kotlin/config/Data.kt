package top.e404.slimefun.stackingmachine.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import org.bukkit.Location
import top.e404.eplugin.config.KtxMapConfig
import top.e404.eplugin.config.TextConfigDefault
import top.e404.eplugin.config.serialization.InlineLocationSerialization
import top.e404.eplugin.serialization.adventure.ComponentSerializer
import top.e404.slimefun.stackingmachine.PL
import top.e404.slimefun.stackingmachine.template.TemplateRecipe
import top.e404.slimefun.stackingmachine.template.recipe.ExactRecipeItem

object Data : KtxMapConfig<Location, Progress>(
    plugin = PL,
    path = "data.json",
    default = TextConfigDefault("{}"),
    kSerializer = InlineLocationSerialization,
    vSerializer = Progress.serializer(),
    format = Json
)

@Serializable
data class Progress(
    var progress: Int,
    val recipe: TemplateRecipe,
    var output: List<ExactRecipeItem>,
    val display: List<@Serializable(ComponentSerializer::class) Component>,
    val magnification: Int,
)