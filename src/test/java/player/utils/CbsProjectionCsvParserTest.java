package player.utils;

import java.util.List;

import player.domain.Player;
import player.parsers.Cbs_Projection_Parser;
import player.parsers.Parser;

/**
 * 
 * @author sean
 * cbs projections is a CSV file in the format of
 * Avail, Name, ... , Rank
 * Waiting for Greatness,Javier Baez SS | CHC,619,84,151,84,36,5,26,78,26,203,10,4,.244,.280,.444,65
 *
 */
public class CbsProjectionCsvParserTest {
	public static void main(String... args) {
		Parser testParser = new Cbs_Projection_Parser(args[0], args[1]);
		testParser.parsePlayers();
		List<Player> players = testParser.getPlayers();
		players.stream().filter(p -> p.getFantasy_team_name().equals("Waiting for Greatness")).forEach(mine -> System.out.println(mine.toString()));
	}
}
