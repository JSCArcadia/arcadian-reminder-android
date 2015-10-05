package com.arcadia.wearapp.realm_objects;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ParseGroup extends RealmObject {
    @PrimaryKey
    private String groupID;

    private String description;
    private String title;

    public ParseGroup() {
    }

    public ParseGroup(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }
}
