package com.ranger.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bound JSON bodies, including requests sent without a Content-Length header. */
@Component @Order(-90) // Spring Security runs first; unauthenticated bodies aren't buffered here.
public class RequestSizeFilter extends OncePerRequestFilter {
    private static final int MAX_BYTES=1_048_576;
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || !(request.getMethod().equals("POST") || request.getMethod().equals("PATCH"));
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        if(request.getContentLengthLong()>MAX_BYTES) { response.sendError(413,"Request body too large."); return; }
        byte[] bytes=request.getInputStream().readNBytes(MAX_BYTES+1);
        if(bytes.length>MAX_BYTES) { response.sendError(413,"Request body too large."); return; }
        HttpServletRequest wrapped=new HttpServletRequestWrapper(request) {
            @Override public int getContentLength(){ return bytes.length; }
            @Override public long getContentLengthLong(){ return bytes.length; }
            @Override public ServletInputStream getInputStream(){
                ByteArrayInputStream input=new ByteArrayInputStream(bytes);
                return new ServletInputStream(){
                    @Override public boolean isFinished(){return input.available()==0;}
                    @Override public boolean isReady(){return true;}
                    @Override public void setReadListener(ReadListener listener){throw new UnsupportedOperationException("Async request reads are not supported.");}
                    @Override public int read(){return input.read();}
                    @Override public int read(byte[] b,int offset,int length){return input.read(b,offset,length);}
                };
            }
            @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
        };
        chain.doFilter(wrapped,response);
    }
}
