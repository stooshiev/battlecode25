package bunniesv3;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.Math.*;

public class SplasherConvolution {
    // float[passable][paint type][secondary mark][fringe=0, center=1]
    static final float[][][] fringeDamageArray = new float[][][] {
            // empty
            {{0, 0}}, // tile is impassible
            // empty, ally1, ally2,   enemy1, enemy2
            {{1, 1}, {0, 0}, {-.7f, -1.5f}, {0, 0}, {0, 0}} // tile is passable
    };
    static final float[][][] centerDamageArray = new float[][][] {
            // empty
            {{0, 0}}, // impassible
            // empty, ally1, ally2,   enemy1, enemy2
            {{1, 1}, {0, 0}, {-.7f, -1.5f}, {2.5f, 2.5f}, {4, 4}} // passable
    };
    static final boolean[][] attackCanReach = new boolean[][] {
            {false, false, false, false, true, false, false, false, false},
            {false, false, false, true,  true, true,  false, false, false},
            {false, false, true,  true,  true, true,  true,  false, false},
            {false, true,  true,  true,  true, true,  true,  true,  false},
            {true,  true,  true,  true,  true, true,  true,  true,  true},
            {false, true,  true,  true,  true, true,  true,  true,  false},
            {false, false, true,  true,  true, true,  true,  false, false},
            {false, false, false, true,  true, true,  false, false, false},
            {false, false, false, false, true, false, false, false, false},
    };
    static final boolean[] canAttack = new boolean[]{
            false, false, true, false, false,
            false, true, true, true, false,
            true, true, true, true, true,
            false, true, true, true, false,
            false, false, true, false, false
    };
    static final int[] attackableIndices = new int[]{
                 2,
             6,  7,  8,
        10, 11, 12, 13, 14,
            16, 17, 18,
                22
    };
    static final int[][] attackPositions = new int[][] {
                              {-2, 0},
                     {-1, -1},{-1, 0},{-1, 1},
            {0, -2}, {0, -1}, {0, 0}, {0, 1}, {0, 2},
                     {1, -1}, {1, 0}, {1, 1},
                              {2, 0}
    };
    // these generate the upcoming lookup tables
    public static int[][][] fringeTable() {
        int[][] fringeOffsets = new int[][]{{2, 0}, {0, 2}, {-2, 0}, {0, -2}};
        int[][][] table = new int[9][9][];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                ArrayList<Integer> fringes = new ArrayList<>();
                for (int[] offset : fringeOffsets) {
                    int fringeI = i + offset[0];
                    int fringeJ = j + offset[1];
                    if (2 <= fringeI && fringeI < 7 && 2 <= fringeJ && fringeJ < 7) {
                        int fiveIndex = 5 * (fringeI - 2) + (fringeJ - 2);
                        if (canAttack[fiveIndex]) {
                            // we will only consider locations that can be attacked
                            fringes.add(fiveIndex);
                        }
                    }
                }
                table[i][j] = new int[fringes.size()];
                for (int n = 0; n < fringes.size(); n++) {
                    int fiveIndex = fringes.get(n);
                    // convert to thirteen index
                    int thirteenIndex = -1;
                    for (int k = 0; k < 13; k++) {
                        if (attackableIndices[k] == fiveIndex) {
                            thirteenIndex = k;
                            break;
                        }
                    }
                    table[i][j][n] = thirteenIndex;
                }
            }
        }

        return table;
    }
    public static int[][][] centerTable() {
        int[][][] table = new int[9][9][];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                ArrayList<Integer> fringes = new ArrayList<>();
                for (int ioffset = -1; ioffset < 2; ioffset++) {
                    for (int joffset = -1; joffset < 2; joffset++) {
                        int centerI = i + ioffset;
                        int centerJ = j + joffset;
                        if (2 <= centerI && centerI < 7 && 2 <= centerJ && centerJ < 7) {
                            int fiveIndex = 5 * (centerI - 2) + (centerJ - 2);
                            if (canAttack[fiveIndex]) {
                                fringes.add(fiveIndex);
                            }
                        }
                    }
                }
                table[i][j] = new int[fringes.size()];
                for (int n = 0; n < fringes.size(); n++) {
                    int fiveIndex = fringes.get(n);
                    // convert to thirteen index
                    int thirteenIndex = -1;
                    for (int k = 0; k < 13; k++) {
                        if (attackableIndices[k] == fiveIndex) {
                            thirteenIndex = k;
                            break;
                        }
                    }
                    table[i][j][n] = thirteenIndex;
                }
            }
        }
        return table;
    }
    public static void printOffsetPairTable() {
        System.out.println("Fringe");
        System.out.println(Arrays.deepToString(fringeTable()));
        System.out.println("Center");
        System.out.println(Arrays.deepToString(centerTable()));
    }
    static int[][][] offsetPairToFringes = new int[][][]{
            {{}, {}, {}, {}, {0}, {}, {}, {}, {}},
            {{}, {}, {}, {1}, {2}, {3}, {}, {}, {}},
            {{}, {}, {4, 0}, {5}, {6}, {7}, {8, 0}, {}, {}},
            {{}, {1}, {2}, {9, 3}, {10}, {11, 1}, {2}, {3}, {}},
            {{4}, {5}, {6}, {7}, {12, 8, 0, 4}, {5}, {6}, {7}, {8}},
            {{}, {9}, {10}, {11, 1}, {2}, {3, 9}, {10}, {11}, {}},
            {{}, {}, {12, 4}, {5}, {6}, {7}, {8, 12}, {}, {}},
            {{}, {}, {}, {9}, {10}, {11}, {}, {}, {}},
            {{}, {}, {}, {}, {12}, {}, {}, {}, {}}
    };
    static int[][][] offsetPairToCenter = new int[][][]{
            {{}, {}, {}, {}, {}, {}, {}, {}, {}},
            {{}, {}, {}, {0}, {0}, {0}, {}, {}, {}},
            {{}, {}, {1}, {0, 1, 2}, {0, 1, 2, 3}, {0, 2, 3}, {3}, {}, {}},
            {{}, {4}, {1, 4, 5}, {0, 1, 2, 4, 5, 6}, {0, 1, 2, 3, 5, 6, 7}, {0, 2, 3, 6, 7, 8}, {3, 7, 8}, {8}, {}},
            {{}, {4}, {1, 4, 5, 9}, {1, 2, 4, 5, 6, 9, 10}, {1, 2, 3, 5, 6, 7, 9, 10, 11}, {2, 3, 6, 7, 8, 10, 11}, {3, 7, 8, 11}, {8}, {}},
            {{}, {4}, {4, 5, 9}, {4, 5, 6, 9, 10, 12}, {5, 6, 7, 9, 10, 11, 12}, {6, 7, 8, 10, 11, 12}, {7, 8, 11}, {8}, {}},
            {{}, {}, {9}, {9, 10, 12}, {9, 10, 11, 12}, {10, 11, 12}, {11}, {}, {}},
            {{}, {}, {}, {12}, {12}, {12}, {}, {}, {}},
            {{}, {}, {}, {}, {}, {}, {}, {}, {}}
    };
    /*
     * Compute attack totals without initializing arrays larger than 5x5
     */

    static MapLocation computeAndAttack(RobotController rc, MapInfo[] nearbyTiles, RobotInfo[] nearbyRobots, float threshold) {
        float[] attackTotals = new float[13];
        if (rc.getRoundNum() == 85) {
            int djksafj = 0;
        }
        MapLocation rcLoc = rc.getLocation();
        // loop through tiles, editing attack totals accordingly
        boolean foundTower = false;
        for (RobotInfo robot : nearbyRobots) {
            if (robot.getType().isTowerType() && !robot.getTeam().equals(rc.getTeam())) {
                // if it's an enemy tower, then that's the best thing to attack
                MapLocation robotLoc = robot.getLocation();
                int xIndex = robotLoc.x - rcLoc.x + 4;
                int yIndex = robotLoc.y - rcLoc.y + 4;
                if (attackCanReach[xIndex][yIndex]) {
                    for (int thirteenIndex : offsetPairToCenter[xIndex][yIndex]) {
                        attackTotals[thirteenIndex] += 100.0f;
                    }
                    for (int thirteenIndex : offsetPairToFringes[xIndex][yIndex]) {
                        attackTotals[thirteenIndex] += 100.0f;
                    }
                }
                foundTower = true;
                break;
            }
        }
        if (!foundTower) {
            for (MapInfo tile : nearbyTiles) {
                MapLocation tileLoc = tile.getMapLocation();
                int xRelative = tileLoc.x - rcLoc.x;
                int yRelative = tileLoc.y - rcLoc.y;
                int xIndex = tileLoc.x - rcLoc.x + 4;
                int yIndex = tileLoc.y - rcLoc.y + 4;
                // disregard tiles that are too far away to be attacked
                if (!attackCanReach[xIndex][yIndex]) {
                    continue;
                }
                int passible = tile.isPassable() ? 1 : 0;
                int paint = tile.getPaint().ordinal();
                int secondaryMark = tile.getMark().isSecondary() ? 1 : 0;
                float fringeContribution = fringeDamageArray[passible][paint][secondaryMark];
                float centerContribution = centerDamageArray[passible][paint][secondaryMark];

                // this is the meat of the operation
                if (centerContribution != 0) {
                    for (int thirteenIndex : offsetPairToCenter[xIndex][yIndex]) {
                        attackTotals[thirteenIndex] += centerContribution;
                    }
                }
                if (fringeContribution != 0) {
                    for (int thirteenIndex : offsetPairToFringes[xIndex][yIndex]) {
                        attackTotals[thirteenIndex] += fringeContribution;
                    }
                }
                continue;
            }
        }

        // attemptAttack phase
        while (true) {
            float maxFound = threshold - 1;
            int maxIndex = -1;
            for (int i = 0; i < 13; i++) {
                if (attackTotals[i] > maxFound) {
                    maxFound = attackTotals[i];
                    maxIndex = i;
                }
            }
            if (maxFound < threshold) {
                return null;
            }
            int[] attackPosition = attackPositions[maxIndex];
            MapLocation attackLocation = new MapLocation(rcLoc.x + attackPosition[0], rcLoc.y + attackPosition[1]);
            try {
                rc.attack(attackLocation);
                return attackLocation;
            } catch (GameActionException gae) {
                System.out.println("Exception in SplasherConvolution something");
                gae.printStackTrace();
                System.out.println("Attempted to attack " + attackLocation);
                attackTotals[maxIndex] = -1000.0f;
            }
        }
    }

    public static boolean attackBestMinimal(RobotController rc, MapInfo[] nearbyTiles, float[] attackTotals, float threshold) {
        MapInfo[] mapInfoGrid = arrangeMapInfosFlat(rc, nearbyTiles, 2);
        boolean[] attemptAttack = new boolean[25];
        while (true) {
            float maxFound = threshold - 1;
            int maxIndex = -1;
            for (int attackableIndex : attackableIndices) {
                if (attemptAttack[attackableIndex]) continue;
                if (attackTotals[attackableIndex] > maxFound) {
                    maxFound = attackTotals[attackableIndex];
                    maxIndex = attackableIndex;
                }
            }
            if (maxFound < threshold) {
                // no good attack spot found
                return false;
            }
            if (mapInfoGrid[maxIndex] != null && rc.canAttack(mapInfoGrid[maxIndex].getMapLocation())) {
                try {
                    rc.attack(mapInfoGrid[maxIndex].getMapLocation());
                    return true;
                } catch (GameActionException gae) {
                    System.out.println("Exception in SplasherConvolution.attackBest()");
                    System.out.println(gae.getMessage());
                    gae.printStackTrace();
                }
            }
            // if that location couldn't be attacked, set it to false
            attemptAttack[maxIndex] = true;
        }
    }

    public static class Tuple<X, Y> {
        public final X v1;
        public final Y v2;
        public Tuple(X v1, Y v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
    }
    public static float[][] kernel = new float[][]{
            {0, 0, 1, 0, 0},
            {0, 1, 1, 1, 0},
            {1, 1, 1, 1, 1},
            {0, 1, 1, 1, 0},
            {0, 0, 1, 0, 0}
    };

    /*
     * Arranges tiles into a grid.
     * @param rc RobotController at the center of the grid
     * @param nearbyTiles a list of MapInfo tiles.
     * @param radius The expected radius of tiles. If the radius is 1, the grid will be 3x3
     */
    public static MapInfo[][] arrangeMapInfos(RobotController rc, MapInfo[] nearbyTiles, int radius) {
        MapInfo[][] grid = new MapInfo[radius * 2 + 1][radius * 2 + 1];
        for (MapInfo tile : nearbyTiles) {
            if (Math.abs(tile.getMapLocation().x - rc.getLocation().x) <= radius &&
                    Math.abs(tile.getMapLocation().y - rc.getLocation().y) <= radius) {
                grid[tile.getMapLocation().x - rc.getLocation().x + radius]
                        [tile.getMapLocation().y - rc.getLocation().y + radius] =
                        tile;
            }
        }
        return grid;
    }

    /*
     * Arranges robots into a grid.
     * @param rc RobotController at the center of the grid
     * @param nearbyTiles a list of MapInfo tiles.
     * @param radius The expected radius of tiles. If the radius is 1, the grid will be 3x3
     */
    public static RobotInfo[][] arrangeRobotInfos(RobotController rc, RobotInfo[] nearbyRobots, int radius) {
        RobotInfo[][] grid = new RobotInfo[radius * 2 + 1][radius * 2 + 1];
        for (RobotInfo robot: nearbyRobots) {
            if (Math.abs(robot.getLocation().x - rc.getLocation().x) <= radius &&
                    Math.abs(robot.getLocation().y - rc.getLocation().y) <= radius) {
                grid[robot.getLocation().x - rc.getLocation().x + radius]
                        [robot.getLocation().y - rc.getLocation().y + radius] = robot;
            }
        }
        return grid;
    }

    /*
     * Returns a matrix whose elements are the desirability for that tile to be attacked by a splasher.
     * A larger element indicates that it is a desirable place for a splasher to attack.
     * The robot's tile is in the middle. (n / 2, n / 2) using integer division.
     * @param rc The splasher.
     * @param nearbyTiles MapInfo array returned by rc.getNearbyMapInfos()
     */
    public static float[][] attackAffinities(RobotController rc, MapInfo[] nearbyTiles) {
        int radius = 3;
        float[][] nearbyColorMatrix = new float[radius * 2 + 1][radius * 2 + 1];
        MapInfo[][] arrangedTiles = arrangeMapInfos(rc, nearbyTiles, radius);
        for (int i = 0; i < radius * 2 + 1; i++) {
            for (int j = 0; j < radius * 2 + 1; j++) {
                MapInfo tile = arrangedTiles[i][j];
                if (tile == null) { // out of bounds
                    nearbyColorMatrix[i][j] = -1.0f;
                } else if (tile.getPaint().isAlly()) { // ally tile
                    // we don't want to paint over ally tiles, it's a waste
                    // we definitely don't want to paint over towers in progress
                    nearbyColorMatrix[i][j] = tile.getMark().isSecondary() ? -2.0f : -1.0f;
                } else if (tile.isWall() || tile.hasRuin()) { // ruin or wall
                    nearbyColorMatrix[i][j] = -1.0f;
                } else if (tile.getPaint().equals(PaintType.ENEMY_PRIMARY) ||
                        tile.getPaint().equals(PaintType.ENEMY_SECONDARY)) {
                    // enemy tile is the best one to splat
                    nearbyColorMatrix[i][j] = 2.0f;
                } else if (tile.getPaint() == PaintType.EMPTY) {
                    // paint empty tiles
                    nearbyColorMatrix[i][j] = 1.2f;
                } else {
                    System.out.println("Splasher encountered unknown tile please tell what it is");
                    System.out.println(tile.getMapLocation());
                    nearbyColorMatrix[i][j] = 0;
                }
            }
        }
        return nearbyColorMatrix;
    }

    /*
     * Performs a convolution on the affinities and kernel
     * @param affinities Indicates which tiles should be painted
     */
    public static float[][] convolve(float[][] affinities) {
        if (affinities.length == 0 || affinities[0].length == 0 || kernel.length == 0 || kernel[0].length == 0) {
            throw new IllegalArgumentException("Arrays must be non-empty.");
        }
        if (affinities.length < kernel.length || affinities[0].length < kernel[0].length) {
            throw new IllegalArgumentException("Kernel size must be smaller than or equal to the affinities size.");
        }
        float[][] result =
                new float[affinities.length - kernel.length + 1][affinities[0].length - kernel[0].length + 1];
        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result[0].length; j++) {
                float sum = 0.0f;
                for (int x = 0; x < kernel.length; x++) {
                    for (int y = 0; y < kernel[0].length; y++) {
                        sum += affinities[i + x][j + y] * kernel[x][y];
                    }
                }
                result[i][j] = sum;
            }
        }
        return result;
    }

    /*
     * More streamlined convolution for the center 3x3 of the splasher's attack zone.
     * Convolution disregards 31% of attack zone for a 64% reduction in tiles examined
     */
    public static float[][] convolve3(float[][] affinities) {
        if (affinities.length == 0 || affinities[0].length == 0 || kernel.length == 0 || kernel[0].length == 0) {
            throw new IllegalArgumentException("Arrays must be non-empty.");
        }
        if (affinities.length < kernel.length || affinities[0].length < kernel[0].length) {
            throw new IllegalArgumentException("Kernel size must be smaller than or equal to the affinities size.");
        }
        float[][] result = new float[affinities.length - 2][affinities[0].length - 2];
        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result[0].length; j++) {
                float sum = 0.0f;
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 3; y++) {
                        sum += affinities[i + x][j + y];
                    }
                }
                result[i][j] = sum;
            }
        }
        return result;
    }

    /*
     * Attacks the best spot, if there is a good enough spot
     * @param rc Splasher
     * @param nearbyTiles MapInfo[] returned by rc.getNearbyMapInfos()
     * @param attackTotals 5x5 Result of the convolution
     * @threshold The attack will only occur if the best attack is greater than the threshold.
     * @returns True if an attack was done, otherwise false
     */
    public static boolean attackBest(RobotController rc, MapInfo[] nearbyTiles, float[][] attackTotals, float threshold) {
        MapInfo[][] mapInfoGrid = arrangeMapInfos(rc, nearbyTiles, 2);
        boolean[][] attemptAttack = new boolean[][]{
                {false, false, true, false, false},
                {false, true, true, true, false},
                {true, true, true, true, true},
                {false, true, true, true, false},
                {false, false, true, false, false}
        };
        System.out.println("Attack totals length: " + attackTotals.length);
        assert (attackTotals.length == 5);
        while (true) {
            float maxFound = threshold - 1;
            int maxi = -1;
            int maxj = -1;
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    if (!attemptAttack[i][j]) continue;
                    if (attackTotals[i][j] > maxFound) {
                        maxFound = attackTotals[i][j];
                        maxi = i;
                        maxj = j;
                    }
                }
            }
            if (maxFound < threshold) {
                // no good attack spot found
                return false;
            }
            if (mapInfoGrid[maxi][maxj] != null && rc.canAttack(mapInfoGrid[maxi][maxj].getMapLocation())) {
                try {
                    rc.attack(mapInfoGrid[maxi][maxj].getMapLocation());
                    return true;
                } catch (GameActionException gae) {
                    System.out.println("Exception in SplasherConvolution.attackBest()");
                    System.out.println(gae.getMessage());
                    gae.printStackTrace();
                }
            }
            // if that location couldn't be attacked, set it to false
            attemptAttack[maxi][maxj] = false;
        }
    }



    /*
     * Arranges tiles into a flat grid.
     * Tiles accessed by x * sidenlength + y
     * @param rc RobotController at the center of the grid
     * @param nearbyTiles a list of MapInfo tiles.
     * @param radius The expected radius of tiles. If the radius is 1, the grid will be 3x3
     */
    public static MapInfo[] arrangeMapInfosFlat(RobotController rc, MapInfo[] nearbyTiles, int radius) {
        int side = radius * 2 + 1;
        MapInfo[] grid = new MapInfo[side * side];
        for (MapInfo tile : nearbyTiles) {
            int xOffset = tile.getMapLocation().x - rc.getLocation().x;
            int yOffset = tile.getMapLocation().y - rc.getLocation().y;
            if (abs(xOffset) <= radius && abs(yOffset) <= radius) {
                grid[side * (xOffset + radius) + yOffset + radius] = tile;
            }
        }
        return grid;
    }

    /*
     * Arranges robots into a flat grid.
     * @param rc RobotController at the center of the grid
     * @param nearbyTiles a list of MapInfo tiles.
     * @param radius The expected radius of tiles. If the radius is 1, the grid will be 3x3
     */
    public static RobotInfo[] arrangeRobotInfosFlat(RobotController rc, RobotInfo[] nearbyRobots, int radius) {
        int side = radius * 2 + 1;
        RobotInfo[] grid = new RobotInfo[side * side];
        for (RobotInfo robot : nearbyRobots) {
            if (radius == 4 || (Math.abs(robot.getLocation().x - rc.getLocation().x) <= radius &&
                    Math.abs(robot.getLocation().y - rc.getLocation().y) <= radius)) {
                grid[
                        side * (robot.getLocation().x - rc.getLocation().x + radius) +
                                robot.getLocation().y - rc.getLocation().y + radius
                        ] = robot;
            }
        }
        return grid;
    }

    // Indexes for the fringes/center of an attack pattern in the 9x9 grid relative to an attack location in a 9x9 grid
    static final int[] attackFringes = new int[]{-18, -2, 2, 18};
    static final int[] attackCenter = new int[]{
            -10, -9, -8,
            -1, 0, 1,
            8, 9, 10
    };
    static final int[] attackToInfo = new int[]{
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33,
            38, 39, 40, 41, 42,
            47, 48, 49, 50, 51,
            56, 57, 58, 59, 60
    };
    /*
     * Returns the total reward for attacking anywhere on a 5x5 grid, considering:
     *   Every tile in every potential attack zone
     *   Enemy towers
     *   Out of bounds/blocking tiles
     *   Outer fringes are not painted if it's an enemy square
     *   It is a waste to paint already painted squares
     *   It is a really big waste if the secondary color of an incomplete tower is painted
     */
    public static float[] computeAttackTotals(RobotController rc, MapInfo[] nearbyTiles, RobotInfo[] nearbyRobots) {
        MapInfo[] mapInfoGrid = arrangeMapInfosFlat(rc, nearbyTiles, 4); // 9x9 center at 4, 4
        RobotInfo[] robotInfoGrid = arrangeRobotInfosFlat(rc, nearbyRobots, 4); // 9x9 center at 4,4
        float[] attackTotals = new float[25]; // 5x5, center at 2,2
        for (int attackIndex = 0; attackIndex < 25; attackIndex++) { // attackTotals index
            int attackLocation = attackToInfo[attackIndex];
            assert (attackLocation == 9 * (attackIndex / 5) + attackIndex % 5 + 20);
            for (int fringeOffset : attackFringes) {
                MapInfo tile = mapInfoGrid[attackLocation + fringeOffset];
                if (tile == null) {
                    continue;
                }
                PaintType color = tile.getPaint();
                if (color.isAlly()) {
                    if (tile.getMark().isSecondary()) {
                        // bad to paint over a secondary color if the tower is under construction
                        attackTotals[attackIndex] -= 1.0f;
                    } // otherwise it's just a waste, so 0 by default
                    continue;
                }
                RobotInfo robot = robotInfoGrid[attackLocation + fringeOffset];
                if (robot != null && robot.getType().isTowerType() && !robot.getTeam().equals(rc.getTeam())) {
                    // if it's an enemy tower, then that's the best thing to attack
                    attackTotals[attackIndex] += 100.0f;
                    continue;
                }
                if (color.equals(PaintType.EMPTY) && tile.isPassable()) {
                    attackTotals[attackIndex] += 1.0f;
                }
            }
            for (int attackCenterOffset : attackCenter) {
                MapInfo tile = mapInfoGrid[attackLocation + attackCenterOffset];
                if (tile == null) {
                    continue;
                }
                PaintType color = tile.getPaint();
                if (color.isAlly()) {
                    if (tile.getMark().isSecondary()) {
                        attackTotals[attackIndex] -= 1.0f;
                    }
                    continue;
                }
                if (color.equals(PaintType.ENEMY_PRIMARY) || color.equals(PaintType.ENEMY_SECONDARY)) {
                    // splashers can paint over enemy tiles here
                    attackTotals[attackIndex] += 2.0f;
                    continue;
                }
                RobotInfo robot = robotInfoGrid[attackLocation + attackCenterOffset];
                if (robot != null && robot.getType().isTowerType() && !robot.getTeam().equals(rc.getTeam())) {
                    attackTotals[attackIndex] += 100.0f;
                    continue;
                }
                if (tile.isPassable()) {
                    // neutral tile
                    attackTotals[attackIndex] += 1.0f;
                }
            }
        }
        return attackTotals;
    }

    public static boolean attackBestFlat(RobotController rc, MapInfo[] nearbyTiles, float[] attackTotals, float threshold) {
        MapInfo[] mapInfoGrid = arrangeMapInfosFlat(rc, nearbyTiles, 2);
        boolean[] attemptAttack = new boolean[25];
        while (true) {
            float maxFound = threshold - 1;
            int maxIndex = -1;
            for (int attackableIndex : attackableIndices) {
                if (attemptAttack[attackableIndex]) continue;
                if (attackTotals[attackableIndex] > maxFound) {
                    maxFound = attackTotals[attackableIndex];
                    maxIndex = attackableIndex;
                }
            }
            if (maxFound < threshold) {
                // no good attack spot found
                return false;
            }
            if (mapInfoGrid[maxIndex] != null && rc.canAttack(mapInfoGrid[maxIndex].getMapLocation())) {
                try {
                    rc.attack(mapInfoGrid[maxIndex].getMapLocation());
                    return true;
                } catch (GameActionException gae) {
                    System.out.println("Exception in SplasherConvolution.attackBest()");
                    System.out.println(gae.getMessage());
                    gae.printStackTrace();
                }
            }
            // if that location couldn't be attacked, set it to false
            attemptAttack[maxIndex] = true;
        }
    }
}
