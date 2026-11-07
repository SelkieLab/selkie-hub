package selkie.host;

import io.github.timer_err.qml4j.engine.QmlEngine;
import io.github.timer_err.qml4j.render.QmlView;
import io.github.timer_err.qml4j.render.items.core.Item;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The Stage-5a focus policy, pinned headlessly (no GLFW window, no screenshots):
 *
 * <ul>
 *   <li>Tab / Shift+Tab walk the {@code activeFocusOnTab} items in order and wrap around;</li>
 *   <li>a pointer press drops the keyboard focus;</li>
 *   <li>a pointer press never <em>grants</em> focus.</li>
 * </ul>
 *
 * <p>Everything goes through the real host path: {@link InputDispatcher#injectKey} /
 * {@code injectClick} followed by {@link InputDispatcher#drain}, exactly like the frame loop.
 */
class FocusTraversalTest {

    private static final String SCENE = ""
            + "import QtQuick 2.15\n"
            + "Rectangle {\n"
            + "    width: 400; height: 300\n"
            + "    MouseArea { objectName: \"f1\"; x: 0; y: 0; width: 120; height: 30; activeFocusOnTab: true }\n"
            + "    MouseArea { objectName: \"f2\"; x: 0; y: 40; width: 120; height: 30; activeFocusOnTab: true }\n"
            + "    MouseArea { objectName: \"f3\"; x: 0; y: 80; width: 120; height: 30; activeFocusOnTab: true }\n"
            + "    MouseArea { objectName: \"plain\"; x: 0; y: 120; width: 120; height: 30; activeFocusOnTab: false }\n"
            + "}\n";

    private static QmlView newView(List<String> focusLog) {
        QmlView view = QmlView.withStockTypes(new QmlEngine());
        view.resources(path -> null);
        view.load(SCENE, "");
        // qml4j calls onFocusChanged(newFocus, oldFocus) - the NEW focus is the first argument.
        view.setFocusListener((newFocus, oldFocus) -> focusLog.add(nameOf(newFocus)));
        view.clearFocus();
        return view;
    }

    private static String nameOf(Item item) {
        return item == null ? "<none>" : item.objectName.peek();
    }

    private static String focusedName(QmlView view) {
        return nameOf(view.focused());
    }

    private static void tab(QmlView view, InputDispatcher input, boolean backwards) {
        input.injectKey(backwards ? QmlView.KEY_BACKTAB : QmlView.KEY_TAB, null, backwards);
        input.drain(view, 1f);
    }

    @Test
    void tabWalksEveryFocusableItemInOrderAndWraps() {
        List<String> log = new ArrayList<>();
        QmlView view = newView(log);
        InputDispatcher input = new InputDispatcher();

        List<String> visited = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tab(view, input, false);
            visited.add(focusedName(view));
        }

        assertEquals(List.of("f1", "f2", "f3", "f1"), visited,
                "Tab must visit the activeFocusOnTab items in order and wrap; focus log=" + log);
        assertEquals(List.of("f1", "f2", "f3", "f1"), log,
                "the focus listener must see every change exactly once, in order");
        view.dispose();
    }

    @Test
    void shiftTabWalksBackwards() {
        List<String> log = new ArrayList<>();
        QmlView view = newView(log);
        InputDispatcher input = new InputDispatcher();

        tab(view, input, false);
        tab(view, input, false);
        assertEquals("f2", focusedName(view));

        tab(view, input, true);
        assertEquals("f1", focusedName(view), "Shift+Tab must go back one item");
        view.dispose();
    }

    @Test
    void pointerPressDropsKeyboardFocusAndNeverGrantsIt() {
        List<String> log = new ArrayList<>();
        QmlView view = newView(log);
        InputDispatcher input = new InputDispatcher();

        Item second = view.findByObjectName("f2");
        assertNotNull(second);
        view.setFocus(second);
        assertNotNull(view.focused(), "precondition: f2 owns the keyboard focus");

        input.injectClick(10f, 10f);
        input.drain(view, 1f);

        assertNull(view.focused(), "a pointer press must clear the keyboard focus");
        view.dispose();
    }

    @Test
    void clickOnAFocusableItemDoesNotForceFocus() {
        List<String> log = new ArrayList<>();
        QmlView view = newView(log);
        InputDispatcher input = new InputDispatcher();

        // f1 sits at 0,0..120,30 - click right inside it.
        input.injectClick(20f, 15f);
        input.drain(view, 1f);

        assertNull(view.focused(),
                "clicking a MouseArea must not silently move the keyboard focus to it");
        view.dispose();
    }
}
