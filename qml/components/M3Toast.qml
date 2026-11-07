import QtQuick 2.15
import ".."

// M3Toast - overlay toast / snackbar. NO Popup, NO Dialog, NO Menu:
// qml4j has no Controls layer, so this is an overlay Rectangle inside the page
// item tree (see contract D). The component itself never captures clicks.
//
// PUBLIC API
//   function show(message)  : show the toast with a message and start the timer
//   function dismiss()      : hide immediately and emit dismissed()
//   property string message : current text
//   property bool shown     : whether the toast is on screen
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
// CONTRACT COMPLIANCE: colours Theme.<role>Hex (inverseSurface /
// inverseOnSurface), radii Theme.shape*, type Theme.type*, spacing Theme.space*.
// The only animation is an opacity fade - the spring budget belongs to the
// switch thumb and the tab indicator.

Item {
    id: root

    property string message: ""
    property bool shown: false
    property int duration: 2600

    signal dismissed()

    function show(message) {
        root.message = message
        root.shown = true
        hideTimer.restart()
    }

    function dismiss() {
        hideTimer.stop()
        if (!root.shown) return
        root.shown = false
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
        opacity: root.shown ? 1.0 : 0.0
        visible: root.shown || opacity > 0.0
        Behavior on opacity { NumberAnimation { duration: 160; easing.type: Easing.OutCubic } }

        Text {
            id: body
            anchors.centerIn: parent
            text: root.message
            color: Theme.inverseOnSurfaceHex
            font.pixelSize: Theme.typeBodyMedium
        }
    }
}
