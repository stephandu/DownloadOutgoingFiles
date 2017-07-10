

import java.io.Serializable;

public class TestObject2  implements Serializable{
  /**
   * 
   */
  private static final long serialVersionUID = -7884640389900266720L;
  private String id;
  private String messageId_s;
  private String messagePartner_s;
  private String messageContent_bin;
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getMessageId_s() {
    return messageId_s;
  }
  public void setMessageId_s(String messageId_s) {
    this.messageId_s = messageId_s;
  }
  public String getMessagePartner_s() {
    return messagePartner_s;
  }
  public void setMessagePartner_s(String messagePartner_s) {
    this.messagePartner_s = messagePartner_s;
  }
  public String getMessageContent_bin() {
    return messageContent_bin;
  }
  public void setMessageContent_bin(String messageContent_bin) {
    this.messageContent_bin = messageContent_bin;
  }

}
