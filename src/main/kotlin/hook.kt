package top.e404.slimefun.stackingmachine

import top.e404.eplugin.hook.EHookManager
import top.e404.eplugin.hook.slimefun.SlimefunHook

object HookManager : EHookManager(PL, SfHook)
object SfHook : SlimefunHook(PL)