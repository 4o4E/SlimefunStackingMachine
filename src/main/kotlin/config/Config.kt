package top.e404.slimefun.stackingmachine.config

import kotlinx.serialization.Serializable
import top.e404.eplugin.config.JarConfigDefault
import top.e404.eplugin.config.KtxConfig
import top.e404.slimefun.stackingmachine.PL

object Config : KtxConfig<Config.Config>(
    plugin = PL,
    path = "config.yml",
    default = JarConfigDefault(PL, "config.yml"),
    serializer = Config.serializer()
) {
    var debug: Boolean
        get() = config.debug
        set(value) {
            config.debug = value
        }

    @Serializable
    data class Config(
        var debug: Boolean,
    )
}