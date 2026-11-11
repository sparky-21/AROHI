package com.arohi.healthcare.services;

import androidx.annotation.NonNull;

import com.arohi.healthcare.models.Hospital;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Free OpenStreetMap Overpass API Client for external nearby hospital discovery.
 * Zero API key, zero credit card required.
 * External hospitals are strictly marked verified=false so users clearly distinguish
 * AROHI-verified hospitals from unverified OpenStreetMap public POIs.
 */
public final class OverpassHospitalService {

    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface OverpassCallback {
        void onResults(List<Hospital> externalHospitals);
    }

    private OverpassHospitalService() {
    }

    public static void fetchNearbyExternalHospitals(double lat, double lng, int radiusMeters,
                                                    @NonNull OverpassCallback callback) {
        if (lat == 0.0 && lng == 0.0) {
            callback.onResults(Collections.emptyList());
            return;
        }

        EXECUTOR.execute(() -> {
            List<Hospital> results = new ArrayList<>();
            HttpURLConnection conn = null;
            try {
                String query = String.format(Locale.US,
                        "[out:json][timeout:10];(node[\"amenity\"=\"hospital\"](around:%d,%f,%f);way[\"amenity\"=\"hospital\"](around:%d,%f,%f););out center 20;",
                        radiusMeters, lat, lng, radiusMeters, lat, lng);

                URL url = new URL(OVERPASS_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                conn.setRequestProperty("User-Agent", "AROHI-Healthcare-Android");

                String body = "data=" + java.net.URLEncoder.encode(query, "UTF-8");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    results.addAll(parseOverpassResponse(sb.toString()));
                }
            } catch (Exception ignored) {
                // Offline or Overpass rate-limit safely degrades to verified AROHI hospital directory
            } finally {
                if (conn != null) conn.disconnect();
            }
            callback.onResults(results);
        });
    }

    @NonNull
    public static List<Hospital> parseOverpassResponse(@NonNull String jsonText) {
        List<Hospital> list = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(jsonText);
            JSONArray elements = root.optJSONArray("elements");
            if (elements == null) return list;

            for (int i = 0; i < elements.length(); i++) {
                JSONObject el = elements.optJSONObject(i);
                if (el == null) continue;
                JSONObject tags = el.optJSONObject("tags");
                if (tags == null) continue;

                String name = tags.optString("name", "").trim();
                if (name.isEmpty()) continue;

                double hLat = el.has("lat") ? el.optDouble("lat", 0.0)
                        : (el.optJSONObject("center") != null ? el.optJSONObject("center").optDouble("lat", 0.0) : 0.0);
                double hLng = el.has("lon") ? el.optDouble("lon", 0.0)
                        : (el.optJSONObject("center") != null ? el.optJSONObject("center").optDouble("lon", 0.0) : 0.0);
                if (hLat == 0.0 && hLng == 0.0) continue;

                String street = tags.optString("addr:street", "");
                String city = tags.optString("addr:city", tags.optString("addr:district", ""));
                String fullAddr = tags.optString("addr:full",
                        (!street.isEmpty() && !city.isEmpty()) ? street + ", " + city :
                                (!street.isEmpty() ? street : (!city.isEmpty() ? city : "OpenStreetMap Public Listing")));
                String phone = tags.optString("phone", tags.optString("contact:phone", ""));
                boolean hasEmergency = tags.optString("emergency", "").equalsIgnoreCase("yes");

                list.add(createExternalHospital(el.optLong("id", i), name, hLat, hLng, fullAddr, city, phone, hasEmergency));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    @NonNull
    public static Hospital createExternalHospital(long osmId, @NonNull String name, double lat, double lng,
                                                  String fullAddr, String city, String phone, boolean hasEmergency) {
        return new Hospital(
                "osm_" + osmId,
                "OSM",
                name,
                fullAddr != null ? fullAddr : "OpenStreetMap Public Listing",
                city != null ? city : "",
                phone != null ? phone : "",
                "",
                hasEmergency && phone != null ? phone : "",
                Hospital.STATUS_ACTIVE,
                0,
                Collections.emptyList(),
                false, // Strictly unverified external OSM directory listing
                "Public OpenStreetMap hospital listing (not an integrated AROHI facility).",
                lat,
                lng
        );
    }
}
