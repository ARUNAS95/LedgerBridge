package dev.ledgerbridge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc
class ApiTest {
 @Autowired MockMvc mvc;@Autowired Transfers transfers;
 String body="{\"reference\":\"INV-42\",\"amountMinor\":12500,\"currency\":\"USD\"}";
 @Test void requiresApiKey()throws Exception{mvc.perform(get("/api/v1/transfers")).andExpect(status().isUnauthorized());}
 @Test void rejectsInvalidAmount()throws Exception{mvc.perform(post("/api/v1/transfers").header("X-API-Key","local-demo-key").header("Idempotency-Key","invalid").contentType(MediaType.APPLICATION_JSON).content(body.replace("12500","-1"))).andExpect(status().isBadRequest());}
 @Test void acceptsAndReplaysSameRecord()throws Exception{for(int i=0;i<2;i++)mvc.perform(post("/api/v1/transfers").header("X-API-Key","local-demo-key").header("Idempotency-Key","replay").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("PENDING"));}
 @Test void rejectsConflictingReplay()throws Exception{transfers.create("conflict",new Transfer("INV-42",12500,"USD"));mvc.perform(post("/api/v1/transfers").header("X-API-Key","local-demo-key").header("Idempotency-Key","conflict").contentType(MediaType.APPLICATION_JSON).content(body.replace("12500","12501"))).andExpect(status().isConflict());}
 @Test void rejectsMalformedIdempotencyKey()throws Exception{mvc.perform(post("/api/v1/transfers").header("X-API-Key","local-demo-key").header("Idempotency-Key","bad key").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());}
 @Test void digestChangesWithAmount(){assertNotEquals(Transfers.digest(new Transfer("INV",100,"USD")),Transfers.digest(new Transfer("INV",101,"USD")));}
 @Test void digestIsDeterministic(){assertEquals(Transfers.digest(new Transfer("INV",100,"USD")),Transfers.digest(new Transfer("INV",100,"USD")));}
}
