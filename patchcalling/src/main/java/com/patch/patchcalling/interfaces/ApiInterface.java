package com.patch.patchcalling.interfaces;

import com.patch.patchcalling.models.AdminRequest;
import com.patch.patchcalling.models.ApiUrlRequest;
import com.patch.patchcalling.models.ContactDeviceId;
import com.patch.patchcalling.models.CreateContact;
import com.patch.patchcalling.models.MerchantSigninResponse;
import com.patch.patchcalling.models.PendingSentiment;
import com.patch.patchcalling.models.VoiceCallStatus;
import com.patch.patchcalling.retrofitresponse.admin.AdminResponse;
import com.patch.patchcalling.retrofitresponse.createcontact.CreateContactResponse;
import com.patch.patchcalling.retrofitresponse.gettoken.Token;
import com.patch.patchcalling.retrofitresponse.messaging.calllogs.CallLogsResponse;
import com.patch.patchcalling.retrofitresponse.messaging.calllogs.FetchParticipantIdResponse;
import com.patch.patchcalling.retrofitresponse.messaging.conversations.FetchConversationResponse;
import com.patch.patchcalling.retrofitresponse.messaging.messages.ChatMessages;
import com.patch.patchcalling.retrofitresponse.messaging.messages.UnreadCount;
import com.patch.patchcalling.retrofitresponse.resolvesentimentresponse.ResolvedSentimentResponse;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by sanyamjain on 21/07/18.
 */

public interface ApiInterface {
    @POST("accounts/{id}/baseUrl")
    Call<Object> getBaseUrl(@Path("id") String accountId, @Body ApiUrlRequest apiUrlRequest);

    @POST("accounts/{id}/contacts/signin")
    Call<CreateContactResponse> createContact(@Path("id") String accountId, @Body CreateContact createContact/*, @Header("Authorization") String authHeader*/);

    @GET("contacts/jwt")
    Call<Token> getToken(@Query("phone") String phone, @Query("cc") String cc, @Query("accountId") String accountId, @Header("Authorization") String authHeader);

    @GET("contacts/jwt")
    Call<Token> getTokenByCuid(@Query("accountId") String accountId, @Query("cuid") String cuid, @Header("Authorization") String authHeader);

    @GET("contacts/jwt/verify")
    Call<Object> verifyToken(@Query("accountId") String accountId, @Query("cuid") String cuid, @Query("token") String token, @Header("Authorization") String authHeader);

    @POST("tags/{id}/map-contacts")
    Call<Object> addTag(@Path("id") String tagId, @Body List<String> contactIdList, @Header("Authorization") String authHeader);

    @DELETE("tags/{id}/map-contacts")
    Call<Object> removeTag(@Path("id") String tagId, @Query("contactId") String contactId, @Header("Authorization") String authHeader);

    @GET("tags")
    Call<UnreadCount> fetchAvailableCampaignTags(@Query("where") String where,@Header("Authorization") String authHeader );

    @PATCH("notifications/{id}")
    Call<ResolvedSentimentResponse> resolvePendingSentiment(@Path("id") String notificationId, @Body PendingSentiment sentiment, @Header("Authorization") String authHeader);

    @FormUrlEncoded
    @POST("merchants/signin")
    Call<MerchantSigninResponse> merchantSignin(@Field("accountId") String accoundId, @Field("cuid") String cuid, @Header("Authorization") String authHeader);

    @GET("conversations")
    Call<List<FetchConversationResponse>> fetchMerchantConversions(@Query("filter") String filter, @Header("Authorization") String authHeader);

    @GET("messages")
    Call<List<ChatMessages>> fetchChatMessages(@Query("filter") String filter, @Header("Authorization") String authHeader);

    @GET("messages/count")
    Call<UnreadCount> fetchUnreadCount(@Query("where") String where,@Header("Authorization") String authHeader );

    @POST("conversations/{id}/update-status")
    Call<Response<Void>> markMessagesSeen(@Path("id") String targetCuid, @Header("Authorization") String authHeader);

    @GET("contacts")
    Call<List<FetchParticipantIdResponse>> fetchParticipantId(@Query("filter") String filter, @Header("Authorization") String authHeader);

    @GET("voice-calls")
    Call<List<CallLogsResponse>> fetchCallLogs(@Query("filter") String filter, @Header("Authorization") String authHeader);

    @POST("Users/login")
    Call<AdminResponse> fetchAdminConifg(@Body AdminRequest adminRequest);

    @PATCH("voice-calls/{id}")
    Call<Object> resolveCallStatus(@Path("id") String callId,  @Body VoiceCallStatus voiceCallStatus, @Header("Authorization") String authHeader);

    @PATCH("contacts/{id}")
    Call<Object> updateDeviceId(@Path("id") String contactId,  @Body ContactDeviceId contactDeviceId, @Header("Authorization") String authHeader);
}