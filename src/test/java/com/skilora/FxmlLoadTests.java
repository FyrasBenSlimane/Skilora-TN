package com.skilora;

import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that catch FXML load errors (e.g. missing getters for "text" / "variant" on TLBadge).
 * - Bean contract: TLBadge must have getText/setText and getVariant/setVariant so FXML can set them.
 * - Actual load: optionally loads a minimal FXML containing TLBadge to verify FXMLLoader path.
 */
@DisplayName("FXML Load & Bean Contract Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FxmlLoadTests {

    private static boolean javaFxStarted;

    @BeforeAll
    static void initJavaFxIfNeeded() throws Exception {
        if (Platform.isFxApplicationThread() || javaFxStarted) {
            javaFxStarted = true;
            return;
        }
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX startup timeout");
            javaFxStarted = true;
        } catch (IllegalStateException e) {
            // Already started
            javaFxStarted = true;
        } catch (Throwable t) {
            // Headless or no display - skip load tests
            javaFxStarted = false;
        }
    }

    // ─── Bean contract: components used in FXML must expose writable properties ───

    @Test
    @Order(1)
    @DisplayName("TLBadge has getText/setText for FXML text attribute")
    void tlBadgeHasTextProperty() throws Exception {
        Class<?> c = TLBadge.class;
        Method get = c.getMethod("getText");
        Method set = c.getMethod("setText", String.class);
        assertNotNull(get);
        assertNotNull(set);
        TLBadge badge = new TLBadge();
        set.invoke(badge, "OK");
        assertEquals("OK", get.invoke(badge));
    }

    @Test
    @Order(2)
    @DisplayName("TLBadge has getVariant/setVariant for FXML variant attribute")
    void tlBadgeHasVariantProperty() throws Exception {
        Class<?> c = TLBadge.class;
        Method get = c.getMethod("getVariant");
        Method setStr = c.getMethod("setVariant", String.class);
        assertNotNull(get);
        assertNotNull(setStr);
        TLBadge badge = new TLBadge();
        setStr.invoke(badge, "SUCCESS");
        Object v = get.invoke(badge);
        assertNotNull(v);
        assertEquals("SUCCESS", v.toString());
    }

    @Test
    @Order(3)
    @DisplayName("Load minimal FXML with TLBadge (text and variant) without PropertyNotFoundException")
    void loadTlBadgeFxml() throws Exception {
        if (!javaFxStarted) {
            // Skip when JavaFX could not be started (e.g. headless CI)
            return;
        }
        URL url = getClass().getResource("/com/skilora/view/tlbadge-test.fxml");
        assertNotNull(url, "tlbadge-test.fxml not on classpath");

        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(url);
                VBox root = loader.load();
                assertNotNull(root);
                assertFalse(root.getChildren().isEmpty());
                assertTrue(root.getChildren().get(0) instanceof TLBadge,
                    "First child should be TLBadge");
                TLBadge badge = (TLBadge) root.getChildren().get(0);
                assertEquals("VERIFIED", badge.getText());
                assertEquals(TLBadge.Variant.SUCCESS, badge.getVariant());
            } catch (Throwable t) {
                err.set(t);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS), "Load timed out");
        if (err.get() != null) {
            throw new AssertionError("FXML load failed (e.g. property text/variant must exist on TLBadge)", err.get());
        }
    }

    @Test
    @Order(4)
    @DisplayName("TLButton.ButtonSize has LARGE for FXML size=\"LARGE\" (e.g. PublicProfileView)")
    void tlButtonSizeHasLarge() {
        TLButton.ButtonSize large = TLButton.ButtonSize.valueOf("LARGE");
        assertNotNull(large);
        assertSame(TLButton.ButtonSize.LARGE, large);
    }

    @Test
    @Order(5)
    @DisplayName("TLCard has getTitle/setTitle for FXML title attribute (PublicProfileView)")
    void tlCardHasTitleProperty() throws Exception {
        TLCard card = new TLCard();
        card.setTitle("About");
        assertEquals("About", card.getTitle());
    }
}
