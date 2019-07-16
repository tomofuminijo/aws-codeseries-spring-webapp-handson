package hello.repositories;

import java.util.Optional;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

import hello.model.Greeting;

@EnableScan
public interface GreetingRepository extends CrudRepository<Greeting, String> {
}
