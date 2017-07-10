
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrUtil {
  public static final String CORE_SIMPLEOBJECT = "simpleobject";
  public static final String CORE_PLSSCITYPAIR = "plsscitypair";
  public static final String ID = "id";
  public static final String FIELD_PERSISTED_DT = "persisted_dt";
  public static final String FIELD_VERSION = "_version_";

  private Logger logger = LoggerFactory.getLogger(SolrUtil.class);
  private Properties properties = new Properties();
  private HashMap<String, SolrServer> solrServers = new HashMap<String, SolrServer>();
  private ExecutorService solrConfigDetector = Executors.newSingleThreadExecutor();
  private String solrServerURL = getProperties().getProperty("SOLR_DEFAULT_SERVER_URL");
  private String solrContexts = getProperties().getProperty("SOLR_DEFAULT_CONTEXTS");
  private String proxyHost = getProperties().getProperty("PROXY_HOST");
  private String proxyPort = getProperties().getProperty("PROXY_PORT");
  private String defaultEmailSender = getProperties().getProperty("DEFAULT_EMAIL_SENDER");

  public static final String UTF8_CHARSET_NAME = "UTF-8";
  private static SolrUtil solrUtil;
  private SolrDocument commonConfig;

  private SolrUtil() {
    init();
  }

  public static SolrUtil getInstance() {
    if (null == solrUtil) {
      solrUtil = new SolrUtil();
    }
    return solrUtil;
  }

  protected void init() {
    solrConfigDetector.execute(new Runnable() {
      public void run() {
        boolean isActive = true;
        while (isActive) {
          try {
            getLogger().info("SolrUtil.SolrConfigDetector - Running...");
            detectSolrConfig();
            getLogger().info("SolrUtil.SolrConfigDetector - Success. Sleep 1 min...");
            Thread.sleep(60000);
          } catch (Throwable t) {
            if (t instanceof InterruptedException) {
              isActive = false;
            }
            try {
              getLogger().warn("SolrUtil.SolrConfigDetector - Failed to detect solr config. Sleep 10s...", t);
              Thread.sleep(10000);
            } catch (InterruptedException e) {
              isActive = false;
            }
          }
        }
      };
    });
  }

  protected void destroy() {
    getLogger().warn("SolrUtil.SolrConfigDetector - Shuting down...");
    solrConfigDetector.shutdownNow();
  }

  private Logger getLogger() {
    return this.logger;
  }

  public Properties getProperties() {
    if (!properties.isEmpty()) {
      return properties;
    }
    try {
      InputStream resourceStream = SolrUtil.class.getClassLoader().getResourceAsStream("csAppConfig.properties");
      properties.load(resourceStream);
      getLogger().info("SolrUtil.getProperties() - Loaded csAppConfig.properties " + properties.toString());
    } catch (Exception e) {
      getLogger().error("SolrUtil.getProperties() - Failed to load csAppConfig.properties file", e);
    }
    return properties;
  }

  public static byte[] getByteArray(String filePath) throws IOException {
    FileInputStream fileInputStream = new FileInputStream(filePath);
    ByteArrayOutputStream fileByteStream = new ByteArrayOutputStream(fileInputStream.available());
    byte[] buffer = new byte[fileInputStream.available()];
    int bytes;
    while ((bytes = fileInputStream.read(buffer)) > 0) {
      fileByteStream.write(buffer, 0, bytes);
      fileByteStream.flush();
    }
    fileInputStream.close();
    return fileByteStream.toByteArray();
  }

  private synchronized void detectSolrConfig() {
    this.getCommonConfigFromSolr();
    String preSolrServerURL = solrServerURL;
    String preSolrContexts = solrContexts;
    if (null != commonConfig) {
      solrServerURL = StringUtils.defaultIfEmpty((String) commonConfig.getFieldValue("SolrServerURL_s"), solrServerURL);
      solrContexts = StringUtils.defaultIfEmpty((String) commonConfig.getFieldValue("SolrContext_s"), solrContexts);
    }
    if (!StringUtils.equals(preSolrServerURL, solrServerURL) || !StringUtils.equals(preSolrContexts, solrContexts)) {
      getLogger().info("SolrUtil.detectSolrConfig() - Changed " + preSolrServerURL + preSolrContexts + " to " + solrServerURL + solrContexts);
      this.solrServers.clear();
    }
  }

  private SolrDocument getCommonConfigFromSolr() {
    SolrQuery query = new SolrQuery("id:CommonConfig");
    QueryRequest queryRequest = new QueryRequest(query);
    try {
      QueryResponse response = queryRequest.process(getSolrServer(CORE_SIMPLEOBJECT));
      if (response != null && response.getResults() != null && response.getResults().size() > 0) {
        commonConfig = response.getResults().get(0);
      }
    } catch (Exception e) {
      getLogger().error("SolrUtil.getCommonConfig() - Failed to query from core:simpleobject [id:CommonConfig]", e);
    }
    return commonConfig;
  }

  private void setSortParams(String sortParams, SolrQuery solrQuery) {
    if (StringUtils.isNotEmpty(sortParams)) {
      String[] sortFields = sortParams.trim().split(" *, *");
      for (String sortField : sortFields) {
        String[] sortFieldAndOrder = sortField.split(" +");
        if (2 == sortFieldAndOrder.length) {
          solrQuery.addSortField(sortFieldAndOrder[0], SolrQuery.ORDER.valueOf(sortFieldAndOrder[1]));
        } else if (1 == sortFieldAndOrder.length) {
          solrQuery.addSortField(sortFieldAndOrder[0], SolrQuery.ORDER.asc);
        }
      }
    }
  }

  public SolrDocument getCommonConfig() {
    if (null == commonConfig) {
      getCommonConfigFromSolr();
    }
    return commonConfig;
  }

  public Object getCommonConfigObject(String key) {
    return getCommonConfig().getFieldValue(key);
  }

  public String getCommonConfig(String key) {
    return (String) getCommonConfig().getFieldValue(key);
  }

  public boolean getCommonConfigBoolean(String key) {
    Object doc = getCommonConfig().getFieldValue(key);
    return (null == doc ? false : (Boolean) doc);
  }

  public Integer getCommonConfigInteger(String key, Integer defaultValue) {
    Integer configValue = null;
    Object doc = getCommonConfig().getFieldValue(key);
    if (doc != null && doc instanceof Integer) {
      configValue = (Integer) doc;
    } else {
      configValue = defaultValue;
    }
    return configValue;
  }

  @SuppressWarnings("unchecked")
  public List<String> getCommonConfigStringList(String key) {
    return (ArrayList<String>) getCommonConfig().getFieldValue(key);
  }

  public SolrServer getSolrServer(String core) {
    if (solrServers.containsKey(core)) {
      return solrServers.get(core);
    }
    for (String solrContext : solrContexts.split(",")) {
      String solrCoreURL = solrServerURL + solrContext + "/" + core;
      try {
        getLogger().info("SolrUtil.getSolrServer() - Ping " + solrCoreURL);
        SolrServer solrServer = new HttpSolrServer(solrCoreURL);
        solrServer.ping();
        solrServers.put(core, solrServer);
        getLogger().warn("SolrUtil.getSolrServer() - Connected to:" + solrCoreURL);
        return solrServer;
      } catch (Throwable e) {
        getLogger().warn("SolrUtil.getSolrServer() - Failed to connect:" + solrCoreURL, e);
      }
    }
    return null;
  }

  public List<SolrDocument> getDocs(String criteria, String coreName, int rows, int start, String sortParams) {
    List<SolrDocument> docs = new ArrayList<SolrDocument>();
    if (StringUtils.isNotEmpty(criteria) && StringUtils.isNotEmpty(coreName)) {
      SolrServer solrServer = getSolrServer(coreName);
      SolrQuery solrQuery = new SolrQuery().setQuery(criteria).setRows(rows).setStart(start);
      this.setSortParams(sortParams, solrQuery);
      QueryRequest queryRequest = new QueryRequest(solrQuery);
      queryRequest.setMethod(METHOD.GET);
      try {
        QueryResponse response = queryRequest.process(solrServer);
        if (response.getResults() != null) {
          docs.addAll(response.getResults());
        }
      } catch (SolrServerException e) {
        e.printStackTrace();
      }
    }
    return docs;
  }

  public SolrDocument getDocByID(String id, String coreName) {
    if (StringUtils.isNotEmpty(id) && StringUtils.isNotEmpty(coreName)) {
      SolrServer solrServer = getSolrServer(coreName);
      SolrQuery solrQuery = new SolrQuery().setParam("id", id).setRows(1);
      QueryRequest queryRequest = new QueryRequest(solrQuery);
      queryRequest.setMethod(METHOD.GET);
      queryRequest.setPath("/get");
      try {
        QueryResponse response = queryRequest.process(solrServer);
        if (response.getResults() != null && response.getResults().size() > 0) {
          return response.getResults().get(0);
        } else if (response.getResponse() != null && response.getResponse().size() > 0) {
          return (SolrDocument) response.getResponse().getVal(0);
        }
      } catch (SolrServerException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
    return null;
  }
  
  public SolrDocument getDoc(String criteria, String coreName) {
    if (StringUtils.isNotEmpty(criteria) && StringUtils.isNotEmpty(coreName)) {
      SolrServer solrServer = getSolrServer(coreName);
      SolrQuery solrQuery = new SolrQuery().setQuery(criteria).setRows(1);
      QueryRequest queryRequest = new QueryRequest(solrQuery);
      queryRequest.setMethod(METHOD.GET);
      try {
        QueryResponse response = queryRequest.process(solrServer);
        if (response.getResults() != null && response.getResults().size() > 0) {
          return response.getResults().get(0);
        }
      } catch (SolrServerException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  public void addDocAutoCommit(SolrInputDocument document, String coreName) {
    if (document != null && StringUtils.isNotEmpty(coreName)) {
      SolrServer solrServer = getSolrServer(coreName);
      try {
        solrServer.add(document);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void addDoc(SolrInputDocument document, String coreName) throws SolrServerException, IOException {
    if (document != null && StringUtils.isNotEmpty(coreName)) {
      SolrServer solrServer = getSolrServer(coreName);
      solrServer.add(document);
      solrServer.commit();
    }
  }

  public void addDocs(List<SolrInputDocument> documents, String coreName) {
    if (documents != null && !documents.isEmpty() && StringUtils.isNotEmpty(coreName)) {
      SolrServer solrServer = getSolrServer(coreName);
      try {
        solrServer.add(documents);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public SolrInputDocument updateSolrFieldsWithId(SolrInputDocument inputDoc, Object id, List<String> fields, Object[] values) {
    inputDoc = this.updateSolrFields(inputDoc, fields, values);
    inputDoc.setField(ID, id);
    return inputDoc;
  }

  public SolrInputDocument updateSolrFields(SolrInputDocument inputDoc, List<String> fields, Object[] values) {
    if (fields != null && values != null && fields.size() <= values.length) {
      for (int i = 0; i < fields.size(); i++) {
        Map<String, Object> updater = new HashMap<String, Object>();
        updater.put("set", values[i]);
        inputDoc.setField(fields.get(i), updater);
      }
    }

    return inputDoc;
  }

  public SolrInputDocument updateSolrFields(SolrInputDocument inputDoc, List<String> fields, SolrDocument values) {
    if (fields != null && values != null) {
      for (int i = 0; i < fields.size(); i++) {
        Map<String, Object> updater = new HashMap<String, Object>();
        updater.put("set", values.getFieldValue(fields.get(i)));
        inputDoc.setField(fields.get(i), updater);
      }
    }

    return inputDoc;
  }

  public void deleteByQuery(String coreName, String query) {
//    if (StringUtil.isNullOrEmpty(query)) {
//      getLogger().warn(this.getClass().getName() + ".deleteByQuery()", "core [" + coreName + "] Empty delete query string is not allowed", null);
//      return;
//    }
    try {
      SolrServer solrServer = getSolrServer(coreName);
      solrServer.deleteByQuery(query);
      solrServer.commit();
    } catch (Throwable e) {
      getLogger().error(this.getClass().getName() + ".deleteByQuery()", "Failed to deleteByQuery from core:" + coreName + " [" + query + "]", e);
    }
  }

  public void sendEmail(String fromAddress, String[] toAddress, String subject, String configName, String content, int triggerParty) throws Exception {
    this.sendEmail(fromAddress, toAddress, new String[] {}, new String[] {}, subject, configName, content, triggerParty, null);
  }

  public void sendEmail(String fromAddress, String[] toAddress, String[] ccAddress, String[] bccAddress, String subject, String configName, String content, int triggerParty, byte[] attachment) throws Exception {
    try {
      getLogger().debug("SendEmail - toAddress: " + toAddress + " || ccAddress: " + ccAddress + " || bccAddress: " + bccAddress);
      String mailServerHost = this.properties.getProperty("SMTP_HOST");
      String mailServerPort = this.properties.getProperty("SMTP_PORT");
      Properties p = new Properties();
      p.put("mail.smtp.host", mailServerHost);
      p.put("mail.smtp.port", mailServerPort);
      // p.put("mail.debug", "true");
      Session mailSession = Session.getInstance(p);
      Message mailMessage = new MimeMessage(mailSession);
      Address from = new InternetAddress(fromAddress);
      mailMessage.setFrom(from);
      for (String mailTo : toAddress) {
        mailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(mailTo));
      }
      for (String ccMail : ccAddress) {
        mailMessage.addRecipient(Message.RecipientType.CC, new InternetAddress(ccMail));
      }
      for (String bccMail : bccAddress) {
        mailMessage.addRecipient(Message.RecipientType.BCC, new InternetAddress(bccMail));
      }
      if (subject != null && !subject.equalsIgnoreCase("")) {
        mailMessage.setSubject(subject);
      } else {
        if (triggerParty == 1) {
          mailMessage.setSubject("The Seer EyE " + configName + " have exceed the monitor threshold!!!");
        } else if (triggerParty == 2) {
          mailMessage.setSubject("[Solr Backend Scheduler] " + configName + " have exceed the monitor threshold!!!");
        } else {
          mailMessage.setSubject(configName + " have exceed the monitor threshold!!!");
        }
      }
      mailMessage.setSentDate(new Date());
      Multipart mainPart = new MimeMultipart();
      MimeBodyPart mimeContent = new MimeBodyPart();
      mimeContent.setContent(content, "text/html; charset=utf-8");
      mainPart.addBodyPart(mimeContent);
      if (null != attachment) {
        MimeBodyPart mimeAttachment = new MimeBodyPart();
        ByteArrayDataSource ds = new ByteArrayDataSource(attachment, "application/octet-stream");
        mimeAttachment.setFileName(MimeUtility.encodeText("result.xls", "utf-8", "B"));
        mimeAttachment.setDataHandler(new DataHandler(ds));
        mainPart.addBodyPart(mimeAttachment);
      }
      mailMessage.setContent(mainPart);
      Transport.send(mailMessage);
    } catch (Exception e) {
      this.getLogger().error("Failed to send alert email.", e);
      throw e;
    }
  }

  public static byte[] zip(String content, String name) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = null;
    DataOutputStream dataOutputStream = null;
    try {
      zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
      zipOutputStream.putNextEntry(new ZipEntry(name));
      dataOutputStream = new DataOutputStream(zipOutputStream);
      dataOutputStream.write(content.getBytes(UTF8_CHARSET_NAME));
      dataOutputStream.flush();
      zipOutputStream.flush();
      zipOutputStream.finish();
    } finally {
      dataOutputStream.close();
      zipOutputStream.close();
    }
    return byteArrayOutputStream.toByteArray();
  }

  public static String unZip(byte[] value) {
    if (null == value) {
      return null;
    }
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value);
    ZipInputStream zipInputStream = null;
    String sourceText = null;
    try {
      zipInputStream = new ZipInputStream(byteArrayInputStream);
      zipInputStream.getNextEntry();
      final int bufferSize = 4096;
      byte[] buffer = new byte[bufferSize];
      int n;
      while ((n = zipInputStream.read(buffer, 0, bufferSize)) != -1) {
        byteArrayOutputStream.write(buffer, 0, n);
      }
      sourceText = byteArrayOutputStream.toString("UTF-8");
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      try {
        if (null != zipInputStream) {
          zipInputStream.close();
          zipInputStream = null;
        }
        if (null != byteArrayInputStream) {
          byteArrayInputStream.close();
          byteArrayInputStream = null;
        }
        if (null != byteArrayOutputStream) {
          byteArrayOutputStream.close();
          byteArrayOutputStream = null;
        }
      } catch (Exception e) {
      }
    }

    return sourceText;
  }

  public String json(Object obj) {
    String json = null;
    try {
      json = JSONObject.fromObject(obj).toString();
    } catch (Throwable e) {
      this.getLogger().warn("Convert HashMap to Json meet exception:", e);
      e.printStackTrace();
    }
    return json;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> jsonMap(Object obj) {
    try {
      return (Map<String, Object>) JSONObject.fromObject(obj);
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return null;
  }

  public void copyFields(SolrInputDocument dest, SolrDocument src, List<String> fields) {
    for (String filed : fields) {
      dest.addField(filed, src.getFieldValue(filed));
    }
  }

  public Proxy getProxy() {
    Proxy p = null;
    try {
      if (proxyHost != null && proxyPort != null && proxyHost.trim().length() > 0 && proxyPort.trim().length() > 0) {
        SocketAddress addr = new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort));
        p = new Proxy(Proxy.Type.HTTP, addr);
      }
    } catch (Throwable t) {
      this.getLogger().warn("Failed to generate proxy. ProxyHost:" + proxyHost + ",ProxyPort:" + proxyPort, t);
      p = null;
    }
    return p;
  }

  public static String getContent(InputStream is) throws IOException {
    try {
      byte[] b = new byte[512];
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      int realRead;
      realRead = is.read(b);
      while (realRead != -1) {
        baos.write(b, 0, realRead);
        realRead = is.read(b);
      }
      return baos.toString("UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    }
  }

  public static String htmlTable(SolrDocument solrDocument) {
    try {
      if (solrDocument != null) {
        StringBuffer html = new StringBuffer();
        html.append("<table cellspacing='2' cellpadding='3' width='100%' class='highlightTable' border='1'>");
        for (String fieldName : solrDocument.getFieldNames()) {
          Object fieldObject = solrDocument.getFieldValue(fieldName);
          if (fieldName.endsWith("_bin")) {
            fieldObject = SolrUtil.unZip((byte[]) fieldObject).replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            html.append("<tr><td valign='top' class='indicateLabel'>" + fieldName + ":</td><td valign='top'><textarea style='width: 1000;height: 600'>" + fieldObject + "</textarea></td></tr>");
          } else {
            String fieldValue = fieldObject.toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            if("id".equals(fieldName)){
              fieldValue = "<a href=\"http://solrprod.cargosmart.org/seer/viewer.htm?solrCore=exception&solrDocID="+fieldValue+"\">"+fieldValue+"</a>";
            } else if (StringUtils.startsWithIgnoreCase(fieldValue, "http://") || StringUtils.startsWithIgnoreCase(fieldValue, "https://")) {
              fieldValue = "<a target='_blank' href=\"" + fieldValue + "\">link</a>";
            } else {
              fieldValue = "<pre class='prettyprint'>" + fieldValue + "</pre>";
            }
            html.append("<tr><td valign='top' class='indicateLabel' width=30%>" + fieldName + ":</td><td valign='top'>" + fieldValue + "</td></tr>");
          }
        }
        html.append("</table>");
        return html.toString();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  public static String htmlTable(String content) {
    String result = "<pre class='prettyprint'>";
    try {
      JSONObject json = JSONObject.fromObject(content);
      if (content.contains("facet_pivot")) {
        result = result + getPivot(json.getJSONObject("facet_counts").getJSONObject("facet_pivot").getJSONArray(json.getJSONObject("responseHeader").getJSONObject("params").getString("facet.pivot")), true);
      } else if (content.contains("facet_fields")) {
        result = result + json.getJSONObject("facet_counts").getJSONObject("facet_fields").getJSONArray(json.getJSONObject("responseHeader").getJSONObject("params").getString("facet.field"));
      } else {
        result = result + content;
      }
    } catch (Throwable e) {
      result = result + getStackTrace(e);
      e.printStackTrace();
    }
    return result + "</pre>";
  }

  public static String getPivot(JSONArray pivot, boolean root) {
    StringBuffer sb = new StringBuffer();
    for (Object obj : pivot) {
      JSONObject json = ((JSONObject) obj);
      sb.append((root ? "\n" : "\t"));
      sb.append(json.getString("value"));
      sb.append("\t");
      sb.append(json.getInt("count"));
      if (json.containsKey("pivot")) {
        sb.append(getPivot(json.getJSONArray("pivot"), false));
      }
    }
    return sb.toString();
  }

  public static void main(String[] args) throws ParseException {
    // DateFormat df = new SimpleDateFormat("yyyyMMdd000000.000");
    // Calendar startDate = Calendar.getInstance();
    // startDate.add(Calendar.DAY_OF_MONTH, -4);
    // String startDateString = df.format(startDate.getTime());
    // System.out.println(startDateString);
    // System.out.println(df.parseObject("20140417133801.000"));
    try {
//      SolrServer solrServer = new HttpSolrServer("http://solrprod.cargosmart.org/solrssmp2p/ssmroutecitypair",createHttpClient());
//      SolrQuery solrQuery = new SolrQuery().setParam("id", "27be0917-cdd3-471b-b3aa-23c4230b7b78").setRows(1);
//      QueryRequest queryRequest = new QueryRequest(solrQuery);
//      queryRequest.setMethod(METHOD.GET);
//      queryRequest.setPath("/get");
//      try {        
//        QueryResponse response = queryRequest.process(solrServer);
//        if (response.getResults() != null && response.getResults().size() > 0) {
//          System.out.println(response.getResults().get(0));
//        } else if(response.getResponse()!=null && response.getResponse().size()>0){
//          SolrDocument doc = (SolrDocument)response.getResponse().getVal(0);
//        }
//      } catch (SolrServerException e) {
//        e.printStackTrace();
//        throw new RuntimeException(e);
//      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

//private static HttpClient createHttpClient() {
//  ModifiableSolrParams params = new ModifiableSolrParams();
//  params.set("maxConnections", 128);
//  params.set("maxConnectionsPerHost", 32);
//  params.set("followRedirects", true);
//  params.set("httpBasicAuthUser", "csccsvn");
//  params.set("httpBasicAuthPassword", "csSI2011");
//  HttpClient httpClient = HttpClientUtil.createClient(params);
//  return httpClient;
//}

  public static String getStackTrace(Throwable exception) {
    if (null == exception) {
      return "";
    }
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    exception.printStackTrace(pw);
    return sw.toString();
  }

  public static Map<String, String> getParameters(String url) {
    HashMap<String, String> paramMap = new HashMap<String, String>();
    try {
      if (StringUtils.isNotEmpty(url)) {
        url = URLDecoder.decode(url, "UTF-8");
        String[] params = StringUtils.substringAfter(url, "?").split("&");
        for (String p : params) {
          paramMap.put(StringUtils.trim(StringUtils.substringBefore(p, "=")), StringUtils.trim(StringUtils.substringAfter(p, "=")));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return paramMap;
  }

  public String getDefaultEmailSender() {
    return defaultEmailSender;
  }

  public void setDefaultEmailSender(String defaultEmailSender) {
    this.defaultEmailSender = defaultEmailSender;
  }
  
  
}
