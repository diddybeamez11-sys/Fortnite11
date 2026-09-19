package com.rubidiumclient.module

import android.content.Context

import com.rubidiumclient.core.relay.RubidiumRelaySession
import com.rubidiumclient.module.combat.*
import com.rubidiumclient.module.movement.*
import com.rubidiumclient.module.misc.*
import com.rubidiumclient.module.visual.*
import com.rubidiumclient.module.social.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ModuleManager {

    private val _modules = mutableListOf<BaseModule>()
    val modules: List<BaseModule> get() = _modules

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    private var initialized = false

    private var prefs: android.content.SharedPreferences? = null
    private const val PREFS_NAME = "eclient_module_ui"
    private const val PREFIX_DISPLAY = "display_name_"

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _modules.forEach { module ->
            prefs?.getString(PREFIX_DISPLAY + module.name, null)?.let { saved ->
                if (saved.isNotBlank()) module.displayName = saved
            }
        }
    }

    fun rename(module: BaseModule, newName: String): Boolean {
        val cleaned = newName.trim().take(40)
        if (cleaned.isBlank()) return false
        module.displayName = cleaned
        prefs?.edit()?.putString(PREFIX_DISPLAY + module.name, cleaned)?.apply()
        _version.value++
        return true
    }

    fun resetName(module: BaseModule) {
        module.displayName = module.name
        prefs?.edit()?.remove(PREFIX_DISPLAY + module.name)?.apply()
        _version.value++
    }

    fun registerAll(vararg mods: BaseModule) {
        if (initialized) {
            return
        }
        initialized = true
        _modules.addAll(mods)
    }

    fun register(vararg mods: BaseModule) = registerAll(*mods)

    fun getAll(): List<BaseModule> = _modules

    fun registerToSession(session: RubidiumRelaySession) {
    }

    fun shortcutModules(): List<BaseModule> =
        _modules.filter { m ->
            m.settings.filterIsInstance<BoolSetting>()
                .any { it.name == "Shortcut" && it.value }
        }

    fun toggle(module: BaseModule) {
        module.setEnabled(!module.isEnabled)
        _version.value++
    }

    fun enable(module: BaseModule) {
        if (!module.isEnabled) { module.setEnabled(true); _version.value++ }
    }

    fun disable(module: BaseModule) {
        if (module.isEnabled) { module.setEnabled(false); _version.value++ }
    }

    fun disableAll() {
        _modules.filter { it.isEnabled }.forEach { it.setEnabled(false) }
        _version.value++
    }

    fun byName(name: String): BaseModule? =
        _modules.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun byCategory(cat: ModuleCategory): List<BaseModule> =
        _modules.filter { it.category == cat }

    /** Forces Compose UIs to refresh after mutable settings/display-name changes. */
    fun notifyUiChanged() { _version.value++ }

    fun enabledCount(): Int = _modules.count { it.isEnabled }

    fun combatModules()   = byCategory(ModuleCategory.COMBAT)
    fun movementModules() = byCategory(ModuleCategory.MOVEMENT)
    fun visualModules()   = byCategory(ModuleCategory.VISUAL)
    fun playerModules()   = byCategory(ModuleCategory.PLAYER)
    fun worldModules()    = byCategory(ModuleCategory.WORLD)
    fun miscModules()     = byCategory(ModuleCategory.MISC)

    // Real ProtoHax-derived module set. MotionFly is the only legacy Rubidium module retained.
    init {
        registerAll(
            EClientKillAura(),
            EClientCrystalAura(),
            EClientInfiniteAura(),
            EClientVelocity(),
            EClientCriticalHit(),
            EClientNoFall(),
            EClientSprint(),
            EClientSpeed(),
            EClientFly(),
            EClientBlink(),
            EClientScaffold(),
            MotionFly()
        )
    }
}
