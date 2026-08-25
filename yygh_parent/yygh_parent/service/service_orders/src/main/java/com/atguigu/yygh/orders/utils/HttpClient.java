package com.atguigu.yygh.orders.utils;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * http请求客户端
 *
 * @author qy
 *
 */
public class HttpClient {

    private String url;
    private Map<String, String> param;
    private int statusCode;
    private String content;
    private String xmlParam;
    private boolean isHttps;
    private boolean isCert = false;
    //证书密码 微信商户号（mch_id）
    private String certPassword;

    public boolean isHttps() {
        return isHttps;
    }

    public void setHttps(boolean isHttps) {
        this.isHttps = isHttps;
    }

    public boolean isCert() {
        return isCert;
    }

    public void setCert(boolean cert) {
        isCert = cert;
    }

    public String getXmlParam() {
        return xmlParam;
    }

    public void setXmlParam(String xmlParam) {
        this.xmlParam = xmlParam;
    }

    public HttpClient(String url, Map<String, String> param) {
        this.url = url;
        this.param = param;
    }

    public HttpClient(String url) {
        this.url = url;
    }

    public String getCertPassword() {
        return certPassword;
    }

    public void setCertPassword(String certPassword) {
        this.certPassword = certPassword;
    }

    public void setParameter(Map<String, String> map) {
        param = map;
    }

    public void addParameter(String key, String value) {
        if (param == null)
            param = new HashMap<String, String>();
        param.put(key, value);
    }

    public void post() throws ClientProtocolException, IOException {
        HttpPost http = new HttpPost(url);
        setEntity(http);
        execute(http);
    }

    public void put() throws ClientProtocolException, IOException {
        HttpPut http = new HttpPut(url);
        setEntity(http);
        execute(http);
    }

    public void get() throws ClientProtocolException, IOException {
        if (param != null) {
            StringBuilder url = new StringBuilder(this.url);
            boolean isFirst = true;
            for (String key : param.keySet()) {
                if (isFirst)
                    url.append("?");
                else
                    url.append("&");
                url.append(key).append("=").append(param.get(key));
            }
            this.url = url.toString();
        }
        HttpGet http = new HttpGet(url);
        execute(http);
    }

    /**
     * set http post,put param
     */
    private void setEntity(HttpEntityEnclosingRequestBase http) {
        if (param != null) {
            List<NameValuePair> nvps = new LinkedList<NameValuePair>();
            for (String key : param.keySet())
                nvps.add(new BasicNameValuePair(key, param.get(key))); // 参数
            http.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8)); // 设置参数
        }
        if (xmlParam != null) {
            http.setEntity(new StringEntity(xmlParam, Consts.UTF_8));
        }
    }
    private void execute(HttpUriRequest http) throws ClientProtocolException,
            IOException {
        try {
            CloseableHttpClient httpClient;
            if (isHttps && isCert) {
                // 功能完善：退款接口加载商户证书，并使用受支持的 TLS 和主机名校验。
                try (FileInputStream inputStream = new FileInputStream(new File(ConstantPropertiesUtils.CERT))) {
                    KeyStore keystore = KeyStore.getInstance("PKCS12");
                    char[] partnerId2charArray = certPassword.toCharArray();
                    keystore.load(inputStream, partnerId2charArray);
                    SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(keystore, partnerId2charArray).build();
                    SSLConnectionSocketFactory sslsf =
                            new SSLConnectionSocketFactory(sslContext,
                                    new String[] { "TLSv1.2" },
                                    null,
                                    new DefaultHostnameVerifier());
                    httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
                }
            } else {
                // 功能完善：普通 HTTPS 使用系统信任库，禁止“信任所有证书”。
                httpClient = HttpClients.createDefault();
            }
            try (CloseableHttpClient client = httpClient;
                 CloseableHttpResponse response = client.execute(http)) {
                if (response != null) {
                    if (response.getStatusLine() != null)
                        statusCode = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();
                    // 响应内容
                    content = EntityUtils.toString(entity, Consts.UTF_8);
                }
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("HTTP request failed", e);
        }
    }
    public int getStatusCode() {
        return statusCode;
    }
    public String getContent() throws ParseException, IOException {
        return content;
    }
}

