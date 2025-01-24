package bunniesv3;

import battlecode.common.*;

class Tower extends RobotPlayer {
	
	public static void runTurnBasedActions(RobotController rc) throws GameActionException {
		if (getTowerLevel(rc) != 3 && rc.getMoney() >= rc.getType().getNextLevel().moneyCost + (getTowerType(rc) == "Money" ? 1 : 2) * UnitType.MOPPER.moneyCost) {
			state = "UPGRADE_SAVING";
			if (rc.canUpgradeTower(rc.getLocation())) {
				rc.upgradeTower(rc.getLocation());
				state = "DEFAULT";
			}
		}
		else if (turnCount % 100 == 99 || state == "BUILD_SAVING") {
			state = "BUILD_SAVING";
			if (rc.getMoney() >= 1300) {
				state = "DEFAULT";
			}
		}
	}
	
	public static void attackPattern0(RobotController rc, MapInfo[] nearbyTiles, RobotInfo[] nearbyRobots) throws GameActionException {

		int rcAttackStrength = rc.getType().attackStrength;
		
		// Does AoE attack no matter what.
		rc.attack(null);
		
		// Aims at the enemy robot it can one-shot with the highest paint count.
		// Aims at the enemy robot with the lowest health if there are none it can one-shot.
		RobotInfo targetRobot = null;
		for (RobotInfo robot : nearbyRobots){
        	if (robot.team != rc.getTeam() && rc.canAttack(robot.location)) {
        		if (targetRobot == null) {
        			targetRobot = robot;
        		}
        		else if (robot.health <= rcAttackStrength && targetRobot.health <= rcAttackStrength) {
        			
        			if (robot.paintAmount > targetRobot.paintAmount) {
        				targetRobot = robot;
        			}
        		}
        		else if (robot.health < targetRobot.health) {
        			targetRobot = robot;
        		}
        	}
        }
		if (targetRobot != null && rc.canAttack(targetRobot.location)) {
			rc.attack(targetRobot.location);
			rc.setIndicatorString("ATTACKED ROBOT AT (" + targetRobot.location.x + ", " + targetRobot.location.y + ")");
			// Builds a mopper in the direction of the target enemy, if possible, if it last sensed three or more enemies.
			if (nearbyRobots.length >= 3) {
				Direction dir = rc.getLocation().directionTo(targetRobot.location);
				MapLocation nextLoc = rc.getLocation().add(dir);
				rc.buildRobot(UnitType.MOPPER, nextLoc);
	            System.out.println("BUILT A MOPPER");
	            rc.setIndicatorString("BUILT A MOPPER");
			}
		}
		
	}
	
	public static RobotInfo createRobot(RobotController rc, int robotType) throws GameActionException {
		
		// Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        // Pick a robot type to build.
        if (robotType == 0 && rc.canBuildRobot(UnitType.SOLDIER, nextLoc)){
            rc.buildRobot(UnitType.SOLDIER, nextLoc);
            System.out.println("BUILT A SOLDIER");
            rc.setIndicatorString("BUILT A SOLDIER");
        }
        else if (robotType == 1 && rc.canBuildRobot(UnitType.MOPPER, nextLoc)){
            rc.buildRobot(UnitType.MOPPER, nextLoc);
            System.out.println("BUILT A MOPPER");
            rc.setIndicatorString("BUILT A MOPPER");
        }
        else if (robotType == 2 && rc.canBuildRobot(UnitType.SPLASHER, nextLoc)){
            rc.buildRobot(UnitType.SPLASHER, nextLoc);
            System.out.println("BUILT A SPLASHER");
            rc.setIndicatorString("BUILT A SPLASHER");
        }
        return rc.senseRobotAtLocation(nextLoc);
	}
	
	public static void createRobot(RobotController rc) throws GameActionException {
		createRobot(rc, rng.nextInt(3));
	}
	
	public static void actOnMessages(RobotController rc, UnpackedMessage[] unpackedMessages,
									 MapInfo[] nearbyTiles, RobotInfo[] nearbyRobots) throws GameActionException {
		robotInNeed = null;
		for (UnpackedMessage m : unpackedMessages) {
			System.out.println("m: " + m);
			if (m != null && turnCount - m.senderTurn <= 1) {
				switch (m.command) {
					case 0: break; // Save Chips
					case 1: break; // Send Robots
					case 2: break; // Send Soldiers
					case 3: sendMoppers(rc, m.locInfo, nearbyTiles, nearbyRobots); break; // Send Moppers
					case 4: break; // Send Splashers
					case UnpackedMessage.REQUEST_PAINT: considerPaintRequest(rc, m.locInfo); break;
				}
			}
		}
		allowPaintRequest(rc);
	}

	public static RobotInfo robotInNeed = null;
	
	public static void considerPaintRequest(RobotController rc, MapLocation loc) throws GameActionException {
		System.out.println(loc);
		RobotInfo robot = rc.senseRobotAtLocation(loc);
		if (rc.getPaint() >= 150 && (float) robot.getPaintAmount() / robot.getType().paintCapacity <= 0.7) {
			if (robotInNeed == null) {
				robotInNeed = robot;
			}
			else if (robotInNeed.type.paintCapacity - robotInNeed.paintAmount < robot.type.paintCapacity - robot.paintAmount) {
				try {
					UnpackedMessage.encodeAndSend(rc, robotInNeed.location, UnpackedMessage.PAINT_DENIED);
				} catch (GameActionException ignored) { }
				robotInNeed = robot;
			}
		}
	}
	
	public static void allowPaintRequest(RobotController rc) throws GameActionException {
		if (robotInNeed != null) {
			rc.setIndicatorString("Robot in Need at " + robotInNeed.location);
			try {
				UnpackedMessage.encodeAndSend(rc, robotInNeed.location, UnpackedMessage.TAKE_PAINT, rc.getLocation(),
						Math.min(robotInNeed.type.paintCapacity - robotInNeed.paintAmount, rc.getPaint() - 100));
			} catch (GameActionException ignored) { }
		}
	}
	
	public static void sendMoppers(RobotController rc, MapLocation target, MapInfo[] nearbyTiles, RobotInfo[] nearbyRobots) throws GameActionException {
		boolean nearbyRobot = false;
		for (RobotInfo robot : nearbyRobots) {
			if (robot.getTeam() == rc.getTeam() && robot.getType() == UnitType.MOPPER) {
				UnpackedMessage.encodeAndSend(rc, robot.getLocation(), UnpackedMessage.GO_TO, target);
				nearbyRobot = true;
				break;
			}
		}
		if (nearbyRobot == false) {
			RobotInfo newRobotInfo = createRobot(rc, 1);
			UnpackedMessage.encodeAndSend(rc, newRobotInfo.getLocation(), UnpackedMessage.GO_TO, target);
		}

	}
	
	public static String getTowerType(RobotController rc) throws GameActionException {
		UnitType towerType = rc.getType();
		if (towerType == UnitType.LEVEL_ONE_PAINT_TOWER || towerType == UnitType.LEVEL_TWO_PAINT_TOWER || towerType == UnitType.LEVEL_THREE_PAINT_TOWER) {
			return "Paint";
		}
		else if (towerType == UnitType.LEVEL_ONE_MONEY_TOWER || towerType == UnitType.LEVEL_TWO_MONEY_TOWER || towerType == UnitType.LEVEL_THREE_MONEY_TOWER) {
			return "Money";
		}
		else if (towerType == UnitType.LEVEL_ONE_DEFENSE_TOWER || towerType == UnitType.LEVEL_TWO_DEFENSE_TOWER || towerType == UnitType.LEVEL_THREE_DEFENSE_TOWER) {
			return "Defense";
		}
		else {
			return "Bunny";
		}
	}
	
	public static int getTowerLevel(RobotController rc) throws GameActionException {
		UnitType towerType = rc.getType();
		if (towerType == UnitType.LEVEL_ONE_PAINT_TOWER || towerType == UnitType.LEVEL_ONE_MONEY_TOWER || towerType == UnitType.LEVEL_ONE_DEFENSE_TOWER) {
			return 1;
		}
		else if (towerType == UnitType.LEVEL_TWO_PAINT_TOWER || towerType == UnitType.LEVEL_TWO_MONEY_TOWER || towerType == UnitType.LEVEL_TWO_DEFENSE_TOWER) {
			return 2;
		}
		else if (towerType == UnitType.LEVEL_THREE_PAINT_TOWER || towerType == UnitType.LEVEL_THREE_MONEY_TOWER || towerType == UnitType.LEVEL_THREE_DEFENSE_TOWER) {
			return 3;
		}
		else {
			return -1;
		}
	}
}