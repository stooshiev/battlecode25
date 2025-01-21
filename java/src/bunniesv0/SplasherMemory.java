package bunniesv0;
import battlecode.common.*;
import java.util.HashSet;
import java.util.HashMap;

public class SplasherMemory {
    static final int COOLDOWN = 50;
    // set of RobotInfo for ally paint towers
    static HashSet<RobotInfo> friendlyPaintTowers = new HashSet<>();
    // ID : round rejected for paint towers that deny paint
    static HashMap<Integer, Integer> unfriendlyPaintTowers = new HashMap<>();
    public static void updateRobotMemory(RobotController rc, RobotInfo[] robotInfos) {
        // check if any paint towers disappeared
        MapLocation rcLoc = rc.getLocation();
        // a paint tower has disappeared if it is within sqrt20 but it cannot be senesed
        friendlyPaintTowers.removeIf(friendlyPaintTower ->
                rcLoc.distanceSquaredTo(friendlyPaintTower.getLocation()) <= 20 &&
                        !rc.canSenseRobot(friendlyPaintTower.ID));
        // forgive paint towers that denied paint after COOLDOWN rounds
        unfriendlyPaintTowers.entrySet().removeIf(entry -> entry.getValue() + 50 <= COOLDOWN);
        for (RobotInfo robot : robotInfos) {
            if (robot.getType().ordinal() / 3 == 1 && robot.getTeam() == rc.getTeam()) {
                // friendly paint tower
                friendlyPaintTowers.add(robot);
            }
        }
    }
    public static MapLocation getNearestFriendlyPaintTower(MapLocation from) {
        MapLocation nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (RobotInfo paintTower : friendlyPaintTowers) {
            int distance = from.distanceSquaredTo(paintTower.getLocation());
            if (distance < nearestDistance && !unfriendlyPaintTowers.containsKey(paintTower.ID)) {
                nearest = paintTower.getLocation();
                nearestDistance = distance;
            }
        }
        return nearest;
    }
    public static void addRejectedTower(int rejectingTowerID, int roundNum) {
        unfriendlyPaintTowers.put(rejectingTowerID, roundNum);
    }
}
