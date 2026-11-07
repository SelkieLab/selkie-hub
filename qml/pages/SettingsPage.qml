import QtQuick 2.15
import ".."
import "../components"

// SettingsPage - Stage 3a settings skeleton.
//
// WHAT THIS PAGE IS
//   Three cards: Appearance (seed colour picker + dark mode), Interface (a UI scale
//   slider that is a Stage 5 hook) and About (version / engine / build lines).
//
// THE SEED PICKER IS REAL
//   Clicking a swatch calls theme.setSeed(0xFF......) on the ThemeBridge, which
//   regenerates all 48 MD3E roles from the new HCT seed and writes them back to the
//   Property fields, so the whole app - rail, top bar, cards, chips - re-seeds live.
//   The ring around the current swatch is bound to Theme.seedHex, which is the
//   bridge's own "#RRGGBB", so it follows a re-seed without local state.
//
// THE UI SCALE SLIDER IS NOT WIRED YET
//   qml4j has no Controls layer (no Slider, no ProgressBar), so the slider is a
//   track Rectangle + a thumb Rectangle + one MouseArea with drag.target. The thumb
//   x is what the drag writes; scaleValue is a binding on that x, so the percentage
//   label updates from the same source that positions the thumb. Applying the value
//   (re-scaling type/space/shape in ThemeBridge) is Stage 5.
//
// PAGE CONTRACT (the shell owns geometry)
//   Root is Item { id: pageRoot } with no x/y/width/height; the shell sets the box.
//   Everything is inside a Flickable so a short window can still reach "About".
//
// INTERACTIVE OBJECTNAMES: seed-swatch-0 ... seed-swatch-7, setting-dark-mode,
//   setting-ui-scale (drag surface), settings-appearance / -interface / -about
//
// CONTRACT COMPLIANCE: colours Theme.<role>Hex, radii Theme.shape* (or an exact half
//   of a measured size for circles), type Theme.type*, spacing Theme.space*. The eight
//   hex literals below are seed INPUTS (material for the generator), not theme roles.

Item {
    id: pageRoot

    // ------------------------------------------------------------------
    // seed swatches - the eight MD3-ish starter seeds
    // ------------------------------------------------------------------
    readonly property var seedSwatches: [
        { hex: "#6750A4", argb: 0xFF6750A4, name: "Baseline purple" },
        { hex: "#4285F4", argb: 0xFF4285F4, name: "Azure" },
        { hex: "#4CAF50", argb: 0xFF4CAF50, name: "Fern" },
        { hex: "#FF6D00", argb: 0xFFFF6D00, name: "Ember" },
        { hex: "#E91E63", argb: 0xFFE91E63, name: "Rose" },
        { hex: "#009688", argb: 0xFF009688, name: "Teal" },
        { hex: "#795548", argb: 0xFF795548, name: "Clay" },
        { hex: "#607D8B", argb: 0xFF607D8B, name: "Slate" }
    ]

    // ------------------------------------------------------------------
    // page geometry
    // ------------------------------------------------------------------
    readonly property real pageMargin: Theme.spaceXl
    readonly property real contentWidth: Math.max(280, Math.min(720, pageRoot.width - 2 * pageRoot.pageMargin))
    readonly property real cardWidth: pageRoot.contentWidth
    readonly property real cardContentWidth: pageRoot.cardWidth - 2 * Theme.spaceL

    // Swatches shrink instead of clipping on a narrow window: eight of them plus the
    // gaps must always fit inside the card's content width.
    readonly property real swatchGap: Theme.spaceS
    readonly property real swatchSize: Math.max(18, Math.min(32, (pageRoot.cardContentWidth - 7 * pageRoot.swatchGap) / 8))
    readonly property real swatchRowHeight: pageRoot.swatchSize + 8

    // ------------------------------------------------------------------
    // UI scale hook (Stage 5 applies it to type/space/shape)
    // ------------------------------------------------------------------
    readonly property real scaleMin: 0.8
    readonly property real scaleMax: 1.4
    readonly property real thumbSize: 20
    readonly property real scaleTravel: Math.max(0, pageRoot.cardContentWidth - pageRoot.thumbSize)

    // A binding on the dragged thumb x: one source of truth for the geometry and the label.
    readonly property real scaleValue: pageRoot.scaleTravel <= 0
                                        ? pageRoot.scaleMin
                                        : pageRoot.scaleMin + (scaleThumb.x / pageRoot.scaleTravel)
                                          * (pageRoot.scaleMax - pageRoot.scaleMin)
    readonly property string scaleLabel: Math.round(pageRoot.scaleValue * 100) + "%"

    readonly property real appearanceCardHeight: 222
    readonly property real interfaceCardHeight: 184
    readonly property real aboutCardHeight: 126

    Flickable {
        id: scroller
        x: pageRoot.pageMargin
        y: pageRoot.pageMargin
        width: pageRoot.contentWidth
        height: Math.max(0, pageRoot.height - 2 * pageRoot.pageMargin)
        contentWidth: width
        contentHeight: contentColumn.height
        clip: true

        Column {
            id: contentColumn
            width: scroller.width
            spacing: Theme.spaceL

            // ==========================================================
            // 1. appearance - seed swatches + dark mode
            // ==========================================================
            M3Card {
                id: appearanceCard
                objectName: "settings-appearance"
                width: pageRoot.cardWidth
                height: pageRoot.appearanceCardHeight
                variant: "elevated"

                Text {
                    x: 0
                    y: 0
                    text: "Appearance"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleMedium
                    font.bold: true
                }

                Text {
                    x: 0
                    y: 30
                    text: "Seed colour"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeLabelLarge
                    font.bold: true
                }

                Row {
                    id: swatchRow
                    x: 0
                    y: 52
                    width: pageRoot.cardContentWidth
                    height: pageRoot.swatchRowHeight
                    spacing: pageRoot.swatchGap

                    Repeater {
                        model: pageRoot.seedSwatches

                        delegate: Rectangle {
                            objectName: "seed-swatch-" + index
                            width: pageRoot.swatchSize
                            height: pageRoot.swatchSize
                            radius: width / 2
                            y: (pageRoot.swatchRowHeight - height) / 2
                            color: modelData.hex
                            // the ring marks the seed the bridge is currently using
                            border.width: Theme.seedHex === modelData.hex ? 2 : 0
                            border.color: Theme.primaryHex

                            MouseArea {
                                anchors.fill: parent
                                onClicked: theme.setSeed(modelData.argb)
                            }
                        }
                    }
                }

                Text {
                    x: 0
                    y: 94
                    width: pageRoot.cardContentWidth
                    text: "Current seed " + Theme.seedHex
                          + " - all 48 roles are regenerated from it, live."
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                    wrapMode: Text.WordWrap
                }

                Rectangle {
                    x: 0
                    y: 122
                    width: pageRoot.cardContentWidth
                    height: 1
                    color: Theme.outlineVariantHex
                    opacity: 0.6
                }

                Text {
                    x: 0
                    y: 134
                    text: "Dark mode"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeLabelLarge
                    font.bold: true
                }

                M3Switch {
                    id: darkSwitch
                    objectName: "setting-dark-mode"
                    x: pageRoot.cardContentWidth - width
                    y: 128
                    checked: Theme.dark
                    onToggled: theme.setDark(checked)
                }

                Text {
                    x: 0
                    y: 166
                    width: pageRoot.cardContentWidth
                    text: "Switches the whole scheme between the light and dark MD3E palette."
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                    wrapMode: Text.WordWrap
                }
            }

            // ==========================================================
            // 2. interface - UI scale slider (drag moves the thumb)
            // ==========================================================
            M3Card {
                id: interfaceCard
                objectName: "settings-interface"
                width: pageRoot.cardWidth
                height: pageRoot.interfaceCardHeight
                variant: "filled"

                Text {
                    x: 0
                    y: 0
                    text: "Interface"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleMedium
                    font.bold: true
                }

                Text {
                    x: 0
                    y: 30
                    text: "UI scale"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeLabelLarge
                    font.bold: true
                }

                Text {
                    id: scaleValueLabel
                    x: pageRoot.cardContentWidth - scaleValueLabel.implicitWidth
                    y: 29
                    text: pageRoot.scaleLabel
                    color: Theme.primaryHex
                    font.pixelSize: Theme.typeTitleSmall
                    font.bold: true
                }

                // slider: track (with a filled portion) + draggable thumb
                Item {
                    id: scaleSlider
                    x: 0
                    y: 62
                    width: pageRoot.cardContentWidth
                    height: 32

                    Rectangle {
                        id: scaleTrack
                        x: 0
                        y: (scaleSlider.height - height) / 2
                        width: scaleSlider.width
                        height: 6
                        radius: Math.min(Theme.shapeFull, height / 2)
                        color: Theme.surfaceContainerHighestHex

                        Rectangle {
                            x: 0
                            y: 0
                            width: scaleThumb.x + scaleThumb.width / 2
                            height: scaleTrack.height
                            radius: Math.min(Theme.shapeFull, height / 2)
                            color: Theme.primaryHex
                        }
                    }

                    Rectangle {
                        id: scaleThumb
                        x: pageRoot.scaleTravel * (1.0 - pageRoot.scaleMin)
                           / Math.max(0.001, pageRoot.scaleMax - pageRoot.scaleMin)
                        y: (scaleSlider.height - height) / 2
                        width: pageRoot.thumbSize
                        height: pageRoot.thumbSize
                        radius: width / 2
                        color: Theme.primaryHex

                        Rectangle {
                            anchors.centerIn: parent
                            width: pageRoot.thumbSize / 2
                            height: pageRoot.thumbSize / 2
                            radius: width / 2
                            color: Theme.onPrimaryHex
                        }
                    }

                    MouseArea {
                        objectName: "setting-ui-scale"
                        anchors.fill: parent
                        // qml4j has no Slider: the MouseArea writes thumb.x directly and
                        // clamps it to the track, so the thumb can never leave the groove.
                        drag.target: scaleThumb
                        drag.axis: "XAxis"
                        drag.minimumX: 0
                        drag.maximumX: pageRoot.scaleTravel
                        hoverEnabled: true
                        preventStealing: true
                    }
                }

                Text {
                    x: 0
                    y: 106
                    width: pageRoot.cardContentWidth
                    text: "Scales type, spacing and corner radii. Stage 5 applies the value - "
                          + "for now the drag only moves the thumb and this label."
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                    wrapMode: Text.WordWrap
                }
            }

            // ==========================================================
            // 3. about - version / engine / build
            // ==========================================================
            M3Card {
                id: aboutCard
                objectName: "settings-about"
                width: pageRoot.cardWidth
                height: pageRoot.aboutCardHeight
                variant: "outlined"

                Text {
                    x: 0
                    y: 0
                    text: "About"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleMedium
                    font.bold: true
                }

                Text {
                    x: 0
                    y: 30
                    width: pageRoot.cardContentWidth
                    text: "Selkie Hub 0.3.0  -  Stage 3a page skeletons"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                }

                Text {
                    x: 0
                    y: 50
                    width: pageRoot.cardContentWidth
                    text: "Engine: qml4j v0.2.38  /  Skija 0.143.17  /  LWJGL 3.3.3  /  JDK 21"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                }

                Text {
                    x: 0
                    y: 70
                    width: pageRoot.cardContentWidth
                    text: "Build: 2026.11.07  -  Windows x64  -  MD3E 48 roles, 6 springs"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                }
            }
        }
    }
}
