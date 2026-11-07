import QtQuick 2.15
import "."

// TopBar - page title, a context-token usage placeholder, and the two host
// entry points (notifications, light/dark).
//
// Public API
//   property string title
//   property string subtitle
//   property real   tokenUsed        context tokens currently occupied
//   property real   tokenTotal       context window size
//   property bool   compact          hide the subtitle/token label when narrow
//   signal notificationsClicked()
//   signal themeToggleClicked()
//   property string themeGlyph       glyph shown on the theme button
Rectangle {
    id: bar

    property string title: ""
    property string subtitle: ""
    property real tokenUsed: 0
    property real tokenTotal: 1
    property bool compact: false
    property string themeGlyph: "D"

    signal notificationsClicked()
    signal themeToggleClicked()

    readonly property real usage: tokenTotal <= 0 ? 0 : Math.min(1, tokenUsed / tokenTotal)
    readonly property real buttonSize: 40

    height: Theme.spaceXl * 2 + 28
    color: Theme.surface

    Text {
        id: titleText
        x: Theme.spaceL
        y: Theme.spaceM
        text: bar.title
        color: Theme.onSurface
        font.pixelSize: Theme.typeTitleLarge
        font.bold: true
    }

    Text {
        x: Theme.spaceL
        y: titleText.y + titleText.height + Theme.spaceXs
        visible: !bar.compact
        text: bar.subtitle
        color: Theme.onSurfaceVariant
        font.pixelSize: Theme.typeBodySmall
    }

    // ---- token usage ----
    Item {
        id: tokenBlock
        visible: !bar.compact
        x: bar.width - width - 2 * bar.buttonSize - Theme.spaceXl
        y: (bar.height - height) / 2
        width: 190
        height: 34

        Text {
            id: tokenLabel
            text: "Context tokens  " + Math.round(bar.usage * 100) + "%"
            color: Theme.onSurfaceVariant
            font.pixelSize: Theme.typeLabelSmall
        }

        Rectangle {
            id: tokenTrack
            y: tokenLabel.height + Theme.spaceXs
            width: parent.width
            height: 6
            radius: Math.min(Theme.shapeFull, height / 2)
            color: Theme.surfaceContainerHighest

            Rectangle {
                width: parent.width * bar.usage
                height: parent.height
                radius: Math.min(Theme.shapeFull, height / 2)
                color: bar.usage > 0.85 ? Theme.error : Theme.primary
            }
        }
    }

    // ---- notifications ----
    Rectangle {
        id: bell
        x: bar.width - 2 * bar.buttonSize - Theme.spaceL - Theme.spaceS
        y: (bar.height - bar.buttonSize) / 2
        width: bar.buttonSize
        height: bar.buttonSize
        radius: Math.min(Theme.shapeFull, height / 2)
        color: Theme.surfaceContainerHigh

        Text {
            anchors.centerIn: parent
            text: "N"
            color: Theme.onSurfaceVariant
            font.pixelSize: Theme.typeLabelLarge
            font.bold: true
        }

        // unread dot
        Rectangle {
            x: parent.width - 12
            y: 6
            width: 8
            height: 8
            radius: 4
            color: Theme.error
        }

        MouseArea {
            anchors.fill: parent
            onClicked: bar.notificationsClicked()
        }
    }

    // ---- theme toggle ----
    Rectangle {
        id: themeButton
        x: bar.width - bar.buttonSize - Theme.spaceL
        y: (bar.height - bar.buttonSize) / 2
        width: bar.buttonSize
        height: bar.buttonSize
        radius: Math.min(Theme.shapeFull, height / 2)
        color: Theme.surfaceContainerHigh

        Text {
            anchors.centerIn: parent
            text: bar.themeGlyph
            color: Theme.onSurface
            font.pixelSize: Theme.typeLabelLarge
            font.bold: true
        }

        MouseArea {
            anchors.fill: parent
            onClicked: bar.themeToggleClicked()
        }
    }

    // bottom hairline
    Rectangle {
        y: bar.height - 1
        width: bar.width
        height: 1
        color: Theme.outlineVariant
        opacity: 0.6
    }
}
