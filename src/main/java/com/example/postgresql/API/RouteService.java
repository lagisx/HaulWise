package com.example.postgresql.API;

import java.util.List;
import java.util.function.Consumer;

public class RouteService {

    private static final java.util.Map<String, List<double[]>> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static void getRouteCoordsYandex(double startLon, double startLat,
                                            double endLon, double endLat,
                                            Consumer<List<double[]>> callback) {

        String key = startLon + "_" + startLat + "_" + endLon + "_" + endLat;

        if (CACHE.containsKey(key)) {
            callback.accept(CACHE.get(key));
            return;
        }

        List<double[]> route = List.of(
                new double[]{startLon, startLat},
                new double[]{endLon,   endLat}
        );

        CACHE.put(key, route);
        callback.accept(route);
    }
}