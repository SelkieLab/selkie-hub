import QtQuick 2.15
import ".."

// M3ColorPicker - MD3E seed-colour picker (the real re-seed entry point).
//
// PUBLIC API
//   property var  seeds        candidate seeds: [{ hex: "#RRGGBB", argb: <Long>, name: "..." }]
//                              Default: 12 MD3-ish starter seeds. The hex string is swatch
//                              paint only; `argb` is what reaches theme.setSeed(int), and it
//                              MUST come from this model (see the seed-apply note below).
//   property int  columns       intended swatch-per-row count (the Flow wraps by width; this
//                               is the geometry hint used to size the swatches)
//   property real swatchSize   swatch diameter, default 44
//   property bool autoApply     true (default): a click calls theme.setSeed(argb) itself.
//                               false: the click only emits seedSelected() and the host page
//                               decides what to do.
//   signal seedSelected(real argb)  emitted on every click, after/before the apply
//   readonly seed preview       the four tone chips read Theme.* roles, so they follow the
//                               live scheme without any local colour state.
//
// SEED-APPLY PITFALL (measured, do not "simplify" this)
//   ThemeBridge takes an `int` ARGB. A literal written inline in a handler
//   (onClicked: theme.setSeed(0xFF6750A4)) is compiled as a JS double > 2^31 and the
//   coercion to int saturates to 0x7FFFFFFF - the whole scheme turns black/olive.
//   Keeping the value in the seeds model and reading modelData.argb routes it through
//   the engine's integral-number path (Double -> Long -> intValue()), which preserves
//   the 32 bits. Never inline a 0xFF... literal in a call.
//
// TONE STRIP
//   The 48-role set exposes no raw P20/P60 tone roles, so the requested 20/40/60/80 ramp
//   maps onto the nearest DOCUMENTED tones of the primary family (no local colour math):
//     20 -> onPrimaryContainer   (P10 ink - the darkest primary step exposed)
//     40 -> primary              (P40)
//     60 -> inversePrimary       (P80 in the light scheme; P40 in dark)
//     80 -> primaryContainer     (P90)
//   The chip labels keep the requested tone numbers so the intent is readable.
//
// CONTRACT COMPLIANCE
//   role colours: Theme.<role>Hex only (the seeds' own hex strings are generator INPUT,
//   exactly like the pre-Stage-5 SettingsPage swatches, not theme roles).
//   radii Theme.shape*, type Theme.type*, spacing Theme.space*. No Controls layer.

Item {
    id: root

    property var seeds: [
        { hex: "#6750A4", argb: 0xFF6750A4, name: "Baseline purple" },
        { hex: "#4285F4", argb: 0xFF4285F4, name: "Azure" },
        { hex: "#00A3A3", argb: 0xFF00A3A3, name: "Teal" },
        { hex: "#4CAF50", argb: 0xFF4CAF50, name: "Fern" },
        { hex: "#9C27B0", argb: 0xFF9C27B0, name: "Violet" },
        { hex: "#E91E63", argb: 0xFFE91E63, name: "Rose" },
        { hex: "#FF6D00", argb: 0xFFFF6D00, name: "Ember" },
        { hex: "#FFC107", argb: 0xFFFFC107, name: "Amber" },
        { hex: "#795548", argb: 0xFF795548, name: "Clay" },
        { hex: "#607D8B", argb: 0xFF607D8B, name: "Slate" },
        { hex: "#3F51B5", argb: 0xFF3F51B5, name: "Indigo" },
        { hex: "#009688", argb: 0xFF009688, name: "Sea" }
    ]

    property int columns: 6
    property real swatchSize: 44
    property real swatchGap: Theme.spaceM
    property bool autoApply: true

    signal seedSelected(real argb)

    readonly property int seedCount: root.seeds === null ? 0 : root.seeds.length
    readonly property string currentSeedHex: Theme.seedHex

    implicitWidth: (root.columns * root.swatchSize) + ((root.columns - 1) * root.swatchGap)
    implicitHeight: contentColumn.height

    // Is this candidate the seed the bridge is currently using?
    // The comparison is on the bridge's own "#RRGGBB" so it follows a re-seed with no
    // local state - including a re-seed triggered from somewhere else.
    function isCurrent(seed) {
        if (seed === null || seed.hex === undefined) return false
        return String(seed.hex).toLowerCase() === String(root.currentSeedHex).toLowerCase()
    }

    function applySeed(seed) {
        if (seed === null || seed.argb === undefined) return
        root.seedSelected(seed.argb)
        if (root.autoApply) theme.setSeed(seed.argb)
    }

    Column {
        id: contentColumn
        width: root.width
        spacing: Theme.spaceM

        // ---- header: label + live seed readout ----
        Row {
            width: contentColumn.width
            height: headerLabel.implicitHeight

            Text {
                id: headerLabel
                text: "Seed colour"
                color: Theme.onSurfaceHex
                font.pixelSize: Theme.typeLabelLarge
                font.bold: true
            }

            Text {
                objectName: "seed-current"
                x: contentColumn.width - implicitWidth
                text: root.currentSeedHex
                color: Theme.primaryHex
                font.pixelSize: Theme.typeLabelLarge
                font.bold: true
            }
        }

        // ---- the 12 candidate seeds ----
        Flow {
            id: swatchFlow
            objectName: "seed-flow"
            width: contentColumn.width
            spacing: root.swatchGap

            Repeater {
                model: root.seeds

                delegate: Item {
                    objectName: "seed-" + index
                    width: root.swatchSize
                    height: root.swatchSize

                    // the candidate colour itself (generator INPUT, not a theme role)
                    Rectangle {
                        anchors.fill: parent
                        radius: Theme.shapeFull
                        color: modelData.hex
                        border.width: 1
                        border.color: Theme.outlineVariantHex
                    }

                    // selected: a primary ring just outside the swatch
                    Rectangle {
                        anchors.fill: parent
                        anchors.margins: -3
                        radius: Theme.shapeFull
                        color: "transparent"
                        border.width: 2
                        border.color: Theme.primaryHex
                        visible: root.isCurrent(modelData)
                    }

                    // selected: a check badge that is legible on ANY seed colour
                    // (surface disc + primary check, so it survives light and dark seeds)
                    Rectangle {
                        anchors.centerIn: parent
                        visible: root.isCurrent(modelData)
                        width: root.swatchSize * 0.53
                        height: width
                        radius: Theme.shapeFull
                        color: Theme.surfaceHex

                        // Fixed-size glyph in its own 12x12 space: inside a ShapePath the
                        // bare identifiers parent/width do not resolve to the Shape box
                        // (measured: a scaled path escaped the badge and streaked across
                        // the page), so the path uses plain geometry numbers.
                        Shape {
                            width: 12
                            height: 12
                            anchors.centerIn: parent

                            ShapePath {
                                strokeColor: Theme.primaryHex
                                strokeWidth: 2
                                fillColor: "transparent"
                                capStyle: "RoundCap"
                                joinStyle: "RoundJoin"
                                startX: 2
                                startY: 6
                                PathLine { x: 5; y: 9 }
                                PathLine { x: 10; y: 3 }
                            }
                        }
                    }

                    MouseArea {
                        anchors.fill: parent
                        onClicked: root.applySeed(modelData)
                    }
                }
            }
        }

        // ---- tonal ramp: 20 / 40 / 60 / 80, from existing roles only ----
        Row {
            id: toneRow
            objectName: "seed-tones"
            width: contentColumn.width
            spacing: Theme.spaceL

            Repeater {
                model: 4

                delegate: Column {
                    spacing: Theme.spaceXs
                    width: toneChip.width

                    Rectangle {
                        id: toneChip
                        objectName: "tone-" + index
                        width: root.swatchSize
                        height: root.swatchSize
                        radius: Theme.shapeS
                        // index 0/1/2/3 -> tone 20/40/60/80 (role mapping in the header note)
                        color: index === 0 ? Theme.onPrimaryContainerHex
                             : index === 1 ? Theme.primaryHex
                             : index === 2 ? Theme.inversePrimaryHex
                             : Theme.primaryContainerHex
                    }

                    Text {
                        text: index === 0 ? "20" : index === 1 ? "40" : index === 2 ? "60" : "80"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeLabelSmall
                    }
                }
            }

            Text {
                anchors.verticalCenter: parent.verticalCenter
                width: contentColumn.width - (toneRow.spacing + root.swatchSize) * 4
                text: "Tonal ramp of the live scheme - 48-role set exposes P10/P40/P80/P90, "
                      + "shown as the 20/40/60/80 steps."
                color: Theme.onSurfaceVariantHex
                font.pixelSize: Theme.typeBodySmall
                wrapMode: Text.WordWrap
            }
        }
    }
}