package dev.ledgerbridge;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.crypto.Credentials;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.utils.Numeric;
import java.math.BigInteger;
import java.util.List;
@Component
public class ChainGateway {
 private final Web3j web3;private final String address,key;
 public ChainGateway(@Value("${ledger.rpc-url}")String rpc,@Value("${ledger.contract}")String address,@Value("${ledger.private-key}")String key){web3=Web3j.build(new HttpService(rpc));this.address=address;this.key=key;}
 private void configured(){if(!address.matches("0x[0-9a-fA-F]{40}"))throw new IllegalStateException("Configure LEDGER_CONTRACT after local deployment");}
 public String read(String id)throws Exception{
  configured();Function f=new Function("records",List.of(new Bytes32(Numeric.hexStringToByteArray(id))),List.of(new TypeReference<Bytes32>(){}));
  var result=web3.ethCall(Transaction.createEthCallTransaction(null,address,FunctionEncoder.encode(f)),DefaultBlockParameterName.LATEST).send();
  if(result.hasError())throw new IllegalStateException(result.getError().getMessage());
  var decoded=FunctionReturnDecoder.decode(result.getValue(),f.getOutputParameters());
  if(decoded.isEmpty())throw new IllegalStateException("No deployed contract at configured address");
  return Numeric.toHexString((byte[])decoded.get(0).getValue());
 }
 public synchronized String anchor(String id,String digest)throws Exception{
  configured();if(key.isBlank())throw new IllegalStateException("Configure local writer key");
  String previous=read(id);
  if(previous.equals(digest))return null; // Already confirmed, e.g. retry after DB write failed.
  if(!previous.equals("0x"+"0".repeat(64)))throw new IllegalStateException("Conflicting on-chain digest");
  Function f=new Function("anchor",List.of(new Bytes32(Numeric.hexStringToByteArray(id)),new Bytes32(Numeric.hexStringToByteArray(digest))),List.of());
  long chainId=web3.ethChainId().send().getChainId().longValueExact();
  if(chainId!=1337)throw new IllegalStateException("This demo writer only supports local chain 1337");
  var credentials=Credentials.create(key);
  var manager=new RawTransactionManager(web3,credentials,chainId);
  var sent=manager.sendTransaction(web3.ethGasPrice().send().getGasPrice(),BigInteger.valueOf(150000),address,FunctionEncoder.encode(f),BigInteger.ZERO);
  if(sent.hasError())throw new IllegalStateException(sent.getError().getMessage());
  var receipt=new PollingTransactionReceiptProcessor(web3,100,100).waitForTransactionReceipt(sent.getTransactionHash());
  if(!receipt.isStatusOK())throw new IllegalStateException("Anchor transaction reverted");
  return receipt.getTransactionHash();
 }
}
