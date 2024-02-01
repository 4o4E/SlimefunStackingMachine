@file:Suppress("DEPRECATION")

package top.e404.slimefun.stackingmachine.template.condition

import io.github.sefiraat.networks.network.NetworkRoot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.mrCookieSlime.Slimefun.api.BlockStorage
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import top.e404.eplugin.config.serialization.IntRangeSerialization
import top.e404.eplugin.util.materialOf
import top.e404.slimefun.stackingmachine.SfHook

@Serializable
sealed interface RecipeCondition {
    /**
     * 该判断在菜单中的显示
     */
    val display: String

    /**
     * 判断配方是否允许匹配
     *
     * @param block 堆叠机器所在的方块
     * @return 是否允许匹配
     */
    fun condition(block: Block, network: NetworkRoot): Boolean
}

@Serializable
@SerialName("world_name")
data class WorldNameCondition(
    override val display: String,
    val worlds: List<String>
) : RecipeCondition {
    override fun condition(block: Block, network: NetworkRoot) = block.world.name in worlds
}

@Serializable
@SerialName("biome")
data class BiomeCondition(
    override val display: String,
    val biomes: List<String>
) : RecipeCondition {
    override fun condition(block: Block, network: NetworkRoot) = block.biome.name in biomes
}

@Serializable
@SerialName("block")
data class BlockCondition(
    override val display: String,
    val list: List<String>,
    val direction: Direction
) : RecipeCondition {
    @Serializable
    data class Direction(val x: Int = 0, val y: Int = 0, val z: Int)

    override fun condition(block: Block, network: NetworkRoot) = block.location.add(
        direction.x.toDouble(),
        direction.y.toDouble(),
        direction.z.toDouble()
    ).let { target ->
        list.any {
            val (type, id) = it.lowercase().split(":")
            when (type.lowercase()) {
                "mc" -> id.equals(target.block.type.name, true)
                "sf" -> id.equals(BlockStorage.getLocationInfo(target, "id"), true)
                else -> error("unknown type: $type")
            }
        }
    }
}

@Serializable
@SerialName("time")
data class TimeCondition(
    override val display: String,
    @Serializable(IntRangeSerialization::class) val range: IntRange,
) : RecipeCondition {
    override fun condition(block: Block, network: NetworkRoot) = block.world.time % 24000 in range
}

@Serializable
@SerialName("require_item")
data class RequireItemCondition(
    override val display: String,
    val item: Item
) : RecipeCondition {
    override fun condition(block: Block, network: NetworkRoot) =
        item.amount
            ?.let { amount ->
                network.allNetworkItems.entries.filter { item.match(it.key) }.sumOf { it.value } > amount
            }
            ?: network.allNetworkItems.keys.any(item::match)

    @Serializable
    sealed interface Item {
        val id: String
        val amount: Int?
        fun match(item: ItemStack): Boolean
    }

    @Serializable
    @SerialName("mc")
    data class McItem(
        override val id: String,
        override val amount: Int? = null
    ) : Item {
        private val material by lazy { materialOf(id) ?: error("未知的mc物品id: $id") }
        override fun match(item: ItemStack) = item.type == material
    }

    @Serializable
    @SerialName("sf")
    data class SfItem(
        override val id: String,
        override val amount: Int? = null
    ) : Item {
        override fun match(item: ItemStack) = SfHook.getId(item) == id
    }
}

@Serializable
@SerialName("any")
data class AnyCondition(
    override val display: String,
    val conditions: List<RecipeCondition>
) : RecipeCondition {
    override fun condition(block: Block, network: NetworkRoot) = conditions.any { it.condition(block, network) }
}

@Serializable
@SerialName("all")
data class AllCondition(
    override val display: String,
    val conditions: List<RecipeCondition>
) : RecipeCondition {
    override fun condition(block: Block, network: NetworkRoot) = conditions.all { it.condition(block, network) }
}