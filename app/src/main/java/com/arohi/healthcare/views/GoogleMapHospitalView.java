package com.arohi.healthcare.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arohi.healthcare.utils.HospitalRoutePlanner;
import com.arohi.healthcare.utils.HospitalRoutePlanner.HospitalNode;
import com.arohi.healthcare.utils.HospitalRoutePlanner.RouteResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom Canvas View rendering a Google Maps themed interactive indoor hospital floor plan.
 * Displays Google Maps vector corridors, colored department zones,
 * Dijkstra/Bellman-Ford shortest-path polyline overlay, and Google-style pin markers.
 */
public class GoogleMapHospitalView extends View {

    public interface OnNodeClickListener {
        void onNodeClicked(@NonNull HospitalNode node);
    }

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corridorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corridorBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint roomFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint roomStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routePolylinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routePolylineGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private HospitalRoutePlanner routePlanner;
    private String currentFloor = "Ground Floor";
    private RouteResult currentRoute;
    private HospitalNode startNode;
    private HospitalNode destNode;
    private OnNodeClickListener nodeClickListener;

    private boolean isNavigating = false;
    private int currentStepIndex = 0;

    private final List<HospitalNode> displayedFloorNodes = new ArrayList<>();

    public GoogleMapHospitalView(Context context) {
        super(context);
        init();
    }

    public GoogleMapHospitalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GoogleMapHospitalView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private boolean isNightMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void init() {
        routePlanner = new HospitalRoutePlanner();
        boolean dark = isNightMode();

        // Google Maps Theme Palette (Dark & Light adaptive)
        bgPaint.setColor(dark ? Color.parseColor("#111813") : Color.parseColor("#E5E3DF"));

        corridorPaint.setColor(dark ? Color.parseColor("#1E2B22") : Color.WHITE);
        corridorBorderPaint.setColor(dark ? Color.parseColor("#354A3C") : Color.parseColor("#D5D3CE"));
        corridorBorderPaint.setStyle(Paint.Style.STROKE);
        corridorBorderPaint.setStrokeWidth(2f);

        roomFillPaint.setStyle(Paint.Style.FILL);
        roomStrokePaint.setStyle(Paint.Style.STROKE);
        roomStrokePaint.setStrokeWidth(3f);

        textPaint.setColor(dark ? Color.parseColor("#F5F8F4") : Color.parseColor("#202124"));
        textPaint.setTextSize(26f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        subTextPaint.setColor(dark ? Color.parseColor("#CBD7C8") : Color.parseColor("#5F6368"));
        subTextPaint.setTextSize(20f);
        subTextPaint.setTextAlign(Paint.Align.CENTER);

        // Google Maps vibrant navigation blue polyline
        routePolylinePaint.setColor(Color.parseColor("#1A73E8"));
        routePolylinePaint.setStyle(Paint.Style.STROKE);
        routePolylinePaint.setStrokeWidth(12f);
        routePolylinePaint.setStrokeCap(Paint.Cap.ROUND);
        routePolylinePaint.setStrokeJoin(Paint.Join.ROUND);

        routePolylineGlowPaint.setColor(Color.parseColor("#4D1A73E8"));
        routePolylineGlowPaint.setStyle(Paint.Style.STROKE);
        routePolylineGlowPaint.setStrokeWidth(22f);
        routePolylineGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        routePolylineGlowPaint.setStrokeJoin(Paint.Join.ROUND);

        markerFillPaint.setStyle(Paint.Style.FILL);
        markerStrokePaint.setStyle(Paint.Style.STROKE);
        markerStrokePaint.setColor(Color.WHITE);
        markerStrokePaint.setStrokeWidth(4f);

        updateFloorNodes();
    }

    public void setRoutePlanner(@NonNull HospitalRoutePlanner planner) {
        this.routePlanner = planner;
        updateFloorNodes();
        invalidate();
    }

    public void setCurrentFloor(@NonNull String floor) {
        this.currentFloor = floor;
        updateFloorNodes();
        invalidate();
    }

    public void setRoute(@Nullable RouteResult route, @Nullable HospitalNode start, @Nullable HospitalNode dest) {
        this.currentRoute = route;
        this.startNode = start;
        this.destNode = dest;
        invalidate();
    }

    public void startNavigation() {
        this.isNavigating = true;
        this.currentStepIndex = 0;
        invalidate();
    }

    public void setNavigationStep(int index) {
        this.currentStepIndex = Math.max(0, index);
        invalidate();
    }

    public void stopNavigation() {
        this.isNavigating = false;
        this.currentStepIndex = 0;
        invalidate();
    }

    public boolean isNavigating() {
        return isNavigating;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public void setOnNodeClickListener(OnNodeClickListener listener) {
        this.nodeClickListener = listener;
    }

    private void updateFloorNodes() {
        displayedFloorNodes.clear();
        if (routePlanner != null) {
            displayedFloorNodes.addAll(routePlanner.getNodesForFloor(currentFloor));
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // 1. Draw Google Maps Base Canvas
        canvas.drawColor(isNightMode() ? Color.parseColor("#111813") : Color.parseColor("#E5E3DF"));

        // 2. Draw Google Maps Intersecting Corridors / Hallways
        drawCorridors(canvas, w, h);

        // 3. Draw Department Blocks for Current Floor
        for (HospitalNode node : displayedFloorNodes) {
            drawDepartmentBlock(canvas, node, w, h);
        }

        // 4. Draw Shortest Path Route Polyline (Dijkstra / Bellman-Ford)
        if (currentRoute != null && currentRoute.getPathNodes() != null && currentRoute.getPathNodes().size() >= 2) {
            drawRoutePolyline(canvas, currentRoute.getPathNodes(), w, h);
        }

        // 5. Draw Google Maps Pin Markers (Start "A" and Destination "B")
        drawMarkers(canvas, w, h);

        // 6. Draw Google Maps Watermark & Scale in bottom corner
        drawGoogleMapsWatermark(canvas, w, h);
    }

    private void drawCorridors(Canvas canvas, int w, int h) {
        // Main central north-south corridor
        RectF mainCorridor = new RectF(w * 0.40f, h * 0.10f, w * 0.60f, h * 0.90f);
        canvas.drawRoundRect(mainCorridor, 12f, 12f, corridorPaint);
        canvas.drawRoundRect(mainCorridor, 12f, 12f, corridorBorderPaint);

        // East-West central connector
        RectF ewCorridor = new RectF(w * 0.10f, h * 0.45f, w * 0.90f, h * 0.58f);
        canvas.drawRoundRect(ewCorridor, 12f, 12f, corridorPaint);
        canvas.drawRoundRect(ewCorridor, 12f, 12f, corridorBorderPaint);

        // Emergency / West Wing Corridor
        RectF westCorridor = new RectF(w * 0.12f, h * 0.60f, w * 0.40f, h * 0.70f);
        canvas.drawRoundRect(westCorridor, 8f, 8f, corridorPaint);
        canvas.drawRoundRect(westCorridor, 8f, 8f, corridorBorderPaint);

        // Pharmacy / East Wing Corridor
        RectF eastCorridor = new RectF(w * 0.60f, h * 0.60f, w * 0.88f, h * 0.70f);
        canvas.drawRoundRect(eastCorridor, 8f, 8f, corridorPaint);
        canvas.drawRoundRect(eastCorridor, 8f, 8f, corridorBorderPaint);
    }

    private void drawDepartmentBlock(Canvas canvas, HospitalNode node, int w, int h) {
        float cx = node.getX() * w;
        float cy = node.getY() * h;
        float rw = w * 0.30f;
        float rh = h * 0.14f;

        RectF rect = new RectF(cx - rw / 2f, cy - rh / 2f, cx + rw / 2f, cy + rh / 2f);

        // Google Maps pastel category color mapping (adaptive for night mode)
        int fillColor;
        int strokeColor;
        String type = node.getDepartmentType();
        boolean dark = isNightMode();

        if ("EMERGENCY".equalsIgnoreCase(type)) {
            fillColor = Color.parseColor(dark ? "#3D1E1E" : "#FCE8E6");
            strokeColor = Color.parseColor("#EA4335");
        } else if ("PHARMACY".equalsIgnoreCase(type)) {
            fillColor = Color.parseColor(dark ? "#193524" : "#E6F4EA");
            strokeColor = Color.parseColor("#34A853");
        } else if ("LABORATORY".equalsIgnoreCase(type)) {
            fillColor = Color.parseColor(dark ? "#1B2C45" : "#E8F0FE");
            strokeColor = Color.parseColor("#4285F4");
        } else if ("RADIOLOGY".equalsIgnoreCase(type)) {
            fillColor = Color.parseColor(dark ? "#382B13" : "#FEF7E0");
            strokeColor = Color.parseColor("#FBBC04");
        } else if ("ICU".equalsIgnoreCase(type) || "OT".equalsIgnoreCase(type)) {
            fillColor = Color.parseColor(dark ? "#2E1C3F" : "#F3E8FD");
            strokeColor = Color.parseColor("#9334E6");
        } else if ("WARD".equalsIgnoreCase(type)) {
            fillColor = Color.parseColor(dark ? "#152E3D" : "#E0F2FE");
            strokeColor = Color.parseColor("#0284C7");
        } else if ("ELEVATOR".equalsIgnoreCase(type) || "STAIRS".equalsIgnoreCase(type)) {
            fillColor = Color.parseColor(dark ? "#243128" : "#F1F3F4");
            strokeColor = Color.parseColor("#1A73E8");
        } else {
            fillColor = dark ? Color.parseColor("#1B241E") : Color.WHITE;
            strokeColor = Color.parseColor("#9AA0A6");
        }

        // Room Card Shadow & Fill
        roomFillPaint.setColor(fillColor);
        canvas.drawRoundRect(rect, 16f, 16f, roomFillPaint);

        roomStrokePaint.setColor(strokeColor);
        canvas.drawRoundRect(rect, 16f, 16f, roomStrokePaint);

        // Room Text
        String name = node.getName();
        if (name.contains("(")) {
            String primary = name.substring(0, name.indexOf("(")).trim();
            String secondary = name.substring(name.indexOf("(")).trim();
            canvas.drawText(primary, cx, cy - 4f, textPaint);
            canvas.drawText(secondary, cx, cy + 24f, subTextPaint);
        } else {
            canvas.drawText(name, cx, cy + 8f, textPaint);
        }
    }

    private void drawRoutePolyline(Canvas canvas, List<HospitalNode> path, int w, int h) {
        if (path == null || path.isEmpty()) return;

        if (!isNavigating) {
            Path polyline = new Path();
            boolean first = true;
            for (HospitalNode node : path) {
                float x = node.getX() * w;
                float y = node.getY() * h;
                if (first) {
                    polyline.moveTo(x, y);
                    first = false;
                } else {
                    polyline.lineTo(x, y);
                }
            }
            // 1. Draw glowing polyline
            canvas.drawPath(polyline, routePolylineGlowPaint);
            // 2. Draw crisp Google Blue route line
            canvas.drawPath(polyline, routePolylinePaint);
        } else {
            // Traversed portion (dim grey)
            if (currentStepIndex > 0) {
                Path traversedPath = new Path();
                boolean first = true;
                int endIdx = Math.min(currentStepIndex, path.size() - 1);
                for (int i = 0; i <= endIdx; i++) {
                    HospitalNode node = path.get(i);
                    float x = node.getX() * w;
                    float y = node.getY() * h;
                    if (first) {
                        traversedPath.moveTo(x, y);
                        first = false;
                    } else {
                        traversedPath.lineTo(x, y);
                    }
                }
                Paint traversedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                traversedPaint.setColor(Color.parseColor("#BDC1C6"));
                traversedPaint.setStyle(Paint.Style.STROKE);
                traversedPaint.setStrokeWidth(8f);
                canvas.drawPath(traversedPath, traversedPaint);
            }

            // Remaining active portion (glowing Google blue)
            if (currentStepIndex < path.size()) {
                Path remainingPath = new Path();
                boolean first = true;
                for (int i = currentStepIndex; i < path.size(); i++) {
                    HospitalNode node = path.get(i);
                    float x = node.getX() * w;
                    float y = node.getY() * h;
                    if (first) {
                        remainingPath.moveTo(x, y);
                        first = false;
                    } else {
                        remainingPath.lineTo(x, y);
                    }
                }
                canvas.drawPath(remainingPath, routePolylineGlowPaint);
                canvas.drawPath(remainingPath, routePolylinePaint);
            }
        }

        // Draw route step waypoints (Google circle dots)
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.WHITE);
        dotPaint.setStyle(Paint.Style.FILL);

        Paint dotStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotStroke.setStyle(Paint.Style.STROKE);
        dotStroke.setStrokeWidth(3f);

        for (int i = 0; i < path.size(); i++) {
            HospitalNode node = path.get(i);
            float x = node.getX() * w;
            float y = node.getY() * h;
            if (isNavigating && i < currentStepIndex) {
                dotStroke.setColor(Color.parseColor("#BDC1C6"));
            } else {
                dotStroke.setColor(Color.parseColor("#1A73E8"));
            }
            canvas.drawCircle(x, y, 7f, dotPaint);
            canvas.drawCircle(x, y, 7f, dotStroke);
        }
    }

    private void drawMarkers(Canvas canvas, int w, int h) {
        // Start Marker (Google Location Dot "A")
        if (startNode != null && (startNode.getFloor().equalsIgnoreCase(currentFloor) || currentRoute != null)) {
            float sx = startNode.getX() * w;
            float sy = startNode.getY() * h;

            markerFillPaint.setColor(Color.parseColor("#1A73E8")); // Google Blue
            canvas.drawCircle(sx, sy, 18f, markerFillPaint);
            canvas.drawCircle(sx, sy, 18f, markerStrokePaint);

            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE);
            p.setTextSize(20f);
            p.setFakeBoldText(true);
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("A", sx, sy + 7f, p);
        }

        // Destination Marker (Google Maps Red Teardrop Pin "B")
        if (destNode != null && (destNode.getFloor().equalsIgnoreCase(currentFloor) || currentRoute != null)) {
            float dx = destNode.getX() * w;
            float dy = destNode.getY() * h;

            // Pin shadow
            Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            shadowPaint.setColor(Color.parseColor("#40000000"));
            canvas.drawOval(new RectF(dx - 12f, dy + 18f, dx + 12f, dy + 26f), shadowPaint);

            // Teardrop path
            Path pinPath = new Path();
            pinPath.moveTo(dx, dy + 20f); // bottom point
            pinPath.cubicTo(dx - 18f, dy + 5f, dx - 18f, dy - 15f, dx, dy - 15f);
            pinPath.cubicTo(dx + 18f, dy - 15f, dx + 18f, dy + 5f, dx, dy + 20f);
            pinPath.close();

            markerFillPaint.setColor(Color.parseColor("#EA4335")); // Google Maps Pin Red
            canvas.drawPath(pinPath, markerFillPaint);

            // White center circle dot
            Paint whiteDot = new Paint(Paint.ANTI_ALIAS_FLAG);
            whiteDot.setColor(Color.WHITE);
            canvas.drawCircle(dx, dy - 4f, 7f, whiteDot);
        }

        // Live Navigation Active GPS Location Marker
        if (isNavigating && currentRoute != null && currentRoute.getPathNodes() != null
                && currentStepIndex >= 0 && currentStepIndex < currentRoute.getPathNodes().size()) {
            HospitalNode activeNode = currentRoute.getPathNodes().get(currentStepIndex);
            if (activeNode.getFloor().equalsIgnoreCase(currentFloor)) {
                float nx = activeNode.getX() * w;
                float ny = activeNode.getY() * h;

                // 1. Semi-transparent glowing radar aura
                Paint auraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                auraPaint.setColor(Color.parseColor("#331A73E8"));
                canvas.drawCircle(nx, ny, 32f, auraPaint);

                // 2. White outer border ring
                Paint whiteRing = new Paint(Paint.ANTI_ALIAS_FLAG);
                whiteRing.setColor(Color.WHITE);
                whiteRing.setStyle(Paint.Style.FILL);
                canvas.drawCircle(nx, ny, 16f, whiteRing);

                // 3. Vibrant Google Blue center dot
                Paint blueCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
                blueCenter.setColor(Color.parseColor("#1A73E8"));
                blueCenter.setStyle(Paint.Style.FILL);
                canvas.drawCircle(nx, ny, 11f, blueCenter);

                // 4. Directional heading cone if there is a next node
                if (currentStepIndex + 1 < currentRoute.getPathNodes().size()) {
                    HospitalNode nextNode = currentRoute.getPathNodes().get(currentStepIndex + 1);
                    float targetX = nextNode.getX() * w;
                    float targetY = nextNode.getY() * h;
                    double angle = Math.atan2(targetY - ny, targetX - nx);

                    float tipX = nx + (float) Math.cos(angle) * 26f;
                    float tipY = ny + (float) Math.sin(angle) * 26f;
                    float leftX = nx + (float) Math.cos(angle + Math.toRadians(140)) * 14f;
                    float leftY = ny + (float) Math.sin(angle + Math.toRadians(140)) * 14f;
                    float rightX = nx + (float) Math.cos(angle - Math.toRadians(140)) * 14f;
                    float rightY = ny + (float) Math.sin(angle - Math.toRadians(140)) * 14f;

                    Path arrowPath = new Path();
                    arrowPath.moveTo(tipX, tipY);
                    arrowPath.lineTo(leftX, leftY);
                    arrowPath.lineTo(nx, ny);
                    arrowPath.lineTo(rightX, rightY);
                    arrowPath.close();

                    Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    arrowPaint.setColor(Color.parseColor("#1A73E8"));
                    arrowPaint.setStyle(Paint.Style.FILL);
                    canvas.drawPath(arrowPath, arrowPaint);
                }

                // 5. Floating "YOU" badge above marker
                Paint badgeBg = new Paint(Paint.ANTI_ALIAS_FLAG);
                badgeBg.setColor(Color.parseColor("#188038"));
                RectF badgeRect = new RectF(nx - 36f, ny - 48f, nx + 36f, ny - 24f);
                canvas.drawRoundRect(badgeRect, 10f, 10f, badgeBg);

                Paint badgeText = new Paint(Paint.ANTI_ALIAS_FLAG);
                badgeText.setColor(Color.WHITE);
                badgeText.setTextSize(14f);
                badgeText.setFakeBoldText(true);
                badgeText.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("YOU", nx, ny - 31f, badgeText);
            }
        }
    }

    private void drawGoogleMapsWatermark(Canvas canvas, int w, int h) {
        Paint wmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wmPaint.setColor(Color.parseColor("#70757A"));
        wmPaint.setTextSize(22f);
        wmPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Google Maps • Hospital Indoor View", 24f, h - 24f, wmPaint);

        // Scale bar
        Paint scalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scalePaint.setColor(Color.parseColor("#70757A"));
        scalePaint.setStrokeWidth(3f);
        canvas.drawLine(w - 140f, h - 24f, w - 30f, h - 24f, scalePaint);
        canvas.drawLine(w - 140f, h - 32f, w - 140f, h - 24f, scalePaint);
        canvas.drawLine(w - 30f, h - 32f, w - 30f, h - 24f, scalePaint);

        wmPaint.setTextAlign(Paint.Align.RIGHT);
        wmPaint.setTextSize(18f);
        canvas.drawText("20 m", w - 40f, h - 34f, wmPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float touchX = event.getX();
            float touchY = event.getY();
            int w = getWidth();
            int h = getHeight();

            if (w > 0 && h > 0) {
                for (HospitalNode node : displayedFloorNodes) {
                    float cx = node.getX() * w;
                    float cy = node.getY() * h;
                    float rw = w * 0.30f;
                    float rh = h * 0.14f;
                    RectF bounds = new RectF(cx - rw / 2f, cy - rh / 2f, cx + rw / 2f, cy + rh / 2f);
                    if (bounds.contains(touchX, touchY)) {
                        if (nodeClickListener != null) {
                            nodeClickListener.onNodeClicked(node);
                        }
                        return true;
                    }
                }
            }
        }
        return true;
    }
}
