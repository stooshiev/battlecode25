package bunniesv3;

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
    //5 - enemy paint tower, 6 - enemy chip tower, 7 - enemy defense tower, 8 - enemy paint (changes very often)
    static HashMap<Integer, HashSet<MapLocation>> importantLocations = new HashMap<>();
    //Using the same integer pattern as mapMemory, keeps track of all coordinates/tiles with corresponding important structure
    
    static Direction prevDir = Direction.CENTER; //previous direction robot moved (if robot is a bunny)
    static MapLocation prevLoc = new MapLocation(0,0);
    static Direction dir = Direction.CENTER;
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
            }
        }

        if (curRuin != null)
            rc.setIndicatorString("Going towards: " + curRuin.getMapLocation().toString());
    
        if (rc.getPaint() <= 50) {
            Soldier.retreatForPaint(rc);
        } else if (curRuin != null){
            isMarking = Soldier.paintNewTower(rc, curRuin);
        } if (!isMarking) {
            // Move and attack randomly if no objective.
            Direction dir = Soldier.methodicalMovement(rc);
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
    }


    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void runMopper(RobotController rc) throws GameActionException{
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
        
        //if low on paint and adjacent to a paint tower
        if (isLowOnPaint && isTowerAdjacent) {
        	Mopper.transferPaintMoppers(rc, currentLocation, paintTowerDir);   	
//    		dir = paintTowerDir.opposite(); //move away from paint tower after done transferring
    	}
        
        //low on paint
        else if (isLowOnPaint) { //retreat case
    		dir = paintTowerDir;
    	}
    	
        //go to nearest enemy paint; IF NEVER SEEN ENEMY PAINT: follow a soldier if not retreating and soldier is within vision radius 
        else {
        	MapLocation nearestEnemyPaint = Mopper.findNearestStructure(rc, 8);
        	if (!nearestEnemyPaint.equals(currentLocation)) {
        		dir = currentLocation.directionTo(nearestEnemyPaint);
        	}
        	else {
	        	MapLocation nearestFriendlySoldierLoc = Mopper.findNearestFriendlySoldier(rc);
	        	//overwritten if mopper is going for enemy paint bc that takes priority
	            if (!currentLocation.equals(nearestFriendlySoldierLoc) && !dir.equals(attackDirection)) {
	            	dir = currentLocation.directionTo(nearestFriendlySoldierLoc);
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
        prevLoc = new MapLocation(currentLocation.x, currentLocation.y);
        
        //Move in chosen direction
        if (rc.canMove(dir)) { //needs to be here if cooldown isnt done
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
        Mopper.updateMapMemory(rc);
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
                    // add this paint tower to a list that deny paint in splasher memory
                    // TODO
                    try {
                        //SplasherMemory.addRejectedTower(message.message.getSenderID(), rc.getRoundNum());
                        SplasherMemory.addRejectedTower(rc.senseRobotAtLocation(message.locInfo).ID, rc.getRoundNum());
                    } catch (GameActionException ignored) {
                        // shouldn't get here
                    }
                }
            }
        }
        if (isRetreating) {
            if (rc.getPaint() >= splasherPaintRetreatThreshold) {
                isRetreating = false;
                navigator = null;
            }
            MapLocation rcLoc = rc.getLocation();
            SplasherMemory.updateRobotMemory(rc, rc.senseNearbyRobots());
            if (navigator == null) {
                // remember where the closest paint tower is
                MapLocation nearestPaintTower = SplasherMemory.getNearestFriendlyPaintTower(rc.getLocation());
                if (nearestPaintTower != null) {
                    navigator = new OrbitPathfinder(rc, nearestPaintTower);
                } else {
                    // if no tower found, continue normal movement, look for tower
                    if (rc.isMovementReady()) {
                        if (splasherDirection == null) {
                            splasherDirection = directions[rng.nextInt(directions.length)];
                        }
                        if (rc.canMove(splasherDirection)) {
                            rc.move(splasherDirection);
                        } else {
                            splasherDirection = null;
                        }
                        SplasherMemory.updateRobotMemory(rc, rc.senseNearbyRobots());
                    }
                }
            }
            MapLocation nearestPaintTower = SplasherMemory.getNearestFriendlyPaintTower(rcLoc);
            int closestTowerDistance = rcLoc.distanceSquaredTo(nearestPaintTower);
            if (closestTowerDistance <= 2) {
                try {
                    UnpackedMessage.encodeAndSend(rc, nearestPaintTower, UnpackedMessage.REQUEST_PAINT, rc.getLocation());
                } catch (GameActionException ignored) { }
                // stay put and wait reply
                return;
            }
            if (navigator != null) {
                if (!nearestPaintTower.equals(navigator.getDest())) {	
                    // if the closest tower is something else
                    navigator = new OrbitPathfinder(rc, nearestPaintTower);
                }
                navigator.step();
            }
        }

        if (rc.getPaint() < splasherPaintRetreatThreshold) {
            isRetreating = true;
            navigator = null;
            splasherDirection = null;
            attackThreshold = 19;
            MarkRuin.ruinLocation = null;
            MarkRuin.pathfinder = null;
            return;
        }

        int markRuinStatus = MarkRuin.markIfFound(rc, null);

        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        SplasherMemory.updateRobotMemory(rc, nearbyRobots);


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

        if (rc.isMovementReady() && markRuinStatus != 6) {
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
