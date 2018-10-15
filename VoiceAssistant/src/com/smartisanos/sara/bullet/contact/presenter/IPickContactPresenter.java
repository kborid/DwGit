package com.smartisanos.sara.bullet.contact.presenter;

import com.smartisanos.sara.bullet.contact.model.VoiceSearchResult;
import com.smartisanos.sara.bullet.contact.view.IPickContactView;

import java.util.List;

public interface IPickContactPresenter {
    void attachView(IPickContactView view);
    void detachView();
    void setShowLabel(boolean showLabel);
    void setSourceType(int sourceType);
    void setSearchType(int searchType);
    void setAbortPreTask(boolean abortPreTask);
    void setQuery(String keyword,int currentPage,int pageSize);
    void setQuery(VoiceSearchResult result, int currentPage, int pageSize);
    void preLoad();
}
