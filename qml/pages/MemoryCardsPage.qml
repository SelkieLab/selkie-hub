import QtQuick 2.15
import ".."
import "../components"

// MemoryCardsPage - Stage 3b memory card manager skeleton.
//
// WHAT THIS PAGE IS
//   The recall surface: a search field, a "New card" action and a grid of memory
//   cards. Each card carries a tag chip, a two-line excerpt and a footer with the
//   last-recall date and the recall count. Stage 4 swaps the hardcoded placeholder
//   rows below for MemoryBridge rows; the layout contract does not change.
//
// THE GRID HAS NO Grid POSITIONER
//   qml4j ships no Grid positioner (docs/qml4j-constraints.md section 5), so the
//   grid is a Flow of fixed-width M3Cards. The column count is DERIVED from
//   pageRoot.width rather than hardcoded: three columns once the grid is at least
//   700px wide, two below that - so the cards stay near 220-240px wide in both
//   window size classes instead of stretching into flat ribbons.
//
// PAGE CONTRACT (the shell owns geometry)
//   Root is Item { id: pageRoot } with NO x/y/width/height. Content is laid out
//   from Theme.spaceXl margins, widths bind to pageRoot.width, and the whole body
//   sits in a Flickable because six cards plus the header can exceed a 720px-tall
//   window.
//
// STYLING
//   Theme.<role>Hex strings only (qml4j resolves a color from a STRING; the
//   Integer form throws), radii from Theme.shape*, sizes from Theme.type*, rhythm
//   from Theme.space*. The only literal color is "transparent", which the engine
//   documents as alpha zero rather than a palette colour.
//
// INTERACTIVE OBJECTNAMES (for scripted host input via --click-item=)
//   memory-search, memory-new-card, memory-card-1 ... memory-card-6, memory-toast

Item {
    id: pageRoot

    // ------------------------------------------------------------------
    // placeholder content - replaced by MemoryBridge in Stage 4
    // ------------------------------------------------------------------
    readonly property int cardCount: 6

    // ------------------------------------------------------------------
    // responsive grid maths (no Grid positioner exists in qml4j)
    // ------------------------------------------------------------------
    readonly property real pageMargin: Theme.spaceXl
    readonly property real gridWidth: Math.max(240, pageRoot.width - 2 * pageRoot.pageMargin)
    readonly property int columns: pageRoot.gridWidth >= 700 ? 3 : 2
    readonly property real cardGap: Theme.spaceL
    readonly property real cardWidth: Math.floor((pageRoot.gridWidth - (pageRoot.columns - 1) * pageRoot.cardGap) / pageRoot.columns)

    // Shared card metrics: the excerpt is clipped to exactly two typeBodySmall
    // lines, the footer sits on the card's bottom padding edge.
    readonly property real cardHeight: 140
    readonly property real chipHeight: 22
    readonly property real excerptTop: 32
    readonly property real excerptHeight: 36
    readonly property real footerTop: 140 - 2 * Theme.spaceL - 14

    Flickable {
        id: scroller
        x: pageRoot.pageMargin
        y: pageRoot.pageMargin
        width: pageRoot.gridWidth
        height: Math.max(0, pageRoot.height - 2 * pageRoot.pageMargin)
        contentWidth: width
        contentHeight: contentColumn.height
        clip: true

        Column {
            id: contentColumn
            width: scroller.width
            spacing: Theme.spaceXl

            // ----------------------------------------------------------
            // header row: search + primary action
            // ----------------------------------------------------------
            Item {
                id: headerRow
                width: contentColumn.width
                height: 56

                M3TextField {
                    id: searchField
                    objectName: "memory-search"
                    x: 0
                    y: 0
                    width: Math.max(160, Math.min(380, headerRow.width - 160))
                    height: 56
                    label: "Search cards"
                    placeholder: "Tag, wording or date"
                    onClicked: toast.show("Search runs on MemoryBridge in Stage 4")
                }

                M3Button {
                    id: newCardButton
                    objectName: "memory-new-card"
                    anchors.right: headerRow.right
                    anchors.verticalCenter: headerRow.verticalCenter
                    text: "New card"
                    onClicked: toast.show("New card editor arrives with MemoryBridge")
                }
            }

            // ----------------------------------------------------------
            // count line - keeps the grid honest about what it shows
            // ----------------------------------------------------------
            Text {
                id: countLine
                width: contentColumn.width
                text: "6 cards  ·  22 recalls  ·  newest 12 Oct 2026"
                color: Theme.onSurfaceVariantHex
                font.pixelSize: Theme.typeLabelMedium
            }

            // ----------------------------------------------------------
            // the card grid: a Flow of fixed-width cards
            // ----------------------------------------------------------
            Flow {
                id: cardGrid
                width: contentColumn.width
                spacing: pageRoot.cardGap

                // ---- card 1 ----
                M3Card {
                    objectName: "memory-card-1"
                    width: pageRoot.cardWidth
                    height: pageRoot.cardHeight
                    variant: "outlined"
                    interactive: true
                    onClicked: toast.show("Opened card: Prefers TypeScript over JavaScript")

                    Rectangle {
                        id: chip1
                        x: 0
                        y: 0
                        width: chipLabel1.implicitWidth + 2 * Theme.spaceM
                        height: pageRoot.chipHeight
                        radius: Math.min(Theme.shapeFull, height / 2)
                        color: Theme.secondaryContainerHex

                        Text {
                            id: chipLabel1
                            x: Theme.spaceM
                            anchors.verticalCenter: parent.verticalCenter
                            text: "Preferences"
                            color: Theme.onSecondaryContainerHex
                            font.pixelSize: Theme.typeLabelSmall
                        }
                    }

                    Text {
                        x: 0
                        y: pageRoot.excerptTop
                        width: parent.width
                        height: pageRoot.excerptHeight
                        text: "Prefers TypeScript over JavaScript on every project."
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeBodySmall
                        wrapMode: Text.WordWrap
                        clip: true
                    }

                    Text {
                        x: 0
                        y: pageRoot.footerTop
                        text: "12 Oct 2026  ·  recalled 3x"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeLabelSmall
                    }
                }

                // ---- card 2 ----
                M3Card {
                    objectName: "memory-card-2"
                    width: pageRoot.cardWidth
                    height: pageRoot.cardHeight
                    variant: "outlined"
                    interactive: true
                    onClicked: toast.show("Opened card: Works on weekdays only")

                    Rectangle {
                        id: chip2
                        x: 0
                        y: 0
                        width: chipLabel2.implicitWidth + 2 * Theme.spaceM
                        height: pageRoot.chipHeight
                        radius: Math.min(Theme.shapeFull, height / 2)
                        color: Theme.secondaryContainerHex

                        Text {
                            id: chipLabel2
                            x: Theme.spaceM
                            anchors.verticalCenter: parent.verticalCenter
                            text: "Schedule"
                            color: Theme.onSecondaryContainerHex
                            font.pixelSize: Theme.typeLabelSmall
                        }
                    }

                    Text {
                        x: 0
                        y: pageRoot.excerptTop
                        width: parent.width
                        height: pageRoot.excerptHeight
                        text: "Works on Selkie Hub on weekdays, 09:00 to 18:00 CET."
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeBodySmall
                        wrapMode: Text.WordWrap
                        clip: true
                    }

                    Text {
                        x: 0
                        y: pageRoot.footerTop
                        text: "09 Oct 2026  ·  recalled 7x"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeLabelSmall
                    }
                }

                // ---- card 3 ----
                M3Card {
                    objectName: "memory-card-3"
                    width: pageRoot.cardWidth
                    height: pageRoot.cardHeight
                    variant: "outlined"
                    interactive: true
                    onClicked: toast.show("Opened card: Dislikes long meetings")

                    Rectangle {
                        id: chip3
                        x: 0
                        y: 0
                        width: chipLabel3.implicitWidth + 2 * Theme.spaceM
                        height: pageRoot.chipHeight
                        radius: Math.min(Theme.shapeFull, height / 2)
                        color: Theme.secondaryContainerHex

                        Text {
                            id: chipLabel3
                            x: Theme.spaceM
                            anchors.verticalCenter: parent.verticalCenter
                            text: "Working style"
                            color: Theme.onSecondaryContainerHex
                            font.pixelSize: Theme.typeLabelSmall
                        }
                    }

                    Text {
                        x: 0
                        y: pageRoot.excerptTop
                        width: parent.width
                        height: pageRoot.excerptHeight
                        text: "Dislikes long meetings - prefers a one-page brief."
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeBodySmall
                        wrapMode: Text.WordWrap
                        clip: true
                    }

                    Text {
                        x: 0
                        y: pageRoot.footerTop
                        text: "04 Oct 2026  ·  recalled 2x"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeLabelSmall
                    }
                }

                // ---- card 4 ----
                M3Card {
                    objectName: "memory-card-4"
                    width: pageRoot.cardWidth
                    height: pageRoot.cardHeight
                    variant: "outlined"
                    interactive: true
                    onClicked: toast.show("Opened card: Builds with Gradle and JDK 21")

                    Rectangle {
                        id: chip4
                        x: 0
                        y: 0
                        width: chipLabel4.implicitWidth + 2 * Theme.spaceM
                        height: pageRoot.chipHeight
                        radius: Math.min(Theme.shapeFull, height / 2)
                        color: Theme.secondaryContainerHex

                        Text {
                            id: chipLabel4
                            x: Theme.spaceM
                            anchors.verticalCenter: parent.verticalCenter
                            text: "Tooling"
                            color: Theme.onSecondaryContainerHex
                            font.pixelSize: Theme.typeLabelSmall
                        }
                    }

                    Text {
                        x: 0
                        y: pageRoot.excerptTop
                        width: parent.width
                        height: pageRoot.excerptHeight
                        text: "Builds with Gradle and JDK 21; reviews diffs in VS Code."
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeBodySmall
                        wrapMode: Text.WordWrap
                        clip: true
                    }

                    Text {
                        x: 0
                        y: pageRoot.footerTop
                        text: "28 Sep 2026  ·  recalled 5x"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeLabelSmall
                    }
                }

                // ---- card 5 ----
                M3Card {
                    objectName: "memory-card-5"
                    width: pageRoot.cardWidth
                    height: pageRoot.cardHeight
                    variant: "outlined"
                    interactive: true
                    onClicked: toast.show("Opened card: Wants a 16-24px rhythm")

                    Rectangle {
                        id: chip5
                        x: 0
                        y: 0
                        width: chipLabel5.implicitWidth + 2 * Theme.spaceM
                        height: pageRoot.chipHeight
                        radius: Math.min(Theme.shapeFull, height / 2)
                        color: Theme.secondaryContainerHex

                        Text {
                            id: chipLabel5
                            x: Theme.spaceM
                            anchors.verticalCenter: parent.verticalCenter
                            text: "Design"
                            color: Theme.onSecondaryContainerHex
                            font.pixelSize: Theme.typeLabelSmall
                        }
                    }

                    Text {
                        x: 0
                        y: pageRoot.excerptTop
                        width: parent.width
                        height: pageRoot.excerptHeight
                        text: "Wants 16 to 24 pixel vertical rhythm on every page."
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeBodySmall
                        wrapMode: Text.WordWrap
                        clip: true
                    }

                    Text {
                        x: 0
                        y: pageRoot.footerTop
                        text: "21 Sep 2026  ·  recalled 1x"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeLabelSmall
                    }
                }

                // ---- card 6 ----
                M3Card {
                    objectName: "memory-card-6"
                    width: pageRoot.cardWidth
                    height: pageRoot.cardHeight
                    variant: "outlined"
                    interactive: true
                    onClicked: toast.show("Opened card: Keep token budgets private")

                    Rectangle {
                        id: chip6
                        x: 0
                        y: 0
                        width: chipLabel6.implicitWidth + 2 * Theme.spaceM
                        height: pageRoot.chipHeight
                        radius: Math.min(Theme.shapeFull, height / 2)
                        color: Theme.secondaryContainerHex

                        Text {
                            id: chipLabel6
                            x: Theme.spaceM
                            anchors.verticalCenter: parent.verticalCenter
                            text: "Boundaries"
                            color: Theme.onSecondaryContainerHex
                            font.pixelSize: Theme.typeLabelSmall
                        }
                    }

                    Text {
                        x: 0
                        y: pageRoot.excerptTop
                        width: parent.width
                        height: pageRoot.excerptHeight
                        text: "Never share internal token budgets outside the hub."
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeBodySmall
                        wrapMode: Text.WordWrap
                        clip: true
                    }

                    Text {
                        x: 0
                        y: pageRoot.footerTop
                        text: "16 Sep 2026  ·  recalled 4x"
                        color: Theme.onSurfaceVariantHex
                        font.pixelSize: Theme.typeLabelSmall
                    }
                }
            }

            // ----------------------------------------------------------
            // empty-state hint: the grid is hardcoded for now, say so
            // ----------------------------------------------------------
            Text {
                id: footerNote
                width: contentColumn.width
                text: "Placeholder rows from the Stage 3b skeleton - MemoryBridge supplies real"
                      + " recall counts and excerpts in Stage 4."
                color: Theme.onSurfaceVariantHex
                font.pixelSize: Theme.typeBodySmall
                wrapMode: Text.WordWrap
            }
        }
    }

    // ------------------------------------------------------------------
    // toast overlay (no Popup layer exists in qml4j) - sits over the page,
    // never captures clicks, so the cards below stay pressable.
    // ------------------------------------------------------------------
    M3Toast {
        id: toast
        objectName: "memory-toast"
        x: 0
        y: 0
        width: pageRoot.width
        height: pageRoot.height
    }
}
