
package com.edxavier.a4squarelist.api.apiModel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Notification {

    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("item")
    @Expose
    private Item item;

    /**
     * 
     * @return
     *     The type
     */
    public String getType() {
        return type;
    }

    /**
     * 
     * @param type
     *     The type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 
     * @return
     *     The item
     */
    public Item getItem() {
        return item;
    }

    /**
     * 
     * @param item
     *     The item
     */
    public void setItem(Item item) {
        this.item = item;
    }

}
