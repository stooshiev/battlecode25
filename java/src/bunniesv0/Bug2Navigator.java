package bunniesv0;
import battlecode.common.*;

public class Bug2Navigator {
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
            return (x + y) / (x - y);
        }
    }
    private final int destX;
    private final int destY;
    private final MapLocation dest;
    private final RobotController rc;
    private final boolean clockwise;
    private float furthestAngle = Float.NaN; // min angle for clockwise, max angle for ccw
    private boolean angleTracking = false;
    private float eightTurns = 0;
    private Direction wall = Direction.CENTER;
    public Bug2Navigator(RobotController rc, MapLocation dest, boolean clockwise) {
        destX = dest.x;
        destY = dest.y;
        this.rc = rc;
        this.dest = dest;
        this.clockwise = clockwise;
    }
    public Bug2Navigator(RobotController rc, MapLocation dest) {
        this(rc, dest, true);
    }
    public void step() {
        if (clockwise) {
            stepClockwise();
            return;
        }
        stepCounterclockwise();
    }
    private void stepClockwise() {
        if (rc.getRoundNum() >= 245) {
            int dkfsj = 0;
        }
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
            if (rc.canMove(best)) {
                try {
                    rc.move(best);
                    adjustTurns(best, relativeX, relativeY);
                    angleTracking = false;
                    furthestAngle = Float.NaN;
                } catch (GameActionException ignored) {
                }
                return;
            } // otherwise we can't move there
            angleTracking = true;
            furthestAngle = eightTurns + angle;
            wall = best;
        }
        // angle tracking is on if we reach here
        Direction attempt = wall.rotateLeft();
        for (int i = 0; i < 7; i++) { // try seven directions before giving up
            if (rc.canMove(attempt)) {
                try {
                    rc.move(attempt);
                    adjustTurns(attempt, relativeX, relativeY);
                    float angle = eightAngle(relativeX - attempt.dx, relativeY - attempt.dy);
                    float adjustedAngle = angle + eightTurns;
                    if (adjustedAngle < furthestAngle) {
                        furthestAngle = adjustedAngle;
                        Direction bestDirection = directions[Math.round(angle)];
                        if (rc.senseMapInfo(rcLoc.add(bestDirection)).isPassable()) {
                            // we have never encountered obstacles in the path in front of us!
                            angleTracking = false;
                            furthestAngle = Float.NaN;
                            wall = Direction.CENTER;
                            return;
                        }
                    }
                    // if it's not the best direction, we have to set wall based on our movement
                    // we went in the direction to the left of the wall
                    if (attempt == Direction.EAST || attempt == Direction.NORTH ||
                            attempt == Direction.WEST || attempt == Direction.SOUTH) {
                        // if we went in a cardinal direction, wall moved from diagonal to cardinal direction
                        wall = wall.rotateRight();
                    } else {
                        // if we went in a diagonal direction, the wall moved from a cardinal direction
                        // to cardinal direction
                        wall = wall.rotateRight().rotateRight();
                    }
                } catch (GameActionException ignored) {}
                return;
            }
            // if we couldn't move that way, then it is also a wall. Also try a new attempt direction
            wall = attempt;
            attempt = attempt.rotateLeft();
        }
        System.out.println("GoTo object could not find a direction");
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
            if (rc.canMove(best)) {
                try {
                    rc.move(best);
                    adjustTurns(best, relativeX, relativeY);
                    angleTracking = false;
                    furthestAngle = Float.NaN;
                } catch (GameActionException ignored) {}
                return;
            } // otherwise we can't move there
            angleTracking = true;
            furthestAngle = eightTurns;
            wall = best;
        }
        // angle tracking is on if we reach here
        Direction attempt = wall.rotateRight();
        for (int i = 0; i < 7; i++) { // try seven directions before giving up
            if (rc.canMove(attempt)) {
                try {
                    rc.move(attempt);
                    adjustTurns(attempt, relativeX, relativeY);
                    float angle = eightAngle(relativeX, relativeY);
                    float adjustedAngle = angle + eightTurns;
                    if (adjustedAngle > furthestAngle) {
                        furthestAngle = adjustedAngle;
                        Direction bestDirection = directions[Math.round(angle)];
                        if (attempt == bestDirection) {
                            // we have never encountered obstacles in the path in front of us!
                            angleTracking = false;
                            furthestAngle = Float.NaN;
                            wall = Direction.CENTER;
                            return;
                        }
                        // if it's not the best direction, we have to set wall based on our movement
                        // we went in the direction to the left of the wall
                        if (attempt == Direction.EAST || attempt == Direction.NORTH ||
                                attempt == Direction.WEST || attempt == Direction.SOUTH) {
                            // if we went in a cardinal direction, wall moved from diagonal to cardinal direction
                            wall = wall.rotateLeft();
                        } else {
                            // if we went in a diagonal direction, the wall moved from a cardinal direction
                            // to cardinal direction
                            wall = wall.rotateLeft().rotateLeft();
                        }
                    }
                } catch (GameActionException ignored) {}
                return;
            }
            // if we couldn't move that way, then it is also a wall. Also try a new attempt direction
            wall = attempt;
            attempt = attempt.rotateRight();
        }
        System.out.println("GoTo object could not find a direction");
    }
    private void adjustTurns(Direction direction, int relativeX, int relativeY) {
        if (relativeY == -1 && direction.dy == 1 && (relativeX > 0 || (relativeX == 0 && direction.dx == 1))) {
            // if moving will jump from 7 to 0
            eightTurns += 8;
        } else if (relativeY == 0 && relativeX > 0 && direction.dy == -1) {
            // if moving will jump from 0 to 7
            eightTurns -= 8;
        }
    }
}
