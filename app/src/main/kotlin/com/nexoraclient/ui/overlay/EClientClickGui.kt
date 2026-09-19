package com.rubidiumclient.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubidiumclient.module.BaseModule
import com.rubidiumclient.module.BoolSetting
import com.rubidiumclient.module.EnumSetting
import com.rubidiumclient.module.FloatSetting
import com.rubidiumclient.module.IntSetting
import com.rubidiumclient.module.ModuleCategory
import com.rubidiumclient.module.ModuleManager
import com.rubidiumclient.module.ModuleSetting
import com.rubidiumclient.module.StringSetting
import com.rubidiumclient.module.misc.ComboShortcut
import com.rubidiumclient.module.misc.CommandHelper
import com.rubidiumclient.ui.theme.RubidiumAccent
import com.rubidiumclient.ui.theme.RubidiumBackground
import com.rubidiumclient.ui.theme.RubidiumOnBackground
import com.rubidiumclient.ui.theme.RubidiumOnSurfaceDim
import com.rubidiumclient.ui.theme.RubidiumOutlineStrong
import com.rubidiumclient.ui.theme.RubidiumSurface
import com.rubidiumclient.ui.theme.RubidiumSurfaceRaised
import com.rubidiumclient.ui.theme.RubidiumSurfaceVar

/**
 * A Rubidium-native recreation of the compact ProtoHax-style ClickGUI.
 * It intentionally uses the existing module/settings APIs instead of copying
 * ProtoHax UI implementation code, so the screen remains compatible with the
 * Android client and its overlay service.
 */
@Composable
fun EClientClickGui(
    onClose: () -> Unit,
    moduleVersion: Int,
    onShortcutChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(ModuleCategory.COMBAT) }
    val modules = remember(moduleVersion, selected) { ModuleManager.byCategory(selected) }
    var selectedModule by remember { mutableStateOf<BaseModule?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(14.dp))
                .background(RubidiumBackground.copy(alpha = 0.97f))
                .border(1.dp, RubidiumOutlineStrong.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .fillMaxHeight()
                    .background(RubidiumSurface)
                    .padding(10.dp)
            ) {
                Text(
                    "ECLIENT",
                    color = RubidiumOnBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "ECLIENT ENGINE",
                    color = RubidiumOnSurfaceDim,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(14.dp))
                ModuleCategory.entries.forEach { category ->
                    val active = category == selected
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) RubidiumAccent.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { selected = category }
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                    ) {
                        Text(
                            category.displayName.uppercase(),
                            color = if (active) RubidiumAccent else RubidiumOnSurfaceDim,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${ModuleManager.enabledCount()} ACTIVE",
                    color = RubidiumOnSurfaceDim,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            selected.displayName,
                            color = RubidiumOnBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "${modules.size} modules",
                            color = RubidiumOnSurfaceDim,
                            fontSize = 10.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(RubidiumSurfaceVar)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("×", color = RubidiumOnBackground, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = RubidiumOutlineStrong.copy(alpha = 0.5f))
                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modules.forEach { module ->
                        ProtoModuleCard(
                            module = module,
                            onToggle = {
                                ModuleManager.toggle(module)
                                onShortcutChanged()
                            },
                            onOpenSettings = { selectedModule = if (selectedModule == module) null else module }
                        )
                        if (selectedModule == module) {
                            ProtoSettings(module = module, onChanged = onShortcutChanged)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ProtoModuleCard(
    module: BaseModule,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var active by remember(module) { mutableStateOf(module.isEnabled) }
    LaunchedEffect(module) { module.enabledFlow.collect { active = it } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) RubidiumSurfaceVar else RubidiumSurfaceRaised)
            .border(
                1.dp,
                if (active) RubidiumAccent.copy(alpha = 0.55f) else RubidiumOutlineStrong.copy(alpha = 0.45f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                module.displayName,
                color = RubidiumOnBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (module.description.isNotBlank()) {
                Text(
                    module.description,
                    color = RubidiumOnSurfaceDim,
                    fontSize = 9.sp,
                    maxLines = 2
                )
            }
        }
        if (module.settings.isNotEmpty()) {
            Text(
                "⚙",
                modifier = Modifier
                    .padding(horizontal = 7.dp)
                    .clickable { onOpenSettings() },
                color = RubidiumOnSurfaceDim,
                fontSize = 14.sp
            )
        }
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (active) RubidiumAccent else RubidiumSurfaceVar)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (active) "ON" else "OFF",
                color = if (active) Color.White else RubidiumOnSurfaceDim,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun ProtoSettings(module: BaseModule, onChanged: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RubidiumSurface.copy(alpha = 0.85f))
            .border(1.dp, RubidiumOutlineStrong.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        module.settings.forEach { setting ->
            if (module is ComboShortcut && setting.name == "Modules") return@forEach
            SettingRow(setting = setting, onShortcutChanged = onChanged)
        }
        if (module is ComboShortcut) {
            ComboShortcutPanel(module = module)
        }
        if (module is CommandHelper) {
            CommandHelperPanel(module = module, onShortcutChanged = onChanged)
        }
    }
}

@Composable
private fun ProtoBoolSetting(setting: BoolSetting, onChanged: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { setting.value = !setting.value; onChanged() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(setting.name, color = RubidiumOnBackground, fontSize = 11.sp)
        Text(if (setting.value) "ON" else "OFF", color = if (setting.value) RubidiumAccent else RubidiumOnSurfaceDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProtoEnumSetting(setting: EnumSetting<*>, onChanged: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clickable { setting.next(); onChanged() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(setting.name, color = RubidiumOnBackground, fontSize = 11.sp)
        Text(setting.value.toString(), color = RubidiumAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProtoNumberSetting(name: String, value: String, onChanged: () -> Unit, setValue: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Keep the overlay dependency-free: tapping a numeric setting cycles
                // it through a small, predictable step rather than opening a second Window.
                val current = value.toFloatOrNull() ?: return@clickable
                val next = current + 1f
                setValue(next.toString())
                onChanged()
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = RubidiumOnBackground, fontSize = 11.sp)
        Text(value, color = RubidiumAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
