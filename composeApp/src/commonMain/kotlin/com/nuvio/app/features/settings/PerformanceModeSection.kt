package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.performance.PerformanceMode
import com.nuvio.app.core.performance.PerformanceModeChoice
import com.nuvio.app.core.ui.Chip

@Composable
internal fun PerformanceModeSection(isTablet: Boolean) {
    val choiceFlow = remember {
        PerformanceMode.ensureLoaded()
        PerformanceMode.choice
    }
    val choice by choiceFlow.collectAsStateWithLifecycle()
    val light by PerformanceMode.light.collectAsStateWithLifecycle()

    val footer = when (choice) {
        PerformanceModeChoice.Auto -> if (light) {
            val reason = PerformanceMode.autoDetectedReason
            "Detectamos um aparelho mais simples" +
                (if (reason.isNotBlank()) " ($reason)" else "") +
                ", então o modo Leve está ligado."
        } else {
            "Seu aparelho dá conta do modo Completo."
        }
        PerformanceModeChoice.Light -> "Modo Leve ligado em todas as telas."
        PerformanceModeChoice.Full -> "Todos os efeitos ligados. Se o app travar, volte para Automático ou Leve."
    } + " Algumas mudanças só valem depois de reabrir o app."

    SettingsSection(
        title = "Desempenho",
        isTablet = isTablet,
    ) {
        SettingsGroup(isTablet = isTablet) {
            SettingsChipRow(
                title = "Modo de desempenho",
                description = "O modo Leve tira o desfoque, as animações e a troca automática do destaque, " +
                    "que pesam em PC e celular mais simples. Nenhuma função some do app.",
                isTablet = isTablet,
                footer = footer,
            ) {
                PerformanceModeChoice.entries.forEach { option ->
                    Chip(
                        label = when (option) {
                            PerformanceModeChoice.Auto -> "Automático"
                            PerformanceModeChoice.Light -> "Leve"
                            PerformanceModeChoice.Full -> "Completo"
                        },
                        selected = choice == option,
                        onClick = { PerformanceMode.setChoice(option) },
                    )
                }
            }
        }
    }
}
