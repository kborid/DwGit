package com.smartisanos.ideapills.data;

import android.database.sqlite.SQLiteDatabase;

public abstract class TransactionTask {
    protected SQLiteDatabase db;
    public TransactionTask(SQLiteDatabase _db) {
        db = _db;
    }

    public TransactionTask(SQLiteDatabase _db, Result _defaultResult) {
        db = _db;
        defaultResult = _defaultResult;
    }

    public static class Result {
        public int i;
        public long l;
        public boolean b;

        public void copyTo(Result other) {
            other.i = i;
            other.l = l;
            other.b = b;
        }
    }

    public Result result;
    public Result defaultResult;

    public abstract void run();

    public Result execute() {
        db.beginTransaction();
        boolean failed = false;
        try {
            result = new Result();
            if (defaultResult != null) {
                defaultResult.copyTo(result);
            }
            run();
            db.setTransactionSuccessful();
        } catch (Exception e) {
            failed = true;
            e.printStackTrace();
        } finally {
            try {
                db.endTransaction();
            } catch (Exception e) {
                failed = true;
                result = new Result();
                e.printStackTrace();
            }
        }
        if (failed) {
            //clean result
            if (defaultResult != null) {
                result = defaultResult;
            } else {
                result = new Result();
            }
        }
        return result;
    }
}