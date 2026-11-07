import QtQuick 2.15
import ".."
import "../components"

// PluginsPage - Stage 4: the whole inventory is live host data.
//
// DATA SOURCE
//   The context object "plugins" (selkie.host.PluginBridge, registered by the host BEFORE
//   view.load) publishes three ready-made lists. This page uses the two pre-split ones:
//   officialItems and thirdPartyItems, plus count / source for the header. There is no
//   placeholder array left in this file - every row is a PluginInfo the host parsed out of
//   data/plugins/<id>/manifest.json.
//
// WHY THE DELEGATE READS FIELDS
//   PluginInfo is a POJO with public fields only (qml4j resolves members through
//   Class.getField, so a Java getter would be invisible). A delegate therefore writes
//   modelData.name, modelData.health, modelData.latencyMs, ... and never modelData.getName().
//   providesText / permissionsText are the host's pre-joined strings ("tool, prompt") because
//   QML cannot read a List<String> reliably; the raw lists still exist host-side for tests.
//
// HEALTH COLOURS
//   healthy -> Theme.primary, degraded -> Theme.error, unknown -> Theme.outline. The dot, the
//   pill border and the pill label all take that role, so the state survives a scheme flip
//   without any literal colour.
//
// FILTERS REALLY FILTER
//   All shows both cards; Official / Third party hide the other section. A Column skips
//   invisible children, so hiding a section leaves no empty slot behind.
//
// DEGRADED MODE
//   When status.preflightOk is false the dev backend is not answering: the page says so at the
//   top in Theme.error and keeps rendering the manifest data it already has, because the
//   manifests are local files and remain valid without the backend.
//
// PAGE CONTRACT (the shell owns geometry)
//   Root is Item { id: pageRoot } with no x/y/width/height; the shell sets the box.
//   Everything is inside a Flickable so a short window can still reach section two.
//
// INTERACTIVE OBJECTNAMES: plugin-filter-all, plugin-filter-official,
//   plugin-filter-third-party, plugins-official, plugins-third-party

Item {
    id: pageRoot

    // ------------------------------------------------------------------
    // filter state - 0 All, 1 Official, 2 Third party
    // ------------------------------------------------------------------
    property int filterIndex: 0

    // ------------------------------------------------------------------
    // live data - selkie.host.PluginBridge as the context object "plugins"
    // ------------------------------------------------------------------
    readonly property int installedCount: plugins.count
    readonly property int officialCount: plugins.officialItems.length
    readonly property int thirdPartyCount: plugins.thirdPartyItems.length
    readonly property string dataSource: plugins.source
    // Degraded state: a FAILED poll, not the pre-poll default. status.preflightOk starts
    // false (and pollCount 0) while the first request is still in flight, so gating on
    // pollCount keeps a healthy launch from flashing an error banner for two frames.
    readonly property bool backendDown: status.preflightOk === false && status.pollCount > 0

    // ------------------------------------------------------------------
    // formatting helpers
    //   Values are passed IN as arguments so the dependency on a bridge property is
    //   recorded by the binding that calls the function, not by the function body.
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

    // ------------------------------------------------------------------
    // page geometry
    // ------------------------------------------------------------------
    readonly property real pageMargin: Theme.spaceXl
    readonly property real contentWidth: Math.max(280, Math.min(720, pageRoot.width - 2 * pageRoot.pageMargin))
    readonly property real cardWidth: pageRoot.contentWidth
    readonly property real cardContentWidth: pageRoot.cardWidth - 2 * Theme.spaceL

    readonly property real filterRowHeight: 40
    readonly property real sectionHeadingHeight: 26
    readonly property real rowHeight: 60
    readonly property real emptyRowHeight: 34
    readonly property real statusRowHeight: 22

    // A card is its rows plus the M3Card padding; an empty group shrinks to one line.
    readonly property real officialCardHeight: 2 * Theme.spaceL
                                                + (pageRoot.officialCount === 0
                                                   ? pageRoot.emptyRowHeight
                                                   : pageRoot.officialCount * pageRoot.rowHeight)
    readonly property real thirdPartyCardHeight: 2 * Theme.spaceL
                                                  + (pageRoot.thirdPartyCount === 0
                                                     ? pageRoot.emptyRowHeight
                                                     : pageRoot.thirdPartyCount * pageRoot.rowHeight)

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

            // ==========================================================
            // count line, straight from the bridge
            // ==========================================================
            Item {
                width: pageRoot.contentWidth
                height: 18

                Text {
                    x: 0
                    y: 0
                    width: parent.width
                    text: pageRoot.installedCount + " installed  ·  "
                          + pageRoot.officialCount + " official  ·  "
                          + pageRoot.thirdPartyCount + " third party  ·  source: "
                          + pageRoot.dataSource
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                    elide: Text.ElideRight
                }
            }

            // ==========================================================
            // section 1: official plugins
            // ==========================================================
            Item {
                width: pageRoot.contentWidth
                height: pageRoot.sectionHeadingHeight
                visible: pageRoot.filterIndex !== 2

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
                visible: pageRoot.filterIndex !== 2
                width: pageRoot.cardWidth
                height: pageRoot.officialCardHeight
                variant: "outlined"

                // empty state: the manifest tree has no official plugin yet
                Text {
                    x: 0
                    y: 0
                    width: pageRoot.cardContentWidth
                    visible: pageRoot.officialCount === 0
                    text: "No official plugins found - add data/plugins/<id>/manifest.json"
                          + " with \"official\": true and reload."
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                    elide: Text.ElideRight
                }

                Column {
                    id: officialRows
                    x: 0
                    y: 0
                    width: pageRoot.cardContentWidth
                    spacing: 0
                    visible: pageRoot.officialCount > 0

                    Repeater {
                        model: plugins.officialItems

                        delegate: Item {
                            width: officialRows.width
                            height: pageRoot.rowHeight

                            Text {
                                x: 0
                                y: 5
                                width: parent.width - 150
                                text: modelData.name
                                color: Theme.onSurfaceHex
                                font.pixelSize: Theme.typeBodyMedium
                                font.bold: true
                                elide: Text.ElideRight
                            }

                            Text {
                                x: 0
                                y: 24
                                width: parent.width - 150
                                text: modelData.description
                                color: Theme.onSurfaceVariantHex
                                font.pixelSize: Theme.typeBodySmall
                                elide: Text.ElideRight
                            }

                            // one meta line: who published it, what it contributes
                            Text {
                                x: 0
                                y: 41
                                width: parent.width - 150
                                text: modelData.publisher + "  ·  v" + modelData.version
                                      + "  ·  provides " + modelData.providesText
                                      + "  ·  needs " + modelData.permissionsText
                                      + "  ·  " + modelData.tokenPolicy
                                color: Theme.onSurfaceVariantHex
                                font.pixelSize: Theme.typeLabelSmall
                                elide: Text.ElideRight
                            }

                            // status pill: 1dp border + label carry the health colour
                            Rectangle {
                                id: officialPill
                                anchors.right: parent.right
                                y: 4
                                width: officialPillLabel.implicitWidth + 2 * Theme.spaceM
                                height: 22
                                radius: Math.min(Theme.shapeFull, height / 2)
                                color: "transparent"
                                border.width: 1
                                border.color: modelData.health === "healthy" ? Theme.primaryHex
                                              : (modelData.health === "degraded" ? Theme.errorHex
                                                                                 : Theme.outlineHex)

                                Text {
                                    id: officialPillLabel
                                    anchors.centerIn: parent
                                    text: modelData.health
                                    color: modelData.health === "healthy" ? Theme.primaryHex
                                           : (modelData.health === "degraded" ? Theme.errorHex
                                                                              : Theme.outlineHex)
                                    font.pixelSize: Theme.typeLabelSmall
                                    font.bold: true
                                }
                            }

                            // measured round trip, only when the backend reported one
                            Text {
                                id: officialLatency
                                anchors.right: parent.right
                                y: 30
                                visible: modelData.latencyMs > 0
                                text: modelData.latencyMs + " ms"
                                color: Theme.onSurfaceVariantHex
                                font.pixelSize: Theme.typeLabelSmall
                            }

                            // hairline between rows, not under the last one
                            Rectangle {
                                x: 0
                                y: parent.height - 1
                                width: parent.width
                                height: 1
                                visible: index < pageRoot.officialCount - 1
                                color: Theme.outlineVariantHex
                                opacity: 0.6
                            }
                        }
                    }
                }
            }

            // ==========================================================
            // section 2: third-party plugins
            // ==========================================================
            Item {
                width: pageRoot.contentWidth
                height: pageRoot.sectionHeadingHeight
                visible: pageRoot.filterIndex !== 1

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
                visible: pageRoot.filterIndex !== 1
                width: pageRoot.cardWidth
                height: pageRoot.thirdPartyCardHeight
                variant: "outlined"

                Text {
                    x: 0
                    y: 0
                    width: pageRoot.cardContentWidth
                    visible: pageRoot.thirdPartyCount === 0
                    text: "No third-party plugins installed - every plugin in data/plugins"
                          + " is marked official."
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodySmall
                    elide: Text.ElideRight
                }

                Column {
                    id: thirdPartyRows
                    x: 0
                    y: 0
                    width: pageRoot.cardContentWidth
                    spacing: 0
                    visible: pageRoot.thirdPartyCount > 0

                    Repeater {
                        model: plugins.thirdPartyItems

                        delegate: Item {
                            width: thirdPartyRows.width
                            height: pageRoot.rowHeight

                            Text {
                                x: 0
                                y: 5
                                width: parent.width - 150
                                text: modelData.name
                                color: Theme.onSurfaceHex
                                font.pixelSize: Theme.typeBodyMedium
                                font.bold: true
                                elide: Text.ElideRight
                            }

                            Text {
                                x: 0
                                y: 24
                                width: parent.width - 150
                                text: modelData.description
                                color: Theme.onSurfaceVariantHex
                                font.pixelSize: Theme.typeBodySmall
                                elide: Text.ElideRight
                            }

                            Text {
                                x: 0
                                y: 41
                                width: parent.width - 150
                                text: modelData.publisher + "  ·  v" + modelData.version
                                      + "  ·  provides " + modelData.providesText
                                      + "  ·  needs " + modelData.permissionsText
                                      + "  ·  " + modelData.tokenPolicy
                                color: Theme.onSurfaceVariantHex
                                font.pixelSize: Theme.typeLabelSmall
                                elide: Text.ElideRight
                            }

                            Rectangle {
                                id: thirdPill
                                anchors.right: parent.right
                                y: 4
                                width: thirdPillLabel.implicitWidth + 2 * Theme.spaceM
                                height: 22
                                radius: Math.min(Theme.shapeFull, height / 2)
                                color: "transparent"
                                border.width: 1
                                border.color: modelData.health === "healthy" ? Theme.primaryHex
                                              : (modelData.health === "degraded" ? Theme.errorHex
                                                                                 : Theme.outlineHex)

                                Text {
                                    id: thirdPillLabel
                                    anchors.centerIn: parent
                                    text: modelData.health
                                    color: modelData.health === "healthy" ? Theme.primaryHex
                                           : (modelData.health === "degraded" ? Theme.errorHex
                                                                              : Theme.outlineHex)
                                    font.pixelSize: Theme.typeLabelSmall
                                    font.bold: true
                                }
                            }

                            Text {
                                id: thirdLatency
                                anchors.right: parent.right
                                y: 30
                                visible: modelData.latencyMs > 0
                                text: modelData.latencyMs + " ms"
                                color: Theme.onSurfaceVariantHex
                                font.pixelSize: Theme.typeLabelSmall
                            }

                            Rectangle {
                                x: 0
                                y: parent.height - 1
                                width: parent.width
                                height: 1
                                visible: index < pageRoot.thirdPartyCount - 1
                                color: Theme.outlineVariantHex
                                opacity: 0.6
                            }
                        }
                    }
                }
            }

            // ==========================================================
            // footer: what the host is reading, and the preflight verdict
            // ==========================================================
            Item {
                width: pageRoot.contentWidth
                height: 20

                Text {
                    x: 0
                    y: 0
                    width: parent.width
                    text: "Source: " + pageRoot.dataSource + "  ·  revision "
                          + plugins.revision + "  ·  preflight: " + status.preflightText
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeLabelSmall
                    elide: Text.ElideRight
                }
            }
        }
    }
}