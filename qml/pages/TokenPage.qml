import QtQuick 2.15
import ".."
import "../components"

// TokenPage - Stage 4: the context budget is live host data.
//
// DATA SOURCE
//   The context object "tokens" (selkie.host.TokenBridge, registered before view.load)
//   publishes the six numbers data/tokens.json holds: used / total and the four-way
//   breakdown persona / recall / tools / history. Nothing on this page is a literal any
//   more - every label, percentage and bar width is a binding on those properties, so a
//   file edit shows up within a frame or two (the bridge re-reads on mtime change).
//
// NOTHING IS HARD-CODED, SO NOTHING CAN DRIFT
//   The summary number, the four row numbers, every percentage and every bar width are all
//   derived from the same two properties (the part and the window), which is why the picture
//   cannot disagree with the numbers printed next to it.
//
// EVERY BAR IS A RECTANGLE
//   qml4j has no Controls layer, no ProgressBar and no Grid positioner, so the usage bar is a
//   clipped, rounded Rectangle track with five absolutely positioned segment Rectangles inside
//   it, and each row bar is a track plus a proportional fill.
//
// PROPORTIONS ARE SHARE OF THE WINDOW
//   Every bar is drawn against the full window (tokens.total), not against the used total, so
//   the four categories are directly comparable and the free space is the obvious remainder.
//
// COLOUR ROLES
//   Persona = primary, Memory recall = secondary, Tool schema = tertiary, Conversation
//   history = error. The error role is the fourth distinct hue for the "expensive fixed
//   cost" category; every bar sits on surfaceContainerHighest so it stays legible in both
//   schemes.
//
// DEGRADED MODE
//   When status.preflightOk is false the note card says the backend is unreachable and that
//   the numbers are the last manifest sample, in Theme.error.
//
// PAGE CONTRACT (the shell owns geometry)
//   Root is Item { id: pageRoot } with no x/y/width/height; the body is a Flickable so the
//   three stacked cards can exceed a short window.
//
// INTERACTIVE OBJECTNAMES: token-summary, token-bar, token-row-1 ... token-row-4

Item {
    id: pageRoot

    // ------------------------------------------------------------------
    // live budget - selkie.host.TokenBridge as the context object "tokens"
    // ------------------------------------------------------------------
    readonly property real windowTokens: tokens.total
    // Dividing by a zero window would produce NaN widths; fall back to 1 (all shares 0).
    readonly property real windowSafe: pageRoot.windowTokens <= 0 ? 1 : pageRoot.windowTokens

    readonly property real personaTokens: tokens.persona
    readonly property real recallTokens: tokens.recall
    readonly property real toolsTokens: tokens.tools
    readonly property real historyTokens: tokens.history

    readonly property real usedTokens: tokens.used
    readonly property real freeTokens: Math.max(0, pageRoot.windowTokens - pageRoot.usedTokens)

    readonly property string dataSource: tokens.source
    // Degraded state: a FAILED poll, not the pre-poll default. status.preflightOk starts
    // false (and pollCount 0) while the first request is still in flight, so gating on
    // pollCount keeps a healthy launch from flashing an error banner for two frames.
    readonly property bool backendDown: status.preflightOk === false && status.pollCount > 0

    // ------------------------------------------------------------------
    // formatting helpers
    //   Values are passed IN as arguments: the binding that calls the function records the
    //   dependency on the bridge property, the function body stays dependency-free.
    // ------------------------------------------------------------------
    function groupThousands(value) {
        var n = Math.round(value);
        var text = "" + n;
        var out = "";
        while (text.length > 3) {
            out = "," + text.substring(text.length - 3) + out;
            text = text.substring(0, text.length - 3);
        }
        return text + out;
    }

    function share(part, whole) {
        if (whole <= 0) {
            return "0%";
        }
        return (Math.round(part / whole * 1000) / 10) + "%";
    }

    // ------------------------------------------------------------------
    // display strings, one per number the page prints
    // ------------------------------------------------------------------
    readonly property string usedLabel: pageRoot.groupThousands(pageRoot.usedTokens)
    readonly property string totalLabel: pageRoot.groupThousands(pageRoot.windowTokens)
    readonly property string usedShareLabel: pageRoot.share(pageRoot.usedTokens, pageRoot.windowTokens)
    readonly property string freeLabel: pageRoot.groupThousands(pageRoot.freeTokens)
    readonly property string freeShareLabel: pageRoot.share(pageRoot.freeTokens, pageRoot.windowTokens)

    readonly property string personaLabel: pageRoot.groupThousands(pageRoot.personaTokens)
    readonly property string recallLabel: pageRoot.groupThousands(pageRoot.recallTokens)
    readonly property string toolsLabel: pageRoot.groupThousands(pageRoot.toolsTokens)
    readonly property string historyLabel: pageRoot.groupThousands(pageRoot.historyTokens)

    readonly property string personaShareLabel: pageRoot.share(pageRoot.personaTokens, pageRoot.windowTokens)
    readonly property string recallShareLabel: pageRoot.share(pageRoot.recallTokens, pageRoot.windowTokens)
    readonly property string toolsShareLabel: pageRoot.share(pageRoot.toolsTokens, pageRoot.windowTokens)
    readonly property string historyShareLabel: pageRoot.share(pageRoot.historyTokens, pageRoot.windowTokens)

    // ------------------------------------------------------------------
    // page geometry
    // ------------------------------------------------------------------
    readonly property real pageMargin: Theme.spaceXl
    readonly property real contentWidth: Math.max(280, Math.min(880, pageRoot.width - 2 * pageRoot.pageMargin))
    readonly property real cardWidth: pageRoot.contentWidth
    readonly property real cardContentWidth: pageRoot.cardWidth - 2 * Theme.spaceL

    readonly property real summaryCardHeight: 260
    readonly property real breakdownCardHeight: 300
    readonly property real noteCardHeight: 176

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
                    text: pageRoot.usedLabel
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleLarge
                    font.bold: true
                }

                Text {
                    x: usedValue.implicitWidth + Theme.spaceS
                    y: 39
                    width: pageRoot.cardContentWidth - usedValue.implicitWidth - Theme.spaceS
                    text: "of " + pageRoot.totalLabel + " tokens in the window"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodyMedium
                    elide: Text.ElideRight
                }

                Text {
                    id: usedPercent
                    x: pageRoot.cardContentWidth - usedPercent.implicitWidth
                    y: 34
                    text: pageRoot.usedShareLabel + " used"
                    color: Theme.primaryHex
                    font.pixelSize: Theme.typeTitleSmall
                    font.bold: true
                }

                // thick usage bar: one clipped track, five segments, free space last
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
                        width: usageBar.width * pageRoot.personaTokens / pageRoot.windowSafe
                        height: usageBar.height
                        color: Theme.primaryHex
                    }

                    Rectangle {
                        x: usageBar.width * pageRoot.personaTokens / pageRoot.windowSafe
                        y: 0
                        width: usageBar.width * pageRoot.recallTokens / pageRoot.windowSafe
                        height: usageBar.height
                        color: Theme.secondaryHex
                    }

                    Rectangle {
                        x: usageBar.width * (pageRoot.personaTokens + pageRoot.recallTokens) / pageRoot.windowSafe
                        y: 0
                        width: usageBar.width * pageRoot.toolsTokens / pageRoot.windowSafe
                        height: usageBar.height
                        color: Theme.tertiaryHex
                    }

                    Rectangle {
                        x: usageBar.width * (pageRoot.personaTokens + pageRoot.recallTokens
                                             + pageRoot.toolsTokens) / pageRoot.windowSafe
                        y: 0
                        width: usageBar.width * pageRoot.historyTokens / pageRoot.windowSafe
                        height: usageBar.height
                        color: Theme.errorHex
                    }

                    Rectangle {
                        x: usageBar.width * (pageRoot.personaTokens + pageRoot.recallTokens
                                             + pageRoot.toolsTokens + pageRoot.historyTokens) / pageRoot.windowSafe
                        y: 0
                        width: usageBar.width * pageRoot.freeTokens / pageRoot.windowSafe
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
                            radius: Math.min(Theme.shapeFull, width / 2)
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
                            text: pageRoot.personaLabel + "  ·  " + pageRoot.personaShareLabel
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
                            radius: Math.min(Theme.shapeFull, width / 2)
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
                            text: pageRoot.recallLabel + "  ·  " + pageRoot.recallShareLabel
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
                            radius: Math.min(Theme.shapeFull, width / 2)
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
                            text: pageRoot.toolsLabel + "  ·  " + pageRoot.toolsShareLabel
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
                            radius: Math.min(Theme.shapeFull, width / 2)
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
                            text: pageRoot.historyLabel + "  ·  " + pageRoot.historyShareLabel
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
                            radius: Math.min(Theme.shapeFull, width / 2)
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
                            text: pageRoot.freeLabel + "  ·  " + pageRoot.freeShareLabel
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
                    text: "Each bar is drawn against the full " + pageRoot.totalLabel
                          + " token window, not against the " + pageRoot.usedLabel
                          + " already used."
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
                            text: pageRoot.personaLabel
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: row1Percent
                            x: parent.width - row1Percent.implicitWidth
                            y: 0
                            text: pageRoot.personaShareLabel
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
                            width: parent.width * pageRoot.personaTokens / pageRoot.windowSafe
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
                            text: pageRoot.recallLabel
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: row2Percent
                            x: parent.width - row2Percent.implicitWidth
                            y: 0
                            text: pageRoot.recallShareLabel
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
                            width: parent.width * pageRoot.recallTokens / pageRoot.windowSafe
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
                            text: pageRoot.toolsLabel
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: row3Percent
                            x: parent.width - row3Percent.implicitWidth
                            y: 0
                            text: pageRoot.toolsShareLabel
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
                            width: parent.width * pageRoot.toolsTokens / pageRoot.windowSafe
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
                            text: pageRoot.historyLabel
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodySmall
                        }

                        Text {
                            id: row4Percent
                            x: parent.width - row4Percent.implicitWidth
                            y: 0
                            text: pageRoot.historyShareLabel
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
                            width: parent.width * pageRoot.historyTokens / pageRoot.windowSafe
                            height: 8
                            radius: Math.min(Theme.shapeFull, height / 2)
                            color: Theme.errorHex
                        }
                    }
                }
            }

            // ==========================================================
            // 3. note card: name the source, and the preflight verdict
            // ==========================================================
            M3Card {
                id: noteCard
                width: pageRoot.cardWidth
                height: pageRoot.noteCardHeight
                variant: "elevated"

                Rectangle {
                    id: sourceChip
                    x: 0
                    y: 0
                    width: sourceChipLabel.implicitWidth + 2 * Theme.spaceM
                    height: 22
                    radius: Math.min(Theme.shapeFull, height / 2)
                    color: pageRoot.backendDown ? Theme.errorContainerHex : Theme.tertiaryContainerHex

                    Text {
                        id: sourceChipLabel
                        x: Theme.spaceM
                        anchors.verticalCenter: parent.verticalCenter
                        text: pageRoot.backendDown ? "MANIFEST DATA ONLY" : "LIVE BRIDGE DATA"
                        color: pageRoot.backendDown ? Theme.onErrorContainerHex
                                                    : Theme.onTertiaryContainerHex
                        font.pixelSize: Theme.typeLabelSmall
                        font.bold: true
                    }
                }

                Text {
                    x: 0
                    y: 36
                    width: pageRoot.cardContentWidth
                    text: tokens.source === "unavailable"
                          ? "data/tokens.json could not be read, so every number above is zero"
                            + " until the file exists."
                          : "These counts come from " + pageRoot.dataSource + ". The host re-reads the"
                            + " file whenever its mtime changes, so editing it updates this page"
                            + " without a restart."
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodyMedium
                    wrapMode: Text.WordWrap
                }

                Text {
                    x: 0
                    y: pageRoot.noteCardHeight - 2 * Theme.spaceL - 34
                    width: pageRoot.cardContentWidth
                    text: "Source: " + pageRoot.dataSource + "  ·  revision " + tokens.revision
                          + "  ·  preflight: " + status.preflightText
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeLabelSmall
                    elide: Text.ElideRight
                }

                Text {
                    x: 0
                    y: pageRoot.noteCardHeight - 2 * Theme.spaceL - 16
                    width: pageRoot.cardContentWidth
                    visible: pageRoot.backendDown
                    text: "Backend unreachable - showing manifest data only"
                    color: Theme.errorHex
                    font.pixelSize: Theme.typeLabelSmall
                    font.bold: true
                    elide: Text.ElideRight
                }
            }
        }
    }
}