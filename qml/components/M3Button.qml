import QtQuick 2.15
import ".."

// M3Button - MD3E button in four variants, with a real disabled state.
//
// PUBLIC API
//   property string text    : the label (English placeholders for now; the host
//                             does not ship a CJK typeface yet)
//   property string variant : "filled" (default) | "tonal" | "outlined" | "text"
//   signal clicked()        : emitted on a user click while enabled
//   inherited Item.enabled  : disabled -> no clicks, 38% opacity, no state layer
//
// USAGE
//   M3Button { text: "Save"; variant: "tonal"; onClicked: save() }
//   M3Button { text: "Disabled"; enabled: false }
//
// CONTRACT COMPLIANCE: colours Theme.<role>Hex (qml4j parses colors from
// STRINGS; the bridge's Integer form throws), radii Theme.shape*,
// type Theme.type*, spacing Theme.space*. Only "transparent" is a literal
// colour and the engine defines it as alpha zero, not a palette colour.

Item {
    id: root

    property string text: ""
    property string variant: "filled"

    signal clicked()

    property real horizontalPadding: root.variant === "text" ? Theme.spaceM : Theme.spaceXl
    property real minHeight: 40

    implicitWidth: Math.max(label.implicitWidth + root.horizontalPadding * 2, root.minHeight * 1.5)
    implicitHeight: root.minHeight

    // Container / content roles per variant. The disabled state re-maps onto
    // surfaceContainerHighest + onSurfaceVariant (MD3's disabled 12%/38% alpha
    // expressed with opaque Theme roles instead of an alpha literal, so the
    // label stays readable).
    property string containerHex: !root.enabled ? Theme.surfaceContainerHighestHex
                                : root.variant === "filled" ? Theme.primaryHex
                                : root.variant === "tonal" ? Theme.secondaryContainerHex
                                : "transparent"
    property string contentHex: !root.enabled ? Theme.onSurfaceVariantHex
                              : root.variant === "filled" ? Theme.onPrimaryHex
                              : root.variant === "tonal" ? Theme.onSecondaryContainerHex
                              : Theme.primaryHex
    property string borderHex: !root.enabled ? Theme.outlineVariantHex : Theme.outlineHex
    property int borderWidth: (root.variant === "outlined" || !root.enabled) ? 1 : 0

    Rectangle {
        id: container
        anchors.fill: parent
        radius: Theme.shapeFull
        color: root.containerHex
        border.width: root.borderWidth
        border.color: root.borderHex
    }

    Rectangle {
        id: stateLayer
        anchors.fill: parent
        radius: Theme.shapeFull
        color: root.variant === "filled" ? Theme.onPrimaryHex : Theme.primaryHex
        opacity: !root.enabled ? 0.0
               : hit.pressed ? 0.16
               : hit.containsMouse ? 0.08
               : 0.0
        Behavior on opacity { NumberAnimation { duration: 120; easing.type: Easing.OutCubic } }
    }

    Text {
        id: label
        anchors.centerIn: parent
        text: root.text
        color: root.contentHex
        font.pixelSize: Theme.typeLabelLarge
        font.bold: root.variant !== "text"
    }

    MouseArea {
        id: hit
        anchors.fill: parent
        enabled: root.enabled
        hoverEnabled: root.enabled
        onClicked: root.clicked()
    }
}
