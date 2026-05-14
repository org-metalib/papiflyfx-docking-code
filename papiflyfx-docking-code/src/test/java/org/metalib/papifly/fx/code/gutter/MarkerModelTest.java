package org.metalib.papifly.fx.code.gutter;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkerModelTest {

    @Test
    void addAndRetrieveMarker() {
        MarkerModel model = new MarkerModel();
        Marker marker = new Marker(5, MarkerType.ERROR, "syntax error");
        model.addMarker(marker);

        assertTrue(model.hasMarkers(5));
        assertEquals(1, model.getMarkersForLine(5).size());
        assertEquals(marker, model.getMarkersForLine(5).get(0));
    }

    @Test
    void noMarkersInitially() {
        MarkerModel model = new MarkerModel();
        assertFalse(model.hasMarkers(0));
        assertEquals(0, model.getMarkersForLine(0).size());
        assertEquals(0, model.getAllMarkers().size());
    }

    @Test
    void removeSpecificMarker() {
        MarkerModel model = new MarkerModel();
        Marker m1 = new Marker(3, MarkerType.ERROR, "err1");
        Marker m2 = new Marker(3, MarkerType.WARNING, "warn1");
        model.addMarker(m1);
        model.addMarker(m2);
        assertEquals(2, model.getMarkersForLine(3).size());

        model.removeMarker(m1);
        assertEquals(1, model.getMarkersForLine(3).size());
        assertEquals(m2, model.getMarkersForLine(3).get(0));
    }

    @Test
    void clearLine() {
        MarkerModel model = new MarkerModel();
        model.addMarker(new Marker(1, MarkerType.ERROR));
        model.addMarker(new Marker(1, MarkerType.WARNING));
        model.addMarker(new Marker(2, MarkerType.INFO));

        model.clearLine(1);
        assertFalse(model.hasMarkers(1));
        assertTrue(model.hasMarkers(2));
    }

    @Test
    void clearAll() {
        MarkerModel model = new MarkerModel();
        model.addMarker(new Marker(0, MarkerType.BREAKPOINT));
        model.addMarker(new Marker(5, MarkerType.BOOKMARK));
        model.clearAll();

        assertEquals(0, model.getAllMarkers().size());
        assertFalse(model.hasMarkers(0));
        assertFalse(model.hasMarkers(5));
    }

    @Test
    void highestPriorityTypeReturnsLowestOrdinal() {
        MarkerModel model = new MarkerModel();
        model.addMarker(new Marker(0, MarkerType.WARNING));
        model.addMarker(new Marker(0, MarkerType.ERROR));
        model.addMarker(new Marker(0, MarkerType.INFO));

        assertEquals(MarkerType.ERROR, model.getHighestPriorityType(0));
    }

    @Test
    void highestPriorityTypeReturnsNullForEmptyLine() {
        MarkerModel model = new MarkerModel();
        assertNull(model.getHighestPriorityType(42));
    }

    @Test
    void changeListenerNotifiedOnAddAndRemove() {
        MarkerModel model = new MarkerModel();
        AtomicInteger count = new AtomicInteger(0);
        model.addChangeListener(count::incrementAndGet);

        Marker m = new Marker(0, MarkerType.ERROR);
        model.addMarker(m);
        assertEquals(1, count.get());

        model.removeMarker(m);
        assertEquals(2, count.get());
    }

    @Test
    void removingMissingMarkerDoesNotNotify() {
        MarkerModel model = new MarkerModel();
        AtomicInteger count = new AtomicInteger(0);
        model.addChangeListener(count::incrementAndGet);

        model.addMarker(new Marker(0, MarkerType.ERROR));
        assertEquals(1, count.get());

        model.removeMarker(new Marker(1, MarkerType.ERROR));
        assertEquals(1, count.get());
    }

    @Test
    void changeListenerNotifiedOnClear() {
        MarkerModel model = new MarkerModel();
        model.addMarker(new Marker(0, MarkerType.ERROR));

        AtomicInteger count = new AtomicInteger(0);
        model.addChangeListener(count::incrementAndGet);

        model.clearAll();
        assertEquals(1, count.get());
    }

    @Test
    void clearAllOnEmptyDoesNotNotify() {
        MarkerModel model = new MarkerModel();
        AtomicInteger count = new AtomicInteger(0);
        model.addChangeListener(count::incrementAndGet);

        model.clearAll();
        assertEquals(0, count.get());
    }

    @Test
    void markerRecordValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Marker(-1, MarkerType.ERROR));
        assertThrows(NullPointerException.class, () -> new Marker(0, null));
    }

    @Test
    void markerNullMessageDefaultsToEmpty() {
        Marker m = new Marker(0, MarkerType.ERROR, null);
        assertEquals("", m.message());
    }

    @Test
    void getAllMarkersReturnsAllAcrossLines() {
        MarkerModel model = new MarkerModel();
        model.addMarker(new Marker(0, MarkerType.ERROR));
        model.addMarker(new Marker(1, MarkerType.WARNING));
        model.addMarker(new Marker(2, MarkerType.INFO));

        assertEquals(3, model.getAllMarkers().size());
    }

    @Test
    void removeChangeListener() {
        MarkerModel model = new MarkerModel();
        AtomicInteger count = new AtomicInteger(0);
        MarkerModel.MarkerChangeListener listener = count::incrementAndGet;
        model.addChangeListener(listener);
        model.addMarker(new Marker(0, MarkerType.ERROR));
        assertEquals(1, count.get());

        model.removeChangeListener(listener);
        model.addMarker(new Marker(1, MarkerType.WARNING));
        assertEquals(1, count.get());
    }
}
