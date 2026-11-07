import QtQuick 2.15
import ".."

// M3Card - MD3E card surface (MD3 expressive container).
//
// PUBLIC API
//   property string variant : "filled" (default) | "elevated" | "outlined"
//       filled   -> surfaceContainerHighest container
//       elevated -> surfaceContainerLow container lifted by an offset scrim
//       outlined -> surface container + 1dp outlineVariant border
//   property real padding     : inner padding, default Theme.spaceL (16)
//   property bool interactive : adds the MD3 state layer + hover cursor and
//                               makes the card emit clicked()
//   default property content  : child items are parented into the padded content
//                               area, so `M3Card { M3Button {} }` lays the button
//                               out inside the card automatically
//   signal clicked()          : only emitted while interactive is true
//
// USAGE
//   M3Card {
//       width: 288
//       variant: "elevated"
//       interactive: true
//       onClicked: console.log("card pressed")
//       Column { spacing: Theme.spaceM; M3Button { text: "Save" } }
//   }
//
// CONTRACT COMPLIANCE
//   colours  : Theme.<role>Hex only (qml4j resolves a color from a STRING; the
//              Integer form on the Java bridge throws ClassCastException).
//              The single non-Theme colour string is "transparent", which the
//              engine documents as alpha zero, not a palette colour.
//   radii    : Theme.shape*   type: Theme.type*   spacing: Theme.space*
//   no page-level geometry is hardcoded: the implicit size comes from the
//   content; the host page sets width/height or lays this item out.

Item {
    id: root

    property string variant: "filled"
    property real padding: Theme.spaceL
    property bool interactive: false

    default property alias content: contentArea.data

    signal clicked()

    implicitWidth: contentArea.childrenRect.width + root.padding * 2
    implicitHeight: contentArea.childrenRect.height + root.padding * 2
    clip: true

    // Container role per variant: all three come from the surfaceContainer range.
    property string containerHex: root.variant === "elevated" ? Theme.surfaceContainerLowHex
                                : root.variant === "outlined" ? Theme.surfaceHex
                                : Theme.surfaceContainerHighestHex

    // MD3 elevation 1 for "elevated": an offset scrim lay-under. Qt's DropShadow
    // is only reachable through Item.layer.effect in qml4j and there is no
    // Controls elevation layer, so the lift is drawn explicitly - still with a
    // Theme role, never a literal colour. Declared FIRST so it paints behind.
    Rectangle {
        id: lift
        anchors.fill: container
        anchors.topMargin: root.variant === "elevated" ? Theme.spaceXs : 0
        radius: Theme.shapeM
        color: Theme.scrimHex
        opacity: root.variant === "elevated" ? 0.16 : 0.0
        visible: root.variant === "elevated"
    }

    Rectangle {
        id: container
        anchors.fill: parent
        radius: Theme.shapeM
        color: root.containerHex
        border.width: root.variant === "outlined" ? 1 : 0
        border.color: Theme.outlineVariantHex
    }

    // State layer: a translucent onSurface wash, expressed with opacity so no
    // alpha colour literal is needed.
    Rectangle {
        id: stateLayer
        anchors.fill: parent
        radius: Theme.shapeM
        color: Theme.onSurfaceHex
        opacity: !root.interactive ? 0.0
               : hit.pressed ? 0.12
               : hit.containsMouse ? 0.08
               : 0.0
        Behavior on opacity { NumberAnimation { duration: 120; easing.type: Easing.OutCubic } }
    }

    // Sits UNDER the content so a Button/Switch inside the card keeps its own
    // hit area; the card only reacts where the content is not interactive.
    MouseArea {
        id: hit
        anchors.fill: parent
        enabled: root.interactive
        hoverEnabled: root.interactive
        onClicked: root.clicked()
    }

    // The default-property container: default children are parented here, with
    // the card padding already applied.
    Item {
        id: contentArea
        anchors.fill: parent
        anchors.margins: root.padding
    }
}
