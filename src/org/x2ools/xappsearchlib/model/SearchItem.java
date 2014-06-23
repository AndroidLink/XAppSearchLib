
package org.x2ools.xappsearchlib.model;

import android.content.Intent;
import android.net.Uri;

public class SearchItem {

    /*
     * Application : 0; Contact : 1
     */
    private int type;

    /*
     * Application : taskId; Contact : _ID
     */
    private int id;

    /*
     * Application : app name; Contact : display name
     */
    private String name;

    // search string 1
    private String pinyin;

    // search string 2
    private String fullpinyin;

    // Application item only
    private String packageName;
    private Intent baseIntent;
    private int icon;

    // contact item only
    private Uri photo;
    private String phoneNumber; // search string 3

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public String getFullpinyin() {
        return fullpinyin;
    }

    public void setFullpinyin(String fullpinyin) {
        this.fullpinyin = fullpinyin;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Intent getBaseIntent() {
        return baseIntent;
    }

    public void setBaseIntent(Intent baseIntent) {
        this.baseIntent = baseIntent;
        baseIntent.toUri(0);
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public Uri getPhoto() {
        return photo;
    }

    public void setPhoto(Uri photo) {
        this.photo = photo;
    }

}
