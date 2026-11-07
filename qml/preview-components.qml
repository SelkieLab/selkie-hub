import QtQuick 2.15
import "."
import "components"

// preview-components.qml - the Stage 2 self-check page for the MD3E component
// library. All six components appear on ONE screen:
//
//   M3Card x3 (elevated / filled / outlined)  - the elevated card carries an
//                                               M3Button + M3Switch, the filled
//                                               card carries the remaining button
//                                               variants, the outlined card an
//                                               M3TextField + disabled button
//   M3Tabs                                     - one row of primary tabs
//   M3TextField x2                             - a second row, filled + empty
//   M3Toast                                    - overlay, shown on load
//
// Everything visible here is a binding onto the Theme singleton, so a host-side
// scheme flip repaints the whole page without touching this file.
//
// Run it with:  gradlew run --args="--qml=preview-components.qml --quit-after=20000"
//
// Interactive objectNames (for scripted host input):
//   switch-main, btn-filled, btn-tonal, btn-outlined, btn-text, btn-disabled,
//   tab-0/1/2 (repeater delegates carry index in their caption), field-name,
//   preview-status, preview-toast

Rectangle {
    id: page
    objectName: "preview-root"

    width: 1000
    height: 720
    color: Theme.backgroundHex

    // Live status line: every component signal writes here, so a screenshot
    // proves the signal actually crossed the component boundary.
    property string status: "Interact: tabs, buttons, switch, text field"

    // State lives HERE, not in a literal bound to a component property: qml4j may
    // re-apply an instance's literal initialiser, so a switch/tab that is also
    // user-mutable must be driven from page state (and kept in sync by its signal).
    property bool switchMainOn: true
    property bool switchSecondOn: false
    property int selectedTab: 0

    Column {
        id: pageColumn
        anchors.left: parent.left
        anchors.right: parent.right
        anchors.top: parent.top
        anchors.leftMargin: Theme.spaceXxl
        anchors.rightMargin: Theme.spaceXxl
        anchors.topMargin: Theme.spaceXl
        spacing: Theme.spaceXl

        Column {
            spacing: Theme.spaceXs

            Text {
                objectName: "preview-title"
                text: "MD3E component library"
                color: Theme.onBackgroundHex
                font.pixelSize: Theme.typeHeadlineSmall
                font.bold: true
            }

            Text {
                objectName: "preview-subtitle"
                text: "Stage 2 self-check - card, button, switch, text field, tabs, toast"
                      + "   |   seed " + Theme.seedHex
                      + "   mode " + (Theme.dark ? "dark" : "light")
                color: Theme.onSurfaceVariantHex
                font.pixelSize: Theme.typeBodyMedium
            }
        }

        // ---- tabs: the indicator is spring-driven by the host channel ----
        M3Tabs {
            id: tabs
            objectName: "preview-tabs"
            width: 620
            labels: ["Overview", "Activity", "Settings"]
            currentIndex: page.selectedTab
            onTabSelected: {
                page.selectedTab = index
                page.status = "M3Tabs.tabSelected -> " + index
            }
        }

        // ---- the six components: three cards side by side ----
        Row {
            spacing: Theme.spaceXl

            // 1) elevated card: M3Button + M3Switch INSIDE a card
            M3Card {
                objectName: "card-elevated"
                width: 288
                variant: "elevated"
                interactive: true
                onClicked: page.status = "M3Card.clicked (elevated)"

                Column {
                    width: parent.width
                    spacing: Theme.spaceM

                    Text {
                        text: "Elevated card"
                        color: Theme.onSurfaceHex
                        font.pixelSize: Theme.typeTitleMedium
                    }

                    Text {
                        text: "surfaceContainerLow + scrim lift"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeBodySmall
                    }

                    M3Button {
                        objectName: "btn-filled"
                        width: parent.width
                        text: "Filled action"
                        variant: "filled"
                        onClicked: page.status = "M3Button.clicked (filled)"
                    }

                    Row {
                        spacing: Theme.spaceM

                        M3Switch {
                            objectName: "switch-main"
                            checked: page.switchMainOn
                            onToggled: {
                                page.switchMainOn = checked
                                page.status = "M3Switch.toggled -> " + (checked ? "on" : "off")
                            }
                        }

                        Text {
                            anchors.verticalCenter: parent.verticalCenter
                            text: "Spring thumb"
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodyMedium
                        }
                    }
                }
            }

            // 2) filled card: the remaining button variants
            M3Card {
                objectName: "card-filled"
                width: 288
                variant: "filled"

                Column {
                    width: parent.width
                    spacing: Theme.spaceM

                    Text {
                        text: "Filled card"
                        color: Theme.onSurfaceHex
                        font.pixelSize: Theme.typeTitleMedium
                    }

                    Text {
                        text: "surfaceContainerHighest"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeBodySmall
                    }

                    M3Button {
                        objectName: "btn-tonal"
                        width: parent.width
                        text: "Tonal action"
                        variant: "tonal"
                        onClicked: page.status = "M3Button.clicked (tonal)"
                    }

                    M3Button {
                        objectName: "btn-outlined"
                        width: parent.width
                        text: "Outlined action"
                        variant: "outlined"
                        onClicked: page.status = "M3Button.clicked (outlined)"
                    }

                    Row {
                        spacing: Theme.spaceM

                        M3Switch {
                            objectName: "switch-second"
                            checked: page.switchSecondOn
                            onToggled: {
                                page.switchSecondOn = checked
                                page.status = "M3Switch.toggled -> " + (checked ? "on" : "off")
                            }
                        }

                        Text {
                            anchors.verticalCenter: parent.verticalCenter
                            text: "Second switch"
                            color: Theme.onSurfaceVariantHex
                            font.pixelSize: Theme.typeBodyMedium
                        }
                    }
                }
            }

            // 3) outlined card: text field + text/disabled buttons
            M3Card {
                objectName: "card-outlined"
                width: 288
                variant: "outlined"

                Column {
                    width: parent.width
                    spacing: Theme.spaceM

                    Text {
                        text: "Outlined card"
                        color: Theme.onSurfaceHex
                        font.pixelSize: Theme.typeTitleMedium
                    }

                    Text {
                        text: "surface + outlineVariant border"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeBodySmall
                    }

                    M3TextField {
                        objectName: "field-name"
                        width: parent.width
                        label: "Project name"
                        placeholder: "Untitled"
                        text: ""
                        onClicked: page.status = "M3TextField.clicked (focus ring on)"
                    }

                    M3Button {
                        objectName: "btn-text"
                        width: parent.width
                        text: "Text action"
                        variant: "text"
                        onClicked: page.status = "M3Button.clicked (text)"
                    }

                    M3Button {
                        objectName: "btn-disabled"
                        width: parent.width
                        text: "Disabled"
                        variant: "filled"
                        enabled: false
                    }
                }
            }
        }

        // ---- text fields: one filled, one empty ----
        Row {
            spacing: Theme.spaceXl

            M3TextField {
                objectName: "field-filled"
                width: 288
                label: "Search"
                placeholder: "Type a query"
                text: "selkie"
                focused: false
            }

            M3TextField {
                objectName: "field-focused"
                width: 288
                label: "Focused"
                placeholder: "Click me"
                text: ""
                focused: true
            }
        }

        Text {
            objectName: "preview-status"
            text: page.status
            color: Theme.primaryHex
            font.pixelSize: Theme.typeBodyMedium
            font.bold: true
        }
    }

    // ---- toast overlay: an overlay Rectangle, never a Popup (contract D) ----
    M3Toast {
        id: toast
        objectName: "preview-toast"
        anchors.fill: parent
        duration: 15000
        onDismissed: page.status = "M3Toast.dismissed"
    }

    // Shown once the first frames are up, so the acceptance screenshot captures it.
    Timer {
        interval: 400
        running: true
        repeat: false
        onTriggered: toast.show("Saved to Selkie Hub")
    }
}