import QtQuick 2.15
import "."

// Stage 1b demo surface: proves the MD3E scheme reaches QML through the Theme
// singleton (which only delegates to the ThemeBridge context object).
//
// Everything visible here is a BINDING onto Theme - flipping light/dark or re-seeding
// in Java must repaint every pixel on this screen without touching this file.

Rectangle {
    width: 1000
    height: 720
    color: Theme.background

    Text {
        x: 32
        y: 18
        text: "Selkie Hub - MD3E theme bridge"
        color: Theme.onBackground
        font.pixelSize: 26
        font.bold: true
    }

    Text {
        objectName: "txt-scheme"
        x: 32
        y: 54
        text: "seed " + Theme.seedHex + "   roles " + Theme.roleCount
              + "   mode " + (Theme.dark ? "dark" : "light")
        color: Theme.onSurfaceVariant
        font.pixelSize: 14
    }

    // ---- the only interactive control: flip light <-> dark ----
    Rectangle {
        objectName: "btn-toggle"
        x: 772
        y: 16
        width: 196
        height: 46
        radius: 23
        color: Theme.primary

        Text {
            objectName: "txt-toggle"
            anchors.centerIn: parent
            text: Theme.dark ? "Switch to light" : "Switch to dark"
            color: Theme.onPrimary
            font.pixelSize: 14
        }

        MouseArea {
            anchors.fill: parent
            onClicked: theme.toggleDark()
        }
    }

    // ---- primary ----
    Rectangle {
        objectName: "swatch-primary"
        x: 32
        y: 96
        width: 222
        height: 104
        radius: 16
        border.width: 1
        border.color: Theme.outlineVariant
        color: Theme.primary

        Text {
            x: 16
            y: 16
            text: "primary"
            color: Theme.onPrimary
            font.pixelSize: 15
            font.bold: true
        }

        Text {
            x: 16
            y: 44
            text: Theme.primaryHex
            color: Theme.onPrimary
            font.pixelSize: 12
        }

        Text {
            x: 16
            y: 70
            text: "on: " + Theme.onPrimaryHex
            color: Theme.onPrimary
            font.pixelSize: 11
        }
    }

    // ---- primaryContainer ----
    Rectangle {
        objectName: "swatch-primaryContainer"
        x: 264
        y: 96
        width: 222
        height: 104
        radius: 16
        border.width: 1
        border.color: Theme.outlineVariant
        color: Theme.primaryContainer

        Text {
            x: 16
            y: 16
            text: "primaryContainer"
            color: Theme.onPrimaryContainer
            font.pixelSize: 15
            font.bold: true
        }

        Text {
            x: 16
            y: 44
            text: Theme.primaryContainerHex
            color: Theme.onPrimaryContainer
            font.pixelSize: 12
        }

        Text {
            x: 16
            y: 70
            text: "on: " + Theme.onPrimaryContainerHex
            color: Theme.onPrimaryContainer
            font.pixelSize: 11
        }
    }

    // ---- secondary ----
    Rectangle {
        objectName: "swatch-secondary"
        x: 496
        y: 96
        width: 222
        height: 104
        radius: 16
        border.width: 1
        border.color: Theme.outlineVariant
        color: Theme.secondary

        Text {
            x: 16
            y: 16
            text: "secondary"
            color: Theme.onSecondary
            font.pixelSize: 15
            font.bold: true
        }

        Text {
            x: 16
            y: 44
            text: Theme.secondaryHex
            color: Theme.onSecondary
            font.pixelSize: 12
        }

        Text {
            x: 16
            y: 70
            text: "on: " + Theme.onSecondaryHex
            color: Theme.onSecondary
            font.pixelSize: 11
        }
    }

    // ---- secondaryContainer ----
    Rectangle {
        objectName: "swatch-secondaryContainer"
        x: 728
        y: 96
        width: 222
        height: 104
        radius: 16
        border.width: 1
        border.color: Theme.outlineVariant
        color: Theme.secondaryContainer

        Text {
            x: 16
            y: 16
            text: "secondaryContainer"
            color: Theme.onSecondaryContainer
            font.pixelSize: 15
            font.bold: true
        }

        Text {
            x: 16
            y: 44
            text: Theme.secondaryContainerHex
            color: Theme.onSecondaryContainer
            font.pixelSize: 12
        }

        Text {
            x: 16
            y: 70
            text: "on: " + Theme.onSecondaryContainerHex
            color: Theme.onSecondaryContainer
            font.pixelSize: 11
        }
    }

    // ---- tertiary ----
    Rectangle {
        objectName: "swatch-tertiary"
        x: 32
        y: 210
        width: 222
        height: 104
        radius: 16
        border.width: 1
        border.color: Theme.outlineVariant
        color: Theme.tertiary

        Text {
            x: 16
            y: 16
            text: "tertiary"
            color: Theme.onTertiary
            font.pixelSize: 15
            font.bold: true
        }

        Text {
            x: 16
            y: 44
            text: Theme.tertiaryHex
            color: Theme.onTertiary
            font.pixelSize: 12
        }

        Text {
            x: 16
            y: 70
            text: "on: " + Theme.onTertiaryHex
            color: Theme.onTertiary
            font.pixelSize: 11
        }
    }

    // ---- tertiaryContainer ----
    Rectangle {
        objectName: "swatch-tertiaryContainer"
        x: 264
        y: 210
        width: 222
        height: 104
        radius: 16
        border.width: 1
        border.color: Theme.outlineVariant
        color: Theme.tertiaryContainer

        Text {
            x: 16
            y: 16
            text: "tertiaryContainer"
            color: Theme.onTertiaryContainer
            font.pixelSize: 15
            font.bold: true
        }

        Text {
            x: 16
            y: 44
            text: Theme.tertiaryContainerHex
            color: Theme.onTertiaryContainer
            font.pixelSize: 12
        }

        Text {
            x: 16
            y: 70
            text: "on: " + Theme.onTertiaryContainerHex
            color: Theme.onTertiaryContainer
            font.pixelSize: 11
        }
    }

    // ---- error ----
    Rectangle {
        objectName: "swatch-error"
        x: 496
        y: 210
        width: 222
        height: 104
        radius: 16
        border.width: 1
        border.color: Theme.outlineVariant
        color: Theme.error

        Text {
            x: 16
            y: 16
            text: "error"
            color: Theme.onError
            font.pixelSize: 15
            font.bold: true
        }

        Text {
            x: 16
            y: 44
            text: Theme.errorHex
            color: Theme.onError
            font.pixelSize: 12
        }

        Text {
            x: 16
            y: 70
            text: "on: " + Theme.onErrorHex
            color: Theme.onError
            font.pixelSize: 11
        }
    }

    // ---- errorContainer ----
    Rectangle {
        objectName: "swatch-errorContainer"
        x: 728
        y: 210
        width: 222
        height: 104
        radius: 16
        border.width: 1
        border.color: Theme.outlineVariant
        color: Theme.errorContainer

        Text {
            x: 16
            y: 16
            text: "errorContainer"
            color: Theme.onErrorContainer
            font.pixelSize: 15
            font.bold: true
        }

        Text {
            x: 16
            y: 44
            text: Theme.errorContainerHex
            color: Theme.onErrorContainer
            font.pixelSize: 12
        }

        Text {
            x: 16
            y: 70
            text: "on: " + Theme.onErrorContainerHex
            color: Theme.onErrorContainer
            font.pixelSize: 11
        }
    }

    // ---- surface ----
    Rectangle {
        objectName: "swatch-surface"
        x: 32
        y: 324
        width: 222
        height: 104
        radius: 16
        border.width: 1
        border.color: Theme.outlineVariant
        color: Theme.surface

        Text {
            x: 16
            y: 16
            text: "surface"
            color: Theme.onSurface
            font.pixelSize: 15
            font.bold: true
        }

        Text {
            x: 16
            y: 44
            text: Theme.surfaceHex
            color: Theme.onSurface
            font.pixelSize: 12
        }

        Text {
            x: 16
            y: 70
            text: "on: " + Theme.onSurfaceHex
            color: Theme.onSurface
            font.pixelSize: 11
        }
    }

    // ---- surfaceVariant ----
    Rectangle {
        objectName: "swatch-surfaceVariant"
        x: 264
        y: 324
        width: 222
        height: 104
        radius: 16
        border.width: 1
        border.color: Theme.outlineVariant
        color: Theme.surfaceVariant

        Text {
            x: 16
            y: 16
            text: "surfaceVariant"
            color: Theme.onSurfaceVariant
            font.pixelSize: 15
            font.bold: true
        }

        Text {
            x: 16
            y: 44
            text: Theme.surfaceVariantHex
            color: Theme.onSurfaceVariant
            font.pixelSize: 12
        }

        Text {
            x: 16
            y: 70
            text: "on: " + Theme.onSurfaceVariantHex
            color: Theme.onSurfaceVariant
            font.pixelSize: 11
        }
    }

    // ---- inverseSurface ----
    Rectangle {
        objectName: "swatch-inverseSurface"
        x: 496
        y: 324
        width: 222
        height: 104
        radius: 16
        border.width: 1
        border.color: Theme.outlineVariant
        color: Theme.inverseSurface

        Text {
            x: 16
            y: 16
            text: "inverseSurface"
            color: Theme.inverseOnSurface
            font.pixelSize: 15
            font.bold: true
        }

        Text {
            x: 16
            y: 44
            text: Theme.inverseSurfaceHex
            color: Theme.inverseOnSurface
            font.pixelSize: 12
        }

        Text {
            x: 16
            y: 70
            text: "on: " + Theme.inverseOnSurfaceHex
            color: Theme.inverseOnSurface
            font.pixelSize: 11
        }
    }

    // ---- surfaceContainerHighest ----
    Rectangle {
        objectName: "swatch-surfaceContainerHighest"
        x: 728
        y: 324
        width: 222
        height: 104
        radius: 16
        border.width: 1
        border.color: Theme.outlineVariant
        color: Theme.surfaceContainerHighest

        Text {
            x: 16
            y: 16
            text: "surfaceContainerHighest"
            color: Theme.onSurfaceVariant
            font.pixelSize: 15
            font.bold: true
        }

        Text {
            x: 16
            y: 44
            text: Theme.surfaceContainerHighestHex
            color: Theme.onSurfaceVariant
            font.pixelSize: 12
        }

        Text {
            x: 16
            y: 70
            text: "on: " + Theme.onSurfaceVariantHex
            color: Theme.onSurfaceVariant
            font.pixelSize: 11
        }
    }

    Text {
        x: 32
        y: 452
        text: "Motion tokens - MD3E springs (Theme.spring.*)"
        color: Theme.onBackground
        font.pixelSize: 16
        font.bold: true
    }

    Text {
        objectName: "spring-fastSpatial"
        x: 32
        y: 478
        text: Theme.spring.fastSpatial
        color: Theme.onSurface
        font.pixelSize: 13
    }

    Text {
        objectName: "spring-defaultSpatial"
        x: 32
        y: 496
        text: Theme.spring.defaultSpatial
        color: Theme.onSurface
        font.pixelSize: 13
    }

    Text {
        objectName: "spring-slowSpatial"
        x: 32
        y: 514
        text: Theme.spring.slowSpatial
        color: Theme.onSurface
        font.pixelSize: 13
    }

    Text {
        objectName: "spring-fastEffects"
        x: 32
        y: 532
        text: Theme.spring.fastEffects
        color: Theme.onSurface
        font.pixelSize: 13
    }

    Text {
        objectName: "spring-defaultEffects"
        x: 32
        y: 550
        text: Theme.spring.defaultEffects
        color: Theme.onSurface
        font.pixelSize: 13
    }

    Text {
        objectName: "spring-slowEffects"
        x: 32
        y: 568
        text: Theme.spring.slowEffects
        color: Theme.onSurface
        font.pixelSize: 13
    }

    Text {
        x: 32
        y: 610
        text: "standard scheme (no overshoot): " + Theme.spring.standardFastSpatial
        color: Theme.onSurfaceVariant
        font.pixelSize: 12
    }

    Text {
        x: 32
        y: 636
        text: "Click the pill to flip light/dark. Every color above is a binding onto Theme."
        color: Theme.outline
        font.pixelSize: 12
    }
}
