import QtQuick 2.15
import "."
import "components"

// Selkie Hub - Stage 2 shell.
//
// Three-pane layout: NavigationRail (left) | page area with TopBar (centre) | detail
// panel (right). The rail collapses to icons below 600px and the detail panel folds
// away below 1000px, so the same tree serves mouse and narrow windows.
//
// Every colour, radius, font size and spring parameter comes from the Theme singleton;
// this file only decides geometry. Nothing here creates a window - the host owns the
// GLFW window, the Skija surface and the render loop.
Rectangle {
    id: root

    width: 1200
    height: 780
    color: Theme.surface

    // ---- responsive thresholds (MD3E window size classes) ----
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
            id: pages
            x: 0
            y: topBar.height
            width: centre.width
            height: centre.height - topBar.height

            // ---------------- page 0: component demo (Stage 2 acceptance) ----------------
            Item {
                id: dashboardPage
                x: 0
                y: 0
                width: pages.width
                height: pages.height
                visible: root.currentPage === 0

                M3Card {
                    id: componentsCard
                    x: Theme.spaceXl
                    y: Theme.spaceXl
                    width: Math.min(720, pages.width - 2 * Theme.spaceXl)
                    height: 330
                    variant: "elevated"

                    Text {
                        x: 0
                        y: 0
                        text: "MD3E components"
                        color: Theme.onSurface
                        font.pixelSize: Theme.typeTitleMedium
                        font.bold: true
                    }

                    Text {
                        x: 0
                        y: 26
                        width: componentsCard.width - 2 * componentsCard.padding
                        text: "Live children of the theme: flip light/dark in the top bar and every"
                              + " surface below repaints without reloading."
                        color: Theme.onSurfaceVariant
                        font.pixelSize: Theme.typeBodySmall
                        wrapMode: Text.WordWrap
                    }

                    // switch row
                    Text {
                        x: 0
                        y: 72
                        text: "Spring switch"
                        color: Theme.onSurface
                        font.pixelSize: Theme.typeLabelLarge
                    }

                    M3Switch {
                        id: demoSwitch
                        x: 150
                        y: 66
                        checked: true
                        onToggled: toast.show("Switch is now " + (checked ? "on" : "off"))
                    }

                    // tabs row
                    M3Tabs {
                        id: demoTabs
                        x: 0
                        y: 128
                        width: Math.min(420, componentsCard.width - 2 * componentsCard.padding)
                        labels: ["Overview", "Activity", "Storage"]
                        currentIndex: 0
                        onTabSelected: toast.show("Tab " + index + " selected")
                    }

                    // button row: a Row positioner flows them, so no fixed x can clip
                    Row {
                        x: 0
                        y: 208
                        spacing: Theme.spaceS

                        M3Button {
                            id: btnFilled
                            text: "Filled"
                            onClicked: toast.show("Filled pressed")
                        }

                        M3Button {
                            text: "Tonal"
                            variant: "tonal"
                            onClicked: toast.show("Tonal pressed")
                        }

                        M3Button {
                            text: "Outlined"
                            variant: "outlined"
                            onClicked: toast.show("Outlined pressed")
                        }

                        M3Button {
                            text: "Text"
                            variant: "text"
                            onClicked: toast.show("Text pressed")
                        }
                    }
                }

                M3Card {
                    id: formCard
                    x: Theme.spaceXl
                    y: componentsCard.y + componentsCard.height + Theme.spaceL
                    width: componentsCard.width
                    height: 220
                    variant: "filled"

                    Text {
                        x: 0
                        y: 0
                        text: "Toast + text field"
                        color: Theme.onSurface
                        font.pixelSize: Theme.typeTitleSmall
                        font.bold: true
                    }

                    M3TextField {
                        id: demoField
                        x: 0
                        y: 36
                        width: Math.min(340, formCard.width - 2 * formCard.padding)
                        label: "Display name"
                        placeholder: "Selkie"
                    }

                    M3Button {
                        id: btnToast
                        x: 0
                        y: 140
                        text: "Show toast"
                        variant: "tonal"
                        onClicked: toast.show("Saved " + (demoField.text.length > 0 ? demoField.text : "settings"))
                    }
                }
            }

            // ---------------- pages 1..5: skeletons ----------------
            Item {
                x: 0
                y: 0
                width: pages.width
                height: pages.height
                visible: root.currentPage === 1

                M3Card {
                    x: Theme.spaceXl
                    y: Theme.spaceXl
                    width: Math.min(560, pages.width - 2 * Theme.spaceXl)
                    height: 180
                    variant: "outlined"

                    Text {
                        x: 0
                        y: 0
                        text: "Plugins"
                        color: Theme.onSurface
                        font.pixelSize: Theme.typeTitleMedium
                        font.bold: true
                    }

                    Text {
                        x: 0
                        y: 34
                        width: parent.width
                        text: "Official and third-party plugin health lands here in Stage 4, fed by"
                              + " PluginBridge. This is the Stage 2 skeleton."
                        color: Theme.onSurfaceVariant
                        font.pixelSize: Theme.typeBodyMedium
                        wrapMode: Text.WordWrap
                    }
                }
            }

            Item {
                x: 0
                y: 0
                width: pages.width
                height: pages.height
                visible: root.currentPage === 2

                M3Card {
                    x: Theme.spaceXl
                    y: Theme.spaceXl
                    width: Math.min(560, pages.width - 2 * Theme.spaceXl)
                    height: 180

                    Text {
                        x: 0
                        y: 0
                        text: "Memory cards"
                        color: Theme.onSurface
                        font.pixelSize: Theme.typeTitleMedium
                        font.bold: true
                    }

                    Text {
                        x: 0
                        y: 34
                        width: parent.width
                        text: "Card grid placeholder - the recall surface arrives in Stage 3/4."
                        color: Theme.onSurfaceVariant
                        font.pixelSize: Theme.typeBodyMedium
                        wrapMode: Text.WordWrap
                    }
                }
            }

            Item {
                x: 0
                y: 0
                width: pages.width
                height: pages.height
                visible: root.currentPage === 3

                M3Card {
                    x: Theme.spaceXl
                    y: Theme.spaceXl
                    width: Math.min(560, pages.width - 2 * Theme.spaceXl)
                    height: 180

                    Text {
                        x: 0
                        y: 0
                        text: "Tokens"
                        color: Theme.onSurface
                        font.pixelSize: Theme.typeTitleMedium
                        font.bold: true
                    }

                    Text {
                        x: 0
                        y: 34
                        width: parent.width
                        text: "Persona injection, memory recall, tool schema and history will be broken"
                              + " out here from TokenBridge in Stage 4."
                        color: Theme.onSurfaceVariant
                        font.pixelSize: Theme.typeBodyMedium
                        wrapMode: Text.WordWrap
                    }
                }
            }

            Item {
                x: 0
                y: 0
                width: pages.width
                height: pages.height
                visible: root.currentPage === 4

                M3Card {
                    x: Theme.spaceXl
                    y: Theme.spaceXl
                    width: Math.min(560, pages.width - 2 * Theme.spaceXl)
                    height: 200

                    Text {
                        x: 0
                        y: 0
                        text: "Settings"
                        color: Theme.onSurface
                        font.pixelSize: Theme.typeTitleMedium
                        font.bold: true
                    }

                    Text {
                        x: 0
                        y: 34
                        text: "Dark mode"
                        color: Theme.onSurface
                        font.pixelSize: Theme.typeLabelLarge
                    }

                    M3Switch {
                        x: 0
                        y: 66
                        checked: Theme.dark
                        onToggled: theme.setDark(checked)
                    }

                    Text {
                        x: 0
                        y: 132
                        text: "Seed colour picker and UI scale arrive in Stage 5."
                        color: Theme.onSurfaceVariant
                        font.pixelSize: Theme.typeBodySmall
                    }
                }
            }

            Item {
                x: 0
                y: 0
                width: pages.width
                height: pages.height
                visible: root.currentPage === 5

                M3Card {
                    x: Theme.spaceXl
                    y: Theme.spaceXl
                    width: Math.min(560, pages.width - 2 * Theme.spaceXl)
                    height: 180
                    variant: "elevated"

                    Text {
                        x: 0
                        y: 0
                        text: "Onboarding"
                        color: Theme.onSurface
                        font.pixelSize: Theme.typeTitleMedium
                        font.bold: true
                    }

                    Text {
                        x: 0
                        y: 34
                        width: parent.width
                        text: "First-run wizard placeholder."
                        color: Theme.onSurfaceVariant
                        font.pixelSize: Theme.typeBodyMedium
                        wrapMode: Text.WordWrap
                    }
                }
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
            text: "Contextual panel - plugin details, memory card preview, or token breakdown"
                  + " depending on the selected page."
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
            text: "Stage 2 skeleton"
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
    // The host theme proof looks up Rectangles named "swatch-<role>" and reads
    // their resolved color after a toggle, so this verifies that a scheme flip
    // really re-runs the QML bindings - independent of window capture.
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
