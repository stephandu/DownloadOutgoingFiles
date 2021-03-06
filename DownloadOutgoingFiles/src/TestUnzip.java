



import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;



public class TestUnzip {

  public static void main(String[] args) {
    String result = "";
    
    String test = "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";

    String zip = "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";
    
    result = SolrQueryBuilder.like("field", "aa");
    
    result = SolrQueryBuilder.escapeQueryChars(SolrQueryBuilder.equals("field", test));   
    
    
    Object fieldObject = Base64.decodeBase64(zip.getBytes());
    
    result = SolrUtil.unZip((byte[]) fieldObject);
    
    System.out.print(result);

    String bb = "";

  }
  
  public static String unzip(String zip) {
    Object fieldObject = Base64.decodeBase64(zip.getBytes());

    return SolrUtil.unZip((byte[]) fieldObject);
  }

}
