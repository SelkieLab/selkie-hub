import QtQuick 2.15
import "."

// TopBar - page title, context-token usage, and the host entry points
// (notifications, light/dark), with a compact overflow menu.
//
// PUBLIC API
//   property string title
//   property string subtitle
//   property real   tokenUsed        context tokens currently occupied
//   property real   tokenTotal       context window size
//   property bool   compact          narrow layout: hide the subtitle/token block
//   property bool   overflowMode     true when the shell is compact OR the bar is < 600dp
//   property bool   overflowOpen     the overflow menu is open
//   signal notificationsClicked()
//   signal themeToggleClicked()
//   property string themeGlyph       glyph shown on the theme button
//
// OVERFLOW MENU (no Controls layer: no Popup/Menu/Dialog in qml4j)
//   Below 600dp (or whenever the shell marks itself compact) the notification and theme
//   buttons fold into a single "..." button. Its menu is a plain Rectangle list drawn
//   inside this bar; the bar raises its own z while the menu is open so the menu paints
//   ABOVE the page host that the shell declares after the bar (siblings sort by z).
//   Dismissal: pick an item, press the "..." button again, or press the scrim that
//   covers the shell's centre pane while the menu is open.
//
//   OBJECTNAMES: topbar-more, topbar-overflow-menu, topbar-menu-notifications,
//   topbar-menu-theme, topbar-bell, topbar-theme
//
// CONTRACT COMPLIANCE: colours Theme.<role>Hex, radii Theme.shape*, type Theme.type*,
// spacing Theme.space*.

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

    readonly property bool overflowMode: bar.compact || bar.width < 600
    property bool overflowOpen: false

    readonly property real usage: tokenTotal <= 0 ? 0 : Math.min(1, tokenUsed / tokenTotal)
    readonly property real buttonSize: 40
    readonly property bool menuOpen: bar.overflowMode && bar.overflowOpen

    height: Theme.spaceXl * 2 + 28
    color: Theme.surfaceHex
    // Lift the whole bar (and its menu) above the page host while the menu is open.
    z: bar.menuOpen ? 50 : 0
    clip: false

    onOverflowModeChanged: bar.overflowOpen = false

    Text {
        id: titleText
        x: Theme.spaceL
        y: Theme.spaceM
        width: Math.max(0, bar.width - (bar.buttonSize * 2 + Theme.spaceXl * 2) - Theme.spaceL)
        text: bar.title
        color: Theme.onSurfaceHex
        font.pixelSize: Theme.typeTitleLarge
        font.bold: true
        elide: Text.ElideRight
    }

    Text {
        x: Theme.spaceL
        y: titleText.y + titleText.height + Theme.spaceXs
        visible: !bar.compact
        text: bar.subtitle
        color: Theme.onSurfaceVariantHex
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
            color: Theme.onSurfaceVariantHex
            font.pixelSize: Theme.typeLabelSmall
        }

        Rectangle {
            id: tokenTrack
            y: tokenLabel.height + Theme.spaceXs
            width: parent.width
            height: 6
            radius: Math.min(Theme.shapeFull, height / 2)
            color: Theme.surfaceContainerHighestHex

            Rectangle {
                width: parent.width * bar.usage
                height: parent.height
                radius: Math.min(Theme.shapeFull, height / 2)
                color: bar.usage > 0.85 ? Theme.errorHex : Theme.primaryHex
            }
        }
    }

    // ---- notifications (full layout only) ----
    Rectangle {
        id: bell
        objectName: "topbar-bell"
        visible: !bar.overflowMode
        x: bar.width - 2 * bar.buttonSize - Theme.spaceL - Theme.spaceS
        y: (bar.height - bar.buttonSize) / 2
        width: bar.buttonSize
        height: bar.buttonSize
        radius: Math.min(Theme.shapeFull, height / 2)
        color: Theme.surfaceContainerHighHex

        Text {
            anchors.centerIn: parent
            text: "N"
            color: Theme.onSurfaceVariantHex
            font.pixelSize: Theme.typeLabelLarge
            font.bold: true
        }

        Rectangle {
            x: parent.width - 12
            y: 6
            width: 8
            height: 8
            radius: Math.min(Theme.shapeFull, height / 2)
            color: Theme.errorHex
        }

        MouseArea {
            anchors.fill: parent
            onClicked: bar.notificationsClicked()
        }
    }

    // ---- theme toggle (full layout only) ----
    Rectangle {
        id: themeButton
        objectName: "topbar-theme"
        visible: !bar.overflowMode
        x: bar.width - bar.buttonSize - Theme.spaceL
        y: (bar.height - bar.buttonSize) / 2
        width: bar.buttonSize
        height: bar.buttonSize
        radius: Math.min(Theme.shapeFull, height / 2)
        color: Theme.surfaceContainerHighHex

        Text {
            anchors.centerIn: parent
            text: bar.themeGlyph
            color: Theme.onSurfaceHex
            font.pixelSize: Theme.typeLabelLarge
            font.bold: true
        }

        MouseArea {
            anchors.fill: parent
            onClicked: bar.themeToggleClicked()
        }
    }

    // ---- overflow button (compact only) ----
    Rectangle {
        id: moreButton
        objectName: "topbar-more"
        visible: bar.overflowMode
        x: bar.width - bar.buttonSize - Theme.spaceL
        y: (bar.height - bar.buttonSize) / 2
        width: bar.buttonSize
        height: bar.buttonSize
        radius: Math.min(Theme.shapeFull, height / 2)
        color: bar.menuOpen ? Theme.primaryContainerHex : Theme.surfaceContainerHighHex
        border.width: bar.activeFocus ? 2 : 0
        border.color: Theme.primaryHex

        Text {
            anchors.centerIn: parent
            text: "..."
            color: bar.menuOpen ? Theme.onPrimaryContainerHex : Theme.onSurfaceHex
            font.pixelSize: Theme.typeLabelLarge
            font.bold: true
        }

        MouseArea {
            anchors.fill: parent
            onClicked: bar.overflowOpen = !bar.overflowOpen
        }
    }

    // ---- scrim: click anywhere in the shell's centre pane to dismiss ----
    // Reparented to the shell's centre pane while open: qml4j hit-tests only inside an
    // ancestor's box, so an overlay that must be clickable below the bar cannot stay a
    // child of the 76dp bar.
    MouseArea {
        id: scrim
        objectName: "topbar-scrim"
        parent: bar.menuOpen && bar.parent ? bar.parent : bar
        visible: bar.menuOpen
        x: bar.menuOpen ? -bar.x : 0
        y: bar.menuOpen ? -bar.y : 0
        width: bar.menuOpen && bar.parent ? bar.parent.width : 0
        height: bar.menuOpen && bar.parent ? bar.parent.height : 0
        z: 40
        onClicked: bar.overflowOpen = false
    }

    // ---- the overflow menu: a plain Rectangle list, never a Popup ----
    Rectangle {
        id: overflowMenu
        objectName: "topbar-overflow-menu"
        // Same reparenting trick as the scrim (see above): the menu lives in the shell's
        // centre pane while it is open so it can extend below the bar AND stay clickable.
        parent: bar.menuOpen && bar.parent ? bar.parent : bar
        visible: bar.menuOpen
        z: 100
        x: bar.menuOpen ? bar.x + bar.width - width - Theme.spaceL : bar.width - width - Theme.spaceL
        y: bar.menuOpen ? bar.y + bar.height - Theme.spaceXs : bar.height - Theme.spaceXs
        width: 232
        height: menuColumn.height + Theme.spaceS * 2
        radius: Theme.shapeM
        color: Theme.surfaceContainerHighHex
        border.width: 1
        border.color: Theme.outlineVariantHex

        Column {
            id: menuColumn
            x: Theme.spaceS
            y: Theme.spaceS
            width: parent.width - Theme.spaceS * 2
            spacing: 0

            // Notifications
            Item {
                id: menuNotifications
                objectName: "topbar-menu-notifications"
                width: menuColumn.width
                height: 44

                Text {
                    anchors.left: parent.left
                    anchors.leftMargin: Theme.spaceM
                    anchors.verticalCenter: parent.verticalCenter
                    text: "Notifications"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeBodyMedium
                }

                Text {
                    anchors.right: parent.right
                    anchors.rightMargin: Theme.spaceM
                    anchors.verticalCenter: parent.verticalCenter
                    text: "1"
                    color: Theme.errorHex
                    font.pixelSize: Theme.typeLabelMedium
                    font.bold: true
                }

                MouseArea {
                    anchors.fill: parent
                    onClicked: {
                        bar.overflowOpen = false
                        bar.notificationsClicked()
                    }
                }
            }

            Rectangle {
                x: Theme.spaceM
                width: menuColumn.width - Theme.spaceM * 2
                height: 1
                color: Theme.outlineVariantHex
                opacity: 0.6
            }

            // Light / dark
            Item {
                id: menuTheme
                objectName: "topbar-menu-theme"
                width: menuColumn.width
                height: 44

                Text {
                    anchors.left: parent.left
                    anchors.leftMargin: Theme.spaceM
                    anchors.verticalCenter: parent.verticalCenter
                    text: "Switch theme"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeBodyMedium
                }

                Text {
                    anchors.right: parent.right
                    anchors.rightMargin: Theme.spaceM
                    anchors.verticalCenter: parent.verticalCenter
                    text: bar.themeGlyph
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeLabelMedium
                    font.bold: true
                }

                MouseArea {
                    anchors.fill: parent
                    onClicked: {
                        bar.overflowOpen = false
                        bar.themeToggleClicked()
                    }
                }
            }
        }
    }

    // bottom hairline
    Rectangle {
        y: bar.height - 1
        width: bar.width
        height: 1
        color: Theme.outlineVariantHex
        opacity: 0.6
    }
}