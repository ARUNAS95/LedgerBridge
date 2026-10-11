package dev.ledgerbridge;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;
@RestController @RequestMapping("/api/v1/transfers")
public class Api {
 private final Transfers transfers; private final ChainGateway chain;
 public Api(Transfers t,ChainGateway c){transfers=t;chain=c;}
 @PostMapping public ResponseEntity<Map<String,Object>> create(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody Transfer t){return ResponseEntity.accepted().body(transfers.create(key,t));}
 @GetMapping public List<Map<String,Object>> list(){return transfers.list();}
 @GetMapping("/{id}") public Map<String,Object> get(@PathVariable String id){return transfers.get(id);}
 @GetMapping("/{id}/verify") public Map<String,Object> verify(@PathVariable String id)throws Exception {var t=transfers.get(id);String onchain=chain.read(id);return Map.of("id",id,"matches",t.get("digest").equals(onchain),"onChainDigest",onchain);}
}
