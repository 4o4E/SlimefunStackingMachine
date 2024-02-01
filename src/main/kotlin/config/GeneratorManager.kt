package top.e404.slimefun.stackingmachine.config

import org.bukkit.command.CommandSender
import top.e404.eplugin.config.KtxConfig.Companion.defaultYaml
import top.e404.eplugin.config.KtxMultiFileConfig
import top.e404.slimefun.stackingmachine.PL
import top.e404.slimefun.stackingmachine.template.recipe.RecipeLocation
import top.e404.slimefun.stackingmachine.template.recipe.RecipeType

object GeneratorManager : KtxMultiFileConfig<Template>(
    plugin = PL,
    dirPath = "generator",
    Template.serializer(),
    format = defaultYaml
) {
    var templates = mapOf<String, Template>()
        private set

    override fun saveDefault(sender: CommandSender?) {
        if (dir.isFile) dir.delete()
        if (!dir.exists()) {
            dir.mkdir()
            TemplateManager::class.java.classLoader
                .getResource("generator.yml")!!
                .readText()
                .let { dir.resolve("generator.yml").writeText(it) }
        }
    }

    override fun load(sender: CommandSender?) {
        super.load(sender)
        val loadResult = mutableMapOf<String, Template>()
        for ((file, template) in this.config) {
            val valid = template.valid(RecipeLocation(RecipeType.GENERATOR, file, template))
            for ((location, message) in valid) {
                PL.sendAndWarn(sender, location.warn(message))
            }
            val exists = loadResult[template.machine]
            if (exists != null) {
                loadResult[template.machine] = Template(
                    template.machine,
                    exists.empty + template.empty,
                    exists.recipes + template.recipes
                )
                continue
            }
            loadResult[template.machine] = template
        }
        templates = loadResult
    }
}