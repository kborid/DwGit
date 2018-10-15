package com.smartisanos.sara.entity;

import java.util.ArrayList;
import java.util.List;
import smartisanos.app.voiceassistant.ApplicationStruct;

public class AppModel implements IModel {
    public ApplicationStruct struct;

    public static List<AppModel> createModel(List<ApplicationStruct> structs) {
        List<AppModel> models = new ArrayList<AppModel>();
        if (structs == null){
            return models;
        }
        AppModel model = null;

        for (int i = 0; i < structs.size(); i++) {
            model = new AppModel();
            model.struct = structs.get(i);
            models.add(model);
        }
        return models;
    }
}
