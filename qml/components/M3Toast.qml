import QtQuick 2.15
import ".."

// M3Toast - overlay toast / snackbar. NO Popup, NO Dialog, NO Menu:
// qml4j has no Controls layer, so this is an overlay Rectangle inside the page
// item tree (see contract D). The component itself never captures clicks.
//
// PUBLIC API
//   function show(message)  : show the toast with a message and start the timer
//   function dismiss()      : hide (animated) and emit dismissed()
//   property string message : current text
//   property bool shown     : whether the toast is currently requested on screen
//   property int duration   : auto-dismiss delay in ms (default 2600)
//   signal dismissed()      : emitted when the toast hides (timer or dismiss())
//
// USAGE (the host page anchors it over the whole surface once):
//   M3Toast {
//       id: toast
//       anchors.fill: parent
//   }
//   ...
//   toast.show("Saved to Selkie Hub")
//
// MOTION (MD3E / QPlayer overlay spec, host-driven)
//   qml4j has no SpringAnimation and no controllable NumberAnimation timeline, so the
//   expand/collapse envelope is integrated by the host and read here:
//     expand : scale 0.92 -> 1.0 over 200 ms, opacity 0 -> 1 over 150 ms
//     collapse: scale 1.0 -> 0.92 and opacity 1 -> 0 over 150 ms, then visible = false
//   motion.overlay("toast") returns one channel per name, so re-showing a toast
//   reuses it instead of leaking a channel per call.
//
// CONTRACT COMPLIANCE: colours Theme.<role>Hex (inverseSurface /
// inverseOnSurface), radii Theme.shape*, type Theme.type*, spacing Theme.space*.
// No literal colour, radius, font size or spring parameter.
Item {
    id: root

    property string message: ""
    property bool shown: false
    property int duration: 2600

    signal dismissed()

    // Host-driven overlay envelope (scale + opacity), see the MOTION block above.
    property var envelope: motion.overlay("toast")

    function show(message) {
        root.message = message
        root.shown = true
        motion.showOverlay(root.envelope)
        hideTimer.restart()
    }

    function dismiss() {
        hideTimer.stop()
        if (!root.shown) return
        root.shown = false
        motion.hideOverlay(root.envelope)
        root.dismissed()
    }

    Timer {
        id: hideTimer
        interval: root.duration
        repeat: false
        onTriggered: root.dismiss()
    }

    Rectangle {
        id: bubble
        anchors.horizontalCenter: parent.horizontalCenter
        anchors.bottom: parent.bottom
        anchors.bottomMargin: Theme.spaceXl
        width: Math.min(root.width - Theme.spaceXl * 2, body.implicitWidth + Theme.spaceXl * 2)
        height: 48
        radius: Theme.shapeXs
        color: Theme.inverseSurfaceHex
        // The channel drives both the fade and the slight scale-up, and it flips
        // visible back to false once the collapse envelope finishes.
        opacity: root.envelope.opacity
        scale: root.envelope.scale
        visible: root.envelope.visible

        Text {
            id: body
            anchors.centerIn: parent
            text: root.message
            color: Theme.inverseOnSurfaceHex
            font.pixelSize: Theme.typeBodyMedium
        }
    }
}
