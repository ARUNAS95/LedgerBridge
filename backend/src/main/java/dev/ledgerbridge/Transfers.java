package dev.ledgerbridge;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.web3j.crypto.Hash;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Service
public class Transfers {
 private final JdbcTemplate db;
 public Transfers(JdbcTemplate db){this.db=db;}
 public static String digest(Transfer t){
  // Length-prefixed reference and integer minor units avoid ambiguous decimals/delimiters.
  String canonical="v1:"+t.reference().length()+":"+t.reference()+":"+t.amountMinor()+":"+t.currency();
  try{return "0x"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));}
  catch(Exception e){throw new IllegalStateException(e);}
 }
 @Transactional public Map<String,Object> create(String key,Transfer t){
  if(key==null || !key.matches("[a-zA-Z0-9_-]{1,64}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Idempotency-Key must be 1-64 letters, digits, underscores or hyphens");
  String digest=digest(t);String id=Hash.sha3String("ledgerbridge:demo:"+key);
  // ON CONFLICT is supported by PostgreSQL and H2 PostgreSQL mode.
  db.update("INSERT INTO transfers(id,request_key,digest,reference,amount_minor,currency,status) VALUES(?,?,?,?,?,?, 'PENDING') ON CONFLICT DO NOTHING",id,key,digest,t.reference(),t.amountMinor(),t.currency());
  Map<String,Object> saved=get(id);
  if(!saved.get("digest").equals(digest))throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency key already used with different content");
  db.update("INSERT INTO outbox(id) VALUES(?) ON CONFLICT DO NOTHING",id);
  return saved;
 }
 public Map<String,Object> get(String id){var rows=db.queryForList("SELECT id,digest,reference,amount_minor AS amountMinor,currency,status,transaction_hash AS transactionHash FROM transfers WHERE id=?",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);return normalize(rows.get(0));}
 public List<Map<String,Object>> list(){return db.queryForList("SELECT id,digest,reference,amount_minor AS amountMinor,currency,status,transaction_hash AS transactionHash FROM transfers ORDER BY created_at DESC LIMIT 100").stream().map(this::normalize).toList();}
 private Map<String,Object> normalize(Map<String,Object> row){Map<String,Object> out=new LinkedHashMap<>();row.forEach((k,v)->out.put(k.equalsIgnoreCase("amountMinor")?"amountMinor":k.equalsIgnoreCase("transactionHash")?"transactionHash":k.toLowerCase(Locale.ROOT),v));return out;}
 public void anchored(String id,String hash){db.update("UPDATE transfers SET status='ANCHORED',transaction_hash=? WHERE id=?",hash,id);}
}
