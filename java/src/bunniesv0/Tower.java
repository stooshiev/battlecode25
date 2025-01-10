package bunniesv0;

import battlecode.common.*;

public class Tower extends RobotPlayer {
	
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
	
	public static void createRandomRobot(RobotController rc) throws GameActionException {
		// Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        // Pick a random robot type to build.
        int robotType = rng.nextInt(3);
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
        }
	}
	
}
