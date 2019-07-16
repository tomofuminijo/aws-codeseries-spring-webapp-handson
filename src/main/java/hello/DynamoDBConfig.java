package hello;

import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverterFactory;



@Configuration
@EnableDynamoDBRepositories(dynamoDBMapperConfigRef = "getDynamoDBMapperConfig", basePackages = "hello.repositories")
public class DynamoDBConfig {
    @Value("${amazon.dynamodb.local}")
	private boolean amazonDynamoDBLocal;

    @Value("${amazon.dynamodb.endpoint}")
	private String amazonDynamoDBEndpoint;
	
    @Value("${amazon.dynamodb.region}")
	private String amazonDynamoDBRegion;
	
    @Value("${amazon.dynamodb.tableprefix}")
	private String amazonDynamoDBTablePrefix;

    @Value("${amazon.credential.profile}")
    private String profile;
    
	@Bean
	public AmazonDynamoDB amazonDynamoDB() {

		AmazonDynamoDB amazonDynamoDB  = null;
		System.out.println("---DyanmoDB Local enabled?: "+ amazonDynamoDBLocal);
		
		if (amazonDynamoDBLocal) {
			System.out.println("---DyanmoDB Local Endpoint: "+ amazonDynamoDBEndpoint);

			amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
	            .withCredentials(amazonAWSCredentials())
				.withEndpointConfiguration(
				new AwsClientBuilder.EndpointConfiguration(amazonDynamoDBEndpoint, amazonDynamoDBRegion))
				.build();

		} else {
			amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
	            .withCredentials(amazonAWSCredentials())
				.build();
		}
		
		return amazonDynamoDB;
	}

	@Bean
	public AWSCredentialsProvider amazonAWSCredentials() {
		return DefaultAWSCredentialsProviderChain.getInstance();
//		return new ProfileCredentialsProvider(profile);
	}

	@Bean
	public DynamoDBMapperConfig.TableNameOverride getTableNameOverride() {
	    return DynamoDBMapperConfig.TableNameOverride.withTableNamePrefix(amazonDynamoDBTablePrefix);
	}
	
	@Bean
	public DynamoDBMapperConfig getDynamoDBMapperConfig(DynamoDBMapperConfig.TableNameOverride tableNameOverride) {
	    DynamoDBMapperConfig.Builder builder = new DynamoDBMapperConfig.Builder();
	    builder.setTableNameOverride(tableNameOverride);
	    builder.setTypeConverterFactory(DynamoDBTypeConverterFactory.standard());
	    return builder.build();
	}
	
	@Bean
	public DynamoDBMapper getDynamoDBMapper(AmazonDynamoDB dynamoDB, DynamoDBMapperConfig config) {
		
		return new DynamoDBMapper(dynamoDB, config);

	}
	
}