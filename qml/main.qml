import QtQuick 2.15
import QtQuick.Controls 2.15

Rectangle {
    id: root
    width: 960
    height: 640
    color: "#1b1a20"

    Rectangle {
        id: card
        x: 48
        y: 48
        width: root.width - 96
        height: root.height - 96
        radius: 28
        color: "#2b2932"

        Rectangle {
            id: accent
            x: 40
            y: 40
            width: 120
            height: 120
            radius: 24
            color: "#8f7bff"
        }

        Text {
            x: 40
            y: 200
            text: "Selkie Hub"
            color: "#f4f1ff"
            font.pixelSize: 56
            font.bold: true
        }

        Text {
            x: 40
            y: 280
            text: "Stage 0 placeholder rendered by qml4j + Skija on GLFW"
            color: "#b9b3c9"
            font.pixelSize: 24
        }

        Text {
            x: 40
            y: 340
            text: "qml/main.qml - loaded from the classpath"
            color: "#7f7894"
            font.pixelSize: 20
        }
    }
}
