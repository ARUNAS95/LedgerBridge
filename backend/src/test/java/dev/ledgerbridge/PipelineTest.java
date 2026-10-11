package dev.ledgerbridge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfSystemProperty(named="blockchain.it",matches="true")
@SpringBootTest(properties={"ledger.events-enabled=true"})
@EmbeddedKafka(partitions=1,topics=Events.TOPIC,bootstrapServersProperty="spring.kafka.bootstrap-servers")
class PipelineTest {
 @Autowired Transfers transfers;@Autowired ChainGateway chain;@Autowired JdbcTemplate db;
 @Test void databaseOutboxKafkaEthereumRoundTrip()throws Exception{
  var row=transfers.create("integration-"+System.nanoTime(),new Transfer("E2E-INVOICE",25000,"USD"));String id=(String)row.get("id");
  long deadline=System.currentTimeMillis()+30000;
  while(System.currentTimeMillis()<deadline && !transfers.get(id).get("status").equals("ANCHORED"))Thread.sleep(200);
  assertEquals("ANCHORED",transfers.get(id).get("status"));assertEquals(row.get("digest"),chain.read(id));
  assertNull(chain.anchor(id,(String)row.get("digest")));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM outbox WHERE id=? AND published=TRUE",Integer.class,id));
 }
}
