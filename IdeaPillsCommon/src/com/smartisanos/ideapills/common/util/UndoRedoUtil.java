package com.smartisanos.ideapills.common.util;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

public class UndoRedoUtil implements TextWatcher {
    private static final int MAX_RECORDE_COUNT = 20;

    private EditText mEditText;
    private ITextChangeListener mListener;
    private boolean mIsUndoRedo;
    private int mOffSet = 0;
    private List<Record> mRecordList = new ArrayList<>();

    public UndoRedoUtil(EditText editText, ITextChangeListener listener) {
        mEditText = editText;
        mListener = listener;
        editText.addTextChangedListener(this);
        createNewRecord(0, 0, null);
    }

    public void unDo() {
        if (canUndo()) {
            mIsUndoRedo = true;
            if (mOffSet <= mRecordList.size()) {
                Record record = mRecordList.get(mOffSet - 1);
                Editable editable = mEditText.getEditableText();
                CharSequence temp = editable.subSequence(record.start, record.end);
                editable.replace(record.start, record.end, record.text);
                record.end = record.start + record.text.length();
                Selection.setSelection(editable, record.end);
                record.text = temp;
            }
            mIsUndoRedo = false;
            mOffSet--;
        }
    }

    public void reDo() {
        if (canRedo()) {
            mIsUndoRedo = true;
            if (mOffSet <= mRecordList.size()) {
                mOffSet++;
                Record record = mRecordList.get(mOffSet - 1);
                Editable editable = mEditText.getEditableText();
                CharSequence temp = editable.subSequence(record.start, record.end);
                editable.replace(record.start, record.end, record.text);
                record.end = record.start + record.text.length();
                Selection.setSelection(editable, record.end);
                record.text = temp;
            }
            mIsUndoRedo = false;
        }
    }

    public boolean canUndo() {
        return mOffSet > 1 && (mOffSet <= mRecordList.size());
    }

    public boolean canRedo() {
        return mRecordList.size() > mOffSet;
    }

    public void cleanRecord() {
        mRecordList.clear();
        mOffSet = 0;
    }

    private void popUp() {
        if (mRecordList.size() > 0) {
            mRecordList.remove(0);
        }
    }

    private void popNext(int count) {
        int len = mRecordList.size();
        if (count > 0 && len >= count) {
            for (int i = 1; i <= count; i++) {
                mRecordList.remove(len - i);
            }
        }
    }

    private void push(Record record) {
        if (record != null) {
            mRecordList.add(record);
        }
    }

    private void createNewRecord(int start, int end, CharSequence text) {
        Record record = new Record(start, end, text);
        int count = mRecordList.size();
        if (count > 0) {
            Record lastRecord = mRecordList.get(count - 1);
            if (record.equals(lastRecord)) {
                return;
            }
        }
        if (count >= MAX_RECORDE_COUNT) {
            popUp();
        }
        push(record);
        mOffSet = mRecordList.size();
    }

    public void removeTextChangedListener() {
        mListener = null;
    }

    public void addTextChangedListener(ITextChangeListener listener) {
        mListener = listener;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (mListener != null) {
            mListener.beforeTextChanged(s, start, count, after);
        }
        if (mIsUndoRedo) {
            return;
        }
        popNext(mRecordList.size() - mOffSet);
        createNewRecord(start, start + after, s.subSequence(start, start + count));
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mListener != null) {
            mListener.onTextChanged(s, start, before, count);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mListener != null) {
            mListener.afterTextChanged(s);
        }
    }

    public interface ITextChangeListener {
        void onTextChanged(CharSequence s, int start, int before, int count);

        void beforeTextChanged(CharSequence s, int start, int count, int after);

        void afterTextChanged(Editable s);
    }

    private static class Record {
        private int start;
        private int end;
        private CharSequence text;

        Record(int start, int end, CharSequence text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }

        public boolean equals(Record record) {
            if (record != null) {
                boolean textequals = false;
                if (text != null) {
                    textequals = text.equals(record.text);
                } else {
                    textequals = record.text == null;
                }
                return record.start == this.start && record.end == this.end && textequals;
            }
            return false;
        }
    }
}