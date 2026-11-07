import QtQuick 2.15
import ".."

// M3TextField - MD3E outlined text field with a floating label and REAL text input.
//
// PUBLIC API
//   property string label       : the field label (floats to the top border while the
//                                 field is focused or filled)
//   property string placeholder : hint shown while text is empty
//   property string text        : the current value (two-way: typing writes it back,
//                                 a host write re-seeds the field)
//   property bool   focused     : programmatic focus request; writing true moves the
//                                 engine's focus onto the editor. READING the live state
//                                 is `hasFocus` (bound to the editor's activeFocus).
//   property bool   hasFocus    : true while the editor owns the engine focus
//   property bool   readOnly    : pass-through to the editor (no typing, still focusable)
//   signature signal clicked()  : a press anywhere in the field
//   signature signal accepted() : Enter pressed while the editor is focused
//
// INPUT CAPABILITY (measured with the engine, not assumed)
//   The editor is a stock qml4j TextInput, i.e. a TextEditable: EventDispatcher routes
//   printable characters, Backspace, Enter, Left/Right/Home/End and the clipboard to the
//   FOCUSED TextEditable only. So focus must live on the TextInput, not on this Item -
//   an Item-level activeFocusOnTab would make Tab land on the wrapper and typing would
//   be swallowed. activeFocusOnTab is therefore set on the editor and the field's ring
//   binds to editor.activeFocus.
//   CURSOR: the engine paints its own blinking caret (Painter.drawTextInput, width
//   max(1, fontSize/16) - about 1px at body size). The stage note asked for an
//   approximated 2px Rectangle bar; a hand-placed bar cannot follow real glyph advances
//   inside the string (qml4j exposes no text-metrics API to QML), so the engine caret is
//   kept - it is correctly positioned and blinks. This is the one deliberate deviation.
//   NOT AVAILABLE: selection by drag, IME/dead keys, and undo (engine has no IME path).
//
// USAGE
//   M3TextField {
//       width: 288
//       label: "Search"
//       placeholder: "Type a query"
//       text: model.query
//       onTextChanged: model.query = text      // optional two-way hook
//       onAccepted: search()
//   }
//
// CONTRACT COMPLIANCE: colours Theme.<role>Hex, radii Theme.shape*,
// type Theme.type*, spacing Theme.space*. No Controls layer (TextInput is a core type).

Item {
    id: root

    property string label: ""
    property string placeholder: ""
    property string text: ""
    property bool focused: false
    property bool readOnly: false

    signal clicked()
    signal accepted()

    // Engine focus lives on the TextInput (see the INPUT CAPABILITY note).
    readonly property bool hasFocus: input.activeFocus
    readonly property bool isFocused: root.hasFocus

    property bool floats: root.hasFocus || root.text.length > 0

    implicitWidth: 288
    implicitHeight: 56
    opacity: root.enabled ? 1.0 : 0.38

    // Programmatic focus request (used by pages that want to pre-focus a field).
    onFocusedChanged: {
        if (root.focused && !input.activeFocus) input.focus = true
    }

    Rectangle {
        id: frame
        anchors.fill: parent
        radius: Theme.shapeXs
        color: Theme.surfaceHex
        border.width: root.hasFocus ? 2 : 1
        border.color: root.hasFocus ? Theme.primaryHex : Theme.outlineHex
    }

    // Focus ring: a 2dp primary outline just outside the frame, bound to the engine's
    // activeFocus so Tab shows it; a plain click on the padding focuses via the editor
    // rather than a hard-coded focus grab.
    Rectangle {
        anchors.fill: parent
        anchors.margins: -3
        radius: Theme.shapeXs
        color: "transparent"
        border.width: 2
        border.color: Theme.primaryHex
        visible: root.hasFocus
    }

    // Notch backdrop behind the floated label so it reads as sitting ON the outline.
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
        color: root.hasFocus ? Theme.primaryHex : Theme.onSurfaceVariantHex
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

    // Click target for the padding around the editor (the editor itself is hit-tested by
    // EventDispatcher, which also places the caret). Declared BEFORE the editor so the
    // editor sits on top.
    MouseArea {
        id: paddingHit
        anchors.fill: parent
        enabled: root.enabled
        hoverEnabled: root.enabled
        onClicked: {
            input.focus = true
            root.clicked()
        }
    }

    // The real editor. Plain text only: no vertical scrolling, single line.
    TextInput {
        id: input
        objectName: "text-input"
        activeFocusOnTab: true
        readOnly: root.readOnly
        enabled: root.enabled
        anchors.left: parent.left
        anchors.right: parent.right
        anchors.leftMargin: Theme.spaceL
        anchors.rightMargin: Theme.spaceL
        anchors.verticalCenter: parent.verticalCenter
        height: Theme.spaceXl
        // TextInput paints from its flat fontSize (Painter.drawTextInput), so set both.
        fontSize: Theme.typeBodyLarge
        font.pixelSize: Theme.typeBodyLarge
        color: Theme.onSurfaceHex
        selectionColor: Theme.primaryContainerHex
        selectedTextColor: Theme.onPrimaryContainerHex
        text: root.text
        onTextChanged: {
            if (root.text !== input.text) root.text = input.text
        }
        onAccepted: root.accepted()
        onActiveFocusChanged: {
            if (input.activeFocus) root.clicked()
        }
    }
}
