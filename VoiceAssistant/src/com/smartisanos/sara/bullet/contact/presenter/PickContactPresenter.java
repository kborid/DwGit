package com.smartisanos.sara.bullet.contact.presenter;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import com.smartisanos.sara.R;
import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.bullet.contact.BulletContactManager;
import com.smartisanos.sara.bullet.contact.adapter.PickContactAdapter;
import com.smartisanos.sara.bullet.contact.compare.ContactNameComparator;
import com.smartisanos.sara.bullet.contact.model.AbsContactItem;
import com.smartisanos.sara.bullet.contact.model.ContactItem;
import com.smartisanos.sara.bullet.contact.model.LabelItem;
import com.smartisanos.sara.bullet.contact.model.VoiceSearchResult;
import com.smartisanos.sara.bullet.contact.view.IPickContactView;
import com.smartisanos.sara.bullet.contact.view.PickContactView;
import com.smartisanos.sara.bullet.util.PinYinUtils;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import smartisanos.app.voiceassistant.ContactStruct;


public class PickContactPresenter implements IPickContactPresenter {

    private static final String TAG = "VoiceAss.PickContactPresenter";
    private static final String ALL_CONTACTS_GROUP = "all_contacts_group";

    private IPickContactView iPickContactView;
    private int sourceType;
    private int searchType;
    private boolean abortPreTask;
    private boolean showAllContactLabel;
    private boolean showStarLabel;
    private boolean showRecentLabel;
    private boolean hasAddstar;
    private int isSupportPageVersion = -1;//是否支持分页
    private PreLoadContactsTask mPreTask;

    @Override
    public void attachView(IPickContactView view) {
        LogUtils.d("attachView() view = " + view);
        iPickContactView = view;
    }

    @Override
    public void detachView() {
        iPickContactView = null;
        cancelTask();
        if (null != mPreTask) {
            mPreTask.cancel(false);
            mPreTask = null;
        }
        BulletContactManager.instance.destroy();
    }

    public void preLoad() {
        if (!BulletContactManager.instance.hasAllContacts()) {
            LogUtils.d(TAG, "preLoad contact, no cache");
            if (null == mPreTask || mPreTask.getStatus() != AsyncTask.Status.RUNNING) {
                mPreTask = new PreLoadContactsTask();
                mPreTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    /**
     * @param abortPreTask
     * 是否终止：例如搜索的时候，第一个搜索词还未搜索完成
     *，第二个搜索词已生成，那么取消之前的搜索任务
     */
    @Override
    public void setAbortPreTask(boolean abortPreTask) {
        this.abortPreTask = abortPreTask;
    }

    @Override
    public void setSourceType(int sourceType) {
        this.sourceType = sourceType;
    }

    @Override
    public void setSearchType(int searchType) {
        this.searchType = searchType;
    }

    @Override
    public void setShowLabel(boolean showLabel) {
        this.showAllContactLabel = showLabel;
        this.showStarLabel = showLabel;
        this.showRecentLabel = showLabel;
    }

    private static Map<String, Integer> getAllContactsIndex(List<AbsContactItem> datas) {
        Map<String, Integer> indexs = new HashMap<>();
        if(datas == null || datas.isEmpty()) {
            return indexs;
        }
        boolean getIndex = false;
        int size = datas.size();
        AbsContactItem item;
        String groupId;
        for(int i = 0; i < size; i++) {
            item = datas.get(i);
            groupId = item.belongsGroup();
            if(getIndex) {
                if(indexs.get(groupId) == null) {
                    indexs.put(groupId, i);
                }
            } else {
                getIndex = ALL_CONTACTS_GROUP.equals(groupId);
            }
        }
        return indexs;
    }

    private static AbsContactItem recentContactLabel() {
        String labelStr = SaraApplication.getInstance().getString(R.string.bullet_contact_recent);
        return new LabelItem(labelStr, R.drawable.select_contact_nearest_icon);
    }

    private static AbsContactItem allContactLabel() {
        String labelStr = SaraApplication.getInstance().getString(R.string.bullet_contact_all);
        LabelItem item = new LabelItem(labelStr, R.drawable.select_contact_all_icon);
        item.setGroupId(ALL_CONTACTS_GROUP);
        return item;
    }

    private static AbsContactItem starContactLabel() {
        String labelStr = SaraApplication.getInstance().getString(R.string.bullet_contact_favorite);
        return new LabelItem(labelStr, R.drawable.select_contact_favorites_icon);
    }

    private static AbsContactItem searchContactLabel() {
        String labelStr = SaraApplication.getInstance().getString(R.string.bullet_contact_search);
        return new LabelItem(labelStr, R.drawable.select_contact_search_icon);
    }

    // for fixed navigation bar overlay, can't click
    private static AbsContactItem emptySpace() {
        return new LabelItem("");
    }


    private final List<LoadDataTask> tasks = new ArrayList<>();

    /**
     * 启动搜索任务
     *
     * @param result 要搜索的信息，填null表示查询所有数据
     */
    @Override
    public void setQuery(VoiceSearchResult result, int currentPage, int pageSize) {
        LogUtils.d("setQuery() : result = " + result + "; currentPage = " + currentPage + ";pageSize = " + pageSize);
        Object[] param = new Object[]{result, currentPage, pageSize};

        startTask(param);
    }

    /**
     * 启动搜索任务
     *
     * @param keyword 要搜索的信息，填null表示查询所有数据
     */
    @Override
    public void setQuery(String keyword, int currentPage, int pageSize) {
        LogUtils.d("setQuery() : keyword = " + keyword + "; currentPage = " + currentPage + ";pageSize = " + pageSize);
        Object[] param = new Object[]{keyword, currentPage, pageSize};

        startTask(param);
    }

    private void startTask(Object[] param) {
        if (abortPreTask) {
            cancelTask();
        }

        LoadDataTask loadDataTask = new LoadDataTask(iPickContactView);
        loadDataTask.setSourceType(sourceType).setSearchType(searchType);
        loadDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, param);
        tasks.add(loadDataTask);
    }

    private void cancelTask() {
        for (LoadDataTask task : tasks) {
            task.cancel(false); // 设为true有风险！
        }
    }

    private void onTaskFinish(LoadDataTask task) {
        tasks.remove(task);
    }

    private boolean isSupportPageByVersion() {
        try {
            //包管理操作管理类
            PackageManager pm = SaraApplication.getInstance().getPackageManager();
            PackageInfo packinfo = pm.getPackageInfo(SaraConstant.PACKAGE_NAME_BULLET, 0);
            if (packinfo != null) {
                String versionName = packinfo.versionName;
                //v0.8.6 第一次使用这个
                if (!TextUtils.isEmpty(versionName) && versionName.compareTo("0.8.6") >= 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private class PreLoadContactsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            LogUtils.d(TAG, "preLoad contact, begin");
            loadAllContacts();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            LogUtils.d(TAG, "preLoad contact, end");
        }
    }

    private class LoadDataTask extends AsyncTask<Object, Void, List<AbsContactItem>> {

        private WeakReference<IPickContactView> pickContactViewWR;
        private int sourceType = PickContactView.SOURCE_TYPE_FRIEND;
        private int searchType = PickContactView.SEARCH_TYPE_VOICE;
        private boolean hasMore = false;

        private LoadDataTask(IPickContactView iPickContactView) {
            pickContactViewWR = new WeakReference<>(iPickContactView);
        }


        private LoadDataTask setSourceType(int sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        private LoadDataTask setSearchType(int searchType) {
            this.searchType = searchType;
            return this;
        }

        @Override
        protected List<AbsContactItem> doInBackground(Object... params) {

            Integer currentPage = (Integer) params[1];
            Integer pageSize = (Integer) params[2];

            List<AbsContactItem> temps = new ArrayList<>();
            switch (sourceType) {
                case PickContactView.SOURCE_TYPE_RECENT: {
                    List<ContactItem> recent = BulletContactManager.loadRecentContacts(null);
                    if (null != recent) {
                        temps.addAll(0, recent);
                    }
                    if (showRecentLabel) {
                        temps.add(0, recentContactLabel());
                    }
                    temps.add(emptySpace());
                }
                break;
                case PickContactView.SOURCE_TYPE_STAR_FRIEND: {
                    temps = queryAllAndStatContactByPage(currentPage, pageSize, temps);
                }
                break;
                case PickContactView.SOURCE_TYPE_SEARCH_FRIEND: {
                    temps = searchFriend(params[0], searchType);
                    if (null != temps) {
                        if (PickContactView.SEARCH_TYPE_TEXT == searchType && showStarLabel) {
                            temps.add(0, searchContactLabel());
                            temps.add(emptySpace());
                        }
                    }
                    IPickContactView iPickContactView = pickContactViewWR.get();
                    if (null != iPickContactView) {
                        ((PickContactAdapter)iPickContactView.getAdapter()).setSearchType(searchType);
                    }
                }
                break;
            }
            return temps;
        }

        private List<AbsContactItem> queryAllAndStatContactByPage(Integer currentPage, Integer pageSize, List<AbsContactItem> temps) {

            Bundle bundle = BulletContactManager.buildPageBundle(currentPage, pageSize);

            List<ContactItem> all = BulletContactManager.loadAllContacts(bundle);

            if (null != all && all.size() > 0) {
                if (!isSupportPage()) {
                    Collections.sort(all, new ContactNameComparator());
                }
                temps.addAll(0, all);
            }
            if (showAllContactLabel) {
                temps.add(0, allContactLabel());
                showAllContactLabel = false;
            }
            if (!hasAddstar) {
                List<ContactItem> star = BulletContactManager.loadStarContacts(null);
                if (null != star && star.size() > 0) {
                    if (!isSupportPage()) {
                        Collections.sort(star, new ContactNameComparator());
                    }
                    temps.addAll(0, star);
                    hasAddstar = true;
                    if (showStarLabel) {
                        temps.add(0, starContactLabel());
                        showStarLabel = false;
                    }
                }
            }
            temps.add(emptySpace());
            return temps;
        }

        private List<AbsContactItem> searchFriend(Object keywordResult, int searchType) {
            LogUtils.d(TAG, "searchFriend() searchType = " + searchType);
            List<AbsContactItem> searchResults = new ArrayList<>();
            if (isNotEmptyData(keywordResult)) {
                List<ContactItem> allContacts = loadAllContacts();

                if (isCancelled()) {
                    return null;
                }

                List<ContactItem> temp = matchContacts(keywordResult, searchType, allContacts);
                LogUtils.d(TAG, "searchFriend() sort searchResult");
                Collections.sort(temp, new ContactNameComparator());
                searchResults.addAll(temp);
            }
            LogUtils.d(TAG, "searchFriend() search size = " + searchResults.size());
            return searchResults;
        }

        private  List<ContactItem> matchContacts(Object keywordResult, int searchType, List<ContactItem> all) {
            LogUtils.d(TAG, "matchContacts()");
            List<ContactItem> allTemp = new ArrayList<>();
            if (PickContactView.SEARCH_TYPE_TEXT == searchType) {
                String keyword = (String) keywordResult;
                allTemp = matchByText(all, keyword, searchType);
            } else if (PickContactView.SEARCH_TYPE_VOICE == searchType) {
                VoiceSearchResult voiceSearchResult = (VoiceSearchResult) keywordResult;
                List<ContactStruct> result = voiceSearchResult.getContactStruct();
                if (result != null && result.size() > 0) {
                    List<String> nameKey = new ArrayList<>();
                    for (ContactStruct contactStruct : result) {
                        String name = contactStruct.getDisplayName();
                        if (!nameKey.contains(name)) {
                            nameKey.add(name);
                            List<ContactItem> temp = findEqualContactItemSearch(name, all);
                            if (null != temp) {
                                allTemp.addAll(temp);
                            }
                        }
                    }
                } else {
                    allTemp = matchByText(all, voiceSearchResult.getResultString(), searchType);
                }
            }
            return allTemp;
        }

        private List<ContactItem> matchByText(List<ContactItem> all, String keyword, int searchType) {
            LogUtils.d(TAG, "matchByText() keyword = " + keyword + ", searchType = " + searchType);
            keyword = (keyword == null) ? "" : keyword;
            List<ContactItem> allTemp = new ArrayList<>();
            Pattern pyPattern = Pattern.compile("[a-zA-Z]+");
            boolean isPinyinKeyword;
            if (PickContactView.SEARCH_TYPE_VOICE == searchType) {
                isPinyinKeyword = true;
                PinYinUtils.PinYinStructInfo pinYinStructInfo = PinYinUtils.getPinYinAndInitialSet(keyword);
                if (pinYinStructInfo == null) {
                    keyword = "";
                } else {
                    keyword = pinYinStructInfo.getPinyin();
                }
            } else {
                isPinyinKeyword = pyPattern.matcher(keyword).matches();
            }
            for (ContactItem item : all) {
                if (isPinyinKeyword) {
                    if (item.containsLetters(keyword)) {
                        item.setItemType(AbsContactItem.ItemType.SEARCH);
                        allTemp.add(item);
                    }
                } else {
                    if ("".equals(keyword)) {
                        return all;
                    }
                    String contact = item.getContactName();
                    if (TextUtils.isEmpty(contact)) {
                        continue;
                    }
                    if (keyword.length() > contact.length()) {
                        continue;
                    }
                    if (contact.contains(keyword)){
                        item.setItemType(AbsContactItem.ItemType.SEARCH);
                        allTemp.add(item);
                    }
                }
            }
            return allTemp;
        }

        private boolean isNotEmptyData(Object keywordResult) {
            if (PickContactView.SEARCH_TYPE_TEXT == searchType) {
                return keywordResult != null && !TextUtils.isEmpty((String) keywordResult);

            } else if (PickContactView.SEARCH_TYPE_VOICE == searchType) {
                return keywordResult != null && !((VoiceSearchResult) keywordResult).isResultEmpty();
            }
            return false;
        }

        private List<ContactItem> findEqualContactItemSearch(String name, List<ContactItem> all) {
            HashMap<String, List<ContactItem>> allHashMap = BulletContactManager.instance.getAllContactsForSearchHashMap();
            if (null != allHashMap && allHashMap.size() > 0) {
                return allHashMap.get(name);
            } else {
                List<ContactItem> contactItems = new ArrayList<>();
                for (ContactItem item : all) {
                    if (!TextUtils.isEmpty(name) && name.equals(item.getContactName())) {
                        contactItems.add(item);
                    }
                }
                return contactItems;
            }
        }

        @Override
        protected void onPostExecute(List<AbsContactItem> absContactItems) {
            LogUtils.d(TAG, "onPostExecute()");

            if (sourceType != PickContactView.SOURCE_TYPE_STAR_FRIEND && absContactItems == null) {
                onTaskFinish(this);
                return;
            }

            IPickContactView iPickContactView = pickContactViewWR.get();
            if (null != iPickContactView) {
                if (sourceType == PickContactView.SOURCE_TYPE_STAR_FRIEND) {
                    iPickContactView.addContactList(absContactItems);
                } else {
                    iPickContactView.refreshContactList(absContactItems);
                }
                // 要是是所有联系人数据 作为参数
                PickContactAdapter pickContactAdapter = ((PickContactAdapter) iPickContactView.getAdapter());
                Map<String, Integer> indexMap = getAllContactsIndex(pickContactAdapter.getData());
                pickContactAdapter.updateIndexes(indexMap);
            }
            onTaskFinish(this);
        }

        @Override
        protected void onCancelled() {
            onTaskFinish(this);
        }
    }

    private List<ContactItem> loadAllContacts() {
        LogUtils.d(TAG, "loadAllContacts()");
        List<ContactItem> allContacts = BulletContactManager.instance.getAllContactsForSearch();
        if (null == allContacts || allContacts.size() <= 0) {
            allContacts = new ArrayList<>();
            List<ContactItem> pageContacts;
            int searchCurrentPage = 0;
            int searchPageSize = 1000;
            boolean haveMoreData = true;
            while (haveMoreData && searchCurrentPage < 10) {
                searchCurrentPage++;
                Bundle bundle = BulletContactManager.buildPageBundle(searchCurrentPage, searchPageSize);
                pageContacts = BulletContactManager.loadAllContacts(bundle);
                if (null == pageContacts || pageContacts.size() == 0) {
                    haveMoreData = false;
                } else {
                    if (!isSupportPage() || pageContacts.size() < searchPageSize) {
                        haveMoreData = false;
                    }
                }
                if (null != pageContacts && pageContacts.size() > 0) {
                    allContacts.addAll(pageContacts);
                }
            }
            BulletContactManager.instance.setAllContactsForSearch(allContacts);
        }
        return allContacts;
    }

    private boolean isSupportPage() {
        if (isSupportPageVersion < 0) {
            if (isSupportPageByVersion()) {
                isSupportPageVersion = 1;
            } else {
                isSupportPageVersion = 0;
            }

            if (isSupportPageVersion > 0) {
                if (iPickContactView != null) {
                    iPickContactView.setLoadMoreEnable(true);
                }
            }
        }
        return isSupportPageVersion > 0;
    }

}
