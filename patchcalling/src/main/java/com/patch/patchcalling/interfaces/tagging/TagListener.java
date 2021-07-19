package com.patch.patchcalling.interfaces.tagging;

import com.patch.patchcalling.models.CampaignTagsResponse;

/**
 * Created by Shivam Sharma on 10-08-2019.
 */
public interface TagListener {
    void onTagAdded();
    void onTagRemoved();
    //void onTagsFetched(CampaignTagsResponse campaignTagsResponse);
    void onFailure(int errorCode);
}
