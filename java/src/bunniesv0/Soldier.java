package bunniesv0;

import battlecode.common.*;

public class Soldier extends RobotPlayer {
static MapLocation towerMain = null;

public static Direction getShortestPathDir(RobotController rc, MapLocation goal) {
    Direction straightDir = rc.getLocation().directionTo(goal);
    Direction nextDir = straightDir;
    
    if (rc.canMove(nextDir)) {
    return nextDir;
    }
    
    nextDir = straightDir.rotateLeft();
    
    if (rc.canMove(nextDir)) {
    return nextDir;
    }
    
    nextDir = straightDir.rotateRight();   
    
    if (rc.canMove(nextDir)) {
    return nextDir;
    }
    
    return Direction.CENTER;
    }

public static MapLocation getTowerLoc() {
return towerMain;
}

public static void setTowerLoc(MapLocation newTowerLoc) {
towerMain = newTowerLoc;
}

public static boolean checkTowerLoc(RobotController rc, MapInfo newTower) throws GameActionException {
if (rc.getTeam() == rc.senseRobotAtLocation(newTower.getMapLocation()).getTeam()) {
setTowerLoc(newTower.getMapLocation());
return true;
}
return false;
}
}