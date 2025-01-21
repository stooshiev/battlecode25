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
	
	public static void refillRobots(RobotController rc, RobotInfo[] nearbyRobots) throws GameActionException {
		RobotInfo targetRobot = null;
		for (RobotInfo robot : nearbyRobots) {
			if (rc.getLocation().isAdjacentTo(robot.location) && rc.getTeam() == rc.getTeam() && (float) robot.getPaintAmount() / robot.getType().paintCapacity <= 0.7) {
				if (targetRobot == null) {
					targetRobot = robot;
				}
				else if (targetRobot.type.paintCapacity - targetRobot.paintAmount <= robot.type.paintCapacity - robot.paintAmount) {
					targetRobot = robot;
				}
			}
		}
		if (targetRobot != null) {
			UnpackedMessage.encodeAndSend(rc, targetRobot.location, UnpackedMessage.TAKE_PAINT, rc.getLocation());
		}
		
	}
	
	public static void actOnMessages(RobotController rc, UnpackedMessage[] unpackedMessages, MapInfo[] nearbyTiles, RobotInfo[] nearbyRobots) throws GameActionException {
		for (UnpackedMessage m : unpackedMessages) {
			switch (m.command) {
				case 0: break; // Save Chips
				case 1: break; // Send Robots
				case 2: break; // Send Soldiers
				case 3: sendMoppers(rc, m.locInfo, nearbyTiles, nearbyRobots); break; // Send Moppers
				case 4: break; // Send Splashers
			}
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
