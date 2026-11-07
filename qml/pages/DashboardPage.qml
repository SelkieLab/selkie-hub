import QtQuick 2.15
import ".."
import "../components"

// DashboardPage - Stage 4: the landing page reads the live bridges.
//
// WHAT THIS PAGE IS
//   Three cards, in reading order: the context budget (used / total number plus an inline
//   usage bar), plugin health (one dot + name + status word per official plugin, first three)
//   and character health (the placeholder stats line plus the backend preflight verdict).
//
// DATA SOURCES
//   tokens  -> selkie.host.TokenBridge   : used / total drive the number, the bar and the
//                                          caption, so the picture cannot disagree with it.
//   plugins -> selkie.host.PluginBridge  : officialItems is a List<PluginInfo>; the delegate
//                                          reads modelData.health / .latencyMs / .name.
//   status  -> selkie.host.StatusBridge  : preflightOk / preflightText / endpoint / pollCount.
//   All three are registered as context objects before view.load.
//
// PLUGIN HEALTH COLOURS
//   healthy -> Theme.primary, degraded -> Theme.error, unknown -> Theme.outline. "unknown" is
//   the state before the first successful backend poll, so a fresh start reads as neutral
//   rather than alarming.
//
// DEGRADED MODE
//   When status.preflightOk is false the top of the page says the backend is unreachable and
//   that the cards below are showing the last manifest sample, in Theme.error.
//
// WHY IT IS BUILT FROM RECTANGLES
//   qml4j has no Controls layer - there is no ProgressBar, no Grid positioner and no Controls
//   ListView row. The usage bar is therefore a track Rectangle plus a fill Rectangle whose
//   width is (ratio * track width), and every plugin row is a plain Item holding one circle
//   Rectangle (radius = half its size) and two Texts.
//
// PAGE CONTRACT (the shell owns geometry)
//   Root is Item { id: pageRoot } with no x/y/width/height - the shell anchors it and sets the
//   box. Content starts at Theme.spaceXl from the top/left of that box and is wrapped in a
//   Flickable so a short window can still reach the third card.
//
// INTERACTIVE OBJECTNAMES: dashboard-budget, dashboard-budget-bar,
//   dashboard-plugins, dashboard-character
//
// CONTRACT COMPLIANCE: colours Theme.<role>Hex (qml4j parses a colour from a STRING; the
//   bridge's Integer form throws), radii Theme.shape* (or an exact half of a measured size for
//   circles), type Theme.type*, spacing Theme.space*.

Item {
    id: pageRoot

    // ------------------------------------------------------------------
    // live telemetry
    // ------------------------------------------------------------------
    readonly property real tokenUsed: tokens.used
    readonly property real tokenTotal: tokens.total
    readonly property real usageRatio: pageRoot.tokenTotal <= 0
                                        ? 0
                                        : Math.min(1, pageRoot.tokenUsed / pageRoot.tokenTotal)
    readonly property bool usageCritical: pageRoot.usageRatio > 0.85
    readonly property real compactionThreshold: 0.85

    readonly property int officialPluginCount: plugins.officialItems.length
    readonly property string pluginSource: plugins.source
    // Degraded state: a FAILED poll, not the pre-poll default. status.preflightOk starts
    // false (and pollCount 0) while the first request is still in flight, so gating on
    // pollCount keeps a healthy launch from flashing an error banner for two frames.
    readonly property bool backendDown: status.preflightOk === false && status.pollCount > 0
    readonly property string preflightLabel: status.preflightOk
                                             ? "Backend online"
                                             : "Backend unreachable"

    // character health stays a Stage 4 seam (no CharacterBridge yet) - the preflight line
    // below it is real.
    readonly property int personaCount: 3
    readonly property string activePersona: "Field Researcher"
    readonly property string lastInjectedLabel: "12s ago"
    readonly property int recalledCards: 2

    // ------------------------------------------------------------------
    // page geometry
    // ------------------------------------------------------------------
    readonly property real pageMargin: Theme.spaceXl
    readonly property real contentWidth: Math.max(280, Math.min(720, pageRoot.width - 2 * pageRoot.pageMargin))
    readonly property real cardWidth: pageRoot.contentWidth
    readonly property real cardContentWidth: pageRoot.cardWidth - 2 * Theme.spaceL

    readonly property real budgetCardHeight: 168
    readonly property real pluginCardHeight: 196
    readonly property real characterCardHeight: 150
    readonly property real statusRowHeight: 22

    // ------------------------------------------------------------------
    // display strings (numbers use typeTitleLarge; the units stay body-sized)
    // ------------------------------------------------------------------
    readonly property real usedK: Math.round(pageRoot.tokenUsed / 100) / 10
    readonly property string usedLabel: pageRoot.usedK + "k"
    readonly property string totalLabel: Math.round(pageRoot.tokenTotal / 1000) + "k"
    readonly property string percentLabel: (Math.round(pageRoot.usageRatio * 1000) / 10) + "%"
    readonly property string freeLabel: (Math.round((1 - pageRoot.usageRatio) * 1000) / 10) + "%"
    readonly property string compactionLabel: Math.round(pageRoot.tokenTotal * pageRoot.compactionThreshold / 1000) + "k"

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
            // degraded banner: the backend is not answering
            // ==========================================================
            Item {
                width: pageRoot.contentWidth
                height: pageRoot.backendDown ? pageRoot.statusRowHeight : 0
                visible: pageRoot.backendDown

                Text {
                    x: 0
                    y: 0
                    width: parent.width
                    text: "Backend unreachable - showing manifest data only"
                    color: Theme.errorHex
                    font.pixelSize: Theme.typeBodySmall
                    font.bold: true
                    wrapMode: Text.WordWrap
                }
            }

            // ==========================================================
            // 1. context budget - big number, inline usage bar, caption
            // ==========================================================
            M3Card {
                id: budgetCard
                objectName: "dashboard-budget"
                width: pageRoot.cardWidth
                height: pageRoot.budgetCardHeight
                variant: "elevated"

                Text {
                    x: 0
                    y: 0
                    text: "Context budget"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleMedium
                    font.bold: true
                }

                // used / total, e.g. "118.4k / 200k"
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
                    y: 35
                    text: "/ " + pageRoot.totalLabel
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodyLarge
                }

                Text {
                    id: percentValue
                    x: pageRoot.cardContentWidth - percentValue.implicitWidth
                    y: 34
                    text: pageRoot.percentLabel + " used"
                    color: pageRoot.usageCritical ? Theme.errorHex : Theme.primaryHex
                    font.pixelSize: Theme.typeTitleSmall
                    font.bold: true
                }

                // inline usage bar: Rectangle track + Rectangle fill.
                // The fill turns error above 85%, the same rule the TopBar uses.
                Item {
                    id: budgetBarBlock
                    objectName: "dashboard-budget-bar"
                    x: 0
                    y: 76
                    width: pageRoot.cardContentWidth
                    height: 18

                    Rectangle {
                        id: budgetTrack
                        x: 0
                        y: (budgetBarBlock.height - height) / 2
                        width: budgetBarBlock.width
                        height: 10
                        radius: Math.min(Theme.shapeFull, height / 2)
                        color: Theme.surfaceContainerHighestHex

                        Rectangle {
                            id: budgetFill
                            x: 0
                            y: 0
                            width: budgetTrack.width * pageRoot.usageRatio
                            height: budgetTrack.height
                            radius: Math.min(Theme.shapeFull, height / 2)
                            color: pageRoot.usageCritical ? Theme.errorHex : Theme.primaryHex
                        }
                    }

                    // 85% auto-compaction threshold: a hairline across the track
                    Rectangle {
                        x: budgetTrack.width * pageRoot.compactionThreshold - width / 2
                        y: 0
                        width: 2
                        height: budgetBarBlock.height
                        radius: Math.min(Theme.shapeFull, width / 2)
                        color: Theme.outlineHex
                    }
                }

                Text {
                    x: 0
                    y: 104
                    width: pageRoot.cardContentWidth
                    text: "Auto-compaction starts at " + pageRoot.compactionLabel
                          + " tokens; the window is still " + pageRoot.freeLabel + " free."
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                    wrapMode: Text.WordWrap
                }
            }

            // ==========================================================
            // 2. plugin health - one row per official plugin (first three)
            // ==========================================================
            M3Card {
                id: pluginCard
                objectName: "dashboard-plugins"
                width: pageRoot.cardWidth
                height: pageRoot.pluginCardHeight
                variant: "filled"

                Text {
                    x: 0
                    y: 0
                    text: "Plugin health"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleMedium
                    font.bold: true
                }

                Text {
                    id: pluginSummary
                    x: pageRoot.cardContentWidth - pluginSummary.implicitWidth
                    y: 2
                    text: pageRoot.officialPluginCount + " official"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeLabelMedium
                }

                // nothing published (empty data/plugins) -> say so instead of an empty card
                Text {
                    x: 0
                    y: 32
                    width: pageRoot.cardContentWidth
                    visible: pageRoot.officialPluginCount === 0
                    text: "No official plugin manifests found in data/plugins."
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                    wrapMode: Text.WordWrap
                }

                Column {
                    id: pluginRows
                    x: 0
                    y: 32
                    width: pageRoot.cardContentWidth
                    spacing: Theme.spaceS
                    visible: pageRoot.officialPluginCount > 0

                    Repeater {
                        model: plugins.officialItems
                        // the dashboard is a summary: only the first three rows
                        windowCount: 3

                        delegate: Item {
                            width: pluginRows.width
                            height: 26

                            // status dot: a square Rectangle rounded to half its size
                            Rectangle {
                                x: 0
                                y: (parent.height - height) / 2
                                width: 10
                                height: 10
                                radius: Math.min(Theme.shapeFull, height / 2)
                                color: modelData.health === "healthy" ? Theme.primaryHex
                                       : (modelData.health === "degraded" ? Theme.errorHex
                                                                          : Theme.outlineHex)
                            }

                            Text {
                                x: 20
                                y: (parent.height - implicitHeight) / 2
                                width: parent.width - 140
                                text: modelData.name
                                color: Theme.onSurfaceHex
                                font.pixelSize: Theme.typeBodyMedium
                                elide: Text.ElideRight
                            }

                            Text {
                                id: stateLabel
                                x: parent.width - stateLabel.implicitWidth
                                y: (parent.height - implicitHeight) / 2
                                text: modelData.health
                                      + (modelData.latencyMs > 0 ? "  ·  " + modelData.latencyMs + " ms" : "")
                                color: modelData.health === "healthy" ? Theme.primaryHex
                                       : (modelData.health === "degraded" ? Theme.errorHex
                                                                          : Theme.outlineHex)
                                font.pixelSize: Theme.typeLabelMedium
                                font.bold: modelData.health === "degraded"
                            }
                        }
                    }
                }

                Rectangle {
                    x: 0
                    y: pluginCard.height - Theme.spaceL - 1
                    width: pageRoot.cardContentWidth
                    height: 1
                    color: Theme.outlineVariantHex
                    opacity: 0.6
                }

                Text {
                    x: 0
                    y: pluginCard.height - Theme.spaceL - 20
                    width: pageRoot.cardContentWidth
                    text: "Source: " + pageRoot.pluginSource + "  ·  preflight: "
                          + status.preflightText
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeLabelSmall
                    elide: Text.ElideRight
                }
            }

            // ==========================================================
            // 3. character health - placeholder stats + the real preflight line
            // ==========================================================
            M3Card {
                id: characterCard
                objectName: "dashboard-character"
                width: pageRoot.cardWidth
                height: pageRoot.characterCardHeight
                variant: "outlined"

                Text {
                    x: 0
                    y: 0
                    text: "Character health"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleMedium
                    font.bold: true
                }

                Text {
                    x: 0
                    y: 30
                    width: pageRoot.cardContentWidth
                    text: pageRoot.personaCount + " personas  ·  last injected "
                          + pageRoot.lastInjectedLabel + "  ·  "
                          + pageRoot.recalledCards + " memory cards recalled"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodyMedium
                    wrapMode: Text.WordWrap
                }

                Text {
                    x: 0
                    y: 56
                    width: pageRoot.cardContentWidth
                    text: "Active persona: " + pageRoot.activePersona + "  ·  58 tokens per injection"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                    wrapMode: Text.WordWrap
                }

                // the one real line on this card: does the backend answer?
                Text {
                    id: preflightLine
                    x: 0
                    y: 82
                    width: pageRoot.cardContentWidth
                    text: pageRoot.preflightLabel + "  ·  " + status.preflightText
                          + "  ·  " + status.endpoint + "  ·  " + status.pollCount + " poll(s)"
                    color: pageRoot.backendDown ? Theme.errorHex : Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeLabelSmall
                    font.bold: pageRoot.backendDown
                    elide: Text.ElideRight
                }
            }
        }
    }
}