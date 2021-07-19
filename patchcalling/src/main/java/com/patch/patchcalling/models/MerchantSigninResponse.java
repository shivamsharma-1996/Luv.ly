package com.patch.patchcalling.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MerchantSigninResponse {

@SerializedName("name")
@Expose
private String name;
@SerializedName("cuid")
@Expose
private String cuid;
@SerializedName("accountId")
@Expose
private String accountId;
@SerializedName("accessToken")
@Expose
private String accessToken;

public String getName() {
return name;
}

public void setName(String name) {
this.name = name;
}

public String getCuid() {
return cuid;
}

public void setCuid(String cuid) {
this.cuid = cuid;
}

public String getAccountId() {
return accountId;
}

public void setAccountId(String accountId) {
this.accountId = accountId;
}

public String getAccessToken() {
return accessToken;
}

public void setAccessToken(String accessToken) {
this.accessToken = accessToken;
}

}