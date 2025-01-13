package bunniesv0;

import battlecode.common.*;
import java.util.*;

public class UnpackedMessage extends RobotPlayer {
	
	int command;
	MapLocation locInfo;
	int turnInfo;
	int senderTurn;
	int senderID;
	
	public UnpackedMessage(int c, MapLocation loc, int turn, int sTurn, int sID) {
		command = c;
		locInfo = loc;
		turnInfo = turn;
		senderTurn = sTurn;
		senderID = sID;
	}
	
	public static Map<String, Integer> bunnyCommandMap = Map.ofEntries(
			Map.entry("Save Chips", 0),
			Map.entry("Send Robots", 1),
            Map.entry("Send Soldiers", 2),
            Map.entry("Send Moppers", 3),
            Map.entry("Send Splashers", 4)
        );
	
	public static Map<String, Integer> towerCommandMap = Map.ofEntries(
			Map.entry("Go To", 0), 
			Map.entry("Take Paint", 1)
        );
	
	public static void encodeAndSend(RobotController rc, MapLocation target, String command, MapLocation locInfo, int turnInfo) throws GameActionException {
		// 5 bits for command type, 12 bits for location info, 11 bits for turn info (28/32 bits used currently)
		int messageContent = (rc.getType().isTowerType() ? towerCommandMap.get(command) : bunnyCommandMap.get(command)) * 134217728
				+ locInfo.x * 2097152 + locInfo.y * 32768 + turnInfo * 16;
		rc.sendMessage(target, messageContent);
	}
	
	public static void encodeAndSend(RobotController rc, MapLocation target, String command, MapLocation locInfo) throws GameActionException {
		encodeAndSend(rc, target, command, locInfo, 2002);
	}
	
	public static void encodeAndSend(RobotController rc, MapLocation target, String command, int turn) throws GameActionException {
		encodeAndSend(rc, target, command, new MapLocation(60, 60), turn);
	}
	
	public static UnpackedMessage[] receiveAndDecode(RobotController rc) throws GameActionException {
		Message[] messages = rc.readMessages(-1);
		UnpackedMessage[] unpackedMessages = new UnpackedMessage[messages.length];
		int bytes;
        for (int count = 0; count < messages.length; count++) {
        	Message m = messages[count];
            bytes = m.getBytes();
        	System.out.println((rc.getType().isTowerType() ? "Tower" : "Bunny") + " received message: '#" + m.getSenderID() + " " + bytes);
        	int c = bytes / 134217728; bytes -= c * 134217728;
        	int x = bytes / 2097152; bytes -= x * 2097152;
        	int y = bytes / 32768; bytes -= y * 32768;
        	int turn = bytes / 16; bytes -= turn * 16;
            unpackedMessages[count] = new UnpackedMessage(c, new MapLocation(x, y), turn, m.getRound(), m.getSenderID());
            count += 1;
        }
        return unpackedMessages;
	}
	
}
