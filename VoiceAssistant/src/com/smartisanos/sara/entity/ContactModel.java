package com.smartisanos.sara.entity;

import java.util.ArrayList;
import java.util.List;
import smartisanos.app.voiceassistant.ContactStruct;

public class ContactModel implements IModel {
    public static final int TYPE_SINGLE = 0;
    public static final int TYPE_MULTI_FISRT = 1;
    public static final int TYPE_MULTI_OTHER = 2;

    public int type;
    public ContactStruct struct;

    public List<ContactStruct> structs;

    public static List<ContactModel> createModel(List<ContactStruct> structs) {
        List<ContactModel> models = new ArrayList<ContactModel>();
        if (structs == null){
            return models;
        }
        ContactModel model = null;

        for (int i = 0; i < structs.size(); i++) {
            model = new ContactModel();
            model.struct = structs.get(i);

            ContactStruct struct = structs.get(i);
            ContactStruct lastStruct = i == 0 ? null : structs.get(i - 1);
            ContactStruct nextStruct = i == structs.size() - 1 ? null : structs.get(i + 1);

            if (lastStruct != null && lastStruct.contactId == struct.contactId) {
                model.type = ContactModel.TYPE_MULTI_OTHER;
            } else if (nextStruct != null && nextStruct.contactId == struct.contactId) {
                model.type = ContactModel.TYPE_MULTI_FISRT;
            } else {
                model.type = ContactModel.TYPE_SINGLE;
            }
            model.structs = structs;

            models.add(model);
        }
        return models;
    }
}
