package bunniesv1;

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

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

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
                    case SPLASHER: runSplasher(rc); // Consider upgrading examplefuncsplayer to use splashers!
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
    public static void runTower(RobotController rc) throws GameActionException{
    	// Sense information about all visible nearby tiles and robots.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        
        // Read incoming messages
        Message[] messages = rc.readMessages(-1);
        for (Message m : messages) {
            System.out.println("Tower received message: '#" + m.getSenderID() + " " + m.getBytes());
        }
        
        Tower.attackPattern0(rc, nearbyTiles, nearbyRobots);
    	
    	if (rc.getPaint() >= 400 && rc.getMoney() >= 700) {
        	Tower.createRandomRobot(rc);
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
            			Soldier.checkTowerLoc(rc, tile);
            			curRuin = tile;
            		}
            	} else {
            		Soldier.checkTowerLoc(rc, tile);
                    curRuin = tile;
            	}
            }
        }
        
        if (curRuin != null) {
        	rc.setIndicatorString(Soldier.getTowerLoc().toString() + " : " + rc.senseRobotAtLocation(Soldier.getTowerLoc()).getTeam().toString());
        } else {
        	rc.setIndicatorString("No Ruin Available");
        }
		
        // Read incoming messages
        //UnpackedMessage[] unpackedMessages = UnpackedMessage.receiveAndDecode(rc);
        

        if (rc.getPaint() <= 50) {
        	MapLocation closeTower = Soldier.getTowerLoc();
        	Direction moveDir = Soldier.getShortestPathDir(rc, Soldier.getTowerLoc());
        	if (rc.canMove(moveDir))
	        	rc.move(moveDir);
//        	for (int i = 0; i < unpackedMessages.length; i++) {
//        		if (unpackedMessages[i].equals("Take Paint")) {
//        			rc.setIndicatorString("Taking Paint From Tower");
//        		}
//        	}
//    		rc.setIndicatorString(Arrays.toString(unpackedMessages));
//        	if (rc.canSenseLocation(closeTower)) {
//        		
//        	}
        	if (rc.canTransferPaint(closeTower, -50)) {
        		rc.transferPaint(closeTower, -50);
        	}
        } else if (curRuin != null){
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
        // Move and attack randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        if (rc.canMove(dir)){
            rc.move(dir);
        }
        if (rc.canMopSwing(dir)){
            rc.mopSwing(dir);
            System.out.println("Mop Swing! Booyah!");
        }
        else if (rc.canAttack(nextLoc)){
            rc.attack(nextLoc);
        }
        // We can also move our code into different methods or classes to better organize it!
        updateEnemyRobots(rc);
    }

    /*
     * Splasher attacks immediately if it sees an empty or enemy tile.
     * It moves in a straight line until it can no longer go in that direction,
     * then picks a new direction to move in.
     */
    private static Direction splasherDirection = null;
    public static void runSplasher(RobotController rc) throws GameActionException {
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapInfo enemyTile = null;
        for (MapInfo current : nearbyTiles) {
            if (current.getPaint().equals(PaintType.EMPTY) ||
                    current.getPaint().equals(PaintType.ENEMY_PRIMARY) ||
                    current.getPaint().equals(PaintType.ENEMY_SECONDARY)) {
                enemyTile = current;
                break;
            }
        }
        if (enemyTile != null) {
            if (rc.canAttack(enemyTile.getMapLocation())) {
                rc.attack(enemyTile.getMapLocation());
            }
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

    public static void updateEnemyRobots(RobotController rc) throws GameActionException{
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
