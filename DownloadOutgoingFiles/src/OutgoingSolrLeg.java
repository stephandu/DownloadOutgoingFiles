

import java.io.Serializable;
import java.util.Date;

public class OutgoingSolrLeg implements Serializable {

  private static final long serialVersionUID = -6567194058436817762L;
  
  private String svcCode;
  private String vslCode;
  private String vslName;
  private String voyExNum;
  private String dir;
  private String polEtd;
  private String podEta;
  
  public OutgoingSolrLeg(){
    
  }
  
  public OutgoingSolrLeg(String svcCode, String vslCode, String vslName, String voyExNum, String dir, String polEtd, String podEta) {
    this.svcCode = svcCode;
    this.vslCode = vslCode;
    this.vslName = vslName;
    this.voyExNum = voyExNum;
    this.dir = dir;
    this.polEtd = polEtd;
    this.podEta = podEta;
  }

  public String getSvcCode() {
    return svcCode;
  }

  public void setSvcCode(String svcCode) {
    this.svcCode = svcCode;
  }

  public String getVslCode() {
    return vslCode;
  }

  public void setVslCode(String vslCode) {
    this.vslCode = vslCode;
  }

  public String getVslName() {
    return vslName;
  }

  public void setVslName(String vslName) {
    this.vslName = vslName;
  }

  public String getVoyExNum() {
    return voyExNum;
  }

  public void setVoyExNum(String voyExNum) {
    this.voyExNum = voyExNum;
  }

  public String getDir() {
    return dir;
  }

  public void setDir(String dir) {
    this.dir = dir;
  }

  public String getPolEtd() {
    return polEtd;
  }

  public void setPolEtd(String polEtd) {
    this.polEtd = polEtd;
  }

  public String getPodEta() {
    return podEta;
  }

  public void setPodEta(String podEta) {
    this.podEta = podEta;
  }

}
