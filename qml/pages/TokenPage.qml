import QtQuick 2.15
import ".."
import "../components"

// TokenPage - Stage 3b context budget breakdown.
//
// WHAT THIS PAGE IS
//   Where the context window went: one summary card (total used vs window, a thick
//   segmented usage bar and its legend), four breakdown rows (persona injection /
//   memory recall / tool schema / conversation history) and a note card that says
//   plainly that the numbers are a Stage 4 bridge placeholder.
//
// EVERY BAR IS A RECTANGLE
//   qml4j has no Controls layer, no ProgressBar and no Grid positioner, so the
//   usage bar is a clipped, rounded Rectangle track with four absolutely
//   positioned segment Rectangles inside it, and each row bar is a track plus a
//   proportional fill. Both derive their widths from the same token properties the
//   labels print, so the bars and the numbers cannot drift apart.
//
// PROPORTIONS ARE SHARE OF THE WINDOW
//   Every row bar is drawn against the full 200,000 token window (not against the
//   used total), so the four bars are directly comparable and the free space is
//   the visually obvious remainder - the same scale as the summary bar above.
//
// COLOUR ROLES
//   Persona = primary, Memory recall = secondary, Tool schema = tertiary,
//   Conversation history = error. The error role is used as a fourth distinct hue
//   for the "expensive fixed cost" category, and every bar sits on
//   surfaceContainerHighest so the hue stays legible in both schemes.
//
// PAGE CONTRACT (the shell owns geometry)
//   Root is Item { id: pageRoot } with no x/y/width/height; the body is a
//   Flickable so the three stacked cards can exceed a short window.
//
// INTERACTIVE OBJECTNAMES: token-summary, token-bar, token-row-1 ... token-row-4

Item {
    id: pageRoot

    // ------------------------------------------------------------------
    // placeholder budget - replaced by TokenBridge in Stage 4
    // ------------------------------------------------------------------
    readonly property real windowTokens: 200000
    readonly property real personaTokens: 24600
    readonly property real memoryTokens: 18900
    readonly property real toolSchemaTokens: 41200
    readonly property real historyTokens: 33700
    readonly property real usedTokens: pageRoot.personaTokens + pageRoot.memoryTokens
                                       + pageRoot.toolSchemaTokens + pageRoot.historyTokens
    readonly property real freeTokens: pageRoot.windowTokens - pageRoot.usedTokens

    // ------------------------------------------------------------------
    // page geometry
    // ------------------------------------------------------------------
    readonly property real pageMargin: Theme.spaceXl
    readonly property real contentWidth: Math.max(280, Math.min(880, pageRoot.width - 2 * pageRoot.pageMargin))
    readonly property real cardWidth: pageRoot.contentWidth
    readonly property real cardContentWidth: pageRoot.cardWidth - 2 * Theme.spaceL

    readonly property real summaryCardHeight: 260
    readonly property real breakdownCardHeight: 300
    readonly property real noteCardHeight: 148

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
            spacing: Theme.spaceXl

            // ==========================================================
            // 1. summary card: total used vs window + segmented bar + legend
            // ==========================================================
            M3Card {
                id: summaryCard
                objectName: "token-summary"
                width: pageRoot.cardWidth
                height: pageRoot.summaryCardHeight
                variant: "filled"

                Text {
                    x: 0
                    y: 0
                    text: "Context window"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleMedium
                    font.bold: true
                }

                // big number (numbers use typeTitleLarge) + its unit
                Text {
                    id: usedValue
                    x: 0
                    y: 30
                    text: "118,400"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleLarge
                    font.bold: true
                }

                Text {
                    x: usedValue.implicitWidth + Theme.spaceS
                    y: 39
                    text: "of 200,000 tokens in the window"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodyMedium
                }

                Text {
                    id: usedPercent
                    x: pageRoot.cardContentWidth - usedPercent.implicitWidth
                    y: 34
                    text: "59.2% used"
                    color: Theme.primaryHex
                    font.pixelSize: Theme.typeTitleSmall
                    font.bold: true
                }

                // thick usage bar: one clipped track, four segments, free space last
                Rectangle {
                    id: usageBar
                    objectName: "token-bar"
                    x: 0
                    y: 74
                    width: pageRoot.cardContentWidth
                    height: 14
                    radius: Math.min(Theme.shapeFull, height / 2)
                    color: Theme.surfaceContainerHighestHex
                    clip: true

                    Rectangle {
                        x: 0
                        y: 0
                        width: usageBar.width * pageRoot.personaTokens / pageRoot.windowTokens
                        height: usageBar.height
                        color: Theme.primaryHex
                    }

                    Rectangle {
                        x: usageBar.width * pageRoot.personaTokens / pageRoot.windowTokens
                        y: 0
                        width: usageBar.width * pageRoot.memoryTokens / pageRoot.windowTokens
                        height: usageBar.height
                        color: Theme.secondaryHex
                    }

                    Rectangle {
                        x: usageBar.width * (pageRoot.personaTokens + pageRoot.memoryTokens) / pageRoot.windowTokens
                        y: 0
                        width: usageBar.width * pageRoot.toolSchemaTokens / pageRoot.windowTokens
                        height: usageBar.height
                        color: Theme.tertiaryHex
                    }

                    Rectangle {
                        x: usageBar.width * (pageRoot.personaTokens + pageRoot.memoryTokens + pageRoot.toolSchemaTokens) / pageRoot.windowTokens
                        y: 0
                        width: usageBar.width * pageRoot.historyTokens / pageRoot.windowTokens
                        height: usageBar.height
                        color: Theme.errorHex
                    }

                    Rectangle {
                        x: usageBar.width * (pageRoot.personaTokens + pageRoot.memoryTokens + pageRoot.toolSchemaTokens + pageRoot.historyTokens) / pageRoot.windowTokens
                        y: 0
                        width: usageBar.width * pageRoot.freeTokens / pageRoot.windowTokens
                        height: usageBar.height
                        color: Theme.surfaceContainerHighestHex
                    }
                }

                // legend: dot + name on the left, count + share on the right
                Column {
                    id: legend
                    x: 0
                    y: 106
                    width: pageRoot.cardContentWidth
                    spacing: Theme.spaceS + 2

                    Item {
                        width: legend.width
                        height: 16

                        Rectangle {
                            x: 0
                            y: 4
                            width: 8
                            height: 8
                            radius: 4
                            color: Theme.primaryHex
                        }

                        Text {
                            x: 18
                            y: 0
                            text: "Persona injection"
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: legend1Value
                            x: parent.width - legend1Value.implicitWidth
                            y: 0
                            text: "24,600  ·  12.3%"
                            color: Theme.onSurfaceHex
                            font.pixelSize: Theme.typeBodySmall
                        }
                    }

                    Item {
                        width: legend.width
                        height: 16

                        Rectangle {
                            x: 0
                            y: 4
                            width: 8
                            height: 8
                            radius: 4
                            color: Theme.secondaryHex
                        }

                        Text {
                            x: 18
                            y: 0
                            text: "Memory recall"
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: legend2Value
                            x: parent.width - legend2Value.implicitWidth
                            y: 0
                            text: "18,900  ·  9.5%"
                            color: Theme.onSurfaceHex
                            font.pixelSize: Theme.typeBodySmall
                        }
                    }

                    Item {
                        width: legend.width
                        height: 16

                        Rectangle {
                            x: 0
                            y: 4
                            width: 8
                            height: 8
                            radius: 4
                            color: Theme.tertiaryHex
                        }

                        Text {
                            x: 18
                            y: 0
                            text: "Tool schema"
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: legend3Value
                            x: parent.width - legend3Value.implicitWidth
                            y: 0
                            text: "41,200  ·  20.6%"
                            color: Theme.onSurfaceHex
                            font.pixelSize: Theme.typeBodySmall
                        }
                    }

                    Item {
                        width: legend.width
                        height: 16

                        Rectangle {
                            x: 0
                            y: 4
                            width: 8
                            height: 8
                            radius: 4
                            color: Theme.errorHex
                        }

                        Text {
                            x: 18
                            y: 0
                            text: "Conversation history"
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: legend4Value
                            x: parent.width - legend4Value.implicitWidth
                            y: 0
                            text: "33,700  ·  16.9%"
                            color: Theme.onSurfaceHex
                            font.pixelSize: Theme.typeBodySmall
                        }
                    }

                    Item {
                        width: legend.width
                        height: 16

                        Rectangle {
                            x: 0
                            y: 4
                            width: 8
                            height: 8
                            radius: 4
                            color: Theme.outlineHex
                        }

                        Text {
                            x: 18
                            y: 0
                            text: "Free space"
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: legend5Value
                            x: parent.width - legend5Value.implicitWidth
                            y: 0
                            text: "81,600  ·  40.8%"
                            color: Theme.onSurfaceHex
                            font.pixelSize: Theme.typeBodySmall
                        }
                    }
                }
            }

            // ==========================================================
            // 2. breakdown card: four rows, each label + value + % + bar
            // ==========================================================
            M3Card {
                id: breakdownCard
                width: pageRoot.cardWidth
                height: pageRoot.breakdownCardHeight
                variant: "outlined"

                Text {
                    x: 0
                    y: 0
                    text: "Breakdown"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleMedium
                    font.bold: true
                }

                Text {
                    x: 0
                    y: 26
                    width: parent.width
                    text: "Each bar is drawn against the full 200,000 token window, not against the"
                          + " 118,400 already used."
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                    wrapMode: Text.WordWrap
                }

                Column {
                    id: rows
                    x: 0
                    y: 64
                    width: pageRoot.cardContentWidth
                    spacing: Theme.spaceL + 4

                    // ---- row 1: persona injection (primary) ----
                    Item {
                        objectName: "token-row-1"
                        width: rows.width
                        height: 36

                        Text {
                            x: 0
                            y: 0
                            text: "Persona injection"
                            color: Theme.onSurfaceHex
                            font.pixelSize: Theme.typeBodyMedium
                        }

                        Text {
                            id: row1Value
                            x: parent.width - 68 - row1Value.implicitWidth
                            y: 1
                            text: "24,600"
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: row1Percent
                            x: parent.width - row1Percent.implicitWidth
                            y: 0
                            text: "12.3%"
                            color: Theme.primaryHex
                            font.pixelSize: Theme.typeLabelLarge
                            font.bold: true
                        }

                        Rectangle {
                            x: 0
                            y: 20
                            width: parent.width
                            height: 8
                            radius: Math.min(Theme.shapeFull, height / 2)
                            color: Theme.surfaceContainerHighestHex
                        }

                        Rectangle {
                            x: 0
                            y: 20
                            width: parent.width * pageRoot.personaTokens / pageRoot.windowTokens
                            height: 8
                            radius: Math.min(Theme.shapeFull, height / 2)
                            color: Theme.primaryHex
                        }
                    }

                    // ---- row 2: memory recall (secondary) ----
                    Item {
                        objectName: "token-row-2"
                        width: rows.width
                        height: 36

                        Text {
                            x: 0
                            y: 0
                            text: "Memory recall"
                            color: Theme.onSurfaceHex
                            font.pixelSize: Theme.typeBodyMedium
                        }

                        Text {
                            id: row2Value
                            x: parent.width - 68 - row2Value.implicitWidth
                            y: 1
                            text: "18,900"
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: row2Percent
                            x: parent.width - row2Percent.implicitWidth
                            y: 0
                            text: "9.5%"
                            color: Theme.secondaryHex
                            font.pixelSize: Theme.typeLabelLarge
                            font.bold: true
                        }

                        Rectangle {
                            x: 0
                            y: 20
                            width: parent.width
                            height: 8
                            radius: Math.min(Theme.shapeFull, height / 2)
                            color: Theme.surfaceContainerHighestHex
                        }

                        Rectangle {
                            x: 0
                            y: 20
                            width: parent.width * pageRoot.memoryTokens / pageRoot.windowTokens
                            height: 8
                            radius: Math.min(Theme.shapeFull, height / 2)
                            color: Theme.secondaryHex
                        }
                    }

                    // ---- row 3: tool schema (tertiary) ----
                    Item {
                        objectName: "token-row-3"
                        width: rows.width
                        height: 36

                        Text {
                            x: 0
                            y: 0
                            text: "Tool schema"
                            color: Theme.onSurfaceHex
                            font.pixelSize: Theme.typeBodyMedium
                        }

                        Text {
                            id: row3Value
                            x: parent.width - 68 - row3Value.implicitWidth
                            y: 1
                            text: "41,200"
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: row3Percent
                            x: parent.width - row3Percent.implicitWidth
                            y: 0
                            text: "20.6%"
                            color: Theme.tertiaryHex
                            font.pixelSize: Theme.typeLabelLarge
                            font.bold: true
                        }

                        Rectangle {
                            x: 0
                            y: 20
                            width: parent.width
                            height: 8
                            radius: Math.min(Theme.shapeFull, height / 2)
                            color: Theme.surfaceContainerHighestHex
                        }

                        Rectangle {
                            x: 0
                            y: 20
                            width: parent.width * pageRoot.toolSchemaTokens / pageRoot.windowTokens
                            height: 8
                            radius: Math.min(Theme.shapeFull, height / 2)
                            color: Theme.tertiaryHex
                        }
                    }

                    // ---- row 4: conversation history (error) ----
                    Item {
                        objectName: "token-row-4"
                        width: rows.width
                        height: 36

                        Text {
                            x: 0
                            y: 0
                            text: "Conversation history"
                            color: Theme.onSurfaceHex
                            font.pixelSize: Theme.typeBodyMedium
                        }

                        Text {
                            id: row4Value
                            x: parent.width - 68 - row4Value.implicitWidth
                            y: 1
                            text: "33,700"
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: row4Percent
                            x: parent.width - row4Percent.implicitWidth
                            y: 0
                            text: "16.9%"
                            color: Theme.errorHex
                            font.pixelSize: Theme.typeLabelLarge
                            font.bold: true
                        }

                        Rectangle {
                            x: 0
                            y: 20
                            width: parent.width
                            height: 8
                            radius: Math.min(Theme.shapeFull, height / 2)
                            color: Theme.surfaceContainerHighestHex
                        }

                        Rectangle {
                            x: 0
                            y: 20
                            width: parent.width * pageRoot.historyTokens / pageRoot.windowTokens
                            height: 8
                            radius: Math.min(Theme.shapeFull, height / 2)
                            color: Theme.errorHex
                        }
                    }
                }
            }

            // ==========================================================
            // 3. note card: say out loud that this is a placeholder
            // ==========================================================
            M3Card {
                id: noteCard
                width: pageRoot.cardWidth
                height: pageRoot.noteCardHeight
                variant: "elevated"

                Rectangle {
                    id: placeholderChip
                    x: 0
                    y: 0
                    width: placeholderLabel.implicitWidth + 2 * Theme.spaceM
                    height: 22
                    radius: Math.min(Theme.shapeFull, height / 2)
                    color: Theme.tertiaryContainerHex

                    Text {
                        id: placeholderLabel
                        x: Theme.spaceM
                        anchors.verticalCenter: parent.verticalCenter
                        text: "STAGE 4 PLACEHOLDER"
                        color: Theme.onTertiaryContainerHex
                        font.pixelSize: Theme.typeLabelSmall
                    }
                }

                Text {
                    x: 0
                    y: 36
                    width: parent.width
                    height: 62
                    text: "These four numbers are a fixed sample, not a live measurement. TokenBridge"
                          + " streams real counts from the model's context window in Stage 4; the bars"
                          + " and rows above are already bound to the properties it will write."
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodyMedium
                    wrapMode: Text.WordWrap
                }

                Text {
                    x: 0
                    y: 100
                    text: "Source: selkie.token.TokenBridge  (not wired yet)"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeLabelSmall
                }
            }
        }
    }
}
