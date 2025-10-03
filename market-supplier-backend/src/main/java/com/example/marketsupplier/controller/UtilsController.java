package com.example.marketsupplier.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@RestController
@RequestMapping("/api/utils")
@CrossOrigin(origins = "*")
public class UtilsController {
    private static final Logger log = LoggerFactory.getLogger(UtilsController.class);

    private final RestTemplate restTemplate;

    @Value("${app.ors.api.key:}")
    private String orsApiKey;
    
    @Value("${app.mapbox.api.key:}")
    private String mapboxApiKey;

    public UtilsController() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(3000);   // 3 saniye
        rf.setReadTimeout(5000);      // 5 saniye
        this.restTemplate = new RestTemplate(rf);
    }

    @GetMapping("/geocode")
    public ResponseEntity<Map<String, Object>> geocode(@RequestParam("q") String query) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + query;
            HttpHeaders headers = new HttpHeaders();
            // Daha güvenilir UA (bazı public OSRM'ler custom UA'ları agresif kısıtlayabiliyor)
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
            headers.set("Accept-Language", "tr");
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<java.util.List> resp = restTemplate.exchange(url, HttpMethod.GET, entity, java.util.List.class);
            if (resp.getBody() != null && !resp.getBody().isEmpty()) {
                Map first = (Map) resp.getBody().get(0);
                return ResponseEntity.ok(Map.of(
                        "lat", Double.parseDouble(first.get("lat").toString()),
                        "lon", Double.parseDouble(first.get("lon").toString())
                ));
            }
            return ResponseEntity.ok(Map.of());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of());
        }
    }

    // OSRM proxy: coords format "lon1,lat1;lon2,lat2[;lon3,lat3...]"
    @GetMapping("/osrm")
    public ResponseEntity<Map> osrm(@RequestParam("coords") String coords) {
        try {
            log.info("/osrm called coords={}", coords);
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
            headers.set("Accept-Language", "tr");
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Primary OSRM
            String base1 = "https://router.project-osrm.org";
            String url1 = base1 + "/route/v1/driving/" + coords + "?alternatives=false&steps=false&overview=full&geometries=geojson&radiuses=20000;20000";
            try {
                long t0 = System.currentTimeMillis();
                ResponseEntity<Map> resp1 = restTemplate.exchange(url1, HttpMethod.GET, entity, Map.class);
                Map body1 = resp1.getBody();
                if (body1 != null && body1.get("routes") != null) {
                    log.info("/osrm success via project-osrm in {} ms", System.currentTimeMillis() - t0);
                    return ResponseEntity.ok(body1);
                }
            } catch (Exception e) { log.warn("/osrm project-osrm failed: {}", e.toString()); }

            // Fallback OSRM (OpenStreetMap.de)
            String base2 = "https://routing.openstreetmap.de/routed-car";
            String url2 = base2 + "/route/v1/driving/" + coords + "?alternatives=false&steps=false&overview=full&geometries=geojson&radiuses=20000;20000";
            try {
                long t0 = System.currentTimeMillis();
                ResponseEntity<Map> resp2 = restTemplate.exchange(url2, HttpMethod.GET, entity, Map.class);
                Map body2 = resp2.getBody();
                if (body2 != null && body2.get("routes") != null) {
                    log.info("/osrm success via osm.de in {} ms", System.currentTimeMillis() - t0);
                    return ResponseEntity.ok(body2);
                }
            } catch (Exception e) { log.warn("/osrm osm.de failed: {}", e.toString()); }

            // Segmente ederek dene (son çare)
            Map stitched = trySegmentedRouting(coords, entity);
            if (!stitched.isEmpty()) {
                log.info("/osrm success via segmented fallback");
                return ResponseEntity.ok(stitched);
            }

            return ResponseEntity.ok(Map.of());
        } catch (Exception e) {
            log.error("/osrm fatal error: {}", e.toString());
            return ResponseEntity.ok(Map.of());
        }
    }

    // Çoklu durak: coords = "lon1,lat1;lon2,lat2;lon3,lat3;..."
    @GetMapping("/mroute")
    public ResponseEntity<Map> multiRoute(@RequestParam("coords") String coords) {
        try {
            log.info("/mroute called coords={}", coords);
            String[] pts = coords.split(";");
            if (pts.length < 2) return ResponseEntity.ok(Map.of());

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
            headers.set("Accept-Language", "tr");
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 0) OpenRouteService FIRST (daha hızlı ve güvenilir)
            if (!orsApiKey.isEmpty()) {
                try {
                    long t0 = System.currentTimeMillis();
                    String orsUrl = "https://api.openrouteservice.org/v2/directions/driving-car";
                    List<List<Double>> orsCoords = Arrays.stream(coords.split(";"))
                            .map(point -> {
                                String[] parts = point.split(",");
                                return List.of(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
                            })
                            .collect(Collectors.toList());

                    Map<String, Object> orsRequestBody = Map.of(
                            "coordinates", orsCoords,
                            "format", "geojson"
                    );

                    HttpHeaders orsHeaders = new HttpHeaders();
                    orsHeaders.set("Authorization", orsApiKey);
                    orsHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
                    orsHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                    HttpEntity<Map<String, Object>> orsEntity = new HttpEntity<>(orsRequestBody, orsHeaders);
                    
                    ResponseEntity<Map> orsResp = restTemplate.exchange(orsUrl, HttpMethod.POST, orsEntity, Map.class);
                    Map orsBody = orsResp.getBody();
                    if (orsBody != null && orsBody.get("routes") != null && !((List) orsBody.get("routes")).isEmpty()) {
                        // Convert ORS response to OSRM-like format
                        Map route = (Map) ((List) orsBody.get("routes")).get(0);
                        Map geometry = (Map) route.get("geometry");
                        Object summaryObj = route.get("summary");
                        double distance = 0.0;
                        double duration = 0.0;
                        
                        if (summaryObj instanceof Map) {
                            Map summary = (Map) summaryObj;
                            distance = ((Number) summary.get("distance")).doubleValue();
                            duration = ((Number) summary.get("duration")).doubleValue();
                        }

                        log.info("/mroute success via ORS in {} ms", System.currentTimeMillis()-t0);
                        return ResponseEntity.ok(Map.of("routes", List.of(Map.of(
                                "geometry", geometry,
                                "distance", distance,
                                "duration", duration
                        ))));
                    }
                } catch (Exception e) { 
                    log.warn("/mroute ORS failed: {}", e.toString()); 
                }
            }

            // 1) OSRM fallback
            String urlAll1 = "https://router.project-osrm.org/route/v1/driving/" + coords + "?alternatives=false&steps=false&overview=full&geometries=geojson";
            try {
                long t0 = System.currentTimeMillis();
                ResponseEntity<Map> rAll1 = restTemplate.exchange(urlAll1, HttpMethod.GET, entity, Map.class);
                if (rAll1.getBody() != null && rAll1.getBody().get("routes") != null) { log.info("/mroute success via project-osrm (all) in {} ms", System.currentTimeMillis()-t0); return ResponseEntity.ok(rAll1.getBody()); }
            } catch (Exception e) { log.warn("/mroute project-osrm(all) failed: {}", e.toString()); }
            
            // 2) OSM.de fallback
            String urlAll2 = "https://routing.openstreetmap.de/routed-car/route/v1/driving/" + coords + "?alternatives=false&steps=false&overview=full&geometries=geojson";
            try {
                long t0 = System.currentTimeMillis();
                ResponseEntity<Map> rAll2 = restTemplate.exchange(urlAll2, HttpMethod.GET, entity, Map.class);
                if (rAll2.getBody() != null && rAll2.getBody().get("routes") != null) { log.info("/mroute success via osm.de (all) in {} ms", System.currentTimeMillis()-t0); return ResponseEntity.ok(rAll2.getBody()); }
            } catch (Exception e) { log.warn("/mroute osm.de(all) failed: {}", e.toString()); }

            List<List<Double>> all = new ArrayList<>();
            double totalKm = 0.0;
            double totalSec = 0.0;
            for (int i = 0; i < pts.length - 1; i++) {
                String a = pts[i];
                String b = pts[i + 1];
                Map seg = getBestRoute(a + ";" + b, entity);
                if (seg.get("routes") instanceof List) {
                    Map r0 = (Map) ((List) seg.get("routes")).get(0);
                    Map geom = (Map) r0.get("geometry");
                    List<List<Double>> coordsList = (List<List<Double>>) geom.get("coordinates");
                    if (all.isEmpty()) all.addAll(coordsList); else for (int k = 1; k < coordsList.size(); k++) all.add(coordsList.get(k));
                    Object dist = r0.get("distance");
                    if (dist instanceof Number) totalKm += ((Number) dist).doubleValue() / 1000.0;
                    Object dur = r0.get("duration");
                    if (dur instanceof Number) totalSec += ((Number) dur).doubleValue();
                }
            }

            if (all.isEmpty()) { 
                log.warn("/mroute all providers failed – creating simple fallback route");
                // Basit fallback: sadece noktaları birleştir
                for (int i = 0; i < pts.length; i++) {
                    String[] parts = pts[i].split(",");
                    all.add(List.of(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
                }
                // Basit mesafe hesapla (haversine)
                totalKm = 0.0;
                for (int i = 1; i < all.size(); i++) {
                    double lat1 = all.get(i-1).get(1);
                    double lon1 = all.get(i-1).get(0);
                    double lat2 = all.get(i).get(1);
                    double lon2 = all.get(i).get(0);
                    totalKm += haversine(lat1, lon1, lat2, lon2);
                }
                totalSec = totalKm * 60.0 / 50.0; // 50 km/h varsayım
            }

            Map<String, Object> geometry = new HashMap<>();
            geometry.put("type", "LineString");
            geometry.put("coordinates", all);
            Map<String, Object> route = new HashMap<>();
            route.put("geometry", geometry);
            route.put("distance", totalKm * 1000.0);
            route.put("duration", totalSec);
            List<Map<String, Object>> routes = new ArrayList<>();
            routes.add(route);
            return ResponseEntity.ok(Map.of("routes", routes));
        } catch (Exception e) {
            log.error("/mroute fatal error: {}", e.toString());
            return ResponseEntity.ok(Map.of());
        }
    }

    private Map getBestRoute(String coords, HttpEntity<Void> entity) {
        try {
            // 1) OSRM ana
            String url1 = "https://router.project-osrm.org/route/v1/driving/" + coords + "?alternatives=false&steps=false&overview=full&geometries=geojson&radiuses=20000;20000";
            try {
                ResponseEntity<Map> r1 = restTemplate.exchange(url1, HttpMethod.GET, entity, Map.class);
                if (r1.getBody() != null && r1.getBody().get("routes") != null) return r1.getBody();
            } catch (Exception ignore) {}
            // 2) OSM.de
            String url2 = "https://routing.openstreetmap.de/routed-car/route/v1/driving/" + coords + "?alternatives=false&steps=false&overview=full&geometries=geojson&radiuses=20000;20000";
            try {
                ResponseEntity<Map> r2 = restTemplate.exchange(url2, HttpMethod.GET, entity, Map.class);
                if (r2.getBody() != null && r2.getBody().get("routes") != null) return r2.getBody();
            } catch (Exception ignore) {}
            // 3) OpenRouteService (varsa)
            Map ors = tryORS(coords, entity);
            if (!ors.isEmpty()) return ors;
            // 3) Segmentli
            return trySegmentedRouting(coords, entity);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map tryORS(String coords, HttpEntity<Void> entity) {
        try {
            if (orsApiKey == null || orsApiKey.isBlank()) return Map.of();
            String[] parts = coords.split(";");
            if (parts.length < 2) return Map.of();
            if (parts.length == 2) {
                // GET start/end
                String[] a = parts[0].split(",");
                String[] b = parts[1].split(",");
                String start = a[0] + "," + a[1];
                String end = b[0] + "," + b[1];
                String url = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=" + orsApiKey + "&start=" + start + "&end=" + end + "&format=geojson";
                ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
                return mapOrsToOsrmLike(resp.getBody());
            } else {
                // POST with coordinates array
                List<List<Double>> coordinates = new ArrayList<>();
                for (String p : parts) {
                    String[] ab = p.split(",");
                    coordinates.add(List.of(Double.parseDouble(ab[0]), Double.parseDouble(ab[1])));
                }
                Map<String, Object> body = new HashMap<>();
                body.put("coordinates", coordinates);
                HttpHeaders h = new HttpHeaders();
                h.setAll(entity.getHeaders().toSingleValueMap());
                h.set("Authorization", orsApiKey);
                h.set("Content-Type", "application/json");
                HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, h);
                String url = "https://api.openrouteservice.org/v2/directions/driving-car/geojson";
                ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, req, Map.class);
                return mapOrsToOsrmLike(resp.getBody());
            }
        } catch (Exception ignore) {
            return Map.of();
        }
    }

    private Map mapOrsToOsrmLike(Map ors) {
        try {
            if (ors == null) return Map.of();
            Object featuresObj = ors.get("features");
            if (!(featuresObj instanceof List) || ((List) featuresObj).isEmpty()) return Map.of();
            Map feat0 = (Map) ((List) featuresObj).get(0);
            Map geometry = (Map) feat0.get("geometry");
            Map props = (Map) feat0.get("properties");
            Map summary = props != null ? (Map) props.get("summary") : null;
            double distance = summary != null && summary.get("distance") instanceof Number ? ((Number) summary.get("distance")).doubleValue() : 0.0;
            double duration = summary != null && summary.get("duration") instanceof Number ? ((Number) summary.get("duration")).doubleValue() : 0.0;
            Map<String, Object> route = new HashMap<>();
            route.put("geometry", geometry);
            route.put("distance", distance);
            route.put("duration", duration);
            List<Map<String, Object>> routes = new ArrayList<>();
            routes.add(route);
            return Map.of("routes", routes);
        } catch (Exception e) {
            return Map.of();
        }
    }

    // Haversine km
    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // OSRM boş dönünce iki noktayı 8 parçaya bölüp birleştir
    private Map trySegmentedRouting(String coords, HttpEntity<Void> entity) {
        try {
            String[] parts = coords.split(";");
            if (parts.length < 2) return Map.of();
            String[] a = parts[0].split(",");
            String[] b = parts[1].split(",");
            double lon1 = Double.parseDouble(a[0]);
            double lat1 = Double.parseDouble(a[1]);
            double lon2 = Double.parseDouble(b[0]);
            double lat2 = Double.parseDouble(b[1]);

            int segments = 8;
            List<List<Double>> combined = new ArrayList<>();
            double totalKm = 0.0;
            for (int i = 0; i < segments; i++) {
                double t1 = (double) i / segments;
                double t2 = (double) (i + 1) / segments;
                double ilon1 = lon1 + (lon2 - lon1) * t1;
                double ilat1 = lat1 + (lat2 - lat1) * t1;
                double ilon2 = lon1 + (lon2 - lon1) * t2;
                double ilat2 = lat1 + (lat2 - lat1) * t2;

                String segCoords = ilon1 + "," + ilat1 + ";" + ilon2 + "," + ilat2;
                String url = "https://routing.openstreetmap.de/routed-car/route/v1/driving/" + segCoords + "?alternatives=false&steps=false&overview=full&geometries=geojson&radiuses=20000;20000";
            try {
                    ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
                    Map body = resp.getBody();
                    if (body != null && body.get("routes") instanceof List) {
                        List routes = (List) body.get("routes");
                        if (!routes.isEmpty()) {
                            Map r0 = (Map) routes.get(0);
                            Map geom = (Map) r0.get("geometry");
                            List<List<Double>> coordsList = (List<List<Double>>) geom.get("coordinates");
                            if (combined.isEmpty()) {
                                combined.addAll(coordsList);
                            } else {
                                for (int k = 1; k < coordsList.size(); k++) combined.add(coordsList.get(k));
                            }
                            Object dist = r0.get("distance");
                            if (dist instanceof Number) totalKm += ((Number) dist).doubleValue() / 1000.0;
                            continue;
                        }
                    }
                } catch (Exception e) { log.warn("segmented osrm.de failed for piece {}: {}", i, e.toString()); }

                // Fallback: kuş uçuşu ekle
                List<Double> p1 = new ArrayList<>(); p1.add(ilon1); p1.add(ilat1);
                List<Double> p2 = new ArrayList<>(); p2.add(ilon2); p2.add(ilat2);
                if (combined.isEmpty()) combined.add(p1);
                combined.add(p2);
                totalKm += haversine(ilat1, ilon1, ilat2, ilon2);
            }

            if (combined.isEmpty()) return Map.of();

            Map<String, Object> geometry = new HashMap<>();
            geometry.put("type", "LineString");
            geometry.put("coordinates", combined);
            Map<String, Object> route = new HashMap<>();
            route.put("geometry", geometry);
            route.put("distance", totalKm * 1000.0);
            List<Map<String, Object>> routes = new ArrayList<>();
            routes.add(route);
            return Map.of("routes", routes);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    @GetMapping("/mapbox-directions")
    public ResponseEntity<Map<String, Object>> getMapboxDirections(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam(value = "waypoints", required = false) String waypoints) {
        try {
            log.info("/mapbox-directions called origin={} destination={} waypoints={}", origin, destination, waypoints);
            
            if (mapboxApiKey == null || mapboxApiKey.isEmpty()) {
                log.warn("Mapbox API key not configured");
                return ResponseEntity.badRequest().body(Map.of("error", "Mapbox API key not configured"));
            }

            // Mapbox Directions API URL oluştur
            StringBuilder url = new StringBuilder("https://api.mapbox.com/directions/v5/mapbox/driving/");
            url.append(origin);
            if (waypoints != null && !waypoints.isEmpty()) {
                url.append(";").append(waypoints);
            }
            url.append(";").append(destination);
            url.append("?access_token=").append(mapboxApiKey);
            url.append("&geometries=polyline");
            url.append("&overview=full");
            url.append("&steps=true");
            url.append("&voice_instructions=true");
            url.append("&banner_instructions=true");

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "MarketSupplier/1.0");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url.toString(), HttpMethod.GET, entity, Map.class);
            
            if (response.getBody() != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("data", response.getBody());
                result.put("source", "mapbox");
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "No response from Mapbox"));
            }
            
        } catch (Exception e) {
            log.error("/mapbox-directions failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mapbox-matrix")
    public ResponseEntity<Map<String, Object>> getMapboxMatrix(
            @RequestParam("coordinates") String coordinates) {
        try {
            log.info("/mapbox-matrix called coordinates={}", coordinates);
            
            if (mapboxApiKey == null || mapboxApiKey.isEmpty()) {
                log.warn("Mapbox API key not configured");
                return ResponseEntity.badRequest().body(Map.of("error", "Mapbox API key not configured"));
            }

            // Mapbox Matrix API URL oluştur
            String url = "https://api.mapbox.com/directions-matrix/v1/mapbox/driving/" + coordinates +
                    "?access_token=" + mapboxApiKey +
                    "&sources=0" +
                    "&destinations=all";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "MarketSupplier/1.0");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getBody() != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("data", response.getBody());
                result.put("source", "mapbox");
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "No response from Mapbox"));
            }
            
        } catch (Exception e) {
            log.error("/mapbox-matrix failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}


