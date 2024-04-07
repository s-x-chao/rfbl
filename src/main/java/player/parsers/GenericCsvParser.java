package player.parsers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import player.exceptions.PositionNotFoundException;

public class GenericCsvParser extends Parser{

	private static final Logger log = LoggerFactory.getLogger(PlayerSearcher.class);

	
	public GenericCsvParser() {
	}

	int[] slots;
	
	public GenericCsvParser(String file, String date, int[] slots){
		super(file, date);
		this.slots = slots;
	}
	
	@Override
	public void parsePlayers() {
		int rankNum = slots[0];
		int nameNum = slots[1];
		int posNum = slots[2];
		int teamNum = slots[3];
		
		List<String> lines = new ArrayList<>();
		File file = new File(getClass().getClassLoader().getResource(getFile()).getFile());
		try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()))) {

			lines = stream
					.collect(Collectors.toList());
			int rank = 0;
			for (String playerLine : lines){
				if (playerLine.startsWith("#")) {
					continue;
				}
				Player player = playerFactory.getPlayer();
				String[] pl = playerLine.split(",");
				String name = pl[nameNum].trim();
				String pos = pl[posNum];
				player.setFullname(name);
				String[] namePieces = player.getFullname().split(" ");
				player.setLastname(namePieces[namePieces.length-1]);  //most likely the last name
				player.setEligible_positions(pos.split("/"));
				try {
					player.setPos(Position.getPosition(player.getEligible_positions()[0].trim()));
				}catch(PositionNotFoundException p){
					log.debug(p.getMessage());
				}
				player.setPro_team(pl[teamNum]);
				try {
					MLBTeam.getMLBTeam(player.getPro_team());
				}catch(MLBTeamNotFoundException p){
					log.debug(p.getMessage());
				}
				if (rankNum >= 0) {
					player.getMentions().get(getName()).setRank(Integer.valueOf(pl[rankNum]));
				}else {
					player.getMentions().get(getName()).setRank(++rank);
				}
				players.add(player);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
