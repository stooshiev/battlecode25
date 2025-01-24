package bunniesv3;
import battlecode.common.*;

import java.util.HashSet;

public class OrbitPathfinder {
    // enum order CENTER, WEST, NORTHWEST, NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST
    static final Direction[] directions = new Direction[]{
            Direction.EAST,
            Direction.NORTHEAST,
            Direction.NORTH,
            Direction.NORTHWEST,
            Direction.WEST,
            Direction.SOUTHWEST,
            Direction.SOUTH,
            Direction.SOUTHEAST,
            Direction.EAST
    };
    static final float[] ordToAngle = new float[]{2, 1, 0, 7, 6, 5, 4, 3, Float.NaN};
    static final int[][] radius3 = new int[][] {
                                         {-3, 0},
                     {-2, -2}, {-2, -1}, {-2, 0}, {-2, 1}, {-2, 2},
                     {-1, -2},                             {-1, 2},
            {0, -3}, {0, -2},                              {0, 2}, {0, 3},
                       {1, -2},                            {1, 2},
                       {2, -2}, {2, -1}, {2, 0}, {2, 1},   {2, 2},
                                         {3, 0}
    };
    // an angle-like metric where going counterclockwise increases the metric and one round adds 8
    static float eightAngle(float x, float y) {
        if (x == 0 && y == 0) {
            throw new IllegalArgumentException("x or y must be nonzero");
        }
        if (y >= 0) {
            if (x >= 0) { // quadrant I
                return (y - x) / (x + y) + 1;
            } // quadrant II
            return (x + y) / (x - y) + 3;
        } else {
            if (x < 0) { // quadrant III
                return (y - x) / (x + y) + 5;
            } // quadrant IV
            return (x + y) / (x - y) + 7;
        }
    }
    private final int destX;
    private final int destY;
    private final MapLocation dest;
    private final RobotController rc;
    private final boolean clockwise;
    public static HashSet<MapLocation> avoidWithinRadius3 = null;
    private static HashSet<MapLocation> avoidDueToRadius3 = new HashSet<>();

    private boolean angleTracking = false;
    private float windingNumber = 0;
    private float wallAngle = Float.NaN;
    private Direction wall = Direction.CENTER;
    private MapLocation rcLocation;
    public boolean disturbed = false;
    public OrbitPathfinder(RobotController rc, MapLocation dest, boolean clockwise) {
        destX = dest.x;
        destY = dest.y;
        this.rc = rc;
        this.dest = dest;
        this.clockwise = clockwise;
        rcLocation = rc.getLocation();
    }
    public OrbitPathfinder(RobotController rc, MapLocation dest) {
        this(rc, dest, true);
    }
    public void step() {
        // recalculate bad squares if avoidWithinRadius3
        if (avoidWithinRadius3 != null) {
            for (MapLocation tower : avoidWithinRadius3) {
                for (int[] offset : radius3) {
                    avoidDueToRadius3.add(new MapLocation(tower.x + offset[0], tower.y + offset[1]));
                }
            }
        } else if (!avoidDueToRadius3.isEmpty()) {
            avoidDueToRadius3 = new HashSet<>();
        }

        if (disturbed || !rc.getLocation().equals(rcLocation)) {
            System.out.println("YOU HAVE DISTURBED THE PATHFINDER! IT WILL NEVER WORK AGAIN!");
        }
        if (clockwise) {
            stepClockwise();
        }
        stepCounterclockwise();
    }
    private void stepClockwise() {
        if (!rc.isMovementReady()) {
            return;
        }
        MapLocation rcLoc = rc.getLocation();
        int relativeX = destX - rcLoc.x;
        int relativeY = destY - rcLoc.y;
        if (relativeX == 0 && relativeY == 0) {
            return;
        }
        if (!angleTracking) {
            float angle = eightAngle(relativeX, relativeY);
            Direction best = directions[Math.round(angle)];
            if (rc.canMove(best) && !avoidDueToRadius3.contains(rc.getLocation().add(best))) {
                try {
                    rc.move(best);
                    windingNumber += turnJump(best, relativeX, relativeY);
                    RobotPlayer.path.addLast(rc.getLocation());
                } catch (GameActionException ignored) {
                }
                rcLocation = rc.getLocation();
                return;
            } // otherwise we can't move there
            angleTracking = true;
            wallAngle = Math.round(angle) + windingNumber; // wall angle should be similar to adjustedAngle
            wall = best;
        }
        // angle tracking is on if we reach here
        Direction attempt = wall.rotateLeft();
        float attemptAngle = wallAngle + 1;
        for (int i = 0; i < 7; i++) { // try seven directions before giving up
            if (rc.canMove(attempt) && !avoidDueToRadius3.contains(rc.getLocation().add(attempt))) {
                try {
                    rc.move(attempt);
                    RobotPlayer.path.add(rc.getLocation());
                    windingNumber += turnJump(attempt, relativeX, relativeY);
                    float angle = eightAngle(relativeX - attempt.dx, relativeY - attempt.dy);
                    float adjustedAngle = angle + windingNumber;
                    if (attemptAngle <= adjustedAngle) {
                        Direction bestDirection = directions[Math.round(angle)];
                        angleTracking = false;
                        wall = Direction.CENTER;
                        wallAngle = Float.NaN;
                        rcLocation = rc.getLocation();
                        return;
                    }
                    // if it's not the best direction, we have to set wall based on our movement
                    // we went in the direction to the left of the wall
                    if (attempt == Direction.EAST || attempt == Direction.NORTH ||
                            attempt == Direction.WEST || attempt == Direction.SOUTH) {
                        // if we went in a cardinal direction, wall moved from diagonal to cardinal direction
                        wall = wall.rotateRight();
                        wallAngle -= 1;
                    } else {
                        // if we went in a diagonal direction, the wall moved from a cardinal direction
                        // to cardinal direction
                        wall = wall.rotateRight().rotateRight();
                        wallAngle -= 2;
                    }
                } catch (GameActionException ignored) {}
                rcLocation = rc.getLocation();
                return;
            }
            // if we couldn't move that way, then it is also a wall. Also try a new attempt direction
            wall = attempt;
            wallAngle = attemptAngle;
            attempt = attempt.rotateLeft();
            attemptAngle += 1;
        }
        System.out.println("GoTo object could not find a direction");
        wallAngle -= 8;
        rcLocation = rc.getLocation();
    }
    private void stepCounterclockwise() {
        if (!rc.isMovementReady()) {
            return;
        }
        MapLocation rcLoc = rc.getLocation();
        int relativeX = destX - rcLoc.x;
        int relativeY = destY - rcLoc.y;
        if (relativeX == 0 && relativeY == 0) {
            return;
        }
        if (!angleTracking) {
            float angle = eightAngle(relativeX, relativeY);
            Direction best = directions[Math.round(angle)];
            if (rc.canMove(best) && !avoidDueToRadius3.contains(rc.getLocation().add(best))) {
                try {
                    rc.move(best);
                    windingNumber += turnJump(best, relativeX, relativeY);
                    RobotPlayer.path.addLast(rc.getLocation());
                } catch (GameActionException ignored) {
                }
                rcLocation = rc.getLocation();
                return;
            } // otherwise we can't move there
            angleTracking = true;
            wallAngle = Math.round(angle) + windingNumber; // wall angle should be similar to adjustedAngle
            wall = best;
        }
        // angle tracking is on if we reach here
        Direction attempt = wall.rotateRight();
        float attemptAngle = wallAngle - 1;
        for (int i = 0; i < 7; i++) { // try seven directions before giving up
            if (rc.canMove(attempt) && !avoidDueToRadius3.contains(rc.getLocation().add(attempt))) {
                try {
                    rc.move(attempt);
                    RobotPlayer.path.add(rc.getLocation());
                    windingNumber += turnJump(attempt, relativeX, relativeY);
                    float angle = eightAngle(relativeX - attempt.dx, relativeY - attempt.dy);
                    float adjustedAngle = angle + windingNumber;
                    if (attemptAngle >= adjustedAngle) {
                        Direction bestDirection = directions[Math.round(angle)];
                        angleTracking = false;
                        wall = Direction.CENTER;
                        wallAngle = Float.NaN;
                        rcLocation = rc.getLocation();
                        return;
                    }
                    // if it's not the best direction, we have to set wall based on our movement
                    // we went in the direction to the left of the wall
                    if (attempt == Direction.EAST || attempt == Direction.NORTH ||
                            attempt == Direction.WEST || attempt == Direction.SOUTH) {
                        // if we went in a cardinal direction, wall moved from diagonal to cardinal direction
                        wall = wall.rotateLeft();
                        wallAngle -= 1;
                    } else {
                        // if we went in a diagonal direction, the wall moved from a cardinal direction
                        // to cardinal direction
                        wall = wall.rotateLeft().rotateLeft();
                        wallAngle -= 2;
                    }
                } catch (GameActionException ignored) {}
                rcLocation = rc.getLocation();
                return;
            }
            // if we couldn't move that way, then it is also a wall. Also try a new attempt direction
            wall = attempt;
            wallAngle = attemptAngle;
            attempt = attempt.rotateRight();
            attemptAngle -= 1;
        }
        System.out.println("GoTo object could not find a direction");
        wallAngle += 8;
        rcLocation = rc.getLocation();
    }
    private float turnJump(Direction direction, int relativeX, int relativeY) {
        if (relativeY == -1 && direction.dy == -1 && (relativeX > 0 || (relativeX == 0 && direction.dx == -1))) {
            // if moving will jump from 7 to 0
            return 8;
        } else if (relativeY == 0 && relativeX > 0 && direction.dy == 1) {
            // if moving will jump from 0 to 7
            return -8;
        }
        return 0;
    }
    public MapLocation getDest() {
        return dest;
    }
}