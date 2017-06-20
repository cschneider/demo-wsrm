/*
 * Copyright 2017 apifocal LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apifocal.demo.gateway.qos;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.HeaderGroup;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP gateway towards an upstream server with a configurable message drop policy.
 */
public class MorpheusGateway extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(MorpheusGateway.class);
    private static final AtomicInteger EXCHANGE_COUNTER = new AtomicInteger(0);

    protected static final HeaderGroup HOPBYHOP_HEADERS;
    static {
        HOPBYHOP_HEADERS = new HeaderGroup();
        String[] headers = new String[] {
            "Connection", 
            "Keep-Alive", 
            "Proxy-Authenticate", 
            "Proxy-Authorization",
            "TE", 
            "Trailers", 
            "Transfer-Encoding", 
            "Upgrade" };
        for (String header : headers) {
            HOPBYHOP_HEADERS.addHeader(new BasicHeader(header, null));
        }
    }

    private String policy = "noop";
    private URL forward;
    private QosPolicy qos;
    private HttpHost target;
    private HttpClient proxy;


    public MorpheusGateway() {
        LOG.info("MorpheusGateway CREATED");
        
        HttpClientBuilder b;

    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public URL getForward() {
        return forward;
    }

    public void setForward(URL forward) {
        this.forward = forward;
    }

    @Override
    public void init() throws ServletException {
    	super.init();

    	qos = policy.equals("delay") ?  new RandomDelayPolicy() : new NoopPolicy();
    	LOG.info("Initializing MorpheusGateway {} policy with qos={} ", policy, qos);

    	try {
			target = URIUtils.extractHost(forward.toURI());
		} catch (URISyntaxException e) {
			throw new ServletException("Failed to parse URI", e);
        }
        RequestConfig.Builder config = RequestConfig.custom()
            .setRedirectsEnabled(false)
            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
            .setConnectTimeout(-1);
        proxy = HttpClientBuilder.create().setDefaultRequestConfig(config.build()).build();
    }

    public void destroy() {
    	if (proxy != null) {
            if (proxy instanceof Closeable) {
    	        try {
    	            ((Closeable)proxy).close();
    	        } catch (IOException e) {
    	            LOG.warn("Failed to destroy MorpheusGateway: {}", e);
    	        }
    	    } else {
    	        LOG.warn("Old version of HttpClient?");
    	        proxy.getConnectionManager().shutdown();
    	    }
    	}
        LOG.info("Destroyed MorpheusGateway");

        super.destroy();
    }

    protected HttpResponse doProxy(HttpServletRequest servletRequest, HttpServletResponse servletResponse, HttpRequest proxyRequest) throws IOException {
        LOG.info("Proxy {} to '{}'", servletRequest.getMethod(), servletRequest.getRequestURI());
        LOG.info("Proxy request line: '{}'", proxyRequest.getRequestLine());
        return proxy.execute(target, proxyRequest);
    }

    protected String rewriteProxyUrl(HttpServletRequest request) {
        LOG.info("Forwarding to '{}'", forward.toString());
        return forward.toString();
    }

    protected HttpRequest createRequest(String method, String uri, HttpServletRequest servletRequest) throws IOException {
        HttpEntityEnclosingRequest pr = new BasicHttpEntityEnclosingRequest(method, uri);
        // Add the input entity (streamed)
        //  note: we don't bother ensuring we close the servletInputStream since the container handles it
        pr.setEntity(new InputStreamEntity(servletRequest.getInputStream(), getContentLength(servletRequest)));
        return pr;
    }

    protected void copyRequestHeader(HttpServletRequest request, HttpRequest proxyRequest, String headerName) {
        //Instead the content-length is effectively set via InputStreamEntity
        if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH) || HOPBYHOP_HEADERS.containsHeader(headerName)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Enumeration<String> headers = request.getHeaders(headerName);
        while (headers.hasMoreElements()) { // possibly more than one value
            String headerValue = headers.nextElement();
            if (headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
                HttpHost host = target; // getTarget(request);
                headerValue = host.getHostName();
                if (host.getPort() != -1) {
                    headerValue += ":" + host.getPort();
                }
            } else if (headerName.equalsIgnoreCase(org.apache.http.cookie.SM.COOKIE)) {
                LOG.warn("TODO: Cookies not supported ({})", headerValue);
                // headerValue = getCookie(headerValue);
            }
            LOG.info("Adding request header {}={}", headerName, headerValue);
            proxyRequest.addHeader(headerName, headerValue);
        }
    }

    protected void copyRequestHeaders(HttpServletRequest request, HttpRequest proxyRequest) {
        // Get an Enumeration of all of the header names sent by the client
        @SuppressWarnings("unchecked")
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            copyRequestHeader(request, proxyRequest, headerName);
        }
    }

    protected void copyResponseHeader(HttpServletRequest request, HttpServletResponse response, Header header) {
        String headerName = header.getName();
        if (HOPBYHOP_HEADERS.containsHeader(headerName)) {
            return;
        }
        String headerValue = header.getValue();
        if (headerName.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE) || headerName.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE2)) {
            LOG.warn("TODO: Cookies not supported ({})", headerValue);
            // copyProxyCookie(request, response, headerValue);
        } else if (headerName.equalsIgnoreCase(HttpHeaders.LOCATION)) {
        	// LOCATION Header may have to be rewritten.
            LOG.warn("TODO: LOCATION: does it need to be rewriten? ({})", headerValue);
        	// response.addHeader(headerName, rewriteUrl(request, headerValue));
        } else {
            LOG.info("Adding response header {}={}", headerName, headerValue);
        	response.addHeader(headerName, headerValue);
        }
    }

    protected void copyResponseHeaders(HttpResponse proxyResponse, HttpServletRequest request, HttpServletResponse response) {
        for (Header header : proxyResponse.getAllHeaders()) {
            copyResponseHeader(request, response, header);
        }
    }

    protected void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse response, HttpRequest proxyRequest, HttpServletRequest request) throws IOException {
        HttpEntity entity = proxyResponse.getEntity();
        if (entity != null) {
            entity.writeTo(response.getOutputStream());
        }
    }

    @SuppressWarnings("deprecation")
	@Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int id = EXCHANGE_COUNTER.incrementAndGet();

        String method = request.getMethod();
        String uri = rewriteProxyUrl(request);
        HttpRequest proxyRequest;

        // spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
        proxyRequest = (request.getHeader(HttpHeaders.CONTENT_LENGTH) != null || request.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) 
            ? createRequest(method, uri, request) : new BasicHttpRequest(method, uri);

        copyRequestHeaders(request, proxyRequest);
        HttpResponse proxyResponse = null;
        try {
        	// Simulate QoS issues before forwarding
            // read request
            LOG.info("Applying QoS policy {}", qos);
            qos.process(request);

            HttpURLConnection upstreamCon = (HttpURLConnection)forward.openConnection();
            upstreamCon.setRequestMethod(request.getMethod());
            for (Enumeration<?> headers = request.getHeaderNames(); headers.hasMoreElements();) {
                String headerName = (String) headers.nextElement();
                StringBuilder headerListValue = null;
                for (Enumeration<?> headerValues = request.getHeaders(headerName); headerValues.hasMoreElements();) {
                    String headerValue = (String) headerValues.nextElement();
                    if (headerListValue == null) {
                        headerListValue = new StringBuilder(headerValue);
                    } else {
                        headerListValue.append(',');
                        headerListValue.append(headerValue);
                    }
                }
                upstreamCon.setRequestProperty(headerName, headerListValue.toString());
            }

            upstreamCon.setDoOutput(true);
            byte bytes[] = new byte[4096];
            try (ServletInputStream is = request.getInputStream(); OutputStream out = upstreamCon.getOutputStream()) {
                int len = -1;
                while ((len = is.read(bytes)) != -1) {
                    out.write(bytes, 0, len);
                }
                out.flush();
            }
            int rc = upstreamCon.getResponseCode();
            response.setStatus(rc);
            for (Map.Entry<String, List<String>> v : upstreamCon.getHeaderFields().entrySet()) {
                for (String s : v.getValue()) {
                    if (v.getKey() != null && s != null) {
                        response.addHeader(v.getKey(), s);
                    }
                }
            }
            try (ServletOutputStream out = response.getOutputStream(); InputStream is = upstreamCon.getInputStream()) {
                int len = -1;
                while ((len = is.read(bytes)) != -1) {
                    out.write(bytes, 0, len);
                }
                out.flush();
            }

        } catch(Exception e) {
            if (proxyRequest instanceof AbortableHttpRequest) {
                AbortableHttpRequest abortableHttpRequest = (AbortableHttpRequest)proxyRequest;
                abortableHttpRequest.abort();
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            } else if (e instanceof ServletException) {
                throw (ServletException)e;
            } else if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            if (proxyResponse != null) {
                consumeQuietly(proxyResponse.getEntity());
            }
        }
    }

    private static long getContentLength(HttpServletRequest request) {
        String contentLengthHeader = request.getHeader("Content-Length");
        if (contentLengthHeader != null) {
            return Long.parseLong(contentLengthHeader);
        }
        return -1L;
    }

    protected static void consumeQuietly(HttpEntity entity) {
        try {
            EntityUtils.consume(entity);
        } catch (IOException e) { // log and ignore
            LOG.warn("Exception while consuming entity", e);
        }
    }

}
