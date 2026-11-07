pragma Singleton
import QtQuick 2.15

// The one QML entry point for MD3E tokens.
//
// DELEGATION ONLY: every value below is a binding onto the ThemeBridge context object
// (registered by selkie.host.Main as "theme"). There is no color math, no default palette
// and no computation in this file - if a token is not in ThemeBridge it does not exist.
//
// Registered in qmldir as: singleton Theme 1.0 Theme.qml

QtObject {
    // ---- scheme state ----
    property bool dark: theme.dark
    property string seedHex: theme.seedHex
    property int roleCount: theme.roleCount

    // ---- the 48 MD3E color roles ----
    property color primary: theme.primaryHex
    property color onPrimary: theme.onPrimaryHex
    property color primaryContainer: theme.primaryContainerHex
    property color onPrimaryContainer: theme.onPrimaryContainerHex
    property color inversePrimary: theme.inversePrimaryHex
    property color primaryFixed: theme.primaryFixedHex
    property color primaryFixedDim: theme.primaryFixedDimHex
    property color onPrimaryFixed: theme.onPrimaryFixedHex
    property color onPrimaryFixedVariant: theme.onPrimaryFixedVariantHex
    property color secondary: theme.secondaryHex
    property color onSecondary: theme.onSecondaryHex
    property color secondaryContainer: theme.secondaryContainerHex
    property color onSecondaryContainer: theme.onSecondaryContainerHex
    property color secondaryFixed: theme.secondaryFixedHex
    property color secondaryFixedDim: theme.secondaryFixedDimHex
    property color onSecondaryFixed: theme.onSecondaryFixedHex
    property color onSecondaryFixedVariant: theme.onSecondaryFixedVariantHex
    property color tertiary: theme.tertiaryHex
    property color onTertiary: theme.onTertiaryHex
    property color tertiaryContainer: theme.tertiaryContainerHex
    property color onTertiaryContainer: theme.onTertiaryContainerHex
    property color tertiaryFixed: theme.tertiaryFixedHex
    property color tertiaryFixedDim: theme.tertiaryFixedDimHex
    property color onTertiaryFixed: theme.onTertiaryFixedHex
    property color onTertiaryFixedVariant: theme.onTertiaryFixedVariantHex
    property color error: theme.errorHex
    property color onError: theme.onErrorHex
    property color errorContainer: theme.errorContainerHex
    property color onErrorContainer: theme.onErrorContainerHex
    property color background: theme.backgroundHex
    property color onBackground: theme.onBackgroundHex
    property color surface: theme.surfaceHex
    property color onSurface: theme.onSurfaceHex
    property color surfaceVariant: theme.surfaceVariantHex
    property color onSurfaceVariant: theme.onSurfaceVariantHex
    property color surfaceTint: theme.surfaceTintHex
    property color inverseSurface: theme.inverseSurfaceHex
    property color inverseOnSurface: theme.inverseOnSurfaceHex
    property color outline: theme.outlineHex
    property color outlineVariant: theme.outlineVariantHex
    property color scrim: theme.scrimHex
    property color surfaceDim: theme.surfaceDimHex
    property color surfaceBright: theme.surfaceBrightHex
    property color surfaceContainerLowest: theme.surfaceContainerLowestHex
    property color surfaceContainerLow: theme.surfaceContainerLowHex
    property color surfaceContainer: theme.surfaceContainerHex
    property color surfaceContainerHigh: theme.surfaceContainerHighHex
    property color surfaceContainerHighest: theme.surfaceContainerHighestHex

    // ---- the same 48 roles as "#RRGGBB" strings (for labels/diagnostics) ----
    property string primaryHex: theme.primaryHex
    property string onPrimaryHex: theme.onPrimaryHex
    property string primaryContainerHex: theme.primaryContainerHex
    property string onPrimaryContainerHex: theme.onPrimaryContainerHex
    property string inversePrimaryHex: theme.inversePrimaryHex
    property string primaryFixedHex: theme.primaryFixedHex
    property string primaryFixedDimHex: theme.primaryFixedDimHex
    property string onPrimaryFixedHex: theme.onPrimaryFixedHex
    property string onPrimaryFixedVariantHex: theme.onPrimaryFixedVariantHex
    property string secondaryHex: theme.secondaryHex
    property string onSecondaryHex: theme.onSecondaryHex
    property string secondaryContainerHex: theme.secondaryContainerHex
    property string onSecondaryContainerHex: theme.onSecondaryContainerHex
    property string secondaryFixedHex: theme.secondaryFixedHex
    property string secondaryFixedDimHex: theme.secondaryFixedDimHex
    property string onSecondaryFixedHex: theme.onSecondaryFixedHex
    property string onSecondaryFixedVariantHex: theme.onSecondaryFixedVariantHex
    property string tertiaryHex: theme.tertiaryHex
    property string onTertiaryHex: theme.onTertiaryHex
    property string tertiaryContainerHex: theme.tertiaryContainerHex
    property string onTertiaryContainerHex: theme.onTertiaryContainerHex
    property string tertiaryFixedHex: theme.tertiaryFixedHex
    property string tertiaryFixedDimHex: theme.tertiaryFixedDimHex
    property string onTertiaryFixedHex: theme.onTertiaryFixedHex
    property string onTertiaryFixedVariantHex: theme.onTertiaryFixedVariantHex
    property string errorHex: theme.errorHex
    property string onErrorHex: theme.onErrorHex
    property string errorContainerHex: theme.errorContainerHex
    property string onErrorContainerHex: theme.onErrorContainerHex
    property string backgroundHex: theme.backgroundHex
    property string onBackgroundHex: theme.onBackgroundHex
    property string surfaceHex: theme.surfaceHex
    property string onSurfaceHex: theme.onSurfaceHex
    property string surfaceVariantHex: theme.surfaceVariantHex
    property string onSurfaceVariantHex: theme.onSurfaceVariantHex
    property string surfaceTintHex: theme.surfaceTintHex
    property string inverseSurfaceHex: theme.inverseSurfaceHex
    property string inverseOnSurfaceHex: theme.inverseOnSurfaceHex
    property string outlineHex: theme.outlineHex
    property string outlineVariantHex: theme.outlineVariantHex
    property string scrimHex: theme.scrimHex
    property string surfaceDimHex: theme.surfaceDimHex
    property string surfaceBrightHex: theme.surfaceBrightHex
    property string surfaceContainerLowestHex: theme.surfaceContainerLowestHex
    property string surfaceContainerLowHex: theme.surfaceContainerLowHex
    property string surfaceContainerHex: theme.surfaceContainerHex
    property string surfaceContainerHighHex: theme.surfaceContainerHighHex
    property string surfaceContainerHighestHex: theme.surfaceContainerHighestHex

    // ---- the six MD3E spring tokens ----
    property QtObject spring: QtObject {
        // canonical MD3E tokens (expressive scheme) - preformatted by SpringBridge
        property string fastSpatial: theme.spring.fastSpatial
        property string defaultSpatial: theme.spring.defaultSpatial
        property string slowSpatial: theme.spring.slowSpatial
        property string fastEffects: theme.spring.fastEffects
        property string defaultEffects: theme.spring.defaultEffects
        property string slowEffects: theme.spring.slowEffects

        // the same tokens as numbers, for host/QML spring math
        property real fastSpatialDampingRatio: theme.spring.fastSpatialDampingRatio
        property real fastSpatialStiffness: theme.spring.fastSpatialStiffness
        property real fastSpatialMass: theme.spring.fastSpatialMass
        property real defaultSpatialDampingRatio: theme.spring.defaultSpatialDampingRatio
        property real defaultSpatialStiffness: theme.spring.defaultSpatialStiffness
        property real defaultSpatialMass: theme.spring.defaultSpatialMass
        property real slowSpatialDampingRatio: theme.spring.slowSpatialDampingRatio
        property real slowSpatialStiffness: theme.spring.slowSpatialStiffness
        property real slowSpatialMass: theme.spring.slowSpatialMass
        property real fastEffectsDampingRatio: theme.spring.fastEffectsDampingRatio
        property real fastEffectsStiffness: theme.spring.fastEffectsStiffness
        property real fastEffectsMass: theme.spring.fastEffectsMass
        property real defaultEffectsDampingRatio: theme.spring.defaultEffectsDampingRatio
        property real defaultEffectsStiffness: theme.spring.defaultEffectsStiffness
        property real defaultEffectsMass: theme.spring.defaultEffectsMass
        property real slowEffectsDampingRatio: theme.spring.slowEffectsDampingRatio
        property real slowEffectsStiffness: theme.spring.slowEffectsStiffness
        property real slowEffectsMass: theme.spring.slowEffectsMass

        // "standard" (no-overshoot) aliases named by the design plan
        property string standardFastSpatial: theme.spring.standardFastSpatial
        property string standardDefaultSpatial: theme.spring.standardDefaultSpatial
        property string standardSlowSpatial: theme.spring.standardSlowSpatial
        property string standardFastEffects: theme.spring.standardFastEffects
        property string standardDefaultEffects: theme.spring.standardDefaultEffects
        property string standardSlowEffects: theme.spring.standardSlowEffects
    }

    // ---- MD3E shape tokens: corner radius in px (ShapeTokens.java, design-tokens.md §3) ----
    // shapeNone 0 (square), shapeXs 4, shapeS 8, shapeM 12, shapeL 16, shapeXl 28, shapeFull 999.
    // shapeFull is the pill/circle sentinel, not an MD3E radius: clamp it for real widgets with
    // Math.min(Theme.shapeFull, Math.min(w, h) / 2) so a Rectangle never gets radius > half its side.
    property real shapeNone: theme.shapeNone
    property real shapeXs: theme.shapeXs
    property real shapeS: theme.shapeS
    property real shapeM: theme.shapeM
    property real shapeL: theme.shapeL
    property real shapeXl: theme.shapeXl
    property real shapeFull: theme.shapeFull

    // ---- MD3E spacing tokens: px on the 4dp grid (SpacingTokens.java) ----
    property real spaceXs: theme.spaceXs
    property real spaceS: theme.spaceS
    property real spaceM: theme.spaceM
    property real spaceL: theme.spaceL
    property real spaceXl: theme.spaceXl
    property real spaceXxl: theme.spaceXxl

    // ---- MD3E typography tokens: font size in px, 1sp == 1px (TypographyTokens.java) ----
    // Full metrics (line height, weight, letter spacing) stay host-side; QML gets the size.
    property real typeDisplayLarge: theme.typeDisplayLarge
    property real typeDisplayMedium: theme.typeDisplayMedium
    property real typeDisplaySmall: theme.typeDisplaySmall
    property real typeHeadlineLarge: theme.typeHeadlineLarge
    property real typeHeadlineMedium: theme.typeHeadlineMedium
    property real typeHeadlineSmall: theme.typeHeadlineSmall
    property real typeTitleLarge: theme.typeTitleLarge
    property real typeTitleMedium: theme.typeTitleMedium
    property real typeTitleSmall: theme.typeTitleSmall
    property real typeBodyLarge: theme.typeBodyLarge
    property real typeBodyMedium: theme.typeBodyMedium
    property real typeBodySmall: theme.typeBodySmall
    property real typeLabelLarge: theme.typeLabelLarge
    property real typeLabelMedium: theme.typeLabelMedium
    property real typeLabelSmall: theme.typeLabelSmall
}