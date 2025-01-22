package bunniesv3;

import java.util.HashSet;

import battlecode.common.*;

public class Mopper extends RobotPlayer {

	public static void updateMapMemory(RobotController rc) throws GameActionException {
		MapInfo[] allMapInfo = rc.senseNearbyMapInfos();
		updateMapMemory(rc, allMapInfo);
	}

	// overloaded to avoid double calculation of rc.senseNearbyMapInfos();
	public static void updateMapMemory(RobotController rc, MapInfo[] allMapInfo) {
		seenEnemyPaint(rc, allMapInfo);
		for (MapInfo tile : allMapInfo) {

			// if statements skip all other calculations bc they are unnecessary if true
			if (tile.isWall()) {
				continue;
			} // walls are not important enough to save in memory

			MapLocation tileLocation = tile.getMapLocation();

			int correspondingNum = 1;
			try {
				RobotInfo robotOnTile = rc.senseRobotAtLocation(tileLocation);
				if (robotOnTile == null) {
	    			continue;
				}

				else {
					UnitType robotType = robotOnTile.getType();
					// System.out.println("ROBOT TYPE: " + robotType.toString());
					if (robotType.isTowerType()) { // real meat and potatoes of the function, differentiates all tower
													// types

						// if it is a paint tower
						if (robotType.equals(UnitType.LEVEL_ONE_PAINT_TOWER)
								|| robotType.equals(UnitType.LEVEL_TWO_PAINT_TOWER)
								|| robotType.equals(UnitType.LEVEL_THREE_PAINT_TOWER)) {
							correspondingNum = 2;
						} // technically unnecessary since default number is two (scuffed coding)

						// if it is a chip tower
						else if (robotType.equals(UnitType.LEVEL_ONE_MONEY_TOWER)
								|| robotType.equals(UnitType.LEVEL_TWO_MONEY_TOWER)
								|| robotType.equals(UnitType.LEVEL_THREE_MONEY_TOWER)) {
							correspondingNum = 3;
						}

						// if it is a defense tower
						else if (robotType.equals(UnitType.LEVEL_ONE_DEFENSE_TOWER)
								|| robotType.equals(UnitType.LEVEL_TWO_DEFENSE_TOWER)
								|| robotType.equals(UnitType.LEVEL_THREE_DEFENSE_TOWER)) {
							correspondingNum = 4;
						}

						if (isEnemy(rc, robotOnTile)) {
							correspondingNum += 3;
						}
					}

				}

				// removes old stuff
				if (mapMemory.keySet().contains(tileLocation)) {
					int oldNum = mapMemory.get(tileLocation);
					if (oldNum != correspondingNum) {
						importantLocations.get(oldNum).remove(tileLocation);
						mapMemory.put(tileLocation, correspondingNum);
						importantLocations.get(correspondingNum).add(tileLocation);
					}
				}

				else {
					mapMemory.put(tileLocation, correspondingNum);
					importantLocations.get(correspondingNum).add(tileLocation);
				}

			}

			catch (GameActionException e) { // exception "should" never happen as all locations in allMapInfo are within
											// vision range
				continue;
			}
//    		}
		}
	}

	// updates memory if enemy paint is within view from current location
	public static void seenEnemyPaint(RobotController rc, MapInfo[] allMapInfo) {

		boolean isEnemyPaint = false;
		MapLocation currentLocation = rc.getLocation();

		for (MapInfo tile : allMapInfo) {
			MapLocation tileLocation = tile.getMapLocation();
			try {
				RobotInfo robotOnTile = rc.senseRobotAtLocation(tileLocation);
				if (robotOnTile == null) {
					if (tile.getPaint().equals(PaintType.ENEMY_PRIMARY)
							|| tile.getPaint().equals(PaintType.ENEMY_SECONDARY)) {
						isEnemyPaint = true;
						break;
					}
				}
			} catch (GameActionException e) {
				continue;
			}

		}

		if (isEnemyPaint) {
			if (mapMemory.keySet().contains(currentLocation)) {
				int oldNum = mapMemory.get(currentLocation);
				if (oldNum != 8) { // sneaking suspicion that this if statement is never true
					importantLocations.get(oldNum).remove(currentLocation);
					mapMemory.remove(currentLocation);

					importantLocations.get(8).add(currentLocation);
					mapMemory.put(currentLocation, 8);
				} else {
					// do nothing
				}
			} else {
				importantLocations.get(8).add(currentLocation);
				mapMemory.put(currentLocation, 8);
			}

		} else {
			if (mapMemory.keySet().contains(currentLocation)) {
				int oldNum = mapMemory.get(currentLocation);
				if (oldNum == 8) { // might ALWAYS be true
					importantLocations.get(oldNum).remove(currentLocation);
					mapMemory.remove(currentLocation);

				} else {
					// do nothing
				}
			}
		}
	}

	public static Direction nearbyEnemyPaintDirection(RobotController rc, int radiussquared) {
		try {
			MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(radiussquared);

			for (MapInfo tile : nearbyTiles) {

				if (tile.getPaint().equals(PaintType.ENEMY_PRIMARY)
						|| tile.getPaint().equals(PaintType.ENEMY_SECONDARY)) {
					return rc.getLocation().directionTo(tile.getMapLocation());
				}

			}

			return Direction.CENTER;
		} catch (GameActionException e) { // shouldnt ever happen, only here because java will get mad if it's not
			return Direction.CENTER;
		}
	}

	public static Direction nearbyEnemyPaintDirection(RobotController rc) {
		MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();

		for (MapInfo tile : nearbyTiles) {

			if (tile.getPaint().equals(PaintType.ENEMY_PRIMARY) || tile.getPaint().equals(PaintType.ENEMY_SECONDARY)) {
				return rc.getLocation().directionTo(tile.getMapLocation());
			}

		}

		return Direction.CENTER;
	}

	public static boolean isEnemy(RobotController rc, RobotInfo otherRobot) {
		return rc.getTeam().opponent().equals(otherRobot.getTeam());
	}

	// returns current location if no paint towers have been seen yet
	public static MapLocation findNearestStructure(RobotController rc, int StructID) {
		MapLocation currentLocation = rc.getLocation();
		HashSet<MapLocation> allMatchingTiles = importantLocations.get(StructID); // all paint towers seen

		if (allMatchingTiles.isEmpty()) {
			return currentLocation;
		}

		// simple "min" algorithm, try optimizing to avoid recalculation!
		MapLocation nearestLocation = currentLocation;
		int shortestDistanceSquared = 1000000;
		for (MapLocation tileLocation : allMatchingTiles) {

			// actual functionality
			int distanceSquared = currentLocation.distanceSquaredTo(tileLocation);
			if (distanceSquared < shortestDistanceSquared) {
				shortestDistanceSquared = distanceSquared;
				nearestLocation = tileLocation;
				if (shortestDistanceSquared <= 2) { // if it is literally impossible to find a closer location
					return nearestLocation;
				}
			}
		}

		return nearestLocation;
	}

	// Finds the best direction to strike; currently swings in direction with most
	// robots, if no robots, returns center
	public static Direction optimalSwing(RobotController rc) {
		int[] swingValues = getSwingValues(rc);
		int maxInd = 0;
		int maxVal = 0;

		for (int ind = 0; ind < 4; ind++) {

			if (swingValues[ind] > maxVal) {
				maxVal = swingValues[ind];
				maxInd = ind;
			}

		}

		if (maxVal == 0) {
			return Direction.CENTER;
		}
		return directions[maxInd * 2]; // swingValue index * 2 = directions index (e.g. South in swingValue is 2, South
										// in directions is 4)

	}

	// Calculates how many robots are hit in a single swing for all cardinal
	// directions (DEPRECATED SINCE VERSION 2.0.0)
	/*
	 * public static int[] getSwingValues(RobotController rc, RobotInfo[]
	 * enemyRobots) { int[] swingValues = {0, 0, 0, 0}; //Index 0: NORTH, Index 1:
	 * EAST, Index 2: SOUTH, Index 3: WEST
	 * 
	 * //Return all 0s if there are no enemy robots around (obviously) if
	 * (enemyRobots.length > 0) {
	 * 
	 * int[] robotSpots = {0, 0, 0, 0, 0, 0, 0, 0}; //0 for no robot, 1 for robot,
	 * in the same order as static variable directions
	 * 
	 * //Iterate through all the enemy robots for (RobotInfo robot : enemyRobots) {
	 * Direction enemyDir = rc.getLocation().directionTo(robot.getLocation());
	 * //direction to the enemy from current robot
	 * robotSpots[directionToInteger.get(enemyDir)] = 1; }
	 * 
	 * swingValues[0] = robotSpots[0] + robotSpots[1] + robotSpots[7]; //sum of
	 * north, northeast, and northwest values; same scheme for all other directions
	 * swingValues[1] = robotSpots[1] + robotSpots[2] + robotSpots[3];
	 * swingValues[2] = robotSpots[3] + robotSpots[4] + robotSpots[5];
	 * swingValues[3] = robotSpots[5] + robotSpots[6] + robotSpots[7];
	 * 
	 * }
	 * 
	 * return swingValues; }
	 */

	// Calculates how many robots are hit in a single swing for all cardinal
	// directions
	public static int[] getSwingValues(RobotController rc) {
		int[] swingValues = { 0, 0, 0, 0 }; // Index 0: NORTH, Index 1: EAST, Index 2: SOUTH, Index 3: WEST
		MapLocation currentLoc = rc.getLocation();

		// all relative locations hit (e.g. (+1, +1) from current location) for each
		// cardinal direction
		int[] NorthRLocsX = { 0, 0, 1, 1, -1, -1 };
		int[] NorthRLocsY = { 1, 2, 1, 2, 1, 2 };
		int[] EastRLocsX = { 1, 2, 1, 2, 1, 2 };
		int[] EastRLocsY = { 0, 0, 1, 1, -1, -1 };
		int[] SouthRLocsX = { 0, 0, 1, 1, -1, -1 };
		int[] SouthRLocsY = { -1, -2, -1, -2, -1, -2 };
		int[] WestRLocsX = { -1, -2, -1, -2, -1, -2 };
		int[] WestRLocsY = { 0, 0, 1, 1, -1, -1 };

		// each direction has 6 tiles to check
		for (int i = 0; i < 6; i++) {
			try {
				// uses senseRobotAtLocation instead of canSenseRobotAtLocation in case location
				// is out of bounds
				boolean isRobotN = rc
						.senseRobotAtLocation(currentLoc.translate(NorthRLocsX[i], NorthRLocsY[i])) == null;
				if (isRobotN) {
					swingValues[0] += 1;
				}
			} catch (GameActionException e) {
				// do nothing
			}
			try {
				// uses senseRobotAtLocation instead of canSenseRobotAtLocation in case location
				// is out of bounds
				boolean isRobotE = rc.senseRobotAtLocation(currentLoc.translate(EastRLocsX[i], EastRLocsY[i])) == null;
				if (isRobotE) {
					swingValues[1] += 1;
				}
			} catch (GameActionException e) {
				// do nothing
			}
			try {
				// uses senseRobotAtLocation instead of canSenseRobotAtLocation in case location
				// is out of bounds
				boolean isRobotS = rc
						.senseRobotAtLocation(currentLoc.translate(SouthRLocsX[i], SouthRLocsY[i])) == null;
				if (isRobotS) {
					swingValues[2] += 1;
				}
			} catch (GameActionException e) {
				// do nothing
			}
			try {
				// uses senseRobotAtLocation instead of canSenseRobotAtLocation in case location
				// is out of bounds
				boolean isRobotW = rc.senseRobotAtLocation(currentLoc.translate(WestRLocsX[i], WestRLocsY[i])) == null;
				if (isRobotW) {
					swingValues[3] += 1;
				}
			} catch (GameActionException e) {
				// do nothing
			}
		}

		return swingValues;
	}

	// Checks for enemy robots in a certain direction
	public static boolean directionEnemyCheck(RobotController rc, Direction dir, RobotInfo[] enemyRobots) {
		if (enemyRobots.length == 0) {
			return false;
		} // if there are no enemies then automatic false
		for (RobotInfo robot : enemyRobots) {
			if (rc.getLocation().directionTo(robot.getLocation()).equals(dir)) { // if the enemy's location is in the
																					// same direction as specified
				return true;
			}
		}
		return false;
	}

	public static RobotInfo[] findEnemyRobots(RobotController rc) throws GameActionException {
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		return enemyRobots;
	}

	// Overloaded version for checking within a specific radius
	public static RobotInfo[] findEnemyRobots(RobotController rc, int radius) throws GameActionException {
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(radius, rc.getTeam().opponent());
		return enemyRobots;
	}

	public static RobotInfo[] findFriendlyRobots(RobotController rc) throws GameActionException {
		RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
		return friendlyRobots;
	}

	// Overloaded version for checking within a specific radius
	public static RobotInfo[] findFriendlyRobots(RobotController rc, int radius) throws GameActionException {
		RobotInfo[] friendlyRobots = rc.senseNearbyRobots(radius, rc.getTeam());
		return friendlyRobots;
	}

	public static MapLocation findNearestFriendlySoldier(RobotController rc) throws GameActionException {
		RobotInfo[] friendlyRobots = findFriendlyRobots(rc);
		MapLocation currentLocation = rc.getLocation();

		// returns currentLocation if it cannot find any nearby friendly soldiers
		if (friendlyRobots.length == 0) {
			return currentLocation;
		}

		MapLocation nearestLocation = currentLocation;
		int shortestDistanceSquared = 100000;
		for (RobotInfo robot : friendlyRobots) {

			if (robot.getType().equals(UnitType.SOLDIER)) {

				MapLocation soldierLocation = robot.getLocation();
				int distanceSquared = currentLocation.distanceSquaredTo(soldierLocation);
				if (distanceSquared < shortestDistanceSquared) {
					nearestLocation = soldierLocation;
					shortestDistanceSquared = distanceSquared;
				}

			}

		}

		return nearestLocation;

	}

	public static void transferPaintMoppers(RobotController rc, MapLocation currentLocation, Direction paintTowerDir)
			throws GameActionException {
		int paintInTower = rc.senseRobotAtLocation(currentLocation.add(paintTowerDir)).getPaintAmount();
		int transferAmount = Math.max(-paintInTower, rc.getPaint() - 90); // needs to be the lesser magnitude of the
																			// two; hence the max not min
		if (rc.canTransferPaint(currentLocation.add(paintTowerDir), transferAmount)) {
//            System.out.println("TRANSFERING PAINT | PAINT AMOUNT: " + transferAmount);
			rc.transferPaint(currentLocation.add(paintTowerDir), transferAmount);
		}
	}

	// Pick new direction to move if cant move in picked direction, stops after all
	// directions are tried
	public static void findValidMoveDir(RobotController rc) {
		int counter = 0;
		while (!rc.canMove(dir) && counter < 8) { // if that direction is invalid change it
			dir = dir.rotateLeft();
			counter++;
		}

		if (counter == 8) {
			dir = Direction.CENTER;
		}
	}

	public static void mopperAttackBehavior(RobotController rc, MapLocation currentLocation, Direction attackDirection)
			throws GameActionException {
		if (!attackDirection.equals(Direction.CENTER)) {
			if (rc.isActionReady()) {
				rc.attack(currentLocation.add(attackDirection));
				// dir = attackDirection;
			}
			/*
			 * else { //stay there until can attack so the paint doesnt disappear from
			 * attack range dir = Direction.CENTER; }
			 */
		}
		// encourages mopper to chase after enemy paint
		/*
		 * else { Direction nearbyEnemyPaint = Mopper.nearbyEnemyPaintDirection(rc, -1);
		 * if (!nearbyEnemyPaint.equals(Direction.CENTER)) { dir = nearbyEnemyPaint;
		 * 
		 * } }
		 */
	}

	// random chance (lower later into the game) to turn left or right; encourages
	// exploration
	public static void explore(RobotController rc) {
		if (rng.nextInt(10 + rc.getRoundNum() / 10) == 0) { // less of a chance later in the game

			int directionDecider = rng.nextInt(2);

			if (directionDecider == 1) {
				dir = dir.rotateRight();
			} else {
				dir = dir.rotateLeft();
			}

		}
	}
}
