package br.com.dti.msa.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MetricCatalogTest {

    private MetricCatalog metricCatalog;

    @BeforeEach
    public void setUp() {
        metricCatalog = new MetricCatalog();
    }

    @Test
    public void testGetMetricKeysForCheckbox_Found() {
        List<String> keys = metricCatalog.getMetricKeysForCheckbox("cpu-uso");
        assertNotNull(keys);
        assertEquals(1, keys.size());
        assertEquals("cpu-uso", keys.get(0));
    }

    @Test
    public void testGetMetricKeysForCheckbox_NotFound() {
        List<String> keys = metricCatalog.getMetricKeysForCheckbox("chave-inexistente");
        assertNotNull(keys);
        assertTrue(keys.isEmpty());
    }

    @Test
    public void testGetCheckboxesForMetricKeys_Match() {
        List<String> savedKeys = List.of("memoria-ram-total", "memoria-ram-disponivel");
        List<String> checkboxes = metricCatalog.getCheckboxesForMetricKeys(savedKeys);
        
        assertEquals(1, checkboxes.size());
        assertEquals("memoria-ram", checkboxes.get(0));
    }
}