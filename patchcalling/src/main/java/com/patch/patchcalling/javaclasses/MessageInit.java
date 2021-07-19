package com.patch.patchcalling.javaclasses;

/**
 * Created by Shivam Sharma on 07-08-2019.
 */
public class MessageInit {

    private static MessageInit instance = null;
    private String cuid;
    private String receiverCuid;
    private Boolean merchant;
    //private Boolean enableCall;
    private String conversationId;

    private MessageInit(){

    }

    public static MessageInit getInstance() {
        if (instance == null) {
            instance = new MessageInit();
        }
        return instance;
    }

    public static void setInstance(MessageInit instance) {
        MessageInit.instance = instance;
    }

    public String getCuid() {
        return cuid;
    }

    public void setCuid(String cuid) {
        this.cuid = cuid;
    }

    public String getReceiverCuid() {
        return receiverCuid;
    }

    public void setReceiverCuid(String receiverCuid) {
        this.receiverCuid = receiverCuid;
    }

    public Boolean getMerchant() {
        return merchant;
    }

    public void setMerchant(Boolean merchant) {
        this.merchant = merchant;
    }

   /* public Boolean isEnableCall() {
        return enableCall;
    }

    public void setEnableCall(Boolean enableCall) {
        this.enableCall = enableCall;
    }*/

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
