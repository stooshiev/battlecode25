package bunniesv0;

import battlecode.common.*;

public class Soldier extends RobotPlayer {
    static MapLocation towerMain = null;
    static int paintAmountRequest = 100;

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
        
        if (rc.senseRobotAtLocation(newTower.getMapLocation()) == null) {
            return false;
        }
        if (rc.getTeam() == rc.senseRobotAtLocation(newTower.getMapLocation()).getTeam()) {
            Soldier.setTowerLoc(newTower.getMapLocation());
            return true;
        }
        return false;
    }

    public static void retreatForPaint(RobotController rc) throws GameActionException {
        MapLocation closeTower = Soldier.getTowerLoc();
        if (closeTower == null)
            return;
        Direction moveDir = Soldier.getShortestPathDir(rc, Soldier.getTowerLoc());
        if (rc.canMove(moveDir))
            rc.move(moveDir);
        if (rc.canTransferPaint(closeTower, -paintAmountRequest)) {
            rc.transferPaint(closeTower, -paintAmountRequest);
        }
    }

    public static boolean paintNewTower(RobotController rc, MapInfo curRuin) throws GameActionException {
        boolean isMarking = false;
        MapLocation targetLoc = curRuin.getMapLocation();
        Direction dir = rc.getLocation().directionTo(targetLoc);
        Direction moveDir = Soldier.getShortestPathDir(rc, targetLoc);
        // Mark the pattern we need to draw to build a tower here if we haven't already.
        MapLocation shouldBeMarked = curRuin.getMapLocation().subtract(dir);
        if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
            rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
            System.out.println("Trying to build a tower at " + targetLoc);
        }
        // Fill in any spots in the pattern with the appropriate paint.
        String[] attackTiles = new String[25];
        int curAttack = 0;
        for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, 8)){
            
            if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY){
            //if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY){
                boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                if (rc.canAttack(patternTile.getMapLocation()) && (!rc.senseMapInfo(patternTile.getMapLocation()).getPaint().equals(PaintType.ENEMY_PRIMARY) && !rc.senseMapInfo(patternTile.getMapLocation()).getPaint().equals(PaintType.ENEMY_SECONDARY))) {
                    rc.attack(patternTile.getMapLocation(), useSecondaryColor);
                    isMarking = true;
                    //attackTiles += attackTiles + ", " + patternTile.getMapLocation().toString();
                }
            }
            attackTiles[curAttack] = patternTile.getMapLocation().toString();
            curAttack += 1;
        }
        // Complete the ruin if we can.
        if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
            rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
            rc.setTimelineMarker("Tower built", 0, 255, 0);
            System.out.println("Built a tower at " + targetLoc + "!");
        }
        if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetLoc)){
            rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetLoc);
            rc.setTimelineMarker("Tower built", 0, 255, 0);
            System.out.println("Built a tower at " + targetLoc + "!");
        }
        if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, targetLoc)){
            rc.completeTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, targetLoc);
            rc.setTimelineMarker("Tower built", 0, 255, 0);
            System.out.println("Built a tower at " + targetLoc + "!");
        }
        
        if (rc.canMove(moveDir))
            rc.move(moveDir);
        return isMarking;
    }

    public static void attackCheckered(RobotController rc, MapLocation attackLoc) throws GameActionException {
        rc.attack(attackLoc, (attackLoc.x + attackLoc.y) % 2 == 0);
    }

    public static Direction methodicalMovement(RobotController rc) throws GameActionException {
        Direction[] bestDirection = null;
        if (rc.getTeam() == Team.A) {
            bestDirection = new Direction[]{
                Direction.NORTH,
                Direction.EAST,
                Direction.WEST,
                Direction.SOUTH
            };
        }
        else if (rc.getTeam() == Team.B) {
            bestDirection = new Direction[]{
                Direction.SOUTH,
                Direction.WEST,
                Direction.EAST,
                Direction.NORTH
            };
        }
        for (int i = 0; i < bestDirection.length; i++) {
            if (rc.canMove(bestDirection[i]))
                return bestDirection[i];
            else if (rc.canMove(bestDirection[i].rotateLeft()))
                return bestDirection[i].rotateLeft();
            else if (rc.canMove(bestDirection[i].rotateRight()))
                return bestDirection[i].rotateRight();
        }
        return Direction.CENTER;
    }

    public static MapInfo checkMarking(RobotController rc, MapInfo tile) throws GameActionException {
        for (MapInfo patternTile : rc.senseNearbyMapInfos(tile.getMapLocation(), 8)) {
            if ((rc.senseMapInfo(patternTile.getMapLocation()).getPaint() != rc.senseMapInfo(patternTile.getMapLocation()).getMark()
            && (!rc.senseMapInfo(patternTile.getMapLocation()).getPaint().equals(PaintType.ENEMY_PRIMARY) && !rc.senseMapInfo(patternTile.getMapLocation()).getPaint().equals(PaintType.ENEMY_SECONDARY))) 
            || rc.senseMapInfo(patternTile.getMapLocation()).getPaint() == PaintType.EMPTY) {
                if (!patternTile.getMapLocation().equals(tile.getMapLocation())) {
                    return tile;
                }
            }
        }
        return null;
    }

    public static void attackEnemyTower(RobotController rc, MapInfo enemyTower) throws GameActionException {
        MapLocation enemyTowerLoc = enemyTower.getMapLocation();
        if (rc.canAttack(enemyTowerLoc)) {
            rc.attack(enemyTowerLoc);
            rc.setIndicatorString("Retreating: " + retreatFromEnemyTower(rc, enemyTowerLoc));
        } else {
            rc.move(getShortestPathDir(rc, enemyTowerLoc));
            if (rc.canAttack(enemyTowerLoc))
                rc.attack(enemyTowerLoc);
        }
    }

    public static boolean retreatFromEnemyTower(RobotController rc, MapLocation enemyTowerLoc) throws GameActionException {
        for (Direction nextDir : directions) {
            if (rc.getLocation().add(nextDir).distanceSquaredTo(enemyTowerLoc) > 9) {
                if (rc.canMove(nextDir)) {
                    rc.move(nextDir);
                    return true;
                }
            }
        }
        return false;
    }
    
    
}