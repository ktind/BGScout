/*
 * Copyright (c) 2014. , Kevin Lee (klee24@gmail.com)
 * All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification,
 *  are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice, this
 *  list of conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ktind.cgm.bgscout.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DownloadSQLiteHelper extends SQLiteOpenHelper {
    public static final String TABLE_DEVICE="devices";
    public static final String COLUMN_ID="_id";
    public static final String COLUMN_NAME="name";
    public static final String DEVICES_TABLE_CREATE = "create TABLE "
            + TABLE_DEVICE+"("
            + COLUMN_ID+" integer primary key autoincrement not null,"
            + COLUMN_NAME+" text not null unique);";
    public static final String TABLE_ROLES="roles";
//    public static final String COLUMN_ID="_id";
    public static final String COLUMN_ROLE="role";
    public static final String ROLES_TABLE_CREATE = "create table "
            + TABLE_ROLES+" ("
            + COLUMN_ID+" integer primary key autoincrement not null,"
            + COLUMN_ROLE+" text not null unique);";
    public static final String TABLE_BATTERY="battery_history";
//    public static final String COLUMN_ID="_id";
    public static final String COLUMN_EPOCH="epoch";
    public static final String COLUMN_DEVICEID="deviceid";
    public static final String COLUMN_ROLEID="roleid";
    public static final String COLUMN_BATTERYLEVEL="batterylevel";
    public static final String BATTERY_TABLE_CREATE = "create table "
            + TABLE_BATTERY+" ( "
            + COLUMN_ID+" integer primary key autoincrement not null, "
            + COLUMN_EPOCH+" integer not null,"
            + COLUMN_DEVICEID+" integer not null, "
            + COLUMN_ROLEID+" integer not null, "
            + COLUMN_BATTERYLEVEL+" real not null, "
            + "foreign key(deviceid) references devices(_id), foreign key(roleid) references roles(_id) );";
    public static final String TABLE_EGV="egv";
//    public static final String COLUMN_ID="_id";
//    public static final String COLUMN_EPOCH="epoch";
//    public static final String COLUMN_DEVICEID="deviceid";
    public static final String COLUMN_EGV="egv";
    public static final String COLUMN_TREND="trend";
    public static final String COLUMN_UNIT="unit";
    private static final String EGV_TABLE_CREATE = "create table "
            + TABLE_EGV +"( "
            + COLUMN_ID +" integer primary key autoincrement not null ,"
            + COLUMN_EPOCH+" integer, "
            + COLUMN_DEVICEID+" integer, "
            + COLUMN_EGV+" integer not null, "
            + COLUMN_TREND+" integer not null, "
            + COLUMN_UNIT+" integer not null, "
            + "foreign key(deviceid) references DEVICES(_id) );";




    DownloadSQLiteHelper(Context context) {
        super(context, DBConstants.DATABASENAME,null, DBConstants.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DEVICES_TABLE_CREATE);
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME,"device_1");
        db.insert(TABLE_DEVICE,null,values);
        values.put(COLUMN_NAME,"device_2");
        db.insert(TABLE_DEVICE,null,values);
        values.put(COLUMN_NAME,"device_3");
        db.insert(TABLE_DEVICE,null,values);
        values.put(COLUMN_NAME,"device_4");
        db.insert(TABLE_DEVICE,null,values);

        values = new ContentValues();
        db.execSQL(ROLES_TABLE_CREATE);
        values.put(COLUMN_ROLE,"uploader");
        db.insert(TABLE_ROLES,null,values);
        values.put(COLUMN_ROLE,"cgm");
        db.insert(TABLE_ROLES,null,values);
        values.put(COLUMN_ROLE,"remote");
        db.insert(TABLE_ROLES,null,values);

        db.execSQL(BATTERY_TABLE_CREATE);
        db.execSQL(EGV_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DBUpgrade", "Upgrading the database ;)");
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_DEVICE);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_ROLES);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_EGV);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_BATTERY);
        onCreate(db);
    }
}
