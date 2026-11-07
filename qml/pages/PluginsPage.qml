import QtQuick 2.15
import ".."
import "../components"

// PluginsPage - Stage 3a plugin management skeleton.
//
// WHAT THIS PAGE IS
//   A filter row (All / Official / Third party), then two grouped sections: three
//   official plugins and two third-party ones. Every row is name + one-line
//   description + a state chip. The last third-party row is deliberately
//   disabled-looking: Theme.surfaceContainerHighest as the row surface and
//   Theme.onSurfaceVariant for its text - opaque roles, never an alpha colour.
//
// WHY THERE ARE NO WIDGET TYPES
//   qml4j has no Controls layer (no ListView row, no Switch inside a list, no Menu),
//   so a row is an Item with Texts, a hairline Rectangle and a chip that is itself a
//   Rectangle + Text. The chips are pills: radius = half the chip height.
//
// FILTERS ARE STYLING-ONLY FOR NOW
//   The three buttons really do switch which one is filled/outlined, but they do not
//   hide rows yet - real filtering needs PluginBridge (Stage 4) so that the counts,
//   the rows and the enable/disable commands all come from one source of truth.
//
// PAGE CONTRACT (the shell owns geometry)
//   Root is Item { id: pageRoot } with no x/y/width/height; the shell sets the box.
//   Everything is inside a Flickable so a short window can still reach section two.
//
// INTERACTIVE OBJECTNAMES: plugin-filter-all, plugin-filter-official,
//   plugin-filter-third-party, plugins-official, plugins-third-party
//
// CONTRACT COMPLIANCE: colours Theme.<role>Hex, radii Theme.shape*, type Theme.type*,
//   spacing Theme.space*. "transparent" is the engine's alpha-zero colour, not a
//   palette role.

Item {
    id: pageRoot

    // ------------------------------------------------------------------
    // filter placeholder - Stage 4 wires this to PluginBridge
    // ------------------------------------------------------------------
    property int filterIndex: 0

    // ------------------------------------------------------------------
    // placeholder inventory - Stage 4 replaces these two models
    // ------------------------------------------------------------------
    readonly property var officialPlugins: [
        {
            name: "Persona Injector",
            description: "Prepends the active persona card to every turn",
            state: "Active",
            tone: "ok",
            disabled: false
        },
        {
            name: "Memory Vault",
            description: "Stores and recalls memory cards from the local vault",
            state: "Active",
            tone: "ok",
            disabled: false
        },
        {
            name: "Tool Registry",
            description: "Publishes tool schemas to the model context",
            state: "Update 1.4.0",
            tone: "info",
            disabled: false
        }
    ]

    readonly property var thirdPartyPlugins: [
        {
            name: "Weather Kit",
            description: "Adds a five-day forecast tool from a community feed",
            state: "Active",
            tone: "ok",
            disabled: false
        },
        {
            name: "Telemetry Sink",
            description: "Ships usage counters to a self-hosted endpoint",
            state: "Disabled",
            tone: "muted",
            disabled: true
        }
    ]

    readonly property int installedCount: pageRoot.officialPlugins.length
                                            + pageRoot.thirdPartyPlugins.length

    // ------------------------------------------------------------------
    // page geometry
    // ------------------------------------------------------------------
    readonly property real pageMargin: Theme.spaceXl
    readonly property real contentWidth: Math.max(280, Math.min(720, pageRoot.width - 2 * pageRoot.pageMargin))
    readonly property real cardWidth: pageRoot.contentWidth
    readonly property real cardContentWidth: pageRoot.cardWidth - 2 * Theme.spaceL

    readonly property real filterRowHeight: 40
    readonly property real sectionHeadingHeight: 26
    readonly property real rowHeight: 52
    readonly property real officialCardHeight: 3 * pageRoot.rowHeight + 2 * Theme.spaceL
    readonly property real thirdPartyCardHeight: 2 * pageRoot.rowHeight + 2 * Theme.spaceL

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
            // filter row - filled = selected, outlined = available
            // ==========================================================
            Row {
                id: filterRow
                width: pageRoot.contentWidth
                height: pageRoot.filterRowHeight
                spacing: Theme.spaceS

                M3Button {
                    objectName: "plugin-filter-all"
                    text: "All"
                    variant: pageRoot.filterIndex === 0 ? "filled" : "outlined"
                    onClicked: pageRoot.filterIndex = 0
                }

                M3Button {
                    objectName: "plugin-filter-official"
                    text: "Official"
                    variant: pageRoot.filterIndex === 1 ? "filled" : "outlined"
                    onClicked: pageRoot.filterIndex = 1
                }

                M3Button {
                    objectName: "plugin-filter-third-party"
                    text: "Third party"
                    variant: pageRoot.filterIndex === 2 ? "filled" : "outlined"
                    onClicked: pageRoot.filterIndex = 2
                }
            }

            Item {
                width: pageRoot.contentWidth
                height: 18

                Text {
                    x: 0
                    y: 0
                    text: pageRoot.installedCount + " installed  ·  "
                          + pageRoot.officialPlugins.length + " official  ·  "
                          + pageRoot.thirdPartyPlugins.length + " third party"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                }
            }

            // ==========================================================
            // section 1: official
            // ==========================================================
            Item {
                width: pageRoot.contentWidth
                height: pageRoot.sectionHeadingHeight

                Text {
                    x: 0
                    y: 0
                    text: "Official"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleMedium
                    font.bold: true
                }
            }

            M3Card {
                id: officialCard
                objectName: "plugins-official"
                width: pageRoot.cardWidth
                height: pageRoot.officialCardHeight
                variant: "outlined"

                Column {
                    id: officialRows
                    x: 0
                    y: 0
                    width: pageRoot.cardContentWidth
                    spacing: 0

                    Repeater {
                        model: pageRoot.officialPlugins

                        delegate: Item {
                            width: officialRows.width
                            height: pageRoot.rowHeight

                            Text {
                                x: 0
                                y: 6
                                text: modelData.name
                                color: Theme.onSurfaceHex
                                font.pixelSize: Theme.typeBodyMedium
                                font.bold: true
                            }

                            Text {
                                x: 0
                                y: 26
                                width: parent.width - 140
                                text: modelData.description
                                color: Theme.onSurfaceVariantHex
                                font.pixelSize: Theme.typeBodySmall
                                elide: Text.ElideRight
                            }

                            // state chip: pill = radius half the height
                            Rectangle {
                                id: officialChip
                                anchors.right: parent.right
                                anchors.verticalCenter: parent.verticalCenter
                                width: officialChipLabel.implicitWidth + 2 * Theme.spaceM
                                height: 24
                                radius: Math.min(Theme.shapeFull, height / 2)
                                color: modelData.tone === "info"
                                       ? Theme.tertiaryContainerHex
                                       : Theme.secondaryContainerHex

                                Text {
                                    id: officialChipLabel
                                    anchors.centerIn: parent
                                    text: modelData.state
                                    color: modelData.tone === "info"
                                           ? Theme.onTertiaryContainerHex
                                           : Theme.onSecondaryContainerHex
                                    font.pixelSize: Theme.typeLabelSmall
                                    font.bold: true
                                }
                            }

                            // hairline between rows, not under the last one
                            Rectangle {
                                x: 0
                                y: parent.height - 1
                                width: parent.width
                                height: 1
                                visible: index < pageRoot.officialPlugins.length - 1
                                color: Theme.outlineVariantHex
                                opacity: 0.6
                            }
                        }
                    }
                }
            }

            // ==========================================================
            // section 2: third party (last row is disabled-looking)
            // ==========================================================
            Item {
                width: pageRoot.contentWidth
                height: pageRoot.sectionHeadingHeight

                Text {
                    x: 0
                    y: 0
                    text: "Third party"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleMedium
                    font.bold: true
                }
            }

            M3Card {
                id: thirdPartyCard
                objectName: "plugins-third-party"
                width: pageRoot.cardWidth
                height: pageRoot.thirdPartyCardHeight
                variant: "outlined"

                Column {
                    id: thirdPartyRows
                    x: 0
                    y: 0
                    width: pageRoot.cardContentWidth
                    spacing: 0

                    Repeater {
                        model: pageRoot.thirdPartyPlugins

                        delegate: Item {
                            width: thirdPartyRows.width
                            height: pageRoot.rowHeight

                            // disabled-looking surface: an opaque role, not an alpha wash
                            Rectangle {
                                x: 0
                                y: 2
                                width: parent.width
                                height: parent.height - 4
                                radius: Theme.shapeS
                                visible: modelData.disabled === true
                                color: Theme.surfaceContainerHighestHex
                            }

                            Text {
                                x: modelData.disabled === true ? Theme.spaceM : 0
                                y: 6
                                text: modelData.name
                                color: modelData.disabled === true
                                       ? Theme.onSurfaceVariantHex
                                       : Theme.onSurfaceHex
                                font.pixelSize: Theme.typeBodyMedium
                                font.bold: true
                            }

                            Text {
                                x: modelData.disabled === true ? Theme.spaceM : 0
                                y: 26
                                width: parent.width - 140
                                text: modelData.description
                                color: Theme.onSurfaceVariantHex
                                font.pixelSize: Theme.typeBodySmall
                                elide: Text.ElideRight
                            }

                            Rectangle {
                                id: thirdChip
                                anchors.right: parent.right
                                anchors.rightMargin: modelData.disabled === true ? Theme.spaceM : 0
                                anchors.verticalCenter: parent.verticalCenter
                                width: thirdChipLabel.implicitWidth + 2 * Theme.spaceM
                                height: 24
                                radius: Math.min(Theme.shapeFull, height / 2)
                                color: modelData.disabled === true
                                       ? "transparent"
                                       : Theme.secondaryContainerHex
                                border.width: modelData.disabled === true ? 1 : 0
                                border.color: Theme.outlineVariantHex

                                Text {
                                    id: thirdChipLabel
                                    anchors.centerIn: parent
                                    text: modelData.state
                                    color: modelData.disabled === true
                                           ? Theme.onSurfaceVariantHex
                                           : Theme.onSecondaryContainerHex
                                    font.pixelSize: Theme.typeLabelSmall
                                    font.bold: true
                                }
                            }

                            Rectangle {
                                x: 0
                                y: parent.height - 1
                                width: parent.width
                                height: 1
                                visible: index < pageRoot.thirdPartyPlugins.length - 1
                                color: Theme.outlineVariantHex
                                opacity: 0.6
                            }
                        }
                    }
                }
            }
        }
    }
}
