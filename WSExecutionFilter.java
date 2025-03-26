import javax.servlet.Filter;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.Collections;
import java.nio.charset.StandardCharsets;

import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;

@WebFilter("/*")
public class WSExecutionFilter implements Filter {

    public void doFilter(
        ServletRequest servletRequest,
        ServletResponse servletResponse,
        FilterChain filterChain
    )
        throws ServletException, IOException
    {
        CachedHttpServletRequest cachedHttpServletRequest =
            new CachedHttpServletRequest((HttpServletRequest) servletRequest);
        ContentCachingResponseWrapper contentCachingResponseWrapper = 
            new ContentCachingResponseWrapper((HttpServletResponse) servletResponse);

        String httpMethod = cachedHttpServletRequest.getMethod();
        String requestUrl = cachedHttpServletRequest.getRequestURL().toString();
        String queryString = cachedHttpServletRequest.getQueryString();
        String requestBody = cachedHttpServletRequest.getCachedBody();

        queryString = queryString == null ? "" : queryString;

        System.out.println("### REQUEST BODY BEGIN ###########################");
        System.out.println(httpMethod + " " + requestUrl + queryString);

        for (String headerName : Collections.list(cachedHttpServletRequest.getHeaderNames())) {
            String headerValue = cachedHttpServletRequest.getHeader(headerName);

            System.out.println(headerName + ": " + headerValue);
        }
        System.out.println(requestBody);
        System.out.println("### REQUEST BODY END   ###########################");

        filterChain.doFilter(cachedHttpServletRequest, contentCachingResponseWrapper);

        int httpStatus = contentCachingResponseWrapper.getStatus();
        String responseBody = new String(contentCachingResponseWrapper.getContentAsByteArray(),
            contentCachingResponseWrapper.getCharacterEncoding());

        contentCachingResponseWrapper.copyBodyToResponse();

        System.out.println("### RESPONSE BODY BEGIN ###########################");
        System.out.println(httpStatus);

        for (String headerName : contentCachingResponseWrapper.getHeaderNames()) {
            String headerValue = contentCachingResponseWrapper.getHeader(headerName);

            System.out.println(headerName + ": " + headerValue);
        }
        System.out.println(responseBody);
        System.out.println("### RESPONSE BODY END   ###########################");
    }

    private class CachedHttpServletRequest extends HttpServletRequestWrapper {

        private byte[] cachedBody;

        public CachedHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            InputStream requestInputStream = request.getInputStream();
            this.cachedBody = StreamUtils.copyToByteArray(requestInputStream);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new CachedBodyServletInputStream(this.cachedBody);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
            return new BufferedReader(new InputStreamReader(byteArrayInputStream));
        }

        public String getCachedBody()
            throws UnsupportedEncodingException
        {
            return new String(this.cachedBody, this.getCharacterEncoding());
        }
    }

    private class CachedBodyServletInputStream extends ServletInputStream {

        private InputStream cachedBodyInputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.cachedBodyInputStream = new ByteArrayInputStream(cachedBody);
        }

        public void setReadListener(javax.servlet.ReadListener readListener) {
            // NOOP
        } 

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public int read() throws IOException {
            return cachedBodyInputStream.read();
        }

        @Override
        public boolean isFinished() {
            try {
                return cachedBodyInputStream.available() == 0;
            }
            catch (IOException e) {
                return true;
            }
        }
    }
}
