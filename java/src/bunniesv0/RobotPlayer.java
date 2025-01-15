package bunniesv0;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public class RobotPlayer {
    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;
    
    static int creationTurn = 0;
    
    static String state = "DEFAULT";

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final long RANDOM_SEED = 6147;
    static final Random rng = new Random(RANDOM_SEED);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };
    
    static HashMap<Direction, Integer> directionToInteger = new HashMap<>();
    static HashMap<MapLocation, Integer> mapMemory = new HashMap<>(); 
    //Each integer represents something different: 1 - ruin, 2 - friendly paint tower, 3 - friendly chip tower, 4 - friendly defense tower, 
    //5 - enemy paint tower, 6 - enemy chip tower, 7 - enemy defense tower
    static HashMap<Integer, HashSet<MapLocation>> importantLocations = new HashMap<>();
    //Using the same integer pattern as mapMemory, keeps track of all coordinates/tiles with corresponding important structure
    
    static Direction prevDir = Direction.CENTER; //previous direction robot moved (if robot is a bunny)
    

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        System.out.println("I'm alive");

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");
        
        creationTurn = turnCount;
        
        //Add directions to direction to integer
        directionToInteger.put(Direction.NORTH, 0);
        directionToInteger.put(Direction.NORTHEAST, 1);
        directionToInteger.put(Direction.EAST, 2);
        directionToInteger.put(Direction.SOUTHEAST, 3);
        directionToInteger.put(Direction.SOUTH, 4);
        directionToInteger.put(Direction.SOUTHWEST, 5);
        directionToInteger.put(Direction.WEST, 6);
        directionToInteger.put(Direction.NORTHWEST, 7);
        
        //add empty sets to important locations
        importantLocations.put(1,  new HashSet<MapLocation>());
        importantLocations.put(2,  new HashSet<MapLocation>());
        importantLocations.put(3,  new HashSet<MapLocation>());
        importantLocations.put(4,  new HashSet<MapLocation>());
        importantLocations.put(5,  new HashSet<MapLocation>());
        importantLocations.put(6,  new HashSet<MapLocation>());
        importantLocations.put(7,  new HashSet<MapLocation>());

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the UnitType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()){
                    case SOLDIER: runSoldier(rc); break; 
                    case MOPPER: runMopper(rc); break;
                    case SPLASHER: runSplasher(rc); break; // Consider upgrading examplefuncsplayer to use splashers!
                    default: runTower(rc); break;
                    }
                }
             catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    /**
     * Run a single turn for towers.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runTower(RobotController rc) throws GameActionException{
    	// Sense information about all visible nearby tiles and robots.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        
        // Planned turn-based actions
        Tower.runTurnBasedActions(rc);
        
        // Read incoming messages
        UnpackedMessage[] unpackedMessages = UnpackedMessage.receiveAndDecode(rc);
        
        Tower.actOnMessages(rc, unpackedMessages, nearbyTiles, nearbyRobots);
        
        Tower.attackPattern0(rc, nearbyTiles, nearbyRobots);
    	
        if (state == "DEFAULT") { // Run all the default behavior
        	if (Tower.getTowerType(rc) == "Paint" && rc.getPaint() >= 450) {
    			Tower.refillRobots(rc, nearbyRobots);
    		}
    		if (Tower.getTowerType(rc) == "Paint" && rc.getPaint() >= 400 && rc.getMoney() >= 700) {
    			Tower.createRobot(rc);
    		}
        }
        
    }


    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */

    public static void runSoldier(RobotController rc) throws GameActionException{
    	// Sense information about all visible nearby tiles.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        boolean isMarking = false;
        
        // Search for a nearby ruin to complete.
        MapInfo curRuin = null;
        for (MapInfo tile : nearbyTiles){
            if (tile.hasRuin()){
            	if (curRuin != null) {
            		if (curRuin.getMapLocation().distanceSquaredTo(rc.getLocation()) > tile.getMapLocation().distanceSquaredTo(rc.getLocation())) {
            			//Soldier.checkTowerLoc(rc, tile);
            			curRuin = tile;
            		}
            	} else {
            		//Soldier.checkTowerLoc(rc, tile);
                    curRuin = tile;
            	}
            }
        }
        
//        if (curRuin != null) {
//        	rc.setIndicatorString(Soldier.getTowerLoc().toString() + " : " + rc.senseRobotAtLocation(Soldier.getTowerLoc()).getTeam().toString());
//        } else {
//        	rc.setIndicatorString("No Ruin Available");
//        }
		
        // Read incoming messages
        //UnpackedMessage[] unpackedMessages = UnpackedMessage.receiveAndDecode(rc);
        

//        if (rc.getPaint() <= 50) {
//        	MapLocation closeTower = Soldier.getTowerLoc();
//        	Direction moveDir = Soldier.getShortestPathDir(rc, Soldier.getTowerLoc());
//        	if (rc.canMove(moveDir))
//	        	rc.move(moveDir);
////        	for (int i = 0; i < unpackedMessages.length; i++) {
////        		if (unpackedMessages[i].equals("Take Paint")) {
////        			rc.setIndicatorString("Taking Paint From Tower");
////        		}
////        	}
////    		rc.setIndicatorString(Arrays.toString(unpackedMessages));
////        	if (rc.canSenseLocation(closeTower)) {
////        		
////        	}
//        	if (rc.canTransferPaint(closeTower, -50)) {
//        		rc.transferPaint(closeTower, -50);
//        	}
//        } else 
        if (curRuin != null){
            MapLocation targetLoc = curRuin.getMapLocation();
            Direction dir = rc.getLocation().directionTo(targetLoc);
            Direction moveDir = Soldier.getShortestPathDir(rc, targetLoc);
            // Mark the pattern we need to draw to build a tower here if we haven't already.
            MapLocation shouldBeMarked = curRuin.getMapLocation().subtract(dir);
            if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
            	rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
                System.out.println("Trying to build a tower at " + targetLoc);
            }
            // Fill in any spots in the pattern with the appropriate paint.
        	String[] attackTiles = new String[25];
        	int curAttack = 0;
            for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, 8)){
            	
            	if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY){
            	//if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY){
                    isMarking = true;
                    boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                    if (rc.canAttack(patternTile.getMapLocation()) && (rc.senseMapInfo(patternTile.getMapLocation()).getPaint() != PaintType.ENEMY_PRIMARY && rc.senseMapInfo(patternTile.getMapLocation()).getPaint() != PaintType.ENEMY_SECONDARY)) {
                        rc.attack(patternTile.getMapLocation(), useSecondaryColor);
                        //attackTiles += attackTiles + ", " + patternTile.getMapLocation().toString();
                    }
                }
            	attackTiles[curAttack] = patternTile.getMapLocation().toString();
            	curAttack += 1;
            }
            // Complete the ruin if we can.
            if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
                rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
                rc.setTimelineMarker("Tower built", 0, 255, 0);
                System.out.println("Built a tower at " + targetLoc + "!");
            }
            if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetLoc)){
                rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetLoc);
                rc.setTimelineMarker("Tower built", 0, 255, 0);
                System.out.println("Built a tower at " + targetLoc + "!");
            }
            if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, targetLoc)){
                rc.completeTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, targetLoc);
                rc.setTimelineMarker("Tower built", 0, 255, 0);
                System.out.println("Built a tower at " + targetLoc + "!");
            }
            
            if (isMarking) {
		        
		        if (rc.canMove(moveDir))
		        	rc.move(moveDir);
            }
        }
        if (!isMarking) {

        // Move and attack randomly if no objective.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        if (rc.canMove(dir)){
        	rc.move(dir);
        }
        }
        // Try to paint beneath us as we walk to avoid paint penalties.
        // Avoiding wasting paint by re-painting our own tiles.
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())){
            rc.attack(rc.getLocation());
        }
    }


    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void runMopper(RobotController rc) throws GameActionException{
        // Move towards enemy side.
    	MapLocation currentLocation = rc.getLocation();
//    	MapInfo currentTile = rc.senseMapInfo(currentLocation);
//        Direction dir = directions[rng.nextInt(directions.length)];
    	Direction dir = prevDir;
        
    	if (rng.nextInt(10 + rc.getRoundNum()/100) == 0) { //random chance (lower later into the game) to turn left or right; encourages exploration
    		
    		int directionDecider = rng.nextInt(2); 
    		
    		if (directionDecider == 1) {dir = dir.rotateRight();}
    		else {dir = dir.rotateLeft();}
    		
    	}
    	
    	if (prevDir.equals(Direction.CENTER)) {
        	//guaranteed to be left side
        	if (currentLocation.x < 10) {
        		dir = Direction.NORTHEAST;
        	}
        	//guaranteed to be bottom
        	else if (currentLocation.y < 10) {
        		dir = Direction.NORTHWEST;
        	}
        	
        	else {
        		dir = directions[rng.nextInt(directions.length)]; //pick random direction and go
        		
        		int counter = 0;
                while (!rc.canMove(dir) && counter < 8){ //if that direction is invalid change it
                    dir = dir.rotateRight();
                    counter++;
                }
        		
                if (counter == 8) {
                	dir = Direction.CENTER;
                }
                
        	}
        }
        
        //if cant move in that direction
        /*if (!rc.canMove(dir)) {
        	//if it is a wall maneuver around
        	if (rc.senseMapInfo(currentLocation.add(dir)).isWall()) {
        		while (!rc.canMove(dir)){
                    dir = directions[rng.nextInt(directions.length)];
                }
        	}
        	else {
        		dir = dir.rotateRight().rotateRight();
        	}
        }*/
    	
        MapLocation nextLocation = currentLocation.add(dir);
        Direction attackDirection = nearbyEnemyPaintDirection(rc, 2);
        //Once movement direction is chosen, make sure that spot is NOT an enemy paint tile, if it is:
        try {
        	PaintType nextLocPaint = rc.senseMapInfo(nextLocation).getPaint();
	        if ((nextLocPaint.equals(PaintType.ENEMY_PRIMARY) || nextLocPaint.equals(PaintType.ENEMY_SECONDARY)) && rc.canAttack(nextLocation)){
	            rc.attack(nextLocation);
	            
	        }
	        //if there is enemy paint robot can mop; center means no nearby enemy paint that can be attacked
	        else if (!attackDirection.equals(Direction.CENTER)) {
	        	dir = Direction.CENTER;
	        	nextLocation = currentLocation;
	        	if (rc.isActionReady()) {
	        		System.out.println("ATTACKING IN DIRECTION: " + attackDirection.toString());
	        		rc.attack(currentLocation.add(attackDirection));
	        	}
	        }
        }
        //happens when robot attempts to sense map info on a tile out of bounds
        catch (GameActionException e) { 
//        	System.out.println("Location causing the problem: " + nextLocation.toString());
        	//if there is enemy paint robot can mop; center means no nearby enemy paint that can be attacked, run this part again bc exception caused it to not run
        	if (!attackDirection.equals(Direction.CENTER)) {
	        	nextLocation = currentLocation;
	        	if (rc.isActionReady()) {
	        		System.out.println("ATTACKING IN DIRECTION: " + attackDirection.toString());
	        		rc.attack(currentLocation.add(attackDirection));
	        	}
	        }
            // set dir to center because the previous direction was off the map
            dir = Direction.CENTER;
        }
        
        
        RobotInfo[] allSeenEnemyRobots = findEnemyRobots(rc);
        RobotInfo[] allSeenFriendlyRobots = findFriendlyRobots(rc);
        Direction paintTowerDir = dir; //direction of nearest paint tower; default = dir if the following if statement is not satisfied
        boolean isLowOnPaint = rc.getPaint() < 50; //the threshold 50 should be changed to an adjustable variable 
        boolean isTowerAdjacent = false; //only true when adjacent to paint tower and low on paint bc otherwise it will not update
        //low on paint or too many nearby enemies or low on health
    	if (isLowOnPaint || allSeenEnemyRobots.length > allSeenFriendlyRobots.length + 2 || rc.getHealth() < 20) { //retreat case
    		MapLocation nearestPaintTowerLoc = findNearestStructure(rc, 2);
    		paintTowerDir = currentLocation.directionTo(nearestPaintTowerLoc);
    		dir = paintTowerDir;
    		isTowerAdjacent = currentLocation.add(paintTowerDir).equals(nearestPaintTowerLoc); 
    		System.out.println("I AM TRYING TO RETURN TO PAINT TOWER, DIRECTION: " + paintTowerDir.toString() + " IS TOWER ADJACENT: " + isTowerAdjacent);
    	}
    	
    	//Reupdate nextLocation in case direction was changed
        nextLocation = currentLocation.add(dir);
        
        //if there is adjacent friendly paint tiles in the direction of travel, prefer using them
    	if (!rc.senseMapInfo(nextLocation).getPaint().isAlly()) {
            boolean turnedLeft = false;
            try {
                if (rc.senseMapInfo(currentLocation.add(dir.rotateLeft())).getPaint().isAlly()) {
                    dir = dir.rotateLeft();
                    turnedLeft = true;
                }
            } catch (GameActionException e) { } // exception added in case dir.rotateLeft() is off the map
            if (!turnedLeft) { try {
    		    if (rc.senseMapInfo(currentLocation.add(dir.rotateRight())).getPaint().isAlly()) {
                    dir = dir.rotateRight();
                }
            } catch (GameActionException e) { } } // exception added in case dir.rotateRight() is off the map
    	}
        
    	//Pick new direction to move if cant move in picked direction, stops after all directions are tried
    	int counter = 0;
        while (!rc.canMove(dir) && counter < 8){
            dir = dir.rotateRight();
            counter++;
        }
        
        //dont move if cant move in any direction
        if (counter == 8) {
        	dir = Direction.CENTER;
        }
    	
        //if low on paint and adjacent to a paint tower
    	if (isLowOnPaint && isTowerAdjacent) {
    		int paintInTower = rc.senseRobotAtLocation(currentLocation.add(paintTowerDir)).getPaintAmount();
    		int transferAmount = Math.max(-paintInTower, rc.getPaint()-80); //needs to be the lesser magnitude of the two; hence the max not min
    		if (rc.canTransferPaint(currentLocation.add(paintTowerDir), transferAmount)) {
                System.out.println("TRANSFERING PAINT | PAINT AMOUNT: " + transferAmount);
                rc.transferPaint(currentLocation.add(paintTowerDir), transferAmount);
            }
    	}
        
        //Reupdate nextLocation in case direction was changed
        nextLocation = currentLocation.add(dir);
        
        //Move in chosen direction
        if (rc.canMove(dir)) { //needs to be here if cooldown isnt done
        	try {
        		rc.move(dir);
        	}
        	catch (GameActionException e) { //trying to move out of bounds
        		dir = dir.rotateLeft().rotateLeft().rotateLeft();
        		rc.move(dir);
        	}
        	
        }
        
        currentLocation = nextLocation; //update current location post moving
        
    	RobotInfo[] enemyRobotsInSwingRadius = findEnemyRobots(rc, 1); //find all enemy robots within swinging distance
    	Direction swingDir = optimalSwing(rc, enemyRobotsInSwingRadius); //find best direction to swing
    	
    	//if swingDir is center that means no enemies around 
    	if (!swingDir.equals(Direction.CENTER) && rc.canMopSwing(swingDir)) {
    		rc.mopSwing(swingDir);
    		System.out.println("I did a mop swing and actually hit an enemy...");
    	}
    	
        prevDir = dir; //update previous direction
        updateEnemyRobots(rc);
        updateMapMemory(rc);
    }
    
    public static void updateMapMemory(RobotController rc) {
    	MapInfo[] allMapInfo = rc.senseNearbyMapInfos();
    	for (MapInfo tile : allMapInfo) {
    		//these two if statements skip all other calculations bc they are unnecessary if either are true
//    		if (tile.isPassable()) {continue;} //no important locations are passable
    		if (tile.isWall()) {continue;} //walls are not important enough to save in memory
    		
    		MapLocation tileLocation = tile.getMapLocation();
    		int correspondingNum = 1;
			try {
    			RobotInfo robotOnTile = rc.senseRobotAtLocation(tileLocation); 
    			if (robotOnTile == null) { 
//	    				System.out.println("NO ROBOT THERE: " + tileLocation.toString());
    				continue;
    			}
    			
    			UnitType robotType = robotOnTile.getType();
//	    			System.out.println("ROBOT TYPE: " + robotType.toString());
    			if (robotType.isTowerType()) { //real meat and potatoes of the function, differentiates all tower types
    				
    				//if it is a paint tower
    				if (robotType.equals(UnitType.LEVEL_ONE_PAINT_TOWER) || 
						robotType.equals(UnitType.LEVEL_TWO_PAINT_TOWER) ||
						robotType.equals(UnitType.LEVEL_THREE_PAINT_TOWER)) {
						correspondingNum = 2;
					} //technically unnecessary since default number is two (scuffed coding)
    				
    				//if it is a chip tower
    				else if (robotType.equals(UnitType.LEVEL_ONE_MONEY_TOWER) || 
						robotType.equals(UnitType.LEVEL_TWO_MONEY_TOWER) ||
						robotType.equals(UnitType.LEVEL_THREE_MONEY_TOWER)) {
						correspondingNum = 3;
					}
    				
    				//if it is a defense tower
    				else if (robotType.equals(UnitType.LEVEL_ONE_DEFENSE_TOWER) || 
						robotType.equals(UnitType.LEVEL_TWO_DEFENSE_TOWER) ||
						robotType.equals(UnitType.LEVEL_THREE_DEFENSE_TOWER)) {
						correspondingNum = 4;
					}
    				
    				if (isEnemy(rc, robotOnTile)) {
    					correspondingNum += 3;
    				}
    				
//    				System.out.println("ADDING LOCATION: " + tileLocation.toString() + " TO TYPE: " + Integer.toString(correspondingNum));
    				mapMemory.put(tileLocation, correspondingNum);
        			importantLocations.get(correspondingNum).add(tileLocation);
				} 
    			
    			else {
//	    				System.out.println(tileLocation.toString() + " IS BUNNY NOT TOWER");
    			}
			}
			catch (GameActionException e) { //exception "should" never happen as all locations in allMapInfo are within vision range
				continue;
			}
//    		}
    	}
    }
    
    public static Direction nearbyEnemyPaintDirection(RobotController rc, int radiussquared) {
    	try {
    		MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(radiussquared);
    		
    		for (MapInfo tile : nearbyTiles) {
    			
    			if (tile.getPaint().equals(PaintType.ENEMY_PRIMARY) ||
                        tile.getPaint().equals(PaintType.ENEMY_SECONDARY)) {
                    return rc.getLocation().directionTo(tile.getMapLocation());
                }
    		
    		}
    		
    		return Direction.CENTER;
    	}
    	catch (GameActionException e) { //shouldnt ever happen, only here because java will get mad if it's not 
    		return Direction.CENTER;
    	}
    }
    
    public static boolean isEnemy(RobotController rc, RobotInfo otherRobot) {
    	return rc.getTeam().opponent().equals(otherRobot.getTeam());
    }
    
    //returns current location if no paint towers have been seen yet
    public static MapLocation findNearestStructure(RobotController rc, int StructID) {
    	MapLocation currentLocation = rc.getLocation();
    	HashSet<MapLocation> allFriendlyPaintTowers = importantLocations.get(StructID); //all paint towers seen
    	
    	if(allFriendlyPaintTowers.isEmpty()) {return currentLocation;}
    	
    	//simple "min" algorithm, try optimizing to avoid recalculation!
    	MapLocation nearestLocation = currentLocation;
    	int shortestDistanceSquared = 100000;
    	for (MapLocation towerLocation : allFriendlyPaintTowers) {
    		int distanceSquared = currentLocation.distanceSquaredTo(towerLocation);
    		if (distanceSquared < shortestDistanceSquared) {
    			shortestDistanceSquared = distanceSquared;
    			nearestLocation = towerLocation;
    		}
    	}
    	
    	return nearestLocation;
    }
    
    //Finds the best direction to strike; currently swings in direction with most robots, if no robots, returns center
    public static Direction optimalSwing(RobotController rc, RobotInfo[] enemyRobots) {
    	int[] swingValues = getSwingValues(rc, enemyRobots);
    	int maxInd = 0; int maxVal = 0;
    	
    	for (int ind = 0; ind < 4; ind++) {
    		
    		if (swingValues[ind] > maxVal) {
    			maxVal = swingValues[ind];
    			maxInd = ind;
    		}
    	
    	}
    	
    	if (maxVal == 0) {return Direction.CENTER;}
    	return directions[maxInd*2]; //swingValue index * 2 = directions index (e.g. South in swingValue is 2, South in directions is 4)
    	
    }
    
    //Calculates how many robots are hit in a single swing for all cardinal directions
    public static int[] getSwingValues(RobotController rc, RobotInfo[] enemyRobots) {
    	int[] swingValues = {0, 0, 0, 0}; //Index 0: NORTH, Index 1: EAST, Index 2: SOUTH, Index 3: WEST 
    	
    	//Return all 0s if there are no enemy robots around (obviously)
    	if (enemyRobots.length > 0) {
    	
	    	int[] robotSpots = {0, 0, 0, 0, 0, 0, 0, 0}; //0 for no robot, 1 for robot, in the same order as static variable directions
	    	
	    	//Iterate through all the enemy robots
	    	for (RobotInfo robot : enemyRobots) {
	    		Direction enemyDir = rc.getLocation().directionTo(robot.getLocation()); //direction to the enemy from current robot
	    		robotSpots[directionToInteger.get(enemyDir)] = 1;
	    	}
	    	
	    	swingValues[0] = robotSpots[0] + robotSpots[1] + robotSpots[7]; //sum of north, northeast, and northwest values; same scheme for all other directions
	    	swingValues[1] = robotSpots[1] + robotSpots[2] + robotSpots[3]; 
	    	swingValues[2] = robotSpots[3] + robotSpots[4] + robotSpots[5]; 
	    	swingValues[3] = robotSpots[5] + robotSpots[6] + robotSpots[7]; 

    	}
    	
     	return swingValues;
    }
    
	//Checks for enemy robots in a certain direction 
    public static boolean directionEnemyCheck(RobotController rc, Direction dir, RobotInfo[] enemyRobots) {
    	if (enemyRobots.length == 0) {return false;} //if there are no enemies then automatic false
		for (RobotInfo robot : enemyRobots) {
			if (rc.getLocation().directionTo(robot.getLocation()).equals(dir)) { //if the enemy's location is in the same direction as specified
				return true;
			}
		}
		return false;
    }
    
    public static RobotInfo[] findEnemyRobots(RobotController rc) throws GameActionException {
    	RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
    	return enemyRobots;
    }
    
    //Overloaded version for checking within a specific radius
    public static RobotInfo[] findEnemyRobots(RobotController rc, int radius) throws GameActionException {
    	RobotInfo[] enemyRobots = rc.senseNearbyRobots(radius, rc.getTeam().opponent());
    	return enemyRobots;
    }
    
    public static RobotInfo[] findFriendlyRobots(RobotController rc) throws GameActionException {
    	RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
    	return enemyRobots;
    }
    
    //Overloaded version for checking within a specific radius
    public static RobotInfo[] findFriendlyRobots(RobotController rc, int radius) throws GameActionException {
    	RobotInfo[] enemyRobots = rc.senseNearbyRobots(radius, rc.getTeam());
    	return enemyRobots;
    }

    /*
     * Splasher attacks immediately if it sees an empty or enemy tile.
     * It moves in a straight line until it can no longer go in that direction,
     * then picks a new direction to move in.
     */
    static Direction splasherDirection = null;
    static float threshold = 9.0f;
    static void runSplasher(RobotController rc) throws GameActionException {
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        int markRuinStatus = MarkRuin.markIfFound(rc, nearbyTiles, nearbyRobots,
                MarkRuin.INITIAL_TOWERS[rng.nextInt(MarkRuin.INITIAL_TOWERS.length - 1)]);

        if (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT && rc.getPaint() >= UnitType.SPLASHER.attackCost) {
            // if it can attack, look around and maybe attack
            float[] damageTotals = SplasherConvolution.computeAttackTotals(rc, nearbyTiles, nearbyRobots);
            boolean attacked = SplasherConvolution.attackBestFlat(rc, nearbyTiles, damageTotals, threshold);
            if (attacked) {
                threshold = 15.0f;
            } else if (threshold > 3.0f) {
                threshold -= 0.1f;
            }
        }

        if (rc.isMovementReady()) {
            if (markRuinStatus == 2 || markRuinStatus == 3) {
                // don't move normally if we're making progress towards marking a pattern
                return;
            }
            if (splasherDirection == null) {
                splasherDirection = directions[rng.nextInt(directions.length)];
            }
            if (rc.canMove(splasherDirection)) {
                rc.move(splasherDirection);
            } else {
                splasherDirection = null;
            }
        }
    }

    static void updateEnemyRobots(RobotController rc) throws GameActionException{
        // Sensing methods can be passed in a radius of -1 to automatically 
        // use the largest possible value.
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length != 0){
            rc.setIndicatorString("There are nearby enemy robots! Scary!");
            // Save an array of locations with enemy robots in them for possible future use.
            MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
            for (int i = 0; i < enemyRobots.length; i++){
                enemyLocations[i] = enemyRobots[i].getLocation();
            }
            RobotInfo[] allyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            // Occasionally try to tell nearby allies how many enemy robots we see.
            if (rc.getRoundNum() % 20 == 0){
                for (RobotInfo ally : allyRobots){
                    if (rc.canSendMessage(ally.location, enemyRobots.length)){
                        rc.sendMessage(ally.location, enemyRobots.length);
                    }
                }
            }
        }
    }
}
