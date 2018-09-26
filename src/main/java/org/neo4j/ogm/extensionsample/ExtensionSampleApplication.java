package org.neo4j.ogm.extensionsample;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.function.Supplier;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.util.Pair;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.staticlabel.StaticLabelModificationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

interface ConfigurationRepository extends Neo4jRepository<ConfigurationNode, Long> {
}

@SpringBootApplication
@EnableNeo4jRepositories
public class ExtensionSampleApplication implements CommandLineRunner {

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private ConfigurationRepository repository;
  private String customer = "Customer1";
  private boolean runningEmbedded = false;

  public static void main(String[] args) {
	SpringApplication.run(ExtensionSampleApplication.class, args);
  }

  @Bean
  public Driver javaDriver() {
	return GraphDatabase.driver("bolt://localhost", AuthTokens.basic("neo4j", "secret"));
  }

  private void createNode(String value) {
	Session session = javaDriver().session();
	session.run("CREATE (n:ConfigurationNode{value: $value})", Collections.singletonMap("value", value));
	System.out.println("Java driver saved a node");
	session.close();
  }

  private void printGraph() {
	if (!runningEmbedded)
	  printRawView();
	printOgmView();
  }

  private void printRawView() {
	System.out.println("\nJava driver");
	Session session = javaDriver().session();
	session.run("match (n) return n").list().forEach(record -> {
	  for (Pair<String, Value> field : record.fields()) {
		Node node = field.value().asNode();
		System.out.println("\tNode identity: " + node);
		System.out.println("\twith labels: " + node.labels());
	  }

	});
	System.out.println("\n");
	session.close();
  }

  private void printOgmView() {
	System.out.println("\nNeo4j-OGM");
	System.out.println("\t" + repository.findAll() + "\n");
  }

  private void clearDatabase() {
	javaDriver().session().run("match (n) detach delete n");
  }

  public void run2(String... args) {
	// set up
	clearDatabase();
	createNode("some config"); // add a simple configuration node with Java driver
	printGraph();

	// save configuration node with SDN/Neo4j-OGM
	repository.save(new ConfigurationNode("OGM-Config"));
	System.out.println("Neo4j-OGM saved a node");
	printGraph();

	// tear down
	javaDriver().close();
	sessionFactory.close();
  }

  //  @Bean
  public Configuration ogmConfiguration() {
	return new Configuration.Builder()
		.uri("bolt://localhost")
		.credentials("neo4j", "secret")
		.withConfigProperty(StaticLabelModificationProvider.CONFIGURATION_KEY, "Customer1")
		.build();
  }

  public void run(String... args) {
	// set up
	clearDatabase();
	createNode("some config"); // add a simple configuration node with Java driver
	printGraph();

	// save configuration node with SDN/Neo4j-OGM
	switchCustomer("Customer1");
	repository.save(new ConfigurationNode("OGM-Config-1"));
	printGraph();

	switchCustomer("Customer2");
	repository.save(new ConfigurationNode("OGM-Config-2"));
	printGraph();

	switchCustomer("Customer1");
	printGraph();
	// tear down
	javaDriver().close();
	sessionFactory.close();
  }

  private void switchCustomer(String customer) {
	System.out.println("Switching customer to: " + customer);
	this.customer = customer;
  }

  //  @Bean
  public Configuration supplierConfiguration() {
	return new Configuration.Builder()
		.uri("bolt://localhost")
		.credentials("neo4j", "secret")
		.withConfigProperty(StaticLabelModificationProvider.CONFIGURATION_KEY, (Supplier) () -> customer)
		.build();
  }

  @Bean
  public Configuration embeddedConfiguration() throws Exception {
	runningEmbedded = true;
	File neo4jDb = Files.createTempDirectory("neo4j.db").toFile();
	return new Configuration.Builder()
		.uri("file://" + neo4jDb.getAbsolutePath())
		.withConfigProperty(StaticLabelModificationProvider.CONFIGURATION_KEY, (Supplier) () -> customer)
		.build();
  }

}
