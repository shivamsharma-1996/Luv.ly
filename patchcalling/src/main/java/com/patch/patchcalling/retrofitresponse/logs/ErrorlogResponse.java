package com.patch.patchcalling.retrofitresponse.logs;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ErrorlogResponse {

    @SerializedName("_id")
    @Expose
    private String _id;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }
}
