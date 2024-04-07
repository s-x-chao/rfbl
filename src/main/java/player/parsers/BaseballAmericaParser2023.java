package player.parsers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import player.PlayerSearcher;
import player.domain.MLBTeam;
import player.domain.Player;
import player.domain.Position;
import player.exceptions.MLBTeamNotFoundException;
import player.exceptions.OutOfSequenceException;
import player.exceptions.PositionNotFoundException;

public class BaseballAmericaParser2023 extends Parser{

	private static final Logger log = LoggerFactory.getLogger(PlayerSearcher.class);

	
	public BaseballAmericaParser2023() {
	}

	public BaseballAmericaParser2023(String file, String date){
		super(file, date);
	}
	
	@Override
	public void parsePlayers() {
	/*
	 * each chunk of text for an individual player looks like this:  I will look for lines that starts with a ^[0-9]
		1. Jackson Holliday
		Baltimore Orioles
		SS		
	 * 
	 */
		List<String> lines = new ArrayList<>();
		File file = new File(getClass().getClassLoader().getResource(getFile()).getFile());
		try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()))) {

			lines = stream
					.collect(Collectors.toList());
			int rank = 0;
			Iterator<String> playerIterator = lines.iterator();
			String playerLine = "";
			Player player = null;
			List<String> playerChunk = new ArrayList<>();
			while (playerIterator.hasNext()){
				playerLine = playerIterator.next();
				if (playerLine.equals("")) {
					if (playerChunk.isEmpty()) {
						continue;
					}else {
						String name = "";
						try {
							name = playerChunk.get(0).split("\\.")[1];
						}catch(IndexOutOfBoundsException p) {
							log.error(p.getMessage());
						}
						player.setFullname(name);
						String[] namePieces = player.getFullname().split(" ");
						player.setLastname(namePieces[namePieces.length-1]);  //most likely the last name
						try {
							player.setMlbTeam(MLBTeam.getMLBTeam(playerChunk.get(1)));
						}catch(MLBTeamNotFoundException p){
							log.error(p.getMessage());
						}
						try {
							player.setPos(Position.getPosition(playerChunk.get(2)));
						}catch(PositionNotFoundException p){
							log.error(p.getMessage());
						}						
					}
					playerChunk.clear();
					continue;
				}
				if (playerLine.matches("^[0-9]+\\.+.*")){
					//this is the rank line of a player, starts the "chunk"
					player = playerFactory.getPlayer();
					player.getMentions().get(getName()).setRank(++rank);
					playerChunk = player.getMentions().get(getName()).getText();
					players.add(player);
				}
				playerChunk.add(playerLine);
			}

		} catch (IOException e) {
			log.error(e.getMessage());
		} 
	}

}
