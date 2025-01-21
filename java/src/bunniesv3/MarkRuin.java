package bunniesv3;

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
    static final int FOUND_AND_NAVIGATING = 6;
    static final int MARKED_BY_OTHER = 7;

    static final UnitType[] INITIAL_TOWERS = new UnitType[]{
            UnitType.LEVEL_ONE_PAINT_TOWER,
            UnitType.LEVEL_ONE_MONEY_TOWER,
            UnitType.LEVEL_ONE_DEFENSE_TOWER
    };
    static MapLocation ruinLocation = null;
    static OrbitPathfinder pathfinder = null;
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
    static int markIfFound(RobotController rc, UnitType towerType)
            throws GameActionException {
        MapLocation rcLoc = rc.getLocation();
        if (rc.getPaint() < GameConstants.MARK_PATTERN_PAINT_COST) {
            return NOT_ENOUGH_PAINT;
        }
        if (ruinLocation == null) {
            // pick a good ruin
            MapLocation[] ruins = rc.senseNearbyRuins(-1);
            if (ruins.length == 0) {
                return NOT_FOUND;
            }
            int startIndex = RobotPlayer.rng.nextInt(ruins.length);
            int index = startIndex;
            do {
                MapLocation ruin = ruins[index];
                if (rc.senseRobotAtLocation(ruin) == null && rc.senseMapInfo(
                        ruin.add(ruin.directionTo(rcLoc))).getMark() == PaintType.EMPTY) {
                    ruinLocation = ruin;
                    break;
                }
                index = (index == ruins.length - 1) ? 0 : index + 1;
            } while (index != startIndex);
            if (ruinLocation == null) {
                return NOT_FOUND;
            }
        }
        if (rc.senseRobotAtLocation(ruinLocation) != null || rc.senseMapInfo(
                ruinLocation.add(ruinLocation.directionTo(rcLoc))).getMark() != PaintType.EMPTY) {
            // it's been marked or a tower has been added while we were traveling there
            pathfinder = null;
            ruinLocation = null;
            return MARKED_BY_OTHER;
        }
        if (towerType == null) {
            towerType = MarkRuin.INITIAL_TOWERS[RobotPlayer.rng.nextInt(MarkRuin.INITIAL_TOWERS.length - 1)];
        }
        if (rc.canMarkTowerPattern(towerType, ruinLocation)) {
            // if there's a markable ruin, try to mark it
            rc.markTowerPattern(towerType, ruinLocation);
            pathfinder = null;
            ruinLocation = null;
            return FOUND_AND_MARKED;
        }
        // If the code reaches here, the ruin is markable but cannot be marked from where rc is
        // Try to take a step towards the ruin
        // If (robot loc, ruin loc) is in the set then it's blocked
        if (pathfinder == null) {
            pathfinder = new OrbitPathfinder(rc, ruinLocation);
        }
        pathfinder.step();
        return FOUND_AND_NAVIGATING;
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
