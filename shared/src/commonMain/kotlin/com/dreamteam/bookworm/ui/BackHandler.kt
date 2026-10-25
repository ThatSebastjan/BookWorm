package com.dreamteam.bookworm.ui

import androidx.compose.runtime.Composable

/** System back (Android back gesture). No-op on platforms without one. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
