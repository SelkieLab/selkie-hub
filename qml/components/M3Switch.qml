import QtQuick 2.15
import ".."

// M3Switch - MD3E switch (52 x 32 track, 24 thumb).
//
// PUBLIC API
//   property bool checked : the switch state (two-way; write it from the host page)
//   property real trackWidth / trackHeight / thumbSize / inset : MD3 geometry,
//       overridable, defaults are the MD3 spec
//   signal toggled(bool checked) : emitted on every user toggle
//   inherited Item.enabled       : disabled switches do not react and render dimmed
//
// USAGE
//   M3Switch {
//       checked: true
//       onToggled: console.log("switch ->", checked)
//   }
//
// MOTION (contract C - qml4j has NO SpringAnimation, motion is HOST-DRIVEN)
//   The thumb position is a pure function of a host spring channel:
//       property var spring: motion.channel("fastSpatial")
//       x: inset + travel * spring.value
//   onCheckedChanged retargets the channel with motion.to(); the host integrates
//   the MotionTokens spring every frame and writes SpringChannel.value, which
//   re-runs the binding. No Behavior is used to fake the spring.
//
// CONTRACT COMPLIANCE: colours Theme.<role>Hex, radii Theme.shape*,
// type Theme.type*, spacing Theme.space*.

Item {
    id: root

    property bool checked: false

    property real trackWidth: 52
    property real trackHeight: Theme.spaceXxl
    property real thumbSize: Theme.spaceXl
    property real inset: Theme.spaceXs

    signal toggled(bool checked)

    implicitWidth: root.trackWidth
    implicitHeight: root.trackHeight
    opacity: root.enabled ? 1.0 : 0.38

    // Host spring channel: 0.0 = off, 1.0 = on. Created once per switch instance.
    property var spring: motion.channel("fastSpatial")

    Component.onCompleted: motion.jump(root.spring, root.checked ? 1.0 : 0.0)
    onCheckedChanged: motion.to(root.spring, root.checked ? 1.0 : 0.0)

    Rectangle {
        id: track
        anchors.fill: parent
        radius: Theme.shapeFull
        color: root.checked ? Theme.primaryHex : Theme.surfaceContainerHighestHex
        border.width: root.checked ? 0 : 2
        border.color: Theme.outlineHex
    }

    Rectangle {
        id: thumb
        width: root.thumbSize
        height: root.thumbSize
        radius: Theme.shapeFull
        anchors.verticalCenter: parent.verticalCenter
        // Spring-driven travel, never a Behavior: the host owns the physics.
        x: root.inset + (root.trackWidth - root.thumbSize - root.inset * 2) * root.spring.value
        color: root.checked ? Theme.onPrimaryHex : Theme.outlineHex
    }

    MouseArea {
        id: hit
        anchors.fill: parent
        enabled: root.enabled
        hoverEnabled: root.enabled
        onClicked: {
            root.checked = !root.checked
            root.toggled(root.checked)
        }
    }
}
