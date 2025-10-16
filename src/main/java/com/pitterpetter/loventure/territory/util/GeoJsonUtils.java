package com.pitterpetter.loventure.territory.util;

import com.pitterpetter.loventure.territory.domain.region.Region;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public final class GeoJsonUtils {

    private GeoJsonUtils() {
    }

    public static Map<String, Object> toFeatureCollection(List<Region> regions) {
        Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection");

        List<Map<String, Object>> features = new ArrayList<>();
        for (Region region : regions) {
            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");

            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("id", region.getId());
            properties.put("sigCd", region.getSigCd());
            properties.put("siDo", region.getSi_do());
            properties.put("guSi", region.getGu_si());
            feature.put("properties", properties);

            feature.put("geometry", geometryToGeoJson(region.getGeom()));
            features.add(feature);
        }

        featureCollection.put("features", features);
        return featureCollection;
    }

    private static Map<String, Object> geometryToGeoJson(Geometry geometry) {
        Map<String, Object> geometryMap = new LinkedHashMap<>();
        if (geometry instanceof Polygon polygon) {
            geometryMap.put("type", "Polygon");
            geometryMap.put("coordinates", polygonToCoords(polygon));
            return geometryMap;
        }
        if (geometry instanceof MultiPolygon multiPolygon) {
            geometryMap.put("type", "MultiPolygon");
            List<Object> coordinates = new ArrayList<>();
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                coordinates.add(polygonToCoords((Polygon) multiPolygon.getGeometryN(i)));
            }
            geometryMap.put("coordinates", coordinates);
            return geometryMap;
        }
        geometryMap.put("type", geometry == null ? "Geometry" : geometry.getGeometryType());
        geometryMap.put("coordinates", List.of());
        return geometryMap;
    }

    private static List<List<List<Double>>> polygonToCoords(Polygon polygon) {
        List<List<List<Double>>> rings = new ArrayList<>();
        rings.add(linearRingToCoords(polygon.getExteriorRing()));
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            rings.add(linearRingToCoords(polygon.getInteriorRingN(i)));
        }
        return rings;
    }

    private static List<List<Double>> linearRingToCoords(LineString ring) {
        List<List<Double>> coords = new ArrayList<>();
        for (Coordinate coordinate : ring.getCoordinates()) {
            coords.add(List.of(coordinate.x, coordinate.y));
        }
        return coords;
    }
}
