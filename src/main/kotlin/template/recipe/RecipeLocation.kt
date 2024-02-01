package top.e404.slimefun.stackingmachine.template.recipe

import top.e404.slimefun.stackingmachine.config.Template
import top.e404.slimefun.stackingmachine.template.TemplateRecipe
import java.io.File

enum class RecipeType(val display: String) {
    MACHINE("堆叠机器"),
    GENERATOR("堆叠发电机");
}

data class RecipeLocation(
    val type: RecipeType,
    val file: File,
    val template: Template,
    val isEmpty: Boolean? = null,
    val recipes: List<TemplateRecipe>? = null,
    val recipeIndex: Int? = null,
    val isInput: Boolean? = null,
    val items: List<RecipeItem>? = null,
    val itemIndex: Int? = null,
    val weightIndex: Int? = null
) {
    fun warn(message: String) = buildString {
        appendLine("配方校验失败: $message")
        appendLine("类型: ${type.display}")
        appendLine("文件: ${file.invariantSeparatorsPath}")
        appendLine("模板: ${template.machine}")
        if (recipes != null) appendLine("配方: ${recipes[recipeIndex!!].name}")
        if (isInput != null) appendLine("物品: ${if (isInput) "input" else "output"}[$itemIndex]${if (weightIndex != null) "[$weightIndex]" else ""}")
    }
}