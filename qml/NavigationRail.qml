import QtQuick 2.15
import "."

// NavigationRail - MD3E wide navigation rail.
//
// Wide (>= 600px): 232px, icon tile + destination label.
// Narrow (< 600px): 88px, icon tile only (the label is hidden, not shrunk) -
// the standard MD3E rail collapse behaviour.
//
// The selection pill's vertical position is driven by a HOST spring channel
// (qml4j has no SpringAnimation), so it settles with a slight overshoot and can be
// interrupted mid-flight, while its parameters come from Theme.spring.fastSpatial*.
//
// Public API
//   property var  destinations   array of { glyph, label }
//   property int  currentIndex
//   property bool collapsed      set by the parent from the window width
//   signal        destinationSelected(int index)
Rectangle {
    id: rail

    property var destinations: []
    property int currentIndex: 0
    property bool collapsed: false

    signal destinationSelected(int index)

    readonly property real expandedWidth: 232
    readonly property real collapsedWidth: 88
    readonly property real destinationHeight: 56
    readonly property real rowGap: Theme.spaceXs
    readonly property real tileSize: 40
    readonly property real rowPitch: destinationHeight + rowGap

    // Animated index: the pill slides, it does not jump.
    property var slideSpring: motion.channel("fastSpatial")

    // Which destination currently holds keyboard focus (-1 when the rail does not).
    // Delegates report into this; the rail's key handlers act on it.
    property int focusedIndex: -1

    // No focus: true here - the shell root owns keyboard activation (see main.qml).
    // Key events bubble from the focused delegate to the root, which is always in the
    // chain even when a window focus change clears the item-level focus.

    function activateFocused() {
        var i = rail.focusedIndex
        if (i < 0) i = rail.currentIndex
        rail.currentIndex = i
        rail.destinationSelected(i)
    }

    // (key handling lives on the shell root - see main.qml)

    width: collapsed ? collapsedWidth : expandedWidth
    height: parent ? parent.height : 720
    color: Theme.surface

    // Right hairline so the rail reads as its own pane.
    Rectangle {
        x: rail.width - 1
        width: 1
        height: rail.height
        color: Theme.outlineVariant
        opacity: 0.6
    }

    // ---- brand ----
    Rectangle {
        id: brand
        x: rail.collapsed ? (rail.width - tileSize) / 2 : Theme.spaceL
        y: Theme.spaceM
        width: tileSize
        height: tileSize
        radius: Theme.shapeM
        color: Theme.primary

        Text {
            anchors.centerIn: parent
            text: "S"
            color: Theme.onPrimary
            font.pixelSize: Theme.typeTitleLarge
            font.bold: true
        }
    }

    Text {
        x: Theme.spaceL + tileSize + Theme.spaceM
        y: (brand.y + brand.height / 2) - height / 2
        visible: !rail.collapsed
        text: "Selkie Hub"
        color: Theme.onSurface
        font.pixelSize: Theme.typeTitleMedium
        font.bold: true
    }

    readonly property real listTop: brand.y + brand.height + Theme.spaceL

    // ---- selection pill (spring-driven) ----
    Rectangle {
        id: pill
        readonly property real pillWidth: rail.collapsed ? rail.width - Theme.spaceM : rail.width - 2 * Theme.spaceS
        x: rail.collapsed ? (rail.width - pillWidth) / 2 : Theme.spaceS
        width: pillWidth
        height: rail.destinationHeight
        radius: Math.min(Theme.shapeFull, height / 2)
        color: Theme.secondaryContainer
        y: rail.listTop + rail.slideSpring.value * rail.rowPitch
    }

    // ---- destinations ----
    Item {
        id: list
        x: 0
        y: rail.listTop
        width: rail.width
        height: rail.height - rail.listTop

        Repeater {
            model: rail.destinations

            delegate: Item {
                id: row
                // Deterministic handle for scripted acceptance: --click-item=nav-3.
                objectName: "nav-" + index
                // The rail is the primary way through the app, so its destinations must be
                // reachable with Tab alone (the engine walks activeFocusOnTab items).
                activeFocusOnTab: true
                // The engine routes key events to items that hold "focus" in their scope;
                // activeFocusOnTab alone makes them Tab-reachable but not key-receiving.
                focus: true
                width: list.width
                height: rail.destinationHeight
                y: index * rail.rowPitch

                // Focus ring: only while this row holds keyboard focus.
                Rectangle {
                    objectName: "nav-focus-ring-" + index
                    anchors.fill: parent
                    anchors.margins: 2
                    radius: Theme.shapeM
                    color: "transparent"
                    border.width: row.activeFocus ? 2 : 0
                    border.color: Theme.primaryHex
                    visible: row.activeFocus
                }

                // icon tile
                Rectangle {
                    id: tile
                    x: rail.collapsed ? (rail.width - rail.tileSize) / 2 : Theme.spaceL + Theme.spaceS
                    y: (rail.destinationHeight - height) / 2
                    width: rail.tileSize
                    height: rail.tileSize
                    radius: Theme.shapeM
                    color: index === rail.currentIndex ? Theme.onSecondaryContainer
                                                       : "transparent"

                    Text {
                        anchors.centerIn: parent
                        text: modelData.glyph === undefined ? "" : modelData.glyph
                        color: index === rail.currentIndex ? Theme.onSecondaryContainer
                                                           : Theme.onSurfaceVariant
                        font.pixelSize: Theme.typeTitleMedium
                        font.bold: true
                    }
                }

                // destination label (wide mode only)
                Text {
                    x: rail.collapsed ? 0 : tile.x + tile.width + Theme.spaceM
                    width: rail.collapsed ? 0 : rail.width - x - Theme.spaceS
                    y: (rail.destinationHeight - height) / 2
                    visible: !rail.collapsed
                    text: modelData.label === undefined ? "" : modelData.label
                    color: index === rail.currentIndex ? Theme.onSecondaryContainer
                                                       : Theme.onSurfaceVariant
                    font.pixelSize: Theme.typeLabelLarge
                    font.bold: index === rail.currentIndex
                }

                MouseArea {
                    anchors.fill: parent
                    onClicked: {
                        rail.currentIndex = index
                        rail.destinationSelected(index)
                    }
                }

                // NOTE: Keys.* must NOT live on a Repeater delegate in qml4j v0.2.38 -
                // attached key signals inside a delegate never fire (measured). The rail
                // handles keys instead and acts on the index it tracks below.
                onActiveFocusChanged: if (activeFocus) rail.focusedIndex = index
            }
        }
    }

    onCurrentIndexChanged: motion.to(rail.slideSpring, rail.currentIndex)
    Component.onCompleted: motion.jump(rail.slideSpring, rail.currentIndex)
}
