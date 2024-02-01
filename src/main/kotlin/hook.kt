package top.e404.slimefun.stackingmachine

import top.e404.eplugin.hook.EHookManager
import top.e404.eplugin.hook.mmoitems.MmoitemsHook
import top.e404.eplugin.hook.slimefun.SlimefunHook

object HookManager : EHookManager(PL, SfHook, MiHook) {
    private fun readResolve(): Any = HookManager
}

object SfHook : SlimefunHook(PL)
object MiHook : MmoitemsHook(PL)