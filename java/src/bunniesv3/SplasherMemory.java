package bunniesv3;
import battlecode.common.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Objects;

public class SplasherMemory {
    static final int COOLDOWN = 50;
    static final int[][] radius3 = new int[][] {
            {-3, 0},
            {-2, -2}, {-2, -1}, {-2, 0}, {-2, 1}, {-2, 2},
            {-1, -2}, {-1, -1}, {-1, 0}, {-1, 1}, {-1, 2},
            {0, -3}, {0, -2}, {0, -1}, {0, 0}, {0, 1}, {0, 2}, {0, 3},
            {1, -2}, {1, -1}, {1, 0}, {1, 1}, {1, 2},
            {2, -2}, {2, -1}, {2, 0}, {2, 1}, {2, 2},
            {3, 0}
    };
    // set of RobotInfo for ally paint towers
    static HashSet<RobotInfo> friendlyPaintTowers = new HashSet<>();
    static HashSet<MapLocation> enemyTowers = new HashSet<>();
//    static HashSet<MapLocation> attackSquares = new HashSet<>();
    // ID : round rejected for paint towers that deny paint
    static HashMap<Integer, Integer> unfriendlyPaintTowers = new HashMap<>();
    public static void updateRobotMemory(RobotController rc, RobotInfo[] robotInfos) {
        // check if any paint towers disappeared
        MapLocation rcLoc = rc.getLocation();
        // a paint tower has disappeared if it is within sqrt20 but it cannot be senesed
        friendlyPaintTowers.removeIf(friendlyPaintTower ->
                rcLoc.distanceSquaredTo(friendlyPaintTower.getLocation()) <= 20 &&
                        !rc.canSenseRobot(friendlyPaintTower.ID));
        for (MapLocation enemyTowerLoc : enemyTowers) {
            if (rcLoc.distanceSquaredTo(enemyTowerLoc) <= 20 &&
                    Arrays.stream(robotInfos).noneMatch(
                            robot -> robot.getLocation().equals(enemyTowerLoc))) {
                // if we are close to an enemy tower, but we can't sense it,
                // that means that the enemy tower is gone
                enemyTowers.remove(enemyTowerLoc);
                // also remove the corresponding danger tiles
//                for (int[] attackOffset : radius3) {
//                    attackSquares.remove(new MapLocation(
//                            enemyTowerLoc.x + attackOffset[0], enemyTowerLoc.y + attackOffset[1]));
//                }
            }
        }
        // forgive paint towers that denied paint after COOLDOWN rounds
        unfriendlyPaintTowers.entrySet().removeIf(entry -> entry.getValue() + 50 <= COOLDOWN);
        for (RobotInfo robot : robotInfos) {
            if (robot.getType().ordinal() / 3 == 1 && robot.getTeam() == rc.getTeam()) {
                // friendly paint tower
                friendlyPaintTowers.add(robot);
            } else if (robot.getType().isTowerType() && robot.getTeam() != RobotPlayer.team) {
                enemyTowers.add(robot.getLocation());
//                for (int[] attackOffset : radius3) {
//                    attackSquares.add(new MapLocation(
//                            robot.getLocation().x + attackOffset[0], robot.getLocation().y + attackOffset[1]));
//                }
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
