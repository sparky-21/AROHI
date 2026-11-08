package com.arohi.healthcare.utils;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Intelligent Indoor Route Planner for AROHI Hospital.
 * Computes optimal pedestrian paths using Dijkstra's Algorithm
 * and Bellman-Ford Shortest Path Algorithm.
 */
public class HospitalRoutePlanner {

    public static class HospitalNode implements Serializable {
        private final String id;
        private final String name;
        private final String floor;
        private final float x; // normalized 0.0 to 1.0 on floor plan canvas
        private final float y;
        private final String departmentType;

        public HospitalNode(String id, String name, String floor, float x, float y, String departmentType) {
            this.id = id;
            this.name = name;
            this.floor = floor;
            this.x = x;
            this.y = y;
            this.departmentType = departmentType;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getFloor() { return floor; }
        public float getX() { return x; }
        public float getY() { return y; }
        public String getDepartmentType() { return departmentType; }

        @NonNull
        @Override
        public String toString() {
            return name + " (" + floor + ")";
        }
    }

    public static class Edge {
        private final String targetNodeId;
        private final double distanceMeters;
        private final String transitInstruction;

        public Edge(String targetNodeId, double distanceMeters, String transitInstruction) {
            this.targetNodeId = targetNodeId;
            this.distanceMeters = distanceMeters;
            this.transitInstruction = transitInstruction;
        }

        public String getTargetNodeId() { return targetNodeId; }
        public double getDistanceMeters() { return distanceMeters; }
        public String getTransitInstruction() { return transitInstruction; }
    }

    public static class RouteResult implements Serializable {
        private final String algorithmUsed;
        private final double totalDistanceMeters;
        private final int estimatedWalkingSeconds;
        private final List<HospitalNode> pathNodes;
        private final List<String> turnByTurnSteps;
        private final List<Double> stepDistances;

        public RouteResult(String algorithmUsed, double totalDistanceMeters,
                           int estimatedWalkingSeconds, List<HospitalNode> pathNodes,
                           List<String> turnByTurnSteps) {
            this(algorithmUsed, totalDistanceMeters, estimatedWalkingSeconds, pathNodes, turnByTurnSteps, new ArrayList<>());
        }

        public RouteResult(String algorithmUsed, double totalDistanceMeters,
                           int estimatedWalkingSeconds, List<HospitalNode> pathNodes,
                           List<String> turnByTurnSteps, List<Double> stepDistances) {
            this.algorithmUsed = algorithmUsed;
            this.totalDistanceMeters = totalDistanceMeters;
            this.estimatedWalkingSeconds = estimatedWalkingSeconds;
            this.pathNodes = pathNodes != null ? pathNodes : new ArrayList<>();
            this.turnByTurnSteps = turnByTurnSteps != null ? turnByTurnSteps : new ArrayList<>();
            this.stepDistances = stepDistances != null ? stepDistances : new ArrayList<>();
        }

        public String getAlgorithmUsed() { return algorithmUsed; }
        public double getTotalDistanceMeters() { return totalDistanceMeters; }
        public int getEstimatedWalkingSeconds() { return estimatedWalkingSeconds; }
        public List<HospitalNode> getPathNodes() { return pathNodes; }
        public List<String> getTurnByTurnSteps() { return turnByTurnSteps; }
        public List<Double> getStepDistances() { return stepDistances != null ? stepDistances : Collections.emptyList(); }

        public String getDistanceFormatted() {
            return String.format(Locale.ROOT, "%.0f meters", totalDistanceMeters);
        }

        public String getTimeFormatted() {
            int mins = Math.max(1, (int) Math.ceil(estimatedWalkingSeconds / 60.0));
            return mins + " min walk";
        }
    }

    private final Map<String, HospitalNode> nodes = new HashMap<>();
    private final Map<String, List<Edge>> adjacencyList = new HashMap<>();

    public HospitalRoutePlanner() {
        initializeHospitalGraph();
    }

    public Map<String, HospitalNode> getAllNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public List<HospitalNode> getNodesForFloor(String floor) {
        List<HospitalNode> list = new ArrayList<>();
        for (HospitalNode node : nodes.values()) {
            if (node.getFloor().equalsIgnoreCase(floor)) {
                list.add(node);
            }
        }
        return list;
    }

    private void addNode(HospitalNode node) {
        nodes.put(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
    }

    private void addBiDirectionalEdge(String u, String v, double distanceMeters, String instruction) {
        adjacencyList.get(u).add(new Edge(v, distanceMeters, instruction));
        adjacencyList.get(v).add(new Edge(u, distanceMeters, "Head back towards " + nodes.get(u).getName()));
    }

    private void initializeHospitalGraph() {
        // GROUND FLOOR NODES
        addNode(new HospitalNode("ENTRANCE", "Hospital Main Entrance", "Ground Floor", 0.50f, 0.90f, "ENTRANCE"));
        addNode(new HospitalNode("RECEPTION", "Main Reception & Lobby", "Ground Floor", 0.50f, 0.65f, "RECEPTION"));
        addNode(new HospitalNode("EMERGENCY", "Emergency & Trauma Bay", "Ground Floor", 0.18f, 0.65f, "EMERGENCY"));
        addNode(new HospitalNode("PHARMACY", "Central Smart Pharmacy", "Ground Floor", 0.82f, 0.65f, "PHARMACY"));
        addNode(new HospitalNode("RADIOLOGY", "Radiology (X-Ray, CT, MRI)", "Ground Floor", 0.50f, 0.35f, "RADIOLOGY"));
        addNode(new HospitalNode("ELEVATOR_G", "Elevator Bank A (Ground)", "Ground Floor", 0.65f, 0.50f, "ELEVATOR"));
        addNode(new HospitalNode("STAIRS_G", "Main Stairs (Ground)", "Ground Floor", 0.35f, 0.50f, "STAIRS"));

        // FLOOR 1 NODES
        addNode(new HospitalNode("ELEVATOR_F1", "Elevator Bank A (Floor 1)", "Floor 1", 0.65f, 0.50f, "ELEVATOR"));
        addNode(new HospitalNode("STAIRS_F1", "Main Stairs (Floor 1)", "Floor 1", 0.35f, 0.50f, "STAIRS"));
        addNode(new HospitalNode("LABORATORY", "Pathology & Blood Lab", "Floor 1", 0.82f, 0.50f, "LABORATORY"));
        addNode(new HospitalNode("OPD_CLINIC", "Specialist OPD Chambers", "Floor 1", 0.18f, 0.50f, "RECEPTION"));
        addNode(new HospitalNode("CARDIOLOGY", "Cardiology Consultation", "Floor 1", 0.50f, 0.25f, "RECEPTION"));

        // FLOOR 2 NODES
        addNode(new HospitalNode("ELEVATOR_F2", "Elevator Bank A (Floor 2)", "Floor 2", 0.65f, 0.50f, "ELEVATOR"));
        addNode(new HospitalNode("STAIRS_F2", "Main Stairs (Floor 2)", "Floor 2", 0.35f, 0.50f, "STAIRS"));
        addNode(new HospitalNode("WARD_AB", "Inpatient Medical Ward (A & B)", "Floor 2", 0.82f, 0.50f, "WARD"));
        addNode(new HospitalNode("WARD_CD", "Inpatient Surgical Ward (C & D)", "Floor 2", 0.18f, 0.50f, "WARD"));
        addNode(new HospitalNode("NURSES_STATION", "Floor 2 Nurses' Station", "Floor 2", 0.50f, 0.50f, "RECEPTION"));

        // FLOOR 3 NODES
        addNode(new HospitalNode("ELEVATOR_F3", "Elevator Bank A (Floor 3)", "Floor 3", 0.65f, 0.50f, "ELEVATOR"));
        addNode(new HospitalNode("STAIRS_F3", "Main Stairs (Floor 3)", "Floor 3", 0.35f, 0.50f, "STAIRS"));
        addNode(new HospitalNode("ICU", "Intensive Care Unit (ICU/CCU)", "Floor 3", 0.50f, 0.20f, "ICU"));
        addNode(new HospitalNode("OT", "Operation Theatre Complex (OT)", "Floor 3", 0.50f, 0.80f, "OT"));

        // BASEMENT NODES
        addNode(new HospitalNode("ELEVATOR_B", "Elevator Bank A (Basement)", "Basement", 0.65f, 0.50f, "ELEVATOR"));
        addNode(new HospitalNode("BASEMENT_CSSD", "CSSD & Biomedical Stores", "Basement", 0.50f, 0.50f, "RECEPTION"));

        // GROUND FLOOR EDGES
        addBiDirectionalEdge("ENTRANCE", "RECEPTION", 20.0, "Walk straight ahead 20m from glass entrance into Central Atrium.");
        addBiDirectionalEdge("RECEPTION", "EMERGENCY", 30.0, "Turn left and walk west 30m down the emergency corridor.");
        addBiDirectionalEdge("RECEPTION", "PHARMACY", 30.0, "Turn right and walk east 30m past the pharmacy counter.");
        addBiDirectionalEdge("RECEPTION", "ELEVATOR_G", 18.0, "Proceed north-east 18m to Elevator Bank A.");
        addBiDirectionalEdge("RECEPTION", "STAIRS_G", 18.0, "Proceed north-west 18m to Main Stairs.");
        addBiDirectionalEdge("ELEVATOR_G", "RADIOLOGY", 25.0, "Walk north 25m into the Diagnostic Radiology wing.");
        addBiDirectionalEdge("STAIRS_G", "RADIOLOGY", 25.0, "Walk north 25m into the Diagnostic Radiology wing.");

        // VERTICAL TRANSIT (Elevator & Stairs connecting all floors)
        addBiDirectionalEdge("ELEVATOR_G", "ELEVATOR_F1", 12.0, "Take Elevator Bank A up to Floor 1.");
        addBiDirectionalEdge("ELEVATOR_F1", "ELEVATOR_F2", 12.0, "Take Elevator Bank A up to Floor 2.");
        addBiDirectionalEdge("ELEVATOR_F2", "ELEVATOR_F3", 12.0, "Take Elevator Bank A up to Floor 3 (Sterile Surgical Zone).");
        addBiDirectionalEdge("ELEVATOR_G", "ELEVATOR_B", 12.0, "Take Elevator Bank A down to Basement.");

        addBiDirectionalEdge("STAIRS_G", "STAIRS_F1", 15.0, "Take Main Stairs up to Floor 1.");
        addBiDirectionalEdge("STAIRS_F1", "STAIRS_F2", 15.0, "Take Main Stairs up to Floor 2.");
        addBiDirectionalEdge("STAIRS_F2", "STAIRS_F3", 15.0, "Take Main Stairs up to Floor 3.");

        // FLOOR 1 EDGES
        addBiDirectionalEdge("ELEVATOR_F1", "LABORATORY", 20.0, "Exit Elevator, turn right into East Wing to Pathology Lab.");
        addBiDirectionalEdge("STAIRS_F1", "OPD_CLINIC", 20.0, "Exit Stairs, turn left into West Wing Specialist OPD Chambers.");
        addBiDirectionalEdge("ELEVATOR_F1", "CARDIOLOGY", 25.0, "Walk north past the waiting lounge into Cardiology OPD.");
        addBiDirectionalEdge("STAIRS_F1", "CARDIOLOGY", 25.0, "Walk north past the waiting lounge into Cardiology OPD.");
        addBiDirectionalEdge("LABORATORY", "CARDIOLOGY", 30.0, "Walk along connecting corridor to Cardiology.");
        addBiDirectionalEdge("OPD_CLINIC", "CARDIOLOGY", 30.0, "Walk along connecting corridor to Cardiology.");

        // FLOOR 2 EDGES (Inpatient Wards)
        addBiDirectionalEdge("ELEVATOR_F2", "NURSES_STATION", 15.0, "Exit elevator to Central Nurses' Duty Station.");
        addBiDirectionalEdge("STAIRS_F2", "NURSES_STATION", 15.0, "Exit stairs to Central Nurses' Duty Station.");
        addBiDirectionalEdge("NURSES_STATION", "WARD_AB", 25.0, "Turn right into East Wing Medical Ward (Ward A & B).");
        addBiDirectionalEdge("NURSES_STATION", "WARD_CD", 25.0, "Turn left into West Wing Surgical Ward (Ward C & D).");

        // FLOOR 3 EDGES (Critical Care & Surgery)
        addBiDirectionalEdge("ELEVATOR_F3", "ICU", 30.0, "Follow sterile corridor north 30m to ICU/CCU Entrance.");
        addBiDirectionalEdge("STAIRS_F3", "ICU", 30.0, "Follow sterile corridor north 30m to ICU/CCU Entrance.");
        addBiDirectionalEdge("ELEVATOR_F3", "OT", 25.0, "Head south 25m towards Operation Theatre complex.");
        addBiDirectionalEdge("STAIRS_F3", "OT", 25.0, "Head south 25m towards Operation Theatre complex.");

        // BASEMENT EDGES
        addBiDirectionalEdge("ELEVATOR_B", "BASEMENT_CSSD", 20.0, "Exit elevator into Central Sterile Supply department.");
    }

    // =========================================================================
    // 1. DIJKSTRA'S SHORTEST PATH ALGORITHM
    // =========================================================================

    public RouteResult findShortestPathDijkstra(String startId, String destinationId) {
        if (!nodes.containsKey(startId) || !nodes.containsKey(destinationId)) {
            return null;
        }

        Map<String, Double> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        Map<String, String> stepInstructions = new HashMap<>();
        Map<String, Double> stepDistMap = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        for (String id : nodes.keySet()) {
            distances.put(id, Double.MAX_VALUE);
        }
        distances.put(startId, 0.0);
        pq.add(startId);

        Set<String> visited = new HashSet<>();

        while (!pq.isEmpty()) {
            String current = pq.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            if (current.equals(destinationId)) break;

            double currentDist = distances.get(current);

            List<Edge> edges = adjacencyList.getOrDefault(current, Collections.emptyList());
            for (Edge edge : edges) {
                String neighbor = edge.getTargetNodeId();
                if (visited.contains(neighbor)) continue;

                double newDist = currentDist + edge.getDistanceMeters();
                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    predecessors.put(neighbor, current);
                    stepInstructions.put(neighbor, edge.getTransitInstruction());
                    stepDistMap.put(neighbor, edge.getDistanceMeters());
                    pq.remove(neighbor);
                    pq.add(neighbor);
                }
            }
        }

        if (distances.get(destinationId) == Double.MAX_VALUE) {
            return null; // No path found
        }

        // Reconstruct path
        List<HospitalNode> path = new ArrayList<>();
        List<String> instructions = new ArrayList<>();
        List<Double> stepDists = new ArrayList<>();

        String curr = destinationId;
        while (curr != null) {
            path.add(0, nodes.get(curr));
            String step = stepInstructions.get(curr);
            if (step != null) {
                instructions.add(0, step);
                stepDists.add(0, stepDistMap.getOrDefault(curr, 0.0));
            }
            curr = predecessors.get(curr);
        }

        double totalDist = distances.get(destinationId);
        int walkSeconds = (int) Math.round(totalDist / 1.2); // avg pedestrian walking pace 1.2 m/s

        return new RouteResult("Fastest walking route",
                totalDist, walkSeconds, path, instructions, stepDists);
    }

    // =========================================================================
    // 2. BELLMAN-FORD SHORTEST PATH ALGORITHM
    // =========================================================================

    public RouteResult findShortestPathBellmanFord(String startId, String destinationId) {
        if (!nodes.containsKey(startId) || !nodes.containsKey(destinationId)) {
            return null;
        }

        Map<String, Double> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        Map<String, String> stepInstructions = new HashMap<>();
        Map<String, Double> stepDistMap = new HashMap<>();

        for (String id : nodes.keySet()) {
            distances.put(id, Double.MAX_VALUE);
        }
        distances.put(startId, 0.0);

        // All edges list
        List<AllEdge> allEdges = new ArrayList<>();
        for (Map.Entry<String, List<Edge>> entry : adjacencyList.entrySet()) {
            String u = entry.getKey();
            for (Edge e : entry.getValue()) {
                allEdges.add(new AllEdge(u, e.getTargetNodeId(), e.getDistanceMeters(), e.getTransitInstruction()));
            }
        }

        int numVertices = nodes.size();

        // Relax edges |V| - 1 times
        for (int i = 1; i < numVertices; i++) {
            boolean anyChange = false;
            for (AllEdge edge : allEdges) {
                double uDist = distances.get(edge.u);
                if (uDist != Double.MAX_VALUE) {
                    double vDist = distances.get(edge.v);
                    if (uDist + edge.weight < vDist) {
                        distances.put(edge.v, uDist + edge.weight);
                        predecessors.put(edge.v, edge.u);
                        stepInstructions.put(edge.v, edge.instruction);
                        stepDistMap.put(edge.v, edge.weight);
                        anyChange = true;
                    }
                }
            }
            if (!anyChange) break;
        }

        if (distances.get(destinationId) == Double.MAX_VALUE) {
            return null;
        }

        List<HospitalNode> path = new ArrayList<>();
        List<String> instructions = new ArrayList<>();
        List<Double> stepDists = new ArrayList<>();
        String curr = destinationId;
        while (curr != null) {
            path.add(0, nodes.get(curr));
            String step = stepInstructions.get(curr);
            if (step != null) {
                instructions.add(0, step);
                stepDists.add(0, stepDistMap.getOrDefault(curr, 0.0));
            }
            curr = predecessors.get(curr);
        }

        double totalDist = distances.get(destinationId);
        int walkSeconds = (int) Math.round(totalDist / 1.2);

        return new RouteResult("Fastest walking route",
                totalDist, walkSeconds, path, instructions, stepDists);
    }

    private static class AllEdge {
        final String u;
        final String v;
        final double weight;
        final String instruction;

        AllEdge(String u, String v, double weight, String instruction) {
            this.u = u;
            this.v = v;
            this.weight = weight;
            this.instruction = instruction;
        }
    }
}
