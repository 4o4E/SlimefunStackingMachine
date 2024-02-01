package top.e404.slimefun.stackingmachine.machine

import io.github.sefiraat.networks.network.NetworkRoot
import io.github.sefiraat.networks.network.stackcaches.ItemRequest
import io.github.sefiraat.networks.slimefun.network.NetworkController
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker
import me.mrCookieSlime.Slimefun.api.BlockStorage
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack
import top.e404.eplugin.EPlugin.Companion.color
import top.e404.eplugin.util.asString
import top.e404.eplugin.util.buildItemStack
import top.e404.eplugin.util.editItemMeta
import top.e404.eplugin.util.emptyItem
import top.e404.slimefun.stackingmachine.DENY_TOUCH
import top.e404.slimefun.stackingmachine.PL
import top.e404.slimefun.stackingmachine.SfHook
import top.e404.slimefun.stackingmachine.buildMenu
import top.e404.slimefun.stackingmachine.config.Data
import top.e404.slimefun.stackingmachine.config.Progress
import top.e404.slimefun.stackingmachine.config.GeneratorManager
import top.e404.slimefun.stackingmachine.config.stacking
import kotlin.math.min

private val c = SlimefunItems.ENERGY_CONNECTOR
private val up = SlimefunItems.ENERGY_REGULATOR
private val center by lazy { SlimefunItem.getById("NTW_QUANTUM_STORAGE_1")!!.item }
private val down = SlimefunItems.SMALL_CAPACITOR

object StackingGenerator : SlimefunItem(
    group,
    SlimefunItemStack(
        "STACKING_GENERATOR",
        Material.FURNACE,
        "&a批量堆叠发电机".color(),
        "&f放几个就有几倍效率".color(),
    ),
    RecipeType.ENHANCED_CRAFTING_TABLE,
    arrayOf(
        c, up, c,
        c, center, c,
        c, down, c,
    )
), InventoryBlock {
    private const val COUNT_KEY = "internal_machine_count"
    private const val ID_KEY = "internal_machine_id"
    private const val STATE_KEY = "stack_machine_state"
    internal enum class MachineState(
        private val material: Material,
        private val message: String,
    ) {
        /**
         * 挂起
         */
        UNINITIALIZED(Material.ORANGE_STAINED_GLASS_PANE, "&c放置之后尚未配置"),

        /**
         * 空机器
         */
        EMPTY_MACHINE(Material.ORANGE_STAINED_GLASS_PANE, "&c空机器"),

        /**
         * 正常运行
         */
        RUN(Material.WHITE_STAINED_GLASS_PANE, "&a正常运行"),

        /**
         * 缺少材料
         */
        LAKE_MATERIAL(Material.RED_STAINED_GLASS_PANE, "&c缺少材料"),

        /**
         * 缺少输入模板
         */
        LAKE_TEMPLATE(Material.RED_STAINED_GLASS_PANE, "&c缺少输入模板"),

        /**
         * 未定义的机器
         */
        UNSUPPORTED_MACHINE(Material.RED_STAINED_GLASS_PANE, "&c不支持的机器"),

        /**
         * 无效配方
         */
        UNKNOWN_RECIPE(Material.RED_STAINED_GLASS_PANE, "&c无效配方"),

        /**
         * 无产出
         */
        EMPTY_OUTPUT(Material.RED_STAINED_GLASS_PANE, "&c无产出"),

        /**
         * 与网络断开连接
         */
        DISCONNECT_TO_NETWORK(Material.RED_STAINED_GLASS_PANE, "&c与网络断开连接"),

        /**
         * 与电网断开连接
         */
        DISCONNECT_TO_ENERGY(Material.RED_STAINED_GLASS_PANE, "&c与电网断开连接"),
        ;

        fun getDisplay(vararg lore: String) = buildItemStack(material, name = message, lore = lore.toList())
        fun getDisplay(lore: List<Component>) = buildItemStack(material, name = message).apply { lore(lore) }
    }

    private var Config.count
        get() = getString(COUNT_KEY).toInt()
        set(value) {
            setValue(COUNT_KEY, value.toString())
        }

    /**
     * 机器id缓存 保证若count为0则id一定为null
     */
    private var Config.id: String?
        get() = getString(ID_KEY).let { it.ifEmpty { null } }
        set(value) {
            setValue(ID_KEY, value ?: "")
        }
    private var Config.state: MachineState
        get() = MachineState.valueOf(getString(STATE_KEY))
        set(value) {
            setValue(STATE_KEY, value.name)
        }

    private val CACHE = buildMenu(
        "&f堆叠发电机",
        "#########",
        "#   #IiI#",
        "#   m#n##",
        "#   #OoO#",
        "#########",
    ) { _: Int, char: Char ->
        when (char) {
            // 背景
            '#' -> ChestMenuUtils.getBackground() to DENY_TOUCH
            // 运行状态
            'm' -> buildItemStack(Material.BLACK_STAINED_GLASS_PANE, name = "&f尚未初始化") to DENY_TOUCH
            // 存储状态
            'n' -> buildItemStack(Material.BLACK_STAINED_GLASS_PANE, name = "&f空机器") to DENY_TOUCH
            // 输入提示
            'I' -> buildItemStack(Material.GREEN_STAINED_GLASS_PANE, name = "&f在空格放入机器") to DENY_TOUCH
            // 输出提示
            'O' -> buildItemStack(Material.GREEN_STAINED_GLASS_PANE, name = "&f从存储中取出机器") to DENY_TOUCH

            else -> null
        }
    }.create(this, this)

    init {
        val templateSlots = CACHE.getSlots(' ')
        val inputSlot = CACHE.getSlot('i')
        val outputSlot = CACHE.getSlot('o')

        val tickHandler = object : BlockTicker() {

            @Deprecated("Deprecated in Java")
            override fun tick(b: Block, selfSfItem: SlimefunItem, config: Config) {
                val selfBlockMenu = BlockStorage.getInventory(b)
                fun updateMachineState(state: MachineState, vararg lore: String) {
                    config.state = state
                    val item = state.getDisplay(*lore)
                    CACHE.updateSlots(selfBlockMenu.toInventory(), 'm', item)
                }

                fun updateMachineState(state: MachineState, lore: List<Component>) {
                    config.state = state
                    val item = state.getDisplay(lore)
                    CACHE.updateSlots(selfBlockMenu.toInventory(), 'm', item)
                }

                fun updateStorageState(id: String?, count: Int) {
                    val item = id?.let {
                        SfHook.getItem(it)?.let { sfi ->
                            ItemStack(sfi.item).editItemMeta { lore = listOf("&f数量: $count".color()) }
                        }
                    } ?: buildItemStack(Material.BLACK_STAINED_GLASS_PANE, name = "&f空机器")
                    CACHE.updateSlots(selfBlockMenu.toInventory(), 'n', item)
                }

                // 检查网络连接
                val search = searchNetwork(b.location)
                if (search == null) {
                    updateMachineState(MachineState.DISCONNECT_TO_NETWORK)
                    return
                }
                val (rootLocation, root) = search
                PL.debug { "找到网络: ${rootLocation.asString}" }
                val energyNet = Slimefun.getNetworkManager()
                    .getNetworkFromLocation(b.location, EnergyNet::class.java)
                    .orElse(null)
                if (energyNet == null) {
                    updateMachineState(MachineState.DISCONNECT_TO_ENERGY)
                    PL.debug { "与电网断开连接" }
                    return
                }

                // 正在发电
                val progress = Data.config[b.location]
                if (progress != null) {
                    // 存电
                    var push = progress.recipe.energy * progress.magnification
                    for ((location, capacitor) in energyNet.capacitors.entries) {
                        val canPush = capacitor.capacity - capacitor.getCharge(location)
                        if (canPush <= 0) continue
                        // 充不完
                        if (push > canPush) {
                            push -= canPush
                            PL.debug { "向${location.asString}中增加电量: $canPush" }
                            capacitor.addCharge(location, canPush)
                            continue
                        }
                        // 充完
                        PL.debug { "向${location.asString}中增加电量: $push" }
                        capacitor.addCharge(location, push)
                        break
                    }
                    progress.progress++

                    val lore = buildList {
                        val percentage = String.format("%.2f", 100.0 * progress.progress / progress.recipe.duration)
                        add(Component.text("&f进度: ${progress.progress} / ${progress.recipe.duration} ($percentage%)".color()))
                        add(Component.text("&f配方发电: ${progress.recipe.energy}".color()))
                        add(Component.text("&f堆叠次数: ${progress.magnification}".color()))
                        add(Component.text("&f当前每tick发电: ${progress.recipe.energy * progress.magnification}".color()))
                        add(Component.text("&f总发电: ${progress.recipe.energy * progress.magnification * progress.recipe.duration}".color()))
                        addAll(progress.display)
                    }
                    updateMachineState(MachineState.RUN, lore)
                    PL.debug {
                        val percentage = String.format("%.2f", 100.0 * progress.progress / progress.recipe.duration)
                        "进度: ${progress.progress} / ${progress.recipe.duration} ($percentage%)"
                    }

                    // 完成
                    if (progress.progress >= progress.recipe.duration) {
                        progress.output.forEach {
                            root.addItemStack(it)
                            if (it.amount != 0) b.world.dropItem(b.location, it)
                        }
                        Data.config.remove(b.location)
                        PL.debug { "完成" }
                        return
                    }
                    return
                }

                // 检查机器输入 合并到机器到count中
                run {
                    // 已有机器
                    val machine = config.id
                    val input = selfBlockMenu.getItemInSlot(inputSlot) ?: return@run
                    val countBefore = config.count
                    if (input.type == Material.AIR) return@run
                    // 拒绝非机器/没有配方的机器
                    val inputMachineId = validMachine(input)
                    if (inputMachineId == null) {
                        PL.debug { "批量堆叠机器拒绝非机器的物品${input.type.name}" }
                        return@run
                    }
                    // 不同机器不合并
                    if (machine != null && countBefore != 0 && machine != inputMachineId) {
                        PL.debug { "批量堆叠机器拒绝不同的机器$inputMachineId != $machine" }
                        return@run
                    }
                    // 新机器
                    if (countBefore == 0) {
                        selfBlockMenu.toInventory().setItem(inputSlot, emptyItem)
                        config.id = inputMachineId
                        config.count = input.amount
                        PL.debug { "批量堆叠机器重置模拟机器$inputMachineId" }
                        return@run
                    }
                    // 合并机器
                    selfBlockMenu.toInventory().setItem(inputSlot, emptyItem)
                    config.id = inputMachineId
                    config.count = countBefore + input.amount
                    PL.debug { "批量堆叠机器合并模拟机器$inputMachineId x ($countBefore + ${input.amount})" }
                }

                // 检查机器输出
                val (internalMachineId, count) = run {
                    val output = selfBlockMenu.getItemInSlot(outputSlot)
                    val internalId = config.id
                    // 输出槽为空
                    if (output == null || output.type == Material.AIR) {
                        // 内部也为空
                        if (internalId == null || config.count == 0) {
                            updateMachineState(MachineState.EMPTY_MACHINE)
                            updateStorageState(null, 0)
                            PL.debug { "空机器" }
                            return
                        }
                        val machine = getById(internalId)!!.item.clone()
                        val canTake = min(machine.maxStackSize, config.count)
                        machine.amount = canTake
                        config.count -= canTake
                        if (config.count == 0) config.id = null
                        selfBlockMenu.toInventory().setItem(outputSlot, machine)
                        return@run internalId to config.count + machine.amount
                    }
                    // 输出槽有物品
                    val outputItemId = getByItem(output)?.id
                    // 输出槽不是粘液科技机器
                    if (outputItemId == null) {
                        PL.debug { "输出槽不是粘液科技机器" }
                        if (config.id == null) {
                            // 空机器
                            updateMachineState(MachineState.EMPTY_MACHINE)
                            updateStorageState(null, 0)
                            PL.debug { "空机器" }
                            return
                        }
                        return@run config.id!! to config.count
                    }

                    // 内部缓存为空 以输出格为准
                    if (internalId == null) return@run outputItemId to output.amount
                    // 有内部缓存 以内部缓存为准
                    // 输出格是其他机器
                    if (outputItemId != internalId) {
                        PL.debug { "输出槽有不同机器" }
                        return@run internalId to config.count
                    }
                    // 输出格是同种物品
                    val canPush = output.maxStackSize - output.amount
                    // 补全一组
                    if (config.count >= canPush) {
                        config.count -= canPush
                        output.amount += canPush
                        if (config.count == 0) config.id = null
                        selfBlockMenu.toInventory().setItem(outputSlot, output)
                        return@run internalId to config.count + output.amount
                    }
                    // 全取出没补全
                    output.amount += config.count
                    selfBlockMenu.toInventory().setItem(outputSlot, output)
                    config.count = 0
                    config.id = null
                    internalId to output.amount
                }
                if (count == 0) {
                    config.id = null
                    PL.debug { "空机器" }
                    updateStorageState(null, 0)
                    updateMachineState(MachineState.EMPTY_MACHINE)
                    return
                }
                PL.debug { "计算完成: ${internalMachineId}x${count}" }
                updateStorageState(internalMachineId, count)
                // config.id = internalMachineId
                // config.count = count

                // 输入模板 合并相同物品
                val inputTemplate = CACHE.getSlots(' ')
                    .mapNotNull { selfBlockMenu.getItemInSlot(it) }
                    .filter { it.type != Material.AIR }
                    .map(ItemStack::clone)

                val machineTemplate = GeneratorManager.templates[internalMachineId]
                if (machineTemplate == null) {
                    updateMachineState(MachineState.UNSUPPORTED_MACHINE)
                    PL.debug { "未定义的机器" }
                    return
                }

                // 空输入模板
                if (inputTemplate.isEmpty()) {
                    // 检查empty配方
                    val r = machineTemplate.empty.firstOrNull { recipe ->
                        recipe.conditions.all { it.condition(b, root) }
                    } ?: run {
                        updateMachineState(MachineState.LAKE_TEMPLATE)
                        PL.debug { "缺少模板" }
                        return
                    }

                    // 开始合成
                    val magnification = count
                    val display = r.display(magnification)
                    val output = r.getResult(magnification)
                    PL.debug { "配方输出: $output" }
                    Data.config[b.location] = Progress(1, r, output, display, magnification)

                    val lore = buildList {
                        add(
                            Component.text(
                                "&f进度: 1 / ${r.duration} (${
                                    String.format(
                                        "%.2f",
                                        100.0 / r.duration
                                    )
                                }%)".color()
                            )
                        )
                        addAll(display)
                    }
                    updateMachineState(MachineState.RUN, lore)
                    return
                }
                val recipe = machineTemplate.recipes.firstOrNull { recipe ->
                    recipe.conditions.all { it.condition(b, root) } && recipe.match(inputTemplate)
                }
                if (recipe == null) {
                    updateMachineState(MachineState.UNKNOWN_RECIPE)
                    PL.debug { "未知配方" }
                    return
                }

                val allNetworkItems = root.allNetworkItems
                // 批量合成的最大倍数
                val calculatedInput = recipe.input.map { recipeItem ->
                    val entry = allNetworkItems.entries.firstOrNull { (item) -> recipeItem.match(item) }
                    if (entry == null) {
                        updateMachineState(MachineState.LAKE_MATERIAL)
                        PL.debug { "缺少材料: $recipeItem" }
                        return
                    }
                    val (item, networkCount) = entry
                    Triple(recipeItem, item, networkCount / recipeItem.amount)
                }
                val magnification = min(calculatedInput.minOf { it.third }, count)
                if (magnification <= 0) {
                    updateMachineState(MachineState.LAKE_MATERIAL)
                    PL.debug { "缺少材料" }
                    return
                }

                PL.debug { "倍率: $magnification" }

                // 抽取原料
                for ((recipeItem, input) in calculatedInput) {
                    root.getItemStack(ItemRequest(input, recipeItem.amount * magnification))
                }

                // 开始合成
                val display = recipe.display(magnification)
                val output = recipe.getResult(magnification)
                PL.debug { "配方输出: $output" }
                Data.config[b.location] = Progress(1, recipe, output, display, magnification)

                val lore = buildList {
                    add(
                        Component.text(
                            "&f进度: 1 / ${recipe.duration} (${String.format("%.2f", 100.0 / recipe.duration)}%)".color()
                        )
                    )
                    addAll(display)
                }
                updateMachineState(MachineState.RUN, lore)
            }

            override fun isSynchronized() = true

        }
        val placeHandler = object : BlockPlaceHandler(false) {
            override fun onPlayerPlace(e: BlockPlaceEvent) {
                Data.config.remove(e.block.location)
                BlockStorage.getLocationInfo(e.block.location).apply {
                    id = ""
                    count = 0
                    state = MachineState.UNINITIALIZED
                }
                BlockStorage.getInventory(e.block).inventory.setItem(44, ChestMenuUtils.getBackground())
            }
        }
        val breakHandler = object : BlockBreakHandler(false, false) {
            override fun onPlayerBreak(e: BlockBreakEvent, item: ItemStack, drops: MutableList<ItemStack>) {
                val progress = Data.config[e.block.location]
                if (progress != null) {
                    for (input in progress.recipe.input) {
                        drops.addAll(input.getItemSingle().stacking(input.amount * progress.magnification))
                    }
                }
                Data.config.remove(e.block.location)
                // 正常掉落
                val blockMenu = BlockStorage.getInventory(e.block)
                // 模板
                templateSlots
                    .mapNotNull { blockMenu.getItemInSlot(it) }
                    .filter { it.type != Material.AIR }
                    .let(drops::addAll)

                // 输出
                val output = blockMenu.getItemInSlot(outputSlot)
                if (output != null && output.type != Material.AIR) drops.add(output)

                // 输入
                val input = blockMenu.getItemInSlot(inputSlot)
                if (input != null && input.type != Material.AIR) drops.add(input)

                // 内部缓存
                val config = BlockStorage.getLocationInfo(e.block.location)
                val id = config.id
                if (id == null) {
                    PL.debug { "玩家${e.player.name}破坏没有指定机器的批量堆叠机器${e.block.location.asString}" }
                    PL.debug { "掉落: $drops" }
                    return
                }
                val count = config.count
                val slimefunItem = getById(id)
                if (slimefunItem == null) {
                    PL.debug { "玩家${e.player.name}破坏批量堆叠机器${e.block.location.asString}时无法找到${id}x${count}" }
                    PL.debug { "掉落: $drops" }
                    return
                }
                repeat(count / slimefunItem.item.maxStackSize) {
                    drops.add(slimefunItem.item.clone().apply { amount = maxStackSize })
                }
                drops.add(slimefunItem.item.clone().apply { amount = count % slimefunItem.item.maxStackSize })
                PL.debug { "掉落: $drops" }
            }
        }

        addItemHandler(tickHandler, placeHandler, breakHandler)
    }

    /**
     * 搜索相邻的网络, 并返回其root位置
     */
    private fun searchNetwork(location: Location): Pair<Location, NetworkRoot>? {
        val networkRoots = NetworkController.getNetworks().entries
        for (face in listOf(
            BlockFace.UP, /* BlockFace.DOWN, // 不检查方块下方, 因为加速机器本体在下方 */
            BlockFace.NORTH, BlockFace.WEST,
            BlockFace.SOUTH, BlockFace.EAST,
        )) {
            val l = location.clone().add(face.direction)
            networkRoots.firstOrNull { (_, v) -> l in v.nodeLocations }?.let { return it.toPair() }
        }
        return null
    }

    fun validMachine(itemStack: ItemStack): String? {
        val byItem = getByItem(itemStack)
        if (byItem == null) {
            PL.debug { "${itemStack.type.name}不是粘液科技机器" }
            return null
        }
        return byItem.id
    }

    override fun getInputSlots() = intArrayOf()

    override fun getOutputSlots() = intArrayOf()
}