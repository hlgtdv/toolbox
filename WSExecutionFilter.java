import javax.servlet.Filter;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import javax.servlet.ServletException;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.Collections;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.output.TeeOutputStream;

@WebFilter("/*")
public class WSExecutionFilter implements Filter {

    public void doFilter(
        ServletRequest servletRequest,
        ServletResponse servletResponse,
        FilterChain filterChain
    )
        throws ServletException, IOException
    {
        CacheServletRequestWrapper cacheServletRequestWrapper =
            new CacheServletRequestWrapper((HttpServletRequest) servletRequest);
        CacheServletResponseWrapper cacheServletResponseWrapper = 
            new CacheServletResponseWrapper((HttpServletResponse) servletResponse);

        String httpMethod = cacheServletRequestWrapper.getMethod();
        String requestUrl = cacheServletRequestWrapper.getRequestURL().toString();
        String queryString = cacheServletRequestWrapper.getQueryString();
        String requestBody = cacheServletRequestWrapper.payload;

        queryString = queryString == null ? "" : queryString;

        System.out.println("### REQUEST BODY BEGIN ###########################");
        System.out.println(httpMethod + " " + requestUrl + queryString);

        for (String headerName : Collections.list(cacheServletRequestWrapper.getHeaderNames())) {
            String headerValue = cacheServletRequestWrapper.getHeader(headerName);

            System.out.println(headerName + ": " + headerValue);
        }
        System.out.println(requestBody);
        System.out.println("### REQUEST BODY END   ###########################");

        filterChain.doFilter(cacheServletRequestWrapper, cacheServletResponseWrapper);

        int httpStatus = cacheServletResponseWrapper.getStatus();
        String responseBody = cacheServletResponseWrapper.getContent();

//        cacheServletResponseWrapper.copyBodyToResponse();

        System.out.println("### RESPONSE BODY BEGIN ###########################");
        System.out.println(httpStatus);

        for (String headerName : cacheServletResponseWrapper.getHeaderNames()) {
            String headerValue = cacheServletResponseWrapper.getHeader(headerName);

            System.out.println(headerName + ": " + headerValue);
        }
        System.out.println(responseBody);
        System.out.println("### RESPONSE BODY END   ###########################");
    }

    class CacheServletRequestWrapper extends HttpServletRequestWrapper {

        public final String payload;

        public CacheServletRequestWrapper(HttpServletRequest request) throws ServletException {
            super(request);

            // read the original payload into the payload variable
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = null;
            try {
                // read the payload into the StringBuilder
                InputStream inputStream = request.getInputStream();
                if (inputStream != null) {
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    char[] charBuffer = new char[128];
                    int bytesRead = -1;
                    while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                        stringBuilder.append(charBuffer, 0, bytesRead);
                    }
                } else {
                    // make an empty string since there is no payload
                    stringBuilder.append("");
                }
            } catch (IOException ex) {
                throw new ServletException("Error reading the request payload", ex) { };
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException iox) {
                        // ignore
                    }
                }
            }
            payload = stringBuilder.toString();
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload.getBytes());
            ServletInputStream inputStream = new ServletInputStream() {
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }

                @Override
                public void setReadListener(javax.servlet.ReadListener readListener) {
                    // NOOP
                } 

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }
            };
            return inputStream;
        }
    }

    class CacheServletResponseWrapper extends HttpServletResponseWrapper {

        TeeServletOutputStream teeStream;

        PrintWriter teeWriter;

        ByteArrayOutputStream bos;

        public CacheServletResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        public String getContent() throws IOException {
            return bos.toString();
        }

        @Override
        public PrintWriter getWriter() throws IOException {

            if (this.teeWriter == null) {
                this.teeWriter = new PrintWriter(new OutputStreamWriter(getOutputStream()));
            }
            return this.teeWriter;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {

            if (teeStream == null) {
                bos = new ByteArrayOutputStream();
                teeStream = new TeeServletOutputStream(getResponse().getOutputStream(), bos);
            }
            return teeStream;
        }

        @Override
        public void flushBuffer() throws IOException {
            if (teeStream != null) {
                teeStream.flush();
            }
            if (this.teeWriter != null) {
                this.teeWriter.flush();
            }
        }

        public class TeeServletOutputStream extends ServletOutputStream {

            private final TeeOutputStream targetStream;

            public TeeServletOutputStream(OutputStream one, OutputStream two) {
                targetStream = new TeeOutputStream(one, two);
            }

            @Override
            public void write(int b) throws IOException {
                this.targetStream.write(b);
            }

            public void flush() throws IOException {
                super.flush();
                this.targetStream.flush();
            }

            public void close() throws IOException {
                super.close();
                this.targetStream.close();
            }

            @Override
            public void setWriteListener(javax.servlet.WriteListener writeListener) {
                // NOOP
            } 

            @Override
            public boolean isReady() {
                return true;
            }
        }
    }
}
