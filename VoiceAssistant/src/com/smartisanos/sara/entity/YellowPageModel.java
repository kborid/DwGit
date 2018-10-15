package com.smartisanos.sara.entity;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import smartisanos.app.numberassistant.YellowPageResult;

public class YellowPageModel implements IModel {

    public YellowPageResult yellowPageResult;

    public static List<YellowPageModel> createModel(List<YellowPageResult> structs) {
        List<YellowPageModel> models = new ArrayList<YellowPageModel>();
        if (structs == null){
            return models;
        }
        YellowPageModel model = null;

        for (int i = 0; i < structs.size(); i++) {
            model = new YellowPageModel();
            model.yellowPageResult = structs.get(i);
            if (TextUtils.isEmpty(model.yellowPageResult.number)) {
                // Do not display empty numebr yellow page.
                continue;
            }
            models.add(model);
        }
        return models;
    }
}
