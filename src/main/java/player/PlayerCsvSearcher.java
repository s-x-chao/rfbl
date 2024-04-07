package player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import player.domain.Player;
import player.domain.Resource;
import player.parsers.Baseball_America_HTML_Parser;
import player.parsers.Cbs_Projection_Parser;
import player.parsers.Parser;
import player.repository.PlayerRepository;
import player.repository.ResourceRepository;

//@SpringBootApplication
public class PlayerCsvSearcher implements CommandLineRunner {

	@Autowired()
	private PlayerRepository playerRepository;

	@Autowired()
	private ResourceRepository resourceRepository;

	List<Player> pool = new ArrayList<>();

	private static final Logger log = LoggerFactory.getLogger(PlayerCsvSearcher.class);

	public static void main(String args[]) {
		SpringApplication.run(PlayerCsvSearcher.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Parser cbsHitterParser = new Cbs_Projection_Parser("batters-202201.csv", "202202");
		cbsHitterParser.parsePlayers();
		pool.addAll(cbsHitterParser.getPlayers());
		Parser cbsPitcherParser = new Cbs_Projection_Parser("pitchers-202201.csv", "202202");
		cbsPitcherParser.parsePlayers();
		pool.addAll(cbsPitcherParser.getPlayers());

		List<Parser> parsers = new ArrayList<>();
		//    	Parser parser1 = new Baseball_America_2017_Parser("2017-Baseball-America.csv");
		//    	parsers.add(parser1);
		////    	Parser parser2 = new MLB_2016("2016-MLB.txt"); 
		////    	parsers.add(parser2);
		//    	Parser parser2b = new MLB_2016("2017-MLB.txt"); 
		//    	parsers.add(parser2b);
		//    	Parser parser3 = new Baseball_Prospectus_2017_Parser("2017-Baseball-Prospectus.csv");
		//    	parsers.add(parser3);  
		//    	Parser parser4 = new ESPN_2017_Parser("2017-ESPN.csv");
		//    	parsers.add(parser4);
		//    	Parser parser4a = new ESPN_2017_APRIL_Parser("2017-ESPN-April.csv");
		//    	parsers.add(parser4a);
		//    	Parser parser4b = new ESPN_2018_Parser("2018-ESPN.csv");
		//    	parsers.add(parser4b);
		//    	Parser parser5 = new Baseball_America_May_2017_Parser("2017-05-Baseball-America.csv");
		//    	parsers.add(parser5);
		//    	Parser parser6 = new Baseball_America_2017_Midseason_Parser("2017-Baseball-America-(M).csv");
		//    	parsers.add(parser6);
		//    	Parser parser7 = new Baseball_America_2018_Parser("2018-Baseball-America.csv");
		//    	parsers.add(parser7);
		//    	Parser parser8 = new Baseball_America_HTML_Parser("2019-Baseball-America-September.html", 201809);
		//    	parsers.add(parser8);
		//    	Parser parser9 = new Baseball_Prospectus_2019_Parser("2019-Baseball-Prospectus.csv");
		//    	parsers.add(parser9);
		//    	Parser parser10 = new Baseball_America_HTML_Parser("2019-Baseball-America-January.html", "201901");
		//    	parsers.add(parser10);
		//    	Parser parser11 = new MLB_HTML_Parser("2019-MLB.html", "201901");
		//    	parsers.add(parser11);
		Parser parser12 = new Baseball_America_HTML_Parser("2020-Baseball-America-January.html", "202001");
		parsers.add(parser12);


		for (Parser parser : parsers){
			parser.parsePlayers();
			List<Player> players = parser.getPlayers();
			Resource resource = parser.getResource();
			log.info("Searching for players listed in {}.", resource.getSite());
			search(players, parser.getName());
			log.info("All players listed in {} cataloged.", resource.getSite());
			log.info("Saving player list for {}", resource.getSite());
			resourceRepository.save(resource);
			log.info("Completed saving player list for {}", resource.getSite());
		}
	}

	public void search(List<Player> players, String site){
		int rank = 1;

		for (Player p : players){

			log.debug(p.getFullname());
			List<Player> playerList = pool.stream().filter(player -> fuzzyMatch(player, p.getFullname())).collect(Collectors.toList());
			if (playerList != null && !playerList.isEmpty()){

				Map<String, List<Player>> playerMap = playerList.stream().collect(Collectors.groupingBy(Player::getFullname));

				String key = FuzzySearch.extractOne(p.getFullname(), playerMap.keySet()).getString();

				playerList = playerMap.get(key);
				List<Player> filteredList = playerList.size() == 1 
						? playerList 
								: playerList.stream().filter(v -> v.equalsRelaxedVersion(p)).collect(Collectors.toList());
				if (filteredList == null || filteredList.isEmpty()) {
					log.error("NOT FOUND: " + p.getFullname());
				}
				if (filteredList.size() > 1) {
					filteredList = filteredList.stream().filter(v -> v.getMlbTeam().name().equals(p.getMlbTeam().name())).collect(Collectors.toList());
				}

				for (Player player_candidate : filteredList){
					if (p.equals(player_candidate) || filteredList.size()==1){
						Player previously_saved_player = playerRepository.findById(player_candidate.getId());
						if (previously_saved_player != null) {
							previously_saved_player.getMentions().computeIfAbsent(site, v-> p.getMentions().get(site));
							previously_saved_player.copy(player_candidate);
							p.copy(previously_saved_player);
							playerRepository.save(previously_saved_player);
							log.debug("Saving existing player - #{}.{}.", rank, p.getFullname());
							rank++;
						}else{
							log.debug("Saving new player - #{}.{}.", rank, p.getFullname());
							rank++;
							p.copy(player_candidate);
							playerRepository.save(p);
						}
					}else{
						log.debug("mismatch found");
					}
					if (p.isFree_agent()){
						printFreeAgent(p, site, rank-1);
					}else{
						if (p.getFantasy_team_name().equals("Waiting for Greatness")){
							printMyPlayer(p, site, rank-1);
						}
					}

				}
			}
		}
	}

	private boolean fuzzyMatch(Player player, String name) {
		return FuzzySearch.ratio(player.getFullname(), name) > 50;
	}

	private void printFreeAgent(Player p, String site, int rank) {
		log.info("Free Agent alert: Ranked {} by {} :", rank, site);
		log.info(p.toString());
	}

	private void printMyPlayer(Player p, String site, int rank) {
		log.info(" *** Future superstar alert: Ranked {} by {} :", rank, site);
		log.info(p.toString());
	}


}