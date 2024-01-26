package top.e404.slimefun.stackingmachine.template.recipe

import top.e404.slimefun.stackingmachine.config.Template
import top.e404.slimefun.stackingmachine.template.TemplateRecipe
import java.io.File

data class RecipeLocation(
    var file: File,
    var template: Template,
    var isEmpty: Boolean,
    var recipes: List<TemplateRecipe>,
    var recipeIndex: Int,
    var isInput: Boolean,
    var items: List<RecipeItem>,
    var itemIndex: Int,
    var weightIndex: Int? = null,
) {
    fun warn(message: String) = """配方校验失败: $message
        |模板: ${template.machine} (${file.absolutePath})
        |配方: ${recipes[recipeIndex].name}
        |物品: ${if (isInput) "input" else "output"}[$itemIndex]${if (weightIndex != null) "[$weightIndex]" else ""}
    """.trimMargin()
}

data class RecipeLocationBuilder(
    var file: File,
    var template: Template,
    var isEmpty: Boolean? = null,
    var recipes: List<TemplateRecipe>? = null,
    var recipeIndex: Int? = null,
    var isInput: Boolean? = null,
    var items: List<RecipeItem>? = null,
    var itemIndex: Int? = null,
    var weightIndex: Int? = null
) {
    fun build() = RecipeLocation(
        file, template, isEmpty!!, recipes!!, recipeIndex!!, isInput!!, items!!, itemIndex!!, weightIndex
    )
}