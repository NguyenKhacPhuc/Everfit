package com.example.everfit.assignment.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.everfit.assignment.R

/**
 * Open Sans, bundled rather than fetched through Google Fonts at runtime: a
 * downloadable font needs Play Services and can fail on a device without it,
 * which would silently fall back to Roboto — exactly the fidelity gap this is
 * here to close. Licensed under the SIL Open Font License.
 */
val OpenSans = FontFamily(
    Font(R.font.open_sans_regular, FontWeight.Normal),
    Font(R.font.open_sans_semibold, FontWeight.SemiBold),
    Font(R.font.open_sans_bold, FontWeight.Bold),
)

/**
 * Values taken from the design spec, not eyeballed.
 *
 * `lineHeight` is generous relative to `fontSize` (20sp on 12sp, 24sp on 16sp),
 * which is deliberate: Compose centres the extra leading, and it is what keeps
 * the two-line day label spaced as the design shows.
 */
internal val EverfitTypography = Typography(
    // Day-of-week label: MON, TUE …
    labelSmall = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp,
    ),
    // Day-of-month number under it: 14, 15 …
    bodyLarge = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    // Workout card title.
    titleMedium = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    // Workout card subtitle: status word and exercise count.
    bodyMedium = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
