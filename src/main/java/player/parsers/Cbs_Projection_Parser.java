package player.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import player.domain.Player;

public class Cbs_Projection_Parser extends Parser {

	private static final Logger log = LoggerFactory.getLogger(Cbs_Projection_Parser.class);
	private static final String path = "cbs";
	private static final String FA = "FA";
	
	public Cbs_Projection_Parser(String file, String date) {
		super(file, date);
	}

	enum Cbs_Headers {
		Avail, Player, Rank
	}
	
	@Override
	public void parsePlayers() {
		File file = new File(getClass().getClassLoader().getResource(path+"/"+getFile()).getFile());
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			in.readLine();
		    Iterable<CSVRecord> records = CSVFormat.DEFAULT
		    	      .withFirstRecordAsHeader()
		    	      .withIgnoreEmptyLines()
		    	      .withAllowMissingColumnNames(true)
		    	      .parse(in);
		    	    for (CSVRecord record : records) {
		    	    	if (record.size() == 1) 
		    	    		continue;
		    	    	Player entry = playerFactory.getPlayer();
		    	        String team = record.get(Cbs_Headers.Avail);
		    	        String player = record.get(Cbs_Headers.Player);
		    	        /*
		    	         * Bryce Harper RF | PHI
		    	         */
		    	        String proTeam = player.split("\\|")[1].trim();
		    	        String[] tokens = player.split("\\|")[0].split(" ");
		    	        String firstName = tokens[0];
		    	        String lastName = tokens[tokens.length-2];
		    	        String position = tokens[tokens.length-1];
		    	        StringBuilder fullName = new StringBuilder();
		    	        for (int i = 0; i < tokens.length-1; i++) {
		    	        	fullName.append(tokens[i]).append(" ");
		    	        }
		    	        String rank = record.get(Cbs_Headers.Rank);
		    	        if (team.equals(FA)) {
		    	        	entry.setFree_agent(true);
		    	        }else {
		    	        	entry.setFantasy_team_name(team);
		    	        	entry.setFree_agent(false);
		    	        }
		    	        entry.setFirstname(firstName);
		    	        entry.setLastname(lastName);
		    	        entry.setFullname(fullName.toString().trim());
		    	        entry.setPosition(position);
		    	        entry.setPro_team(proTeam);
		    	        entry.setEligible_positions(new String[] {position});
		    	        entry.getMentions().get(getName()).setRank(Integer.valueOf(rank));
		    	        entry.setId();
		    	        if (entry.getId() == null) {
		    	        	continue;
		    	        }
		    	        players.add(entry);
		    	    }
		} catch (IOException e) {
			log.error("Unable to process "+file);
		}
	}

	
}
