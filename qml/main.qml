import QtQuick 2.15
import "."
import "components"
import "pages"

// Selkie Hub - Stage 3 shell.
//
// Three panes: NavigationRail (left) | TopBar + page (centre) | details panel (right).
// The rail collapses to icons below 600px and the details panel folds away below 1100px,
// so one tree serves both a desktop window and a narrow one.
//
// Page content lives in qml/pages/*Page.qml. The shell owns geometry (each page is given
// x/y/width/height and shown by index) and the TopBar owns the page title, so pages never
// repeat their own name as a heading.
//
// Every colour, radius, font size and spring parameter comes from the Theme singleton;
// this file only decides geometry. The window itself belongs to the host.
Rectangle {
    id: root

    width: 1200
    height: 780
    color: Theme.surface

    readonly property bool railCollapsed: width < 600
    readonly property bool panelVisible: width >= 1100
    readonly property real panelWidth: panelVisible ? 300 : 0

    readonly property var destinations: [
        { glyph: "D", label: "Dashboard" },
        { glyph: "P", label: "Plugins" },
        { glyph: "M", label: "Memory" },
        { glyph: "T", label: "Tokens" },
        { glyph: "S", label: "Settings" },
        { glyph: "O", label: "Onboarding" }
    ]

    readonly property var pageTitles: [
        "Dashboard", "Plugins", "Memory cards", "Tokens", "Settings", "Onboarding"
    ]

    readonly property var pageSubtitles: [
        "Everything at a glance",
        "Official and third-party plugins",
        "What the hub remembers",
        "Where the context budget goes",
        "Appearance and behaviour",
        "First-run setup"
    ]

    property int currentPage: 0

    // Page transition envelope: the host springs progress 0 -> 1 with defaultSpatial,
    // which overshoots slightly. Binding opacity needs a clamp, so it is applied on the
    // page host only (not on the chrome, which must stay put).
    property var pageEnvelope: motion.pageChannel()

    onCurrentPageChanged: {
        motion.pageJump(root.pageEnvelope, 0.0)
        motion.pageIn(root.pageEnvelope)
    }

    Component.onCompleted: motion.pageJump(root.pageEnvelope, 1.0)

    // ------------------------------------------------------------------
    // Keyboard activation at the shell root.
    //
    // Measured qml4j behaviour: Keys.* attached to a Repeater delegate never fires, and a
    // window focus change can clear item-level focus. Key events do reach a focused
    // ancestor, so the shell handles Enter/Space once, here, and dispatches to whatever
    // the rail last recorded as focused.
    // ------------------------------------------------------------------
    focus: true

    Keys.onReturnPressed: rail.activateFocused()
    Keys.onSpacePressed: rail.activateFocused()

    // ------------------------------------------------------------------
    // left pane
    // ------------------------------------------------------------------
    NavigationRail {
        id: rail
        x: 0
        y: 0
        height: root.height
        collapsed: root.railCollapsed
        destinations: root.destinations
        currentIndex: root.currentPage
        onDestinationSelected: root.currentPage = index
    }

    // ------------------------------------------------------------------
    // centre pane
    // ------------------------------------------------------------------
    Item {
        id: centre
        x: rail.width
        y: 0
        width: root.width - rail.width - root.panelWidth
        height: root.height

        TopBar {
            id: topBar
            x: 0
            y: 0
            width: centre.width
            compact: centre.width < 720
            title: root.pageTitles[root.currentPage]
            subtitle: root.pageSubtitles[root.currentPage]
            tokenUsed: 118400
            tokenTotal: 200000
            themeGlyph: Theme.dark ? "L" : "D"
            onThemeToggleClicked: theme.toggleDark()
            onNotificationsClicked: toast.show("No new notifications")
        }

        Item {
            id: pageHost
            x: 0
            y: topBar.height
            width: centre.width
            height: centre.height - topBar.height
            // The spring overshoots past 1.0; clamp so opacity never exceeds 1.
            opacity: Math.min(1, root.pageEnvelope.progress)

            DashboardPage {
                x: 0
                y: 0
                width: pageHost.width
                height: pageHost.height
                visible: root.currentPage === 0
            }

            PluginsPage {
                x: 0
                y: 0
                width: pageHost.width
                height: pageHost.height
                visible: root.currentPage === 1
            }

            MemoryCardsPage {
                x: 0
                y: 0
                width: pageHost.width
                height: pageHost.height
                visible: root.currentPage === 2
            }

            TokenPage {
                x: 0
                y: 0
                width: pageHost.width
                height: pageHost.height
                visible: root.currentPage === 3
            }

            SettingsPage {
                x: 0
                y: 0
                width: pageHost.width
                height: pageHost.height
                visible: root.currentPage === 4
            }

            OnboardingPage {
                x: 0
                y: 0
                width: pageHost.width
                height: pageHost.height
                visible: root.currentPage === 5
            }
        }
    }

    // ------------------------------------------------------------------
    // right pane
    // ------------------------------------------------------------------
    Rectangle {
        id: panel
        visible: root.panelVisible
        x: root.width - root.panelWidth
        y: 0
        width: root.panelWidth
        height: root.height
        color: Theme.surfaceContainerLow

        Rectangle {
            x: 0
            width: 1
            height: panel.height
            color: Theme.outlineVariant
            opacity: 0.6
        }

        Text {
            x: Theme.spaceL
            y: Theme.spaceXl
            text: "Details"
            color: Theme.onSurface
            font.pixelSize: Theme.typeTitleMedium
            font.bold: true
        }

        Text {
            x: Theme.spaceL
            y: Theme.spaceXl + 30
            width: panel.width - 2 * Theme.spaceL
            text: "Contextual panel - plugin details, memory card preview, or the token breakdown"
                  + " for the selected page."
            color: Theme.onSurfaceVariant
            font.pixelSize: Theme.typeBodySmall
            wrapMode: Text.WordWrap
        }

        Rectangle {
            x: Theme.spaceL
            y: Theme.spaceXl + 96
            width: panel.width - 2 * Theme.spaceL
            height: 1
            color: Theme.outlineVariant
        }

        Text {
            x: Theme.spaceL
            y: Theme.spaceXl + 112
            text: "Stage 3 skeleton"
            color: Theme.onSurfaceVariant
            font.pixelSize: Theme.typeLabelSmall
        }
    }

    // ------------------------------------------------------------------
    // toast overlay (no Popup layer exists in qml4j)
    // ------------------------------------------------------------------
    M3Toast {
        id: toast
        x: (root.width - width) / 2
        y: root.height - height - Theme.spaceXl
    }

    // ------------------------------------------------------------------
    // Hidden in-process probes.
    //
    // The host theme proof looks up Rectangles named "swatch-<role>" and reads their
    // resolved color after a toggle, so this verifies that a scheme flip really re-runs
    // the QML bindings - independent of window capture.
    // ------------------------------------------------------------------
    Item {
        visible: false
        width: 1
        height: 1

        Rectangle { objectName: "swatch-primary"; width: 1; height: 1; color: Theme.primary }
        Rectangle { objectName: "swatch-secondary"; width: 1; height: 1; color: Theme.secondary }
        Rectangle { objectName: "swatch-surface"; width: 1; height: 1; color: Theme.surface }
        Rectangle { objectName: "swatch-onSurface"; width: 1; height: 1; color: Theme.onSurface }
        Rectangle { objectName: "swatch-secondaryContainer"; width: 1; height: 1; color: Theme.secondaryContainer }
        Rectangle { objectName: "swatch-surfaceContainerHighest"; width: 1; height: 1; color: Theme.surfaceContainerHighest }
    }
}
