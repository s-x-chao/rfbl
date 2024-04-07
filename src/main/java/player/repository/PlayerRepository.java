package player.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import player.domain.Player;

@Component
public interface PlayerRepository extends MongoRepository<Player, String> {

    public List<Player> findByFirstname(String firstname);
    public List<Player> findByLastname(String lastname);

    public Player findById(String id);
//    public List<Player> findByLastName(String lastName);

}
