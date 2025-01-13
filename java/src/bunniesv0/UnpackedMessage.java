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
		// 5 bits
		int messageContent = rc.getType().isTowerType() ? towerCommandMap.get(command) : bunnyCommandMap.get(command);
		messageContent <<= 6; // make space for 6 bits
		messageContent += locInfo.x; // 6 bits
		messageContent <<= 6;
		messageContent += locInfo.y; // 6 bits
		messageContent <<= 11;
		messageContent += turnInfo; // 11 bits
		messageContent <<= 4;
		// 4 unused bits remain
		rc.sendMessage(target, messageContent);
		System.out.println(String.format("Sent message: command %d, x%d, y%d, turn %d, unused %d",
				command, locInfo.x, locInfo.y, turnInfo, 0));
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
			int unused = bytes & 0b1111;
			bytes >>= 4;
			int turn = bytes & 0b11111111111;
			bytes >>= 11;
			int y = bytes & 0b111111;
			bytes >>= 6;
			int x = bytes & 0b111111;
			bytes >>= 6;
			int c = bytes & 0b11111;
			bytes >>= 5;
			// now bytes should either be all 0s (if bytes was +) or all 1s (if bytes was -)

			System.out.println(String.format("Received message: command %d, x%d, y%d, turn %d, unused %d",
					c, x, y, turn, unused));
            unpackedMessages[count] = new UnpackedMessage(c, new MapLocation(x, y), turn, m.getRound(), m.getSenderID());
            count += 1;
        }
        return unpackedMessages;
	}
	
}
