import QtQuick 2.15
import ".."
import "../components"

// SettingsPage - Stage 5b settings.
//
// WHAT THIS PAGE IS
//   Three cards: Appearance (M3ColorPicker seed picker + dark-mode switch), Interface
//   (the UI-scale hook) and About.
//
// THE SEED PICKER IS REAL
//   M3ColorPicker owns the candidate seeds and calls theme.setSeed(argb) on the
//   ThemeBridge, which regenerates all 48 MD3E roles from the new HCT seed and writes them
//   back to the Property fields, so the whole app - rail, top bar, cards, chips - re-seeds
//   live. The picker's ring/check bind to Theme.seedHex, i.e. to the bridge's own value, so
//   they follow a re-seed triggered from anywhere, with no local state.
//   The seed hex strings live in the picker's model on purpose: an inline 0xFF... literal
//   passed straight to setSeed() saturates to 0x7FFFFFFF (see M3ColorPicker's header).
//
// THE UI SCALE SLIDER IS STILL A HOOK
//   qml4j has no Controls layer (no Slider), so the slider is a track Rectangle + a thumb
//   Rectangle + one MouseArea with drag.target. Applying the value (re-scaling
//   type/space/shape in ThemeBridge) stays a host task.
//
// PAGE CONTRACT (the shell owns geometry)
//   Root is Item { id: pageRoot } with no x/y/width/height; the shell sets the box.
//   Everything is inside a Flickable so a short window can still reach "About".
//
// INTERACTIVE OBJECTNAMES: settings-appearance, settings-seed-picker (seed-N inside it),
//   setting-dark-mode, settings-interface, setting-ui-scale (drag surface), settings-about
//
// CONTRACT COMPLIANCE: colours Theme.<role>Hex, radii Theme.shape* (or an exact half of a
//   measured size for circles), type Theme.type*, spacing Theme.space*.

Item {
    id: pageRoot

    // ------------------------------------------------------------------
    // page geometry
    // ------------------------------------------------------------------
    readonly property real pageMargin: Theme.spaceXl
    readonly property real contentWidth: Math.max(280, Math.min(720, pageRoot.width - 2 * pageRoot.pageMargin))
    readonly property real cardWidth: pageRoot.contentWidth
    readonly property real cardContentWidth: pageRoot.cardWidth - 2 * Theme.spaceL

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
            // 1. appearance - seed picker + dark mode
            // ==========================================================
            M3Card {
                id: appearanceCard
                objectName: "settings-appearance"
                width: pageRoot.cardWidth
                variant: "elevated"

                Column {
                    width: pageRoot.cardContentWidth
                    spacing: Theme.spaceM

                    Text {
                        text: "Appearance"
                        color: Theme.onSurfaceHex
                        font.pixelSize: Theme.typeTitleMedium
                        font.bold: true
                    }

                    M3ColorPicker {
                        objectName: "settings-seed-picker"
                        width: parent.width
                    }

                    Rectangle {
                        width: parent.width
                        height: 1
                        color: Theme.outlineVariantHex
                        opacity: 0.6
                    }

                    Row {
                        width: parent.width
                        spacing: Theme.spaceL

                        M3Switch {
                            id: darkSwitch
                            objectName: "setting-dark-mode"
                            anchors.verticalCenter: parent.verticalCenter
                            checked: Theme.dark
                            onToggled: theme.setDark(checked)
                        }

                        Column {
                            width: parent.width - darkSwitch.width - Theme.spaceL
                            spacing: Theme.spaceXs

                            Text {
                                text: "Dark mode"
                                color: Theme.onSurfaceHex
                                font.pixelSize: Theme.typeLabelLarge
                                font.bold: true
                            }

                            Text {
                                width: parent.width
                                text: "Switches the whole scheme between the light and dark MD3E palette."
                                color: Theme.onSurfaceVariantHex
                                font.pixelSize: Theme.typeBodySmall
                                wrapMode: Text.WordWrap
                            }
                        }
                    }
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
                    text: "Scales type, spacing and corner radii. Applies to type/space/shape in "
                          + "ThemeBridge - the drag only moves the thumb and this label."
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
                    text: "Selkie Hub 0.5.0  -  Stage 5 polish"
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
