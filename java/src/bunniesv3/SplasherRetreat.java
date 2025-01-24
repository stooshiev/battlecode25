package bunniesv3;

import battlecode.common.*;

public class SplasherRetreat {
    public static OrbitPathfinder connectedTileNavigator = null;

    public static void retreat(RobotController rc) {
        MapLocation rcLoc = rc.getLocation();
        SplasherMemory.updateRobotMemory(rc, rc.senseNearbyRobots());
        if (RobotPlayer.navigator == null && connectedTileNavigator == null) {
            // remember where the closest paint tower is
            MapLocation nearestPaintTower = SplasherMemory.getNearestFriendlyPaintTower(rc.getLocation());
            if (nearestPaintTower != null) {
                RobotPlayer.navigator = new OrbitPathfinder(rc, nearestPaintTower);
            } else {
                // if no tower found, continue normal movement, look for tower
                if (rc.isMovementReady()) {
                    if (RobotPlayer.splasherDirection == null) {
                        RobotPlayer.splasherDirection = RobotPlayer.directions[
                                RobotPlayer.rng.nextInt(RobotPlayer.directions.length)];
                    }
                    if (rc.canMove(RobotPlayer.splasherDirection)) {
                        try {
                            rc.move(RobotPlayer.splasherDirection);
                            RobotPlayer.path.addLast(rc.getLocation());
                        } catch (GameActionException ignored) {}
                    } else {
                        RobotPlayer.splasherDirection = null;
                    }
                    SplasherMemory.updateRobotMemory(rc, rc.senseNearbyRobots());
                }
            }
        }
        MapLocation nearestPaintTower = SplasherMemory.getNearestFriendlyPaintTower(rcLoc);
        int closestTowerDistance = rcLoc.distanceSquaredTo(nearestPaintTower);
        if (closestTowerDistance <= 2 && connectedTileNavigator == null) {
            try {
                if (!rc.senseMapInfo(rcLoc).getPaint().isAlly()) {
                    // if the robot is on non-ally paint, it cannot communicate with the tower
                    // find connected paint and go there
                    for (Direction dir : Constants.directions) {
                        if (rc.senseMapInfo(nearestPaintTower.add(dir)).getPaint().isAlly()) {
                            // found a connected tile
                            connectedTileNavigator = new OrbitPathfinder(rc, nearestPaintTower.add(dir));
                            break;
                        }
                    }
                    if (connectedTileNavigator == null) {
                        // if we haven't found a connecting tile, it is an unfriendly tower
                        SplasherMemory.addRejectedTower(rc.senseRobotAtLocation(nearestPaintTower).ID, rc.getRoundNum());
                    }
                    return;
                } else { // if message can be sent:
                    UnpackedMessage.encodeAndSend(rc, nearestPaintTower, UnpackedMessage.REQUEST_PAINT, rc.getLocation());
                }
            } catch (GameActionException ignored) { }
            // stay put and wait reply
            return;
        }
        if (connectedTileNavigator != null) {
            connectedTileNavigator.step();
            if (rc.getLocation().equals(connectedTileNavigator.getDest())) {
                connectedTileNavigator = null;
            }
        }
        else if (RobotPlayer.navigator != null) {
            if (!nearestPaintTower.equals(RobotPlayer.navigator.getDest())) {
                // if the closest tower is different than before
                RobotPlayer.navigator = new OrbitPathfinder(rc, nearestPaintTower);
            }
            RobotPlayer.navigator.step();
        }
    }
}
