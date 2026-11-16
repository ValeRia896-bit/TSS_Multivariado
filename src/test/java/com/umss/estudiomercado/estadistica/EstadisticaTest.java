package com.umss.estudiomercado.estadistica;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que el motor estadístico produce los mismos resultados que el "Informe de Calidad y Validación"
 * (cuartiles por mediana de mitades, percentil por rango más cercano, moda, rango...).
 */
class EstadisticaTest {

    private static double num(Map<String, Object> m, String clave) {
        return ((Number) m.get(clave)).doubleValue();
    }

    @Test
    void descriptivaCoincideConElInformeDeCalidad() {
        double[] tiempos = {4, 5, 5, 5, 7, 8, 8, 10, 10, 12, 12, 15, 18, 20, 22};
        Map<String, Object> r = Descriptiva.calcular(tiempos, "Tiempo de espera");
        assertEquals(15, ((Number) r.get("n")).intValue());
        assertEquals(10.0, num(r, "mediana"), 1e-9);
        assertEquals(5.0, num(r, "q1"), 1e-9);
        assertEquals(15.0, num(r, "q3"), 1e-9);
        assertEquals(18.0, num(r, "rango"), 1e-9);
        assertEquals(10.7333, num(r, "media"), 1e-3);
        assertEquals(List.of(5.0), r.get("moda"));
        @SuppressWarnings("unchecked")
        Map<String, Object> perc = (Map<String, Object>) r.get("percentiles");
        assertEquals(20.0, num(perc, "P90"), 1e-9);
    }

    @Test
    void chiCuadradoDeIndependencia() {
        Map<String, Object> r = ChiCuadrado.independencia(List.of("A", "B"), List.of("X", "Y"),
                new long[][]{{10, 20}, {30, 40}}, 0.05, "v1", "v2");
        assertEquals(0.7937, num(r, "chi2"), 1e-3);
        assertEquals(1, ((Number) r.get("gl")).intValue());
        assertEquals(0.373, num(r, "p"), 1e-3);
        assertEquals(3.8415, num(r, "chi2Critico"), 1e-3);
        assertEquals(Boolean.FALSE, r.get("rechazaH0"));
    }

    @Test
    void distribucionesConValoresDeTabla() {
        assertEquals(7.8147, Distribuciones.chi2Inv(0.95, 3), 1e-3);
        assertEquals(2.1448, Distribuciones.tInv(0.975, 14), 1e-3);
    }

    @Test
    void sinDatosNoRompe() {
        Map<String, Object> r = Descriptiva.calcular(new double[0], "Años");
        assertEquals(Boolean.TRUE, r.get("sinDatos"));
        Map<String, Object> c = ChiCuadrado.independencia(List.of("A"), List.of("X"), new long[][]{{3}}, 0.05, "a", "b");
        assertEquals(Boolean.FALSE, c.get("aplicable"));
        assertTrue(c.get("interpretacion").toString().length() > 10);
    }

    @Test
    void multivariadoCoincideConElDocumentoDeAnalisisExploratorio() {
        // Los 10 titulados del documento "Análisis Exploratorio Multivariable": x = área, y = financiamiento, z = años.
        List<String> xs = List.of("Ciencia de Datos", "Ciberseguridad", "Ing. Software", "Ciencia de Datos", "Redes",
                "Ciberseguridad", "Ing. Software", "Ciencia de Datos", "Ing. Software", "Redes");
        List<String> ys = List.of("Becas Estatales", "Recursos Propios", "Financiamiento Empresa", "Becas Estatales",
                "Crédito Educativo", "Recursos Propios", "Financiamiento Empresa", "Becas Estatales",
                "Crédito Educativo", "Recursos Propios");
        List<Double> zs = List.of(3.0, 5.0, 1.0, 7.0, 2.0, 4.0, 6.0, 3.0, 5.0, 2.0);
        Map<String, Object> r = Multivariado.cruce(xs, ys, zs,
                List.of("Ciencia de Datos", "Ciberseguridad", "Ing. Software", "Redes"),
                List.of("Becas Estatales", "Recursos Propios", "Financiamiento Empresa", "Crédito Educativo"),
                "Área de interés", "Fuente de financiamiento", "Antigüedad de egreso");
        assertEquals(10, ((Number) r.get("n")).intValue());
        assertEquals(3.8, num(r, "mediaGeneral"), 1e-9);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> celdas = (List<Map<String, Object>>) r.get("celdas");
        assertEquals(6, celdas.size());
        assertEquals(3, ((Number) celdas.get(0).get("n")).intValue());      // Ciencia de Datos × Becas Estatales
        assertEquals(4.33, num(celdas.get(0), "media"), 1e-9);
        assertEquals(4.5, num(celdas.get(1), "media"), 1e-9);              // Ciberseguridad × Recursos Propios
        assertEquals(3.5, num(celdas.get(2), "media"), 1e-9);              // Ing. Software × Financiamiento Empresa
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> porX = (List<Map<String, Object>>) r.get("resumenX");
        assertEquals(100.0, num(porX.get(0), "pctDominante"), 1e-9);        // Ciencia de Datos: 100 % Becas Estatales
        assertEquals("Becas Estatales", porX.get(0).get("dominante"));
    }
}
