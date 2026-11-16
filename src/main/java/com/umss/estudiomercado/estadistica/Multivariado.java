package com.umss.estudiomercado.estadistica;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.umss.estudiomercado.estadistica.Num.mapa;
import static com.umss.estudiomercado.estadistica.Num.r2;

/**
 * Análisis exploratorio multivariable: dos variables categóricas (x, y) y una numérica (z).
 * Produce la tabulación cruzada con frecuencia n y media de z por cada combinación (x, y),
 * los resúmenes marginales y los hallazgos en lenguaje natural.
 */
public final class Multivariado {
    private Multivariado() {}

    /**
     * @param xs      categoría x de cada registro (sin nulos)
     * @param ys      categoría y de cada registro (sin nulos)
     * @param zs      valor numérico z de cada registro (sin nulos)
     * @param ordenX  orden de presentación de las categorías de x
     * @param ordenY  orden de presentación de las categorías de y
     */
    public static Map<String, Object> cruce(List<String> xs, List<String> ys, List<Double> zs,
                                            List<String> ordenX, List<String> ordenY,
                                            String ex, String ey, String ez) {
        int n = zs.size();
        if (n == 0) {
            return mapa("n", 0, "etiquetaX", ex, "etiquetaY", ey, "etiquetaZ", ez, "celdas", List.of(),
                    "resumenX", List.of(), "resumenY", List.of(), "puntos", List.of(), "hallazgos", List.of(),
                    "interpretacion", "Aún no hay registros con las tres variables respondidas.");
        }
        Map<String, List<Double>> porCelda = new HashMap<>();
        Map<String, List<Double>> porX = new HashMap<>(), porY = new HashMap<>();
        Map<String, Map<String, Integer>> nXY = new HashMap<>(), nYX = new HashMap<>();
        List<Map<String, Object>> puntos = new ArrayList<>();
        double suma = 0, min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            String x = xs.get(i), y = ys.get(i);
            double z = zs.get(i);
            porCelda.computeIfAbsent(x + "\u0000" + y, k -> new ArrayList<>()).add(z);
            porX.computeIfAbsent(x, k -> new ArrayList<>()).add(z);
            porY.computeIfAbsent(y, k -> new ArrayList<>()).add(z);
            nXY.computeIfAbsent(x, k -> new HashMap<>()).merge(y, 1, Integer::sum);
            nYX.computeIfAbsent(y, k -> new HashMap<>()).merge(x, 1, Integer::sum);
            puntos.add(mapa("x", x, "y", y, "z", z));
            suma += z;
            min = Math.min(min, z);
            max = Math.max(max, z);
        }
        double mediaGeneral = suma / n;

        List<Map<String, Object>> celdas = new ArrayList<>();
        for (String x : ordenX) {
            for (String y : ordenY) {
                List<Double> v = porCelda.get(x + "\u0000" + y);
                if (v == null) continue;
                celdas.add(mapa("x", x, "y", y, "n", v.size(), "media", r2(media(v)),
                        "minimo", r2(minimo(v)), "maximo", r2(maximo(v))));
            }
        }
        List<Map<String, Object>> resumenX = marginal(ordenX, porX, nXY, n);
        List<Map<String, Object>> resumenY = marginal(ordenY, porY, nYX, n);

        List<String> hallazgos = new ArrayList<>();
        for (Map<String, Object> g : resumenX) {
            hallazgos.add(String.format(Locale.US,
                    "«%s» (n=%d): %s promedio %.2f (mín %s, máx %s). %s",
                    g.get("grupo"), g.get("n"), ez.toLowerCase(), g.get("media"), formato(g.get("minimo")),
                    formato(g.get("maximo")), concentracion(g, ey)));
        }
        Map<String, Object> mayor = extremo(resumenX, true), menor = extremo(resumenX, false);
        String lectura = String.format(Locale.US,
                "Se cruzaron «%s», «%s» y «%s» con %d registros; el promedio general de «%s» es %.2f. "
                        + "El grupo de «%s» con mayor promedio es «%s» (%.2f) y el de menor promedio es «%s» (%.2f).",
                ex, ey, ez, n, ez, mediaGeneral, ex, mayor.get("grupo"), ((Number) mayor.get("media")).doubleValue(),
                menor.get("grupo"), ((Number) menor.get("media")).doubleValue());
        return mapa("n", n, "etiquetaX", ex, "etiquetaY", ey, "etiquetaZ", ez,
                "mediaGeneral", r2(mediaGeneral), "minimo", r2(min), "maximo", r2(max),
                "celdas", celdas, "resumenX", resumenX, "resumenY", resumenY, "puntos", puntos,
                "hallazgos", hallazgos, "interpretacion", lectura);
    }

    private static List<Map<String, Object>> marginal(List<String> orden, Map<String, List<Double>> z,
                                                      Map<String, Map<String, Integer>> cruce, int total) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String g : orden) {
            List<Double> v = z.get(g);
            if (v == null) continue;
            String dom = null;
            int nDom = -1;
            for (String o : cruce.get(g).keySet().stream().sorted().toList()) {
                int c = cruce.get(g).get(o);
                if (c > nDom) { nDom = c; dom = o; }
            }
            out.add(mapa("grupo", g, "n", v.size(), "pctTotal", r2(v.size() * 100.0 / total),
                    "media", r2(media(v)), "minimo", r2(minimo(v)), "maximo", r2(maximo(v)),
                    "dominante", dom, "nDominante", nDom, "pctDominante", r2(nDom * 100.0 / v.size())));
        }
        return out;
    }

    private static String concentracion(Map<String, Object> g, String ey) {
        double p = ((Number) g.get("pctDominante")).doubleValue();
        if (p >= 99.995) return String.format(Locale.US, "El 100%% se concentra en «%s» (%d de %d).",
                g.get("dominante"), g.get("nDominante"), g.get("n"));
        return String.format(Locale.US, "La categoría más frecuente de «%s» es «%s» (%.1f%%, %d de %d).",
                ey, g.get("dominante"), p, g.get("nDominante"), g.get("n"));
    }

    private static Map<String, Object> extremo(List<Map<String, Object>> l, boolean mayor) {
        Map<String, Object> best = l.get(0);
        for (Map<String, Object> g : l) {
            double a = ((Number) g.get("media")).doubleValue(), b = ((Number) best.get("media")).doubleValue();
            if (mayor ? a > b : a < b) best = g;
        }
        return best;
    }

    private static String formato(Object o) {
        double d = ((Number) o).doubleValue();
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.US, "%.2f", d);
    }

    private static double media(List<Double> v) {
        double s = 0;
        for (double d : v) s += d;
        return s / v.size();
    }

    private static double minimo(List<Double> v) {
        return v.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
    }

    private static double maximo(List<Double> v) {
        return v.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
    }
}
