package top.e404.slimefun.stackingmachine.debug

import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerQuitEvent
import top.e404.eplugin.listener.EListener
import top.e404.slimefun.stackingmachine.PL

object MachineLogger : EListener(PL) {
    private val debugLocations = mutableMapOf<Location, MutableSet<CommandSender>>()

    fun debug(location: Location, msg: () -> String) {
        val senders = debugLocations[location] ?: return
        val message = msg()
        senders.forEach { PL.sendMsgWithPrefix(it, message) }
    }

    /**
     * 切换debug接收
     *
     * @param location 位置
     * @param sender 接收者
     * @return 当前状态
     */
    fun switchDebug(location: Location, sender: CommandSender): Boolean {
        val senders = debugLocations.getOrPut(location) { mutableSetOf() }
        if (senders.remove(sender)) {
            if (senders.isEmpty()) debugLocations.remove(location)
            return false
        }
        senders.add(sender)
        return true
    }
    
    // 玩家退出游戏时从debug消息接收者中移除
    @EventHandler
    fun PlayerQuitEvent.onEvent() {
        val iterator = debugLocations.iterator()
        while (iterator.hasNext()) {
            val (_, senders) = iterator.next()
            if (senders.remove(player) && senders.isEmpty()) {
                iterator.remove()
            }
        }
    }
}