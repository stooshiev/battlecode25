package bunniesv3;

import battlecode.common.*;

import java.util.*;


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
    
    static Team team = Team.NEUTRAL;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final long RANDOM_SEED = 6147;
    static Random rng = new Random(RANDOM_SEED);

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
    //5 - enemy paint tower, 6 - enemy chip tower, 7 - enemy defense tower, 8 - locations where enemy paint was seen
    static HashMap<Integer, HashSet<MapLocation>> importantLocations = new HashMap<>();
    //Using the same integer pattern as mapMemory, keeps track of all coordinates/tiles with corresponding important structure
    
    static Direction prevDir = Direction.CENTER; //previous direction robot moved (if robot is a bunny)
    static MapLocation prevLoc = new MapLocation(0,0);
    static LinkedList<MapLocation> path = new LinkedList<>();
    static Direction dir = Direction.CENTER;
    static MapLocation targetLoc = null;
    static OrbitPathfinder navigator = null;
    

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        rng = new Random(RANDOM_SEED + 60 * 60 * rc.getRoundNum() + 60 * rc.getLocation().x + rc.getLocation().y);
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
        importantLocations.put(8,  new HashSet<MapLocation>());
        
        //update prevLoc to be current location
        prevLoc = rc.getLocation();
        path.addLast(prevLoc);
        team = rc.getTeam();
        
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
        	if (turnCount < 3) { //spawns soldiers at the beginning of the game
        		Tower.createRobot(rc, 0);
        	}
    		if (rc.getPaint() >= 400 && rc.getMoney() >= 700) {
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
        MapInfo posRuin = null;
        MapInfo enemyTowerRuin = null;

        for (MapInfo tile : nearbyTiles){
            if (tile.hasRuin()){
            	if (curRuin != null) {
            		if (curRuin.getMapLocation().distanceSquaredTo(rc.getLocation()) > tile.getMapLocation().distanceSquaredTo(rc.getLocation())) {
            			Soldier.checkTowerLoc(rc, tile);
            			posRuin = Soldier.checkMarking(rc, tile);
                        if (posRuin != null)
                            curRuin = posRuin;
            		}
            	} else {
            		Soldier.checkTowerLoc(rc, tile);
                    curRuin = Soldier.checkMarking(rc, tile);
            	}
                
                if (rc.senseRobotAtLocation(tile.getMapLocation()) != null) {
                    if (rc.senseRobotAtLocation(tile.getMapLocation()).getTeam().equals(rc.getTeam().opponent())) {
                        enemyTowerRuin = tile;
                        rc.setIndicatorString("" + rc.senseRobotAtLocation(tile.getMapLocation()).getTeam());//.toString());
                    }
                }
                
            }
        }

        //rc.setIndicatorString("Deciding action");
    
        if (enemyTowerRuin != null) {
            rc.setIndicatorString("Attacking tower");
            Soldier.attackEnemyTower(rc, enemyTowerRuin);
        } else if (rc.getPaint() <= 75) {
            rc.setIndicatorString("Getting paint");
            Soldier.retreatForPaint(rc);
        } else if (curRuin != null){
            rc.setIndicatorString("Building tower");
            isMarking = Soldier.paintNewTower(rc, curRuin);
        }
        
        if (!isMarking) {
            // Move and attack randomly if no objective.
            Direction dir = Soldier.methodicalMovement(rc);
            if (!dir.equals(Direction.CENTER)) {
            	if (prevLoc.equals(rc.getLocation())) {
            		dir = directions[rng.nextInt(directions.length)];
            	}
            	else {
            		dir = prevDir;
            	}
            }
            MapLocation nextLoc = rc.getLocation().add(dir);
            if (rc.canMove(dir)){
                rc.move(dir);
            }
        }
        // Try to paint beneath us as we walk to avoid paint penalties.
        // Avoiding wasting paint by re-painting our own tiles.
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())) {
            Soldier.attackCheckered(rc, rc.getLocation());
        }
        
        prevLoc = rc.getLocation();
        prevDir = dir;
    }

    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void runMopper(RobotController rc) throws GameActionException{
    	
    	Mopper.updateMapMemory(rc);
    	MapLocation currentLocation = rc.getLocation();
    	dir = prevDir;
    	
    	//Mopper.explore(rc); //adds a little bit of randonmess to mopper code
    	
    	//Not the same as prevDir.equals(Direction.CENTER) 
    	if (prevLoc.equals(currentLocation)) {
    		dir = prevDir.opposite();
    		if (prevDir.equals(Direction.CENTER)) {
				dir = directions[rng.nextInt(directions.length)]; //pick random direction and go
        		Mopper.findValidMoveDir(rc);
    		}
    	}
    	
        MapLocation nextLocation = currentLocation.add(dir);
    	nextLocation = currentLocation.add(dir);
        Direction attackDirection = Mopper.nearbyEnemyPaintDirection(rc, 2);
        //Once movement direction is chosen, make sure that spot is NOT an enemy paint tile, if it is:
        try {
        	PaintType nextLocPaint = rc.senseMapInfo(nextLocation).getPaint();
	        if ((nextLocPaint.equals(PaintType.ENEMY_PRIMARY) || nextLocPaint.equals(PaintType.ENEMY_SECONDARY)) && rc.canAttack(nextLocation)){
	            rc.attack(nextLocation);
	        }
	        //if there is enemy paint robot can mop; center means no nearby enemy paint that can be attacked
	        else {
	        	Mopper.mopperAttackBehavior(rc, currentLocation, attackDirection);
	        }
        }
        //happens when robot attempts to sense map info on a tile out of bounds
        catch (GameActionException e) { 
        	Mopper.mopperAttackBehavior(rc, currentLocation, attackDirection);
        }
        
        //UNCOMMENT OUT UP TO THE ELSE IF STATEMENT IF RETREATING MOPPERS IS WANTED
        MapLocation nearestPaintTowerLoc = Mopper.findNearestStructure(rc, 2);
        Direction paintTowerDir = currentLocation.directionTo(nearestPaintTowerLoc); //direction of nearest paint tower; default = dir if the following if statement is not satisfied
        boolean isLowOnPaint = rc.getPaint() < 30; //the threshold 50 should be changed to an adjustable variable 
        boolean isTowerAdjacent = currentLocation.add(paintTowerDir).equals(nearestPaintTowerLoc); 
        boolean useNavigator = false;
        
        //if low on paint and adjacent to a paint tower
        if (isLowOnPaint && isTowerAdjacent) {
        	Mopper.transferPaintMoppers(rc, currentLocation, paintTowerDir); 
    	}
        
        //low on paint
        else if (isLowOnPaint && !useNavigator) { //retreat case
    		if (!rc.senseMapInfo(currentLocation.add(paintTowerDir)).isPassable()) {
    			navigator = new OrbitPathfinder(rc, nearestPaintTowerLoc);
    			navigator.step();
    			useNavigator = true;
    		}
    		else {
    			dir = paintTowerDir;
    		}
    	}
        
        else if (isLowOnPaint && useNavigator) {
        	navigator.step();
        	useNavigator = !rc.senseMapInfo(currentLocation.add(paintTowerDir)).isPassable();
        }
        
    	
        //go to nearest enemy paint THATS NOT WITHIN ENEMY TOWER ATTACK RADIUS; 
        //IF NEVER SEEN ENEMY PAINT: follow a soldier if not retreating and soldier is within vision radius 
        else {
        	MapLocation nearestEnemyPaint = Mopper.findNearestSafeStructure(rc, 8);
        	if (useNavigator) {
        		navigator.step();
        		dir = currentLocation.directionTo(nearestEnemyPaint);
        		useNavigator = !rc.senseMapInfo(currentLocation.add(dir)).isPassable();
        	}
        	else if (!nearestEnemyPaint.equals(currentLocation) && attackDirection.equals(Direction.CENTER)) {
//        		targetLoc = nearestEnemyPaint;
        		dir = currentLocation.directionTo(nearestEnemyPaint);
        		useNavigator = !rc.senseMapInfo(currentLocation.add(dir)).isPassable();
        		if (useNavigator) {
        			navigator = new OrbitPathfinder(rc, nearestEnemyPaint);
        			navigator.step();
        		}
        	}
        	else if (!attackDirection.equals(Direction.CENTER)){ //if there is enemy paint in attack range there stay there
        		dir = Direction.CENTER;
        		useNavigator = false;
        	}
        	else {
        		nearestEnemyPaint = Mopper.nearbyEnemyPaint(rc);
        		Direction nearestEnemyPaintDir = currentLocation.directionTo(nearestEnemyPaint);
        		if (!nearestEnemyPaintDir.equals(Direction.CENTER)) { //if can see near enemy paint go there
        			useNavigator = !rc.senseMapInfo(currentLocation.add(nearestEnemyPaintDir)).isPassable();
        			if (useNavigator) {
        				navigator = new OrbitPathfinder(rc, nearestEnemyPaint);
            			navigator.step();
        			}
        			dir = nearestEnemyPaintDir;
        		}
        		else { 
		        	MapLocation nearestFriendlySoldierLoc = Mopper.findNearestFriendlySoldier(rc);
		        	//overwritten if mopper is going for enemy paint bc that takes priority
		            if (!currentLocation.equals(nearestFriendlySoldierLoc) && !dir.equals(attackDirection)) {
		            	dir = currentLocation.directionTo(nearestFriendlySoldierLoc);
		            }
        		}
        	}
    	}
            
    	//Reupdate nextLocation in case direction was changed
        nextLocation = currentLocation.add(dir);
        
        //if there is adjacent friendly paint tiles in the direction of travel, prefer using them
        try {
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
        }
        catch (GameActionException e) {} //exception added in case nextLocation is off the map
    	//only matters if the current direction of travel is somehow impossible; doesnt catch for out of bounds
    	Mopper.findValidMoveDir(rc); 
    	
        //Reupdate nextLocation in case direction was changed
        nextLocation = currentLocation.add(dir);
        prevLoc = new MapLocation(currentLocation.x, currentLocation.y); //needs to be a deep copy not just pointer
        
//        rc.setIndicatorString("Direction of movement: " + dir.toString() + " Is using orbit pathfinder: " + useNavigator);
        //Move in chosen direction
        if (rc.canMove(dir) && !useNavigator) { //needs to be here if cooldown isnt done
        	try {	
        		rc.move(dir);
        	}
        	catch (GameActionException e) { //trying to move out of bounds
        		dir = dir.rotateLeft().rotateLeft();//.rotateLeft();
        		rc.move(dir);
        	}
        	
        }
        
        currentLocation = nextLocation; //update current location post moving
        
        //END OF MOVEMENT FOR MOPPER
        
    	RobotInfo[] enemyRobotsInSwingRadius = Mopper.findEnemyRobots(rc, 2); //find all enemy robots within swinging distance
    	if (enemyRobotsInSwingRadius.length > 0) {
    		Direction swingDir = Mopper.optimalSwing(rc); //find best direction to swing
        	
        	//if swingDir is center that means no enemies around 
        	if (!swingDir.equals(Direction.CENTER) && rc.canMopSwing(swingDir)) {
        		rc.mopSwing(swingDir);
//        		System.out.println("I did a mop swing and actually hit an enemy...");
        	}
    	}
    	
        prevDir = dir; //update previous direction
//        updateEnemyRobots(rc);
        
    }
    
    

    /*
     * Splasher attacks immediately if it sees an empty or enemy tile.
     * It moves in a straight line until it can no longer go in that direction,
     * then picks a new direction to move in.
     */
    static Direction splasherDirection = null;
    static float attackThreshold = 19.8f;
    static boolean isRetreating = false;
    static int splasherPaintRetreatThreshold = 100;
    static void runSplasher(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() >= 103 && rc.getID() == 12569) {
            int djaf = 0;
        }
        // Read messages. Currently only processes "take paint" and "paint denied"
        for (UnpackedMessage message : UnpackedMessage.receiveAndDecode(rc)) {
            if (message.message.getRound() == rc.getRoundNum()) {
                if (message.command == UnpackedMessage.TAKE_PAINT) {
                    if (message.turnInfo == UnpackedMessage.INVALID_ROUND_NUM) {
                        // guess at a paint amount to take
                        for (int transfer = 300; transfer > 0; transfer -= 50) {
                            if (rc.canTransferPaint(message.locInfo, -transfer)) {
                                rc.transferPaint(message.locInfo, -transfer);
                            }
                        }
                    } else {
                            // message.turnInfo is the amount that Splasher may take
                            int transfer = Math.min(message.turnInfo, 300 - rc.getPaint());
                            if (rc.canTransferPaint(message.locInfo, -transfer)) {
                                rc.transferPaint(message.locInfo, -transfer);
                            }
                        }
                    }
                else if (message.command == UnpackedMessage.PAINT_DENIED) {
                    SplasherMemory.addRejectedTower(message.message.getSenderID(), rc.getRoundNum());
                }
            }
        }

        // if it's retreating, run the retreating function. Alo make sure that it actually needs to be retreating
        if (isRetreating) {
            if (rc.getPaint() >= RobotPlayer.splasherPaintRetreatThreshold) {
                isRetreating = false;
                navigator = null;
            }
            SplasherRetreat.retreat(rc);
        }
        // check if we need to be retreating (due to low paint or damage). If we are, set the static variables.
        if (rc.getPaint() < splasherPaintRetreatThreshold) {
            isRetreating = true;
            navigator = null;
            splasherDirection = null;
            attackThreshold = 19;
            MarkRuin.ruinLocation = null;
            MarkRuin.pathfinder = null;
            return;
        }

        // mark a ruin if it sees one
        int markRuinStatus = MarkRuin.markIfFound(rc, null);

        // sense tiles, update memory
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        SplasherMemory.updateRobotMemory(rc, nearbyRobots);
        OrbitPathfinder.avoidWithinRadius3 = SplasherMemory.enemyTowers;

        // compute a good place to attack, and attack if it's good enough
        if (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT &&
                rc.getPaint() >= UnitType.SPLASHER.attackCost) {
            // if it can attack, look around and maybe attack
            MapLocation attackLocation = SplasherConvolution.computeAndAttack(rc, nearbyTiles, nearbyRobots,
                    attackThreshold);
            if (attackLocation != null) {
                attackThreshold = 18.0f;
            } else if (attackThreshold > 3.0f) {
                attackThreshold -= 0.1f;
            }
        }

        // move in a straight line until hitting a wall, the pick another random direction
        if (rc.isMovementReady() && markRuinStatus != 6) {
            if (splasherDirection == null) {
                splasherDirection = directions[rng.nextInt(directions.length)];
            }
            MapLocation next = rc.getLocation().add(splasherDirection);
            if (rc.canMove(splasherDirection) && SplasherMemory.enemyTowers.stream().noneMatch(enemyTowerLoc ->
                    enemyTowerLoc.isWithinDistanceSquared(next, 9))) {
                rc.move(splasherDirection);
                path.addLast(rc.getLocation());
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