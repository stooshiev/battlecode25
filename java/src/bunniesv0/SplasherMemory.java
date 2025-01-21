package bunniesv0;
import battlecode.common.*;
import java.util.HashSet;

public class SplasherMemory {
    static HashSet<RobotInfo> friendlyPaintTowers = new HashSet<>();
    public static void updateRobotMemory(RobotController rc, RobotInfo[] robotInfos) {
        // check if any paint towers dissapeared
        MapLocation rcLoc = rc.getLocation();
        // a paint tower has dissapeared if it is within sqrt20 but it cannot be senesed
        friendlyPaintTowers.removeIf(friendlyPaintTower ->
                rcLoc.distanceSquaredTo(friendlyPaintTower.getLocation()) <= 20 &&
                        !rc.canSenseRobot(friendlyPaintTower.ID));
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
            if (distance < nearestDistance) {
                nearest = paintTower.getLocation();
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
