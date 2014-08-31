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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by klee24 on 8/30/14.
 */
public class DownloadDataSource {
    private SQLiteDatabase dbHelper;
    private DownloadSQLiteHelper downloadDBHelper;
    private String[] allDeviceColumns = {DownloadSQLiteHelper.COLUMN_ID,DownloadSQLiteHelper.COLUMN_NAME};
    private String[] allRoleColumns = {DownloadSQLiteHelper.COLUMN_ID,DownloadSQLiteHelper.COLUMN_ROLE};
    private String[] allBatteryColumns = {DownloadSQLiteHelper.COLUMN_ROLEID,DownloadSQLiteHelper.COLUMN_EPOCH,DownloadSQLiteHelper.COLUMN_DEVICEID,DownloadSQLiteHelper.COLUMN_ROLEID,DownloadSQLiteHelper.COLUMN_BATTERYLEVEL};
    private String[] allEGVColumns = {DownloadSQLiteHelper.COLUMN_ID,DownloadSQLiteHelper.COLUMN_EPOCH,DownloadSQLiteHelper.COLUMN_DEVICEID,DownloadSQLiteHelper.COLUMN_EGV,DownloadSQLiteHelper.COLUMN_UNIT,DownloadSQLiteHelper.COLUMN_TREND};
//    private String[] allAlertsColumns = {DownloadSQLiteHelper.COLUMN_ID,DownloadSQLiteHelper.COLUMN_EPOCH,DownloadSQLiteHelper.COLUMN_SEVERITY,DownloadSQLiteHelper.COLUMN_MESSAGE,DownloadSQLiteHelper.COLUMN_DEVICEID};

    public DownloadDataSource(Context context){
        downloadDBHelper = new DownloadSQLiteHelper(context);
    }

    public void open() throws SQLException {
        dbHelper = downloadDBHelper.getWritableDatabase();
//      Uncomment this to erase the DB =)
//        downloadDBHelper.onUpgrade(dbHelper,1,1);
    }

    public void close(){
        downloadDBHelper.close();
    }

    /*
    Devices
     */
    public Device createDevice(String name){
        ContentValues values = new ContentValues();
        values.put(DownloadSQLiteHelper.COLUMN_NAME,name);
        long insertId = dbHelper.insert(DownloadSQLiteHelper.COLUMN_NAME,null,values);
        Cursor cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_DEVICE, allDeviceColumns,DownloadSQLiteHelper.COLUMN_ID+"="+insertId,null,null,null,null,null);
        cursor.moveToFirst();
        Device newDevice = cursorToDevice(cursor);
        cursor.close();
        return newDevice;
    }

    public Device getDevice(long id){
        ContentValues values = new ContentValues();
        Cursor cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_DEVICE,allDeviceColumns,DownloadSQLiteHelper.COLUMN_ID+"="+id,null,null,null,null,null);
        cursor.moveToFirst();
        Device newDevice = cursorToDevice(cursor);
        cursor.close();
        return newDevice;
    }

    public Device getDevice(String name){
        ContentValues values = new ContentValues();
        Cursor cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_DEVICE,allDeviceColumns,DownloadSQLiteHelper.COLUMN_NAME+"=\""+name+"\"",null,null,null,null,null);
        cursor.moveToFirst();
        Device newDevice = cursorToDevice(cursor);
        cursor.close();
        return newDevice;
    }

    public void deleteDevice(Device device){
        long id = device.getId();
        dbHelper.delete(DownloadSQLiteHelper.TABLE_DEVICE, DownloadSQLiteHelper.COLUMN_ID + "=" + id, null);
    }

    public List<Device> getAllDevices(){
        List<Device> devices = new ArrayList<Device>();
        Cursor cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_DEVICE, allDeviceColumns,null,null,null,null,null,null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()){
            Device device = cursorToDevice(cursor);
            devices.add(device);
            cursor.moveToNext();
        }
        cursor.close();
        return devices;
    }

    private Device cursorToDevice(Cursor cursor){
        Device device=new Device();
        device.setId(cursor.getLong(0));
        device.setName(cursor.getString(1));
        return device;
    }

    /*
    Roles
     */
    public Role createRole(String name){
        ContentValues values = new ContentValues();
        values.put(DownloadSQLiteHelper.COLUMN_ROLE,name);
        long insertId = dbHelper.insert(DownloadSQLiteHelper.TABLE_ROLES,null,values);
        Cursor cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_ROLES, allRoleColumns,DownloadSQLiteHelper.COLUMN_ROLE+"="+insertId,null,null,null,null,null);
        cursor.moveToFirst();
        Role newRole = cursorToRole(cursor);
        cursor.close();
        return newRole;
    }

    public void deleteRole(Role role){
        long id = role.getId();
        dbHelper.delete(DownloadSQLiteHelper.TABLE_ROLES, DownloadSQLiteHelper.COLUMN_ID + "=" + id, null);
    }

    public Role getRole(long id){
        ContentValues values = new ContentValues();
        Cursor cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_ROLES,allRoleColumns,DownloadSQLiteHelper.COLUMN_ID+"="+id,null,null,null,null,null);
        cursor.moveToFirst();
        Role newRole = cursorToRole(cursor);
        cursor.close();
        return newRole;
    }

    public Role getRole(String name){
        ContentValues values = new ContentValues();
        Cursor cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_ROLES,allRoleColumns,DownloadSQLiteHelper.COLUMN_ROLE+"=\""+name+"\"",null,null,null,null,null);
        cursor.moveToFirst();
        Role newRole = cursorToRole(cursor);
        cursor.close();
        return newRole;
    }


    public List<Role> getAllRoles(){
        List<Role> roles = new ArrayList<Role>();
        Cursor cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_ROLES, allRoleColumns,null,null,null,null,null,null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()){
            Role role = cursorToRole(cursor);
            roles.add(role);
            cursor.moveToNext();
        }
        cursor.close();
        return roles;
    }

    private Role cursorToRole(Cursor cursor){
        Role role=new Role();
        role.setId(cursor.getLong(0));
        role.setRole(cursor.getString(1));
        return role;
    }

    /*
    Battery
     */
    public Battery createBattery(float batteryLevel,Device device,Role role, long epoch){
        ContentValues values = new ContentValues();
        values.put(DownloadSQLiteHelper.COLUMN_BATTERYLEVEL,batteryLevel);
        values.put(DownloadSQLiteHelper.COLUMN_EPOCH,epoch);
        values.put(DownloadSQLiteHelper.COLUMN_DEVICEID,device.getId());
        values.put(DownloadSQLiteHelper.COLUMN_ROLEID,role.getId());
        long insertId = dbHelper.insert(DownloadSQLiteHelper.TABLE_BATTERY,null,values);
        Cursor cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_BATTERY, allBatteryColumns,DownloadSQLiteHelper.COLUMN_ID+"="+insertId,null,null,null,null,null);
        cursor.moveToFirst();
        Battery newBattery = cursorToBattery(cursor);
        cursor.close();
        return newBattery;
    }

    public Battery createBattery(float batteryLevel,String deviceName,String roleName, long epoch){
        Role role=getRole(roleName);
        Device device=getDevice(deviceName);
        return createBattery(batteryLevel,device,role,epoch);
    }

    public ArrayList<Battery> getBatteryHistory(Device device,Role role){
        ArrayList<Battery> batteries=new ArrayList<Battery>();
        Cursor cursor;
        if (role!=null)
            cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_BATTERY,allBatteryColumns,DownloadSQLiteHelper.COLUMN_DEVICEID+"="+device.getId()+" and "+DownloadSQLiteHelper.COLUMN_ROLEID+"="+role.getId(),null,null,null,null,null);
        else
            cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_BATTERY,allBatteryColumns,DownloadSQLiteHelper.COLUMN_DEVICEID+"="+device.getId(),null,null,null,null,null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()){
            Battery battery = new Battery();
            battery=cursorToBattery(cursor);
            batteries.add(battery);
            cursor.moveToNext();
        }
        cursor.close();
        return batteries;
//        return getBatteryHistory(device.getId(),role.getId());
    }

    public ArrayList<Battery> getBatteryHistory(long deviceID, long roleID){
        Device device=getDevice(deviceID);
        Role role=getRole(roleID);
        return getBatteryHistory(device,role);
    }

    public ArrayList<Battery> getBatteryHistory(String deviceName, String roleName){
        Device device=getDevice(deviceName);
        Role role=getRole(roleName);
        return getBatteryHistory(device,role);
    }

    public ArrayList<Battery> getBatteryHistory(long deviceID){
        Device device=getDevice(deviceID);
        return getBatteryHistory(device,null);
    }

    public ArrayList<Battery> getBatteryHistory(String deviceName){
        Device device=getDevice(deviceName);
        return getBatteryHistory(device,null);
    }


    private Battery cursorToBattery(Cursor cursor){
        Battery battery=new Battery();
        battery.setId(cursor.getLong(0));
        battery.setEpoch(cursor.getLong(1));
        battery.setDeviceid(cursor.getLong(2));
        battery.setRoleid(cursor.getLong(3));
        battery.setBatterylevel(cursor.getFloat(4));
        return battery;
    }

    /*
    EGV
     */
    public EGV createEGV(long epoch, long deviceID, int egv, int unit, int trend){
        ContentValues values = new ContentValues();
        values.put(DownloadSQLiteHelper.COLUMN_EPOCH,epoch);
        values.put(DownloadSQLiteHelper.COLUMN_DEVICEID,deviceID);
        values.put(DownloadSQLiteHelper.COLUMN_EGV,egv);
        values.put(DownloadSQLiteHelper.COLUMN_UNIT,unit);
        values.put(DownloadSQLiteHelper.COLUMN_TREND,trend);
        long insertId = dbHelper.insert(DownloadSQLiteHelper.TABLE_EGV,null,values);
        Cursor cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_EGV,allEGVColumns,DownloadSQLiteHelper.COLUMN_ID+"="+insertId,null,null,null,null,null);
        cursor.moveToFirst();
        EGV newEGV = cursorToEGV(cursor);
        cursor.close();
        return newEGV;
    }

    public EGV createEGV(long epoch, String deviceName, int egv, int unit, int trend){
        Device device=getDevice(deviceName);
        return createEGV(epoch,device.getId(),egv,unit,trend);
    }

    public ArrayList<EGV> getEGVHistory(String deviceName){
        Device device=getDevice(deviceName);
        return getEGVHistory(device);
    }

    public ArrayList<EGV> getEGVHistory(Device device){
        return getEGVHistory(device.getId());
    }

    public ArrayList<EGV> getEGVHistory(long deviceID){
        ArrayList<EGV> egvHistory = new ArrayList<EGV>();
        Cursor cursor = dbHelper.query(DownloadSQLiteHelper.TABLE_EGV,allEGVColumns,DownloadSQLiteHelper.COLUMN_DEVICEID+"="+deviceID,null,null,null,null,null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()){
            EGV egv = new EGV();
            egv=cursorToEGV(cursor);
            egvHistory.add(egv);
            cursor.moveToNext();
        }
        return egvHistory;
    }

    public EGV cursorToEGV(Cursor cursor){
        EGV egv = new EGV();
        egv.setId(cursor.getLong(0));
        egv.setEpoch(cursor.getLong(1));
        egv.setDeviceid(cursor.getLong(2));
        egv.setEgv(cursor.getInt(3));
        egv.setUnit(cursor.getInt(4));
        egv.setTrend(cursor.getInt(5));
        return egv;
    }
}
