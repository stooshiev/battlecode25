package bunniesv3;

import battlecode.common.*;
import java.util.*;

public class UnpackedMessage extends RobotPlayer {
	
	int command;
	MapLocation locInfo;
	int turnInfo;
	int senderTurn;
	int senderID;
	Message message;

	static final int INVALID_ROUND_NUM = 2002;

	// Commands from bunny to tower
	static final int SAVE_CHIPS = 0;
	static final int SEND_ROBOTS = 1;
	static final int SEND_SOLDIERS = 2;
	static final int SEND_MOPPERS = 3;
	static final int SEND_SPLASHERS = 4;
	static final int REQUEST_PAINT = 5;

	// Commands from tower to bunny
	static final int GO_TO = 0;
	static final int TAKE_PAINT = 1;
	static final int PAINT_DENIED = 2;
	
	public UnpackedMessage(int c, MapLocation loc, int turn, int sTurn, int sID) {
		command = c;
		locInfo = loc;
		turnInfo = turn;
		senderTurn = sTurn;
		senderID = sID;
	}

	public static void encodeAndSend(RobotController rc, MapLocation target, int command,
									 MapLocation locInfo, int turnInfo)
			throws GameActionException {
		// 5 bits for command type, 12 bits for location info, 11 bits for turn info (28/32 bits used currently)
		// 5 bits
		int messageContent = command;
		messageContent <<= 6; // make space for 6 bits
		messageContent += locInfo.x; // 6 bits
		messageContent <<= 6;
		messageContent += locInfo.y; // 6 bits
		messageContent <<= 11;
		messageContent += turnInfo; // 11 bits
		messageContent <<= 4;
		// 4 unused bits remain
		System.out.println("Sending message " + messageContent);
		System.out.printf("Message content: command %d, x%d, y%d, turn %d, unused %d%n",
				command, locInfo.x, locInfo.y, turnInfo, 0);
		rc.sendMessage(target, messageContent);
	}
	
	public static void encodeAndSend(RobotController rc, MapLocation target, int command, MapLocation locInfo)
			throws GameActionException {
		encodeAndSend(rc, target, command, locInfo, INVALID_ROUND_NUM);
	}

	public static void encodeAndSend(RobotController rc, MapLocation target, int command, int turn)
			throws GameActionException {
		encodeAndSend(rc, target, command, new MapLocation(60, 60), turn);
	}

	public static void encodeAndSend(RobotController rc, MapLocation target, int command) throws GameActionException {
		encodeAndSend(rc, target, command, new MapLocation(60, 60), INVALID_ROUND_NUM);
	}
	
	public static UnpackedMessage[] receiveAndDecode(RobotController rc) throws GameActionException {
		Message[] messages = rc.readMessages(-1);
		UnpackedMessage[] unpackedMessages = new UnpackedMessage[messages.length];
		int bytes;
        for (int count = 0; count < messages.length; count++) {
        	Message m = messages[count];
            bytes = m.getBytes();
        	System.out.println((rc.getType().isTowerType() ? "Tower" : "Bunny") +
					" received message: '#" + m.getSenderID() + " " + bytes);
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

			System.out.printf("Message content: command %d, x%d, y%d, turn %d, unused %d%n",
					c, x, y, turn, unused);
            unpackedMessages[count] = new UnpackedMessage(c, new MapLocation(x, y), turn,
					m.getRound(), m.getSenderID());
			unpackedMessages[count].message = m;
            count += 1;
        }
        return unpackedMessages;
	}
	
}