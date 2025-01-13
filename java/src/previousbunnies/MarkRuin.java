package previousbunnies;

import battlecode.common.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class MarkRuin {
    static class Tuple<E, F> {
        public E v1;
        public F v2;
        public Tuple(E v1, F v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Tuple<?, ?> tuple = (Tuple<?, ?>) o;

            if (!Objects.equals(v1, tuple.v1)) return false;
            return Objects.equals(v2, tuple.v2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(v1, v2);
        }
    }

    static final int NOT_FOUND = 0;
    static final int FOUND_AND_MARKED = 1;
    static final int FOUND_AND_APPROACHING = 2;
    static final int FOUND_AND_CANT_MOVE = 3;
    static final int FOUND_AND_BLOCKED = 4;
    static final int NOT_ENOUGH_PAINT = 5;

    static final UnitType[] INITIAL_TOWERS = new UnitType[]{
            UnitType.LEVEL_ONE_PAINT_TOWER,
            UnitType.LEVEL_ONE_MONEY_TOWER,
            UnitType.LEVEL_ONE_DEFENSE_TOWER
    };

    /*
     * Stores pairs of locations (loc1, loc2) where loc1.add(loc1.directionTo(loc2) is impassible and not a robot
     */
    static Set<Tuple<MapLocation, MapLocation>> impassablePaths = new HashSet<Tuple<MapLocation, MapLocation>>();

    /*
     * Bunny looks around for ruins and identifies the first one it sees. If there's a ruin, it checks if it
     * has been marked. If it's been marked, it attempts to complete the tower pattern. If it hasn't been marked,
     * Bunny attempts to mark it. If it is unsuccessful, it takes a step towards the tower.
     * Returns
     * NOT_FOUND if no empty ruin was found
     * FOUND_AND_MARKED if Bunny marked the ruin
     * FOUND_AND_APPROACHING if Bunny cannot mark it, so it decides to step forward
     * FOUND_AND_CANT_MOVE if Bunny cannot move towards the ruin
     * FOUND_AND_BLOCKED if the path Bunny needs to get to the tower is blocked
     */
    static int markIfFound(RobotController rc, MapInfo[] nearbyTiles, RobotInfo[] nearbyRobots, UnitType towerType)
            throws GameActionException {
        if (rc.getPaint() < GameConstants.MARK_PATTERN_PAINT_COST) {
            return NOT_ENOUGH_PAINT;
        }
        // Search for a nearby ruin to complete.
        MapInfo curRuin = null;
        for (MapInfo tile : nearbyTiles){
            if (tile.hasRuin()){
                if (curRuin == null) {
                    curRuin = tile;
                } else {
                    // stable way of choosing a tower
                    MapLocation curLocation = curRuin.getMapLocation();
                    MapLocation tileLocation = tile.getMapLocation();
                    int c1 = curLocation.x;
                    int c2 = tileLocation.x;
                    if (c1 == c2) {
                        c1 = curLocation.y;
                        c2 = tileLocation.y;
                    }
                    curRuin = c1 < c2 ? curRuin : tile;
                }
            }
        }
        if (curRuin == null) {
            // if there is no nearby ruin, then do nothing
            return NOT_FOUND;
        }
        // make sure there isn't already a tower there
        for (RobotInfo robot : nearbyRobots) {
            if (robot.getLocation().equals(curRuin.getMapLocation())) {
                return NOT_FOUND;
            }
        }
        MapLocation targetLoc = curRuin.getMapLocation();
        MapLocation rcLoc = rc.getLocation();
        boolean unmarked = (rc.senseMapInfo(targetLoc.add(targetLoc.directionTo(rcLoc))).getMark()
                        == PaintType.EMPTY);
        if (!unmarked) {
            // if there's a ruin that's already marked, then try to complete the ruin
            if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
                rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
                rc.setTimelineMarker("Tower built", 0, 255, 0);
                System.out.println("Built a tower at " + targetLoc + "!");
            }
            return NOT_FOUND;
        }
        boolean canMark = rc.canMarkTowerPattern(towerType, targetLoc);
        if (canMark) {
            // if there's a markable ruin, try to mark it
            rc.markTowerPattern(towerType, targetLoc);
            return FOUND_AND_MARKED;
        }
        // If the code reaches here, the ruin is markable but cannot be marked from where rc is
        // Try to take a step towards the ruin
        // If (robot loc, ruin loc) is in the set then it's blocked
        if (impassablePaths.contains(new Tuple<MapLocation, MapLocation>(rcLoc, targetLoc))) {
            return FOUND_AND_BLOCKED;
        }
        // otherwise try to go towards the path
        Direction dir = rcLoc.directionTo(targetLoc);
        if (rc.canMove(dir)) {
            // if the target cannot be reached from the next tile (in the set), then this should be put in the set too
            if (impassablePaths.contains(new Tuple<MapLocation, MapLocation>(rcLoc.add(dir), targetLoc))) {
                impassablePaths.add(new Tuple<MapLocation, MapLocation>(rcLoc, targetLoc));
                return FOUND_AND_BLOCKED;
            }
            rc.move(dir);
            return FOUND_AND_APPROACHING;
        }
        // if cannot move find out why
        MapLocation next = rcLoc.add(dir);
        MapInfo nextTile = rc.senseMapInfo(next);
        if (nextTile.isWall() || nextTile.hasRuin()) {
            // if it's because it is blocked by a wall or ruin (lol), it will always be blocked
            impassablePaths.add(new Tuple<MapLocation, MapLocation>(rcLoc, targetLoc));
            return FOUND_AND_BLOCKED;
        }
        return FOUND_AND_CANT_MOVE;
    }

    static boolean attemptCompleteTowerPattern(RobotController rc, MapLocation targetLoc)
            throws GameActionException {
        for (UnitType towerType : INITIAL_TOWERS) {
            if (rc.canCompleteTowerPattern(towerType, targetLoc)) {
                rc.completeTowerPattern(towerType, targetLoc);
                rc.setTimelineMarker("Tower built", 0, 255, 0);
                System.out.println("Built a tower at " + targetLoc + "!");
                return true;
            }
        }
        return false;
    }
}
