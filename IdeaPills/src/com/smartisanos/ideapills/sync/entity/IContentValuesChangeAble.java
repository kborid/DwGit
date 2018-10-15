package com.smartisanos.ideapills.sync.entity;

import android.content.ContentValues;

public interface IContentValuesChangeAble {
    void fromContentValues(ContentValues contentValues);

    ContentValues toContentValues();
}
