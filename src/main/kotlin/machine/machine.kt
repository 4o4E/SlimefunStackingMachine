package top.e404.slimefun.stackingmachine.machine

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack
import org.bukkit.Material
import org.bukkit.NamespacedKey
import top.e404.slimefun.stackingmachine.PL

val categoryId by lazy { NamespacedKey(PL, "STACKING_MACHINE_CATEGORY") }
val categoryItem = CustomItemStack(Material.DIAMOND, "&4堆叠机器")
val group = ItemGroup(categoryId, categoryItem)