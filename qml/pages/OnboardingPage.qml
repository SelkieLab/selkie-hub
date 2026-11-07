import QtQuick 2.15
import ".."
import "../components"

// OnboardingPage - Stage 3b first-run wizard skeleton.
//
// WHAT THIS PAGE IS
//   Step 1 of 4 of the first-run flow: a step indicator, a hero card that explains
//   what the hub is and offers "Get started" / "Skip", and a three-item checklist
//   of the things the user will do next. Every control is a skeleton - the buttons
//   raise a toast, nothing navigates yet.
//
// THE STEP INDICATOR HAS NO CONTROLS
//   qml4j ships no Controls layer, no Stepper and no image assets in the QML tree,
//   so the indicator is four Rectangle circles joined by three 2px Rectangle
//   connectors. Step 1 is filled with Theme.primary; the remaining three sit on
//   Theme.surfaceContainerHighest, which is the "not reached yet" surface in both
//   schemes.
//
// THE CHECKMARK IS DRAWN, NOT AN IMAGE
//   Each checklist row has a small filled circle (Theme.primary) with a Text tick
//   glyph inside it - no PNG, no SVG, no icon font.
//
// PAGE CONTRACT (the shell owns geometry)
//   Root is Item { id: pageRoot } with no x/y/width/height; the body is a
//   Flickable so the indicator + hero + checklist survive a short window.
//
// INTERACTIVE OBJECTNAMES: onboarding-start, onboarding-skip, onboarding-checklist

Item {
    id: pageRoot

    // ------------------------------------------------------------------
    // page geometry
    // ------------------------------------------------------------------
    readonly property real pageMargin: Theme.spaceXl
    readonly property real contentWidth: Math.max(280, Math.min(680, pageRoot.width - 2 * pageRoot.pageMargin))
    readonly property real cardContentWidth: pageRoot.contentWidth - 2 * Theme.spaceL

    // step indicator metrics: 24px circle, 40px connector -> 64px pitch
    readonly property real stepSize: 24
    readonly property real stepGap: 40
    readonly property real stepPitch: pageRoot.stepSize + pageRoot.stepGap
    readonly property real indicatorWidth: 4 * pageRoot.stepSize + 3 * pageRoot.stepGap
    readonly property int totalSteps: 4

    readonly property real heroCardHeight: 268
    readonly property real checklistCardHeight: 204

    Flickable {
        id: scroller
        x: pageRoot.pageMargin
        y: pageRoot.pageMargin
        width: pageRoot.contentWidth
        height: Math.max(0, pageRoot.height - 2 * pageRoot.pageMargin)
        contentWidth: width
        contentHeight: contentColumn.height
        clip: true

        Column {
            id: contentColumn
            width: scroller.width
            spacing: Theme.spaceXl

            // ==========================================================
            // step indicator: 4 circles, 3 connectors, step 1 filled
            // ==========================================================
            Item {
                id: stepIndicator
                objectName: "onboarding-steps"
                width: contentColumn.width
                height: 56

                Rectangle {
                    id: step1
                    x: 0
                    y: 0
                    width: pageRoot.stepSize
                    height: pageRoot.stepSize
                    radius: Math.min(Theme.shapeFull, height / 2)
                    color: Theme.primaryHex

                    Text {
                        id: step1Number
                        x: (step1.width - step1Number.implicitWidth) / 2
                        y: (step1.height - step1Number.implicitHeight) / 2
                        text: "1"
                        color: Theme.onPrimaryHex
                        font.pixelSize: Theme.typeLabelSmall
                        font.bold: true
                    }
                }

                Rectangle {
                    id: step1Link
                    x: pageRoot.stepSize
                    y: pageRoot.stepSize / 2 - 1
                    width: pageRoot.stepGap
                    height: 2
                    color: Theme.outlineVariantHex
                }

                Rectangle {
                    id: step2
                    x: pageRoot.stepPitch
                    y: 0
                    width: pageRoot.stepSize
                    height: pageRoot.stepSize
                    radius: Math.min(Theme.shapeFull, height / 2)
                    color: Theme.surfaceContainerHighestHex

                    Text {
                        id: step2Number
                        x: (step2.width - step2Number.implicitWidth) / 2
                        y: (step2.height - step2Number.implicitHeight) / 2
                        text: "2"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeLabelSmall
                        font.bold: true
                    }
                }

                Rectangle {
                    id: step2Link
                    x: pageRoot.stepPitch + pageRoot.stepSize
                    y: pageRoot.stepSize / 2 - 1
                    width: pageRoot.stepGap
                    height: 2
                    color: Theme.outlineVariantHex
                }

                Rectangle {
                    id: step3
                    x: pageRoot.stepPitch * 2
                    y: 0
                    width: pageRoot.stepSize
                    height: pageRoot.stepSize
                    radius: Math.min(Theme.shapeFull, height / 2)
                    color: Theme.surfaceContainerHighestHex

                    Text {
                        id: step3Number
                        x: (step3.width - step3Number.implicitWidth) / 2
                        y: (step3.height - step3Number.implicitHeight) / 2
                        text: "3"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeLabelSmall
                        font.bold: true
                    }
                }

                Rectangle {
                    id: step3Link
                    x: pageRoot.stepPitch * 2 + pageRoot.stepSize
                    y: pageRoot.stepSize / 2 - 1
                    width: pageRoot.stepGap
                    height: 2
                    color: Theme.outlineVariantHex
                }

                Rectangle {
                    id: step4
                    x: pageRoot.stepPitch * 3
                    y: 0
                    width: pageRoot.stepSize
                    height: pageRoot.stepSize
                    radius: Math.min(Theme.shapeFull, height / 2)
                    color: Theme.surfaceContainerHighestHex

                    Text {
                        id: step4Number
                        x: (step4.width - step4Number.implicitWidth) / 2
                        y: (step4.height - step4Number.implicitHeight) / 2
                        text: "4"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeLabelSmall
                        font.bold: true
                    }
                }

                // captions, centred under their circle
                Text {
                    id: caption1
                    x: 0
                    y: 34
                    text: "Welcome"
                    color: Theme.primaryHex
                    font.pixelSize: Theme.typeLabelSmall
                    font.bold: true
                }

                Text {
                    id: caption2
                    x: pageRoot.stepPitch
                    y: 34
                    text: "Theme"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeLabelSmall
                }

                Text {
                    id: caption3
                    x: pageRoot.stepPitch * 2
                    y: 34
                    text: "Plugins"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeLabelSmall
                }

                Text {
                    id: caption4
                    x: pageRoot.stepPitch * 3
                    y: 34
                    text: "Memory"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeLabelSmall
                }

                Text {
                    id: stepCount
                    x: stepIndicator.width - stepCount.implicitWidth
                    y: 4
                    text: "Step 1 of 4"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeLabelMedium
                }
            }

            // ==========================================================
            // hero card: headline, two lines of body copy, primary + skip
            // ==========================================================
            M3Card {
                id: heroCard
                objectName: "onboarding-hero"
                width: pageRoot.contentWidth
                height: pageRoot.heroCardHeight
                variant: "elevated"

                Text {
                    id: heroHeadline
                    x: 0
                    y: 0
                    text: "Welcome to Selkie Hub"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeHeadlineSmall
                    font.bold: true
                }

                Text {
                    id: heroBody
                    x: 0
                    y: 42
                    width: Math.min(pageRoot.cardContentWidth, 470)
                    height: 46
                    text: "A local-first companion that remembers how you work. Your theme, plugins and"
                          + " memory cards stay on this machine."
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeBodyMedium
                    wrapMode: Text.WordWrap
                    clip: true
                }

                M3Button {
                    id: startButton
                    objectName: "onboarding-start"
                    x: 0
                    y: 150
                    width: Math.max(168, startButton.implicitWidth)
                    height: 48
                    text: "Get started"
                    onClicked: toast.show("Step 2 - choose a seed colour - lands in Stage 5")
                }

                M3Button {
                    id: skipButton
                    objectName: "onboarding-skip"
                    x: startButton.width + Theme.spaceS
                    y: 154
                    text: "Skip"
                    variant: "text"
                    onClicked: toast.show("First-run flow skipped")
                }

                Text {
                    id: heroHint
                    x: 0
                    y: 214
                    text: "About two minutes  ·  every choice can be changed later in Settings"
                    color: Theme.onSurfaceVariantHex
                    font.pixelSize: Theme.typeLabelMedium
                }
            }

            // ==========================================================
            // checklist card: 3 items, tick = filled circle + Text glyph
            // ==========================================================
            M3Card {
                id: checklistCard
                objectName: "onboarding-checklist"
                width: pageRoot.contentWidth
                height: pageRoot.checklistCardHeight
                variant: "filled"

                Text {
                    id: checklistHeading
                    x: 0
                    y: 0
                    text: "Before you start"
                    color: Theme.onSurfaceHex
                    font.pixelSize: Theme.typeTitleMedium
                    font.bold: true
                }

                // ---- item 1 ----
                Item {
                    x: 0
                    y: 40
                    width: pageRoot.cardContentWidth
                    height: 40

                    Rectangle {
                        id: tick1
                        x: 0
                        y: 0
                        width: 20
                        height: 20
                        radius: Math.min(Theme.shapeFull, height / 2)
                        color: Theme.primaryHex

                        Text {
                            id: tick1Glyph
                            x: (tick1.width - tick1Glyph.implicitWidth) / 2
                            y: (tick1.height - tick1Glyph.implicitHeight) / 2
                            text: "✓"
                            color: Theme.onPrimaryHex
                            font.pixelSize: Theme.typeLabelMedium
                            font.bold: true
                        }
                    }

                    Text {
                        x: 32
                        y: 0
                        text: "Choose a seed colour"
                        color: Theme.onSurfaceHex
                        font.pixelSize: Theme.typeBodyMedium
                    }

                    Text {
                        x: 32
                        y: 20
                        text: "Pick the palette every surface is derived from."
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeBodySmall
                    }
                }

                // ---- item 2 ----
                Item {
                    x: 0
                    y: 84
                    width: pageRoot.cardContentWidth
                    height: 40

                    Rectangle {
                        id: tick2
                        x: 0
                        y: 0
                        width: 20
                        height: 20
                        radius: Math.min(Theme.shapeFull, height / 2)
                        color: Theme.primaryHex

                        Text {
                            id: tick2Glyph
                            x: (tick2.width - tick2Glyph.implicitWidth) / 2
                            y: (tick2.height - tick2Glyph.implicitHeight) / 2
                            text: "✓"
                            color: Theme.onPrimaryHex
                            font.pixelSize: Theme.typeLabelMedium
                            font.bold: true
                        }
                    }

                    Text {
                        x: 32
                        y: 0
                        text: "Install a plugin"
                        color: Theme.onSurfaceHex
                        font.pixelSize: Theme.typeBodyMedium
                    }

                    Text {
                        x: 32
                        y: 20
                        text: "Start with the official workspace plugin."
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeBodySmall
                    }
                }

                // ---- item 3 ----
                Item {
                    x: 0
                    y: 128
                    width: pageRoot.cardContentWidth
                    height: 40

                    Rectangle {
                        id: tick3
                        x: 0
                        y: 0
                        width: 20
                        height: 20
                        radius: Math.min(Theme.shapeFull, height / 2)
                        color: Theme.primaryHex

                        Text {
                            id: tick3Glyph
                            x: (tick3.width - tick3Glyph.implicitWidth) / 2
                            y: (tick3.height - tick3Glyph.implicitHeight) / 2
                            text: "✓"
                            color: Theme.onPrimaryHex
                            font.pixelSize: Theme.typeLabelMedium
                            font.bold: true
                        }
                    }

                    Text {
                        x: 32
                        y: 0
                        text: "Load your first memory card"
                        color: Theme.onSurfaceHex
                        font.pixelSize: Theme.typeBodyMedium
                    }

                    Text {
                        x: 32
                        y: 20
                        text: "One card is enough to see recall work end to end."
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeBodySmall
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // toast overlay (no Popup layer exists in qml4j)
    // ------------------------------------------------------------------
    M3Toast {
        id: toast
        objectName: "onboarding-toast"
        x: 0
        y: 0
        width: pageRoot.width
        height: pageRoot.height
    }
}
