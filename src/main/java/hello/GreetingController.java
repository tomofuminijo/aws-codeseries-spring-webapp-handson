package hello;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Value;

import hello.model.Greeting;
import hello.repositories.GreetingRepository;
import java.util.Optional;


@Controller
public class GreetingController {
	@Autowired
	private GreetingRepository greetingRepository;
	
	@Autowired
	private AmazonDynamoDB dynamoDB;

	@Autowired
	private DynamoDBMapper dynamoDBMapper;


    @GetMapping("/greeting")
    public String greeting(@RequestParam(name="lang", required=false, defaultValue="en") String lang, Model model) {
        
        Greeting greeting = greetingRepository.findById(lang).get();
        model.addAttribute("hello", greeting.getHello());
        model.addAttribute("lang", lang);
        model.addAttribute("langname", greeting.getLangname());
        return "greeting";
    }


	@GetMapping("/init")
	private String init() throws Exception {
	    
	    // Retrieve Mapper
        String ddbGreetingTableName = dynamoDBMapper.generateCreateTableRequest(Greeting.class).getTableName();

        System.out.println(" -- Greeting Table Name: " + ddbGreetingTableName);
        // Create a table with a primary hash key named 'name', which holds a string
        CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(ddbGreetingTableName)
            .withKeySchema(new KeySchemaElement().withAttributeName("lang").withKeyType(KeyType.HASH))
            .withAttributeDefinitions(new AttributeDefinition().withAttributeName("lang").withAttributeType(ScalarAttributeType.S))
            .withBillingMode(BillingMode.PAY_PER_REQUEST);
// If you wanna provisioned throughput, use bellow code.
//            .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

        // Create table if it does not exist yet
        TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);

        System.out.println("Create Table request has issued. waiting until actived");

        // wait for the table to move into ACTIVE state
        TableUtils.waitUntilActive(dynamoDB, ddbGreetingTableName);

        // Describe our new table
        DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(ddbGreetingTableName);
        TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
        System.out.println("Table Description: " + tableDescription);

	    
		greetingRepository.save(new Greeting("en", "Hello", "English"));
		greetingRepository.save(new Greeting("ja", "こんにちは", "Japanese"));
		greetingRepository.save(new Greeting("zh", "您好", "Chinese"));
		greetingRepository.save(new Greeting("ko", "여보세요", "Korean"));
		greetingRepository.save(new Greeting("el", "Saluton", "Greek"));
		greetingRepository.save(new Greeting("it", "Ciao", "Italian"));
		greetingRepository.save(new Greeting("de", "Hallo", "German"));
		greetingRepository.save(new Greeting("fr", "Bonjour", "French"));
		return "index";
	}

}