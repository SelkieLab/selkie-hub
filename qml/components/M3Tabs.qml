import QtQuick 2.15
import ".."

// M3Tabs - MD3E primary tabs with a spring-driven selection indicator.
//
// PUBLIC API
//   property var labels   : the tab captions, e.g. ["Overview", "Activity", "Settings"]
//   property int currentIndex : the selected tab (two-way; host may set it)
//   signal tabSelected(int index) : emitted when the user picks a tab
//   property real indicatorThickness : MD3 indicator height, default 3
//
// USAGE
//   M3Tabs {
//       width: 560
//       labels: ["Overview", "Activity", "Settings"]
//       currentIndex: 0
//       onTabSelected: page.load(index)
//   }
//
// MOTION (contract C - qml4j has NO SpringAnimation, motion is HOST-DRIVEN)
//   The indicator x is driven by a host spring channel holding the CONTINUOUS
//   tab position (0.0, 1.0, 2.0 ...):
//       property var spring: motion.channel("fastSpatial")
//       x: spring.value * tabWidth
//   onCurrentIndexChanged retargets with motion.to(); the host integrates the
//   MotionTokens fastSpatial spring each frame. No Behavior is used to fake it.
//
// CONTRACT COMPLIANCE: colours Theme.<role>Hex, radii Theme.shape*,
// type Theme.type*, spacing Theme.space*.

Item {
    id: root

    property var labels: []
    property int currentIndex: 0
    property real indicatorThickness: 3

    signal tabSelected(int index)

    // Engine focus (FocusManager): Tab-reachable. Left/Right move the selection while
    // focused (Keys handler), a mouse click still picks a tab directly.
    activeFocusOnTab: true
    Keys.onLeftPressed: root.selectIndex(root.currentIndex - 1)
    Keys.onRightPressed: root.selectIndex(root.currentIndex + 1)

    function selectIndex(index) {
        if (root.count <= 0) return
        var next = Math.max(0, Math.min(root.count - 1, index))
        if (next === root.currentIndex) return
        root.currentIndex = next
        root.tabSelected(next)
    }

    property int count: root.labels === null ? 0 : root.labels.length
    property real tabWidth: root.count > 0 ? root.width / root.count : root.width

    implicitWidth: 320
    implicitHeight: 48

    // Host spring channel holding the continuous tab index.
    property var spring: motion.channel("fastSpatial")

    Component.onCompleted: motion.jump(root.spring, root.currentIndex)
    onCurrentIndexChanged: motion.to(root.spring, root.currentIndex)

    Repeater {
        model: root.labels
        delegate: Item {
            width: root.tabWidth
            height: root.height
            x: index * root.tabWidth

            Text {
                anchors.centerIn: parent
                text: modelData
                color: index === root.currentIndex ? Theme.primaryHex : Theme.onSurfaceVariantHex
                font.pixelSize: Theme.typeTitleSmall
                font.bold: index === root.currentIndex
            }

            MouseArea {
                anchors.fill: parent
                onClicked: {
                    root.currentIndex = index
                    root.tabSelected(index)
                }
            }
        }
    }

    // Spring-driven indicator: position is the channel value, never a Behavior.
    Rectangle {
        id: indicator
        width: root.tabWidth
        height: root.indicatorThickness
        y: root.height - root.indicatorThickness
        x: root.spring.value * root.tabWidth
        radius: Theme.shapeFull
        color: Theme.primaryHex
    }

    // Focus ring around the whole tab strip, bound to the engine's activeFocus.
    Rectangle {
        objectName: "focus-ring"
        anchors.fill: parent
        anchors.margins: -2
        radius: Theme.shapeS
        color: "transparent"
        border.width: 2
        border.color: Theme.primaryHex
        visible: root.activeFocus
    }
}
