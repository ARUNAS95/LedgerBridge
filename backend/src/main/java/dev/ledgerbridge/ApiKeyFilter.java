package dev.ledgerbridge;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Component
public class ApiKeyFilter extends OncePerRequestFilter {
 private final byte[] key;
 public ApiKeyFilter(@Value("${ledger.api-key}")String key){if(key.isBlank())throw new IllegalArgumentException("API key cannot be empty");this.key=key.getBytes(StandardCharsets.UTF_8);}
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  if(req.getRequestURI().startsWith("/api/")){
   String provided=req.getHeader("X-API-Key");
   if(provided==null || !MessageDigest.isEqual(key,provided.getBytes(StandardCharsets.UTF_8))){res.sendError(401,"Valid X-API-Key required");return;}
  }
  chain.doFilter(req,res);
 }
}
