package bunniesv0;

import battlecode.common.*;

import static java.lang.Math.*;

public class SplasherConvolution {
    public class Tuple<X, Y> {
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
                } else if (tile.getPaint() == PaintType.ENEMY_PRIMARY || tile.getPaint() == PaintType.ENEMY_SECONDARY) {
                    // enemy tile
                    nearbyColorMatrix[i][j] = 1.0f;
                } else if (tile.getPaint() == PaintType.EMPTY) {
                    // paint empty tiles
                    nearbyColorMatrix[i][j] = 1.2f;
                } else {
                    System.out.println("Splasher encountered unknown tile please tell what it is");
                    System.out.println(tile.getMapLocation());
//                    try {
//                        rc.setIndicatorDot(tile.getMapLocation(), 0, 255, 0);
//                    } catch (GameActionException e) {
//                        System.out.println("Couldn't place an indicator dot sorry");
//                    }
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
}
