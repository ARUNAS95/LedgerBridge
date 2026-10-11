package dev.ledgerbridge;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.util.backoff.FixedBackOff;
import org.apache.kafka.clients.admin.NewTopic;
import java.util.concurrent.TimeUnit;
@Component @ConditionalOnProperty(name="ledger.events-enabled",havingValue="true")
public class Events {
 public static final String TOPIC="ledgerbridge.transfer.accepted.v1";
 private final JdbcTemplate db;private final KafkaTemplate<String,String> kafka;private final Transfers transfers;private final ChainGateway chain;
 public Events(JdbcTemplate db,KafkaTemplate<String,String> kafka,Transfers t,ChainGateway chain){this.db=db;this.kafka=kafka;transfers=t;this.chain=chain;}
 @Bean NewTopic topic(){return new NewTopic(TOPIC,1,(short)1);}
 @Bean ConcurrentKafkaListenerContainerFactory<String,String> kafkaListenerContainerFactory(ConsumerFactory<String,String> cf){var f=new ConcurrentKafkaListenerContainerFactory<String,String>();f.setConsumerFactory(cf);f.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000,FixedBackOff.UNLIMITED_ATTEMPTS)));return f;}
 @Scheduled(fixedDelay=1000) @Transactional public void publish()throws Exception{
  // Lock rows across replicas. Crash after broker ack can redeliver; consumer/contract are idempotent.
  for(var row:db.queryForList("SELECT id FROM outbox WHERE published=FALSE ORDER BY created_at LIMIT 20 FOR UPDATE SKIP LOCKED")){
   String id=(String)row.get("id");if(id==null)id=(String)row.get("ID");
   kafka.send(TOPIC,id,id).get(10,TimeUnit.SECONDS);db.update("UPDATE outbox SET published=TRUE WHERE id=?",id);
  }
 }
 @KafkaListener(topics=TOPIC) public void consume(String id)throws Exception{
  var record=transfers.get(id);String hash=chain.anchor(id,(String)record.get("digest"));
  // Keep existing tx hash on duplicate deliveries; zero hash is not fabricated for chain recovery.
  if(hash!=null)transfers.anchored(id,hash);else db.update("UPDATE transfers SET status='ANCHORED' WHERE id=?",id);
 }
}
