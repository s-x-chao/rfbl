package player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import player.domain.Player;
import player.domain.PlayerResponse;
import player.domain.Resource;
import player.parsers.BaseballAmericaParser2023;
import player.parsers.Baseball_America_HTML_Parser;
import player.parsers.Baseball_America_Latest_Parser;
import player.parsers.FantasyPro_SP_Ranks_CSV;
import player.parsers.GenericCsvParser;
import player.parsers.MLB_HTML_Parser;
import player.parsers.Parser;
import player.repository.PlayerRepository;
import player.repository.ResourceRepository;

@SuppressWarnings("unused")
@SpringBootApplication
public class PlayerSearcher implements CommandLineRunner {

    @Autowired()
    private PlayerRepository playerRepository;
    
    @Autowired()
    private ResourceRepository resourceRepository;
    
	
	private static final Logger log = LoggerFactory.getLogger(PlayerSearcher.class);
    
    @Value("${access_token}")
    private String access_token;
    @Value("${search_url}")
    private String url;
    
    public static void main(String args[]) {
        SpringApplication.run(PlayerSearcher.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
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
//    	Parser parser11 = new MLB_HTML_Parser2("2023-MLB.html", "202304");
//    	parsers.add(parser11);
//    	Parser parser12 = new Baseball_America_HTML_Parser("2023-Baseball-America-April.html", "202304");
//    	parsers.add(parser12);
//    	Parser parser13 = new Baseball_America_Latest_Parser("2024-Baseball-America-Spring.txt", "202402");
//    	parsers.add(parser13);
    	Parser parser14 = new GenericCsvParser("2024-MLB.txt", "202404", new int[] {0,1,2,4});
    	parsers.add(parser14);    	

    	
    	
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
        RestTemplate restTemplate = new RestTemplate();
        
//        String access_token = CbsAuthenticationUtils.getAuthKey();
        
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_PLAIN));
        restTemplate.getMessageConverters().add(converter);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
     		.queryParam("response_format", "json")
        .queryParam("league_id", "rfbl2006")
        .queryParam("access_token",access_token)
        .queryParam("name", "corey seager")
        .queryParam("eligible_only", "0")
        .queryParam("version", "3.0")
        .queryParam("free_agents_only", "1");
        
        int rank = 1;

        for (Player p : players){
        	//offline cached portion
//        	playerList = cachedPlayers.get(p.getFullname());
//        	if (playerList == null || playerList.isEmpty()){
//        		playerList = cachedPlayers.get(p.getLastname());
//        	}
        	//online endpoint seeking portion
        	List<Player> playerList = null;
        	log.debug(p.getFullname());
        	if (playerList == null || playerList.isEmpty()){
        		builder.replaceQueryParam("name", p.getFullname().trim());
        		PlayerResponse response = restTemplate.getForObject(builder.build().toUriString(), PlayerResponse.class);
        		playerList = response.getBody().getPlayers(); 
        		if (playerList == null || playerList.isEmpty()){
        			builder.replaceQueryParam("name", p.getLastname().trim());
        			response = restTemplate.getForObject(builder.build().toUriString(), PlayerResponse.class);
        			playerList = response.getBody().getPlayers(); 
        			if (playerList == null || playerList.isEmpty()){
            			builder.replaceQueryParam("name", p.getFullname().split(" ")[0]);
            			response = restTemplate.getForObject(builder.build().toUriString(), PlayerResponse.class);
            			playerList = response.getBody().getPlayers(); 
            			if (playerList == null || playerList.isEmpty()){
            				log.warn(p + " was not found");
            				rank++;
            				continue;
            			}
        			}
        		}
        	}
        	if (playerList != null && !playerList.isEmpty()){
        		
        		List<Player> filteredList = playerList.size() == 1 
        										? playerList 
        										: playerList.stream().filter(v -> v.equalsRelaxedVersion(p)).collect(Collectors.toList());
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
    					if ("8".equals(p.getOwned_by_team_id())){
    						printMyPlayer(p, site, rank-1);
    					}
    				}
        			
        		}
        	}
        }

    	
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