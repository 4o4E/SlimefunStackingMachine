package top.e404.slimefun.stackingmachine.machine

import io.github.sefiraat.networks.network.NetworkRoot
import io.github.sefiraat.networks.slimefun.network.NetworkController
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.inventory.ItemStack
import top.e404.eplugin.util.asString
import top.e404.slimefun.stackingmachine.PL

val categoryId by lazy { NamespacedKey(PL, "STACKING_MACHINE_CATEGORY") }
val categoryItem = CustomItemStack(Material.DIAMOND, "&4堆叠机器")
val group = ItemGroup(categoryId, categoryItem)
private fun findEmpty(b: Block): Block? {
    val x = b.x
    val y = b.y
    val z = b.z
    val world = b.world

    fun find(y: Int): Block? {
        for ((dx, dz) in listOf(
            Pair(0, 0),

            Pair(1, 0),
            Pair(0, 1),
            Pair(-1, 0),
            Pair(0, -1),

            Pair(1, 1),
            Pair(-1, 1),
            Pair(-1, -1),
            Pair(1, -1),
        )) {
            val block = world.getBlockAt(x + dx, y, z + dz)
            if (block.isEmpty) {
                return block
            }
        }
        return null
    }

    for (dy in y until world.maxHeight) {
        find(y)?.let { return it }
    }
    for (dy in y downTo 0) {
        find(y)?.let { return it }
    }
    return null
}

fun exportItem(b: Block, items: List<ItemStack>) {
    val temp = items.toMutableList()
    while (true) {
        val block = findEmpty(b) ?: run {
            PL.warn("无法找到${b.location.asString}方块周围的空间用于放置箱子")
            return
        }
        block.type = Material.CHEST
        val chest = block.state as Chest
        for (i in 0 until 27) {
            chest.blockInventory.setItem(i, temp.removeFirst().also { println("add $it") })
            // 全部导出了
            if (temp.isEmpty()) {
                return
            }
        }
    }
}



/**
 * 搜索相邻的网络, 并返回其root位置
 */
fun searchNetwork(location: Location): Pair<Location, NetworkRoot>? {
    val networkRoots = NetworkController.getNetworks().entries
    for (face in listOf(
        BlockFace.UP, BlockFace.DOWN,
        BlockFace.NORTH, BlockFace.WEST,
        BlockFace.SOUTH, BlockFace.EAST,
    )) {
        val l = location.clone().add(face.direction)
        networkRoots.firstOrNull { (_, v) -> l in v.nodeLocations }?.let { return it.toPair() }
    }
    return null
}