import QtQuick 2.15
import ".."

// M3TextField - MD3E outlined text field with a floating label.
//
// PUBLIC API
//   property string label       : the field label (floats to the top border when
//                                 the field is focused or filled)
//   property string placeholder : hint shown while text is empty
//   property string text        : the current value (two-way from the host page)
//   property bool focused       : true while the field owns the focus ring.
//                                 Clicking the field sets it; the host may also
//                                 write it. (Keyboard input arrives in stage 3/5 -
//                                 this stage proves the clickable field, the value
//                                 display and the focus border.)
//   signal clicked()            : emitted on a user click (text entry hook point)
//   inherited Item.enabled      : disabled fields dim to 38% and ignore clicks
//
// USAGE
//   M3TextField {
//       width: 288
//       label: "Search"
//       placeholder: "Type a query"
//       text: model.query
//       focused: true
//   }
//
// CONTRACT COMPLIANCE: colours Theme.<role>Hex, radii Theme.shape*,
// type Theme.type*, spacing Theme.space*.

Item {
    id: root

    property string label: ""
    property string placeholder: ""
    property string text: ""
    property bool focused: false

    signal clicked()

    property bool floats: root.focused || root.activeFocus || root.text.length > 0

    implicitWidth: 288
    implicitHeight: 56
    opacity: root.enabled ? 1.0 : 0.38

    Rectangle {
        id: frame
        anchors.fill: parent
        radius: Theme.shapeXs
        color: Theme.surfaceHex
        border.width: (root.focused || root.activeFocus) ? 2 : 1
        border.color: (root.focused || root.activeFocus) ? Theme.primaryHex : Theme.outlineHex
    }

    // Notch backdrop behind the floated label so it reads as sitting ON the
    // outline (a Rectangle in the field's own surface colour, not a literal).
    Rectangle {
        id: labelNotch
        visible: root.floats && root.label.length > 0
        x: Theme.spaceL - Theme.spaceXs
        y: 0 - labelFloated.implicitHeight / 2
        width: labelFloated.implicitWidth + Theme.spaceS * 2
        height: labelFloated.implicitHeight
        color: Theme.surfaceHex
    }

    Text {
        id: labelFloated
        visible: root.floats && root.label.length > 0
        x: Theme.spaceL
        y: 0 - labelFloated.implicitHeight / 2
        text: root.label
        color: (root.focused || root.activeFocus) ? Theme.primaryHex : Theme.onSurfaceVariantHex
        font.pixelSize: Theme.typeBodySmall
    }

    Text {
        id: labelResting
        visible: !root.floats && root.label.length > 0
        anchors.left: parent.left
        anchors.leftMargin: Theme.spaceL
        anchors.verticalCenter: parent.verticalCenter
        text: root.label
        color: Theme.onSurfaceVariantHex
        font.pixelSize: Theme.typeBodyLarge
    }

    Text {
        id: value
        visible: root.text.length > 0
        anchors.left: parent.left
        anchors.leftMargin: Theme.spaceL
        anchors.verticalCenter: parent.verticalCenter
        text: root.text
        color: Theme.onSurfaceHex
        font.pixelSize: Theme.typeBodyLarge
    }

    Text {
        id: hint
        // With a label present the hint only shows once the label has floated,
        // otherwise the resting label and the hint would overlap (MD3 rule).
        visible: root.text.length === 0 && (root.floats || root.label.length === 0)
        anchors.left: parent.left
        anchors.leftMargin: Theme.spaceL
        anchors.verticalCenter: parent.verticalCenter
        text: root.placeholder
        color: Theme.onSurfaceVariantHex
        font.pixelSize: Theme.typeBodyLarge
    }

    MouseArea {
        id: hit
        anchors.fill: parent
        enabled: root.enabled
        hoverEnabled: root.enabled
        onClicked: {
            root.focus = true
            root.focused = true
            root.clicked()
        }
    }
}
