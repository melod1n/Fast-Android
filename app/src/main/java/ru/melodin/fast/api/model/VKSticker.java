package ru.melodin.fast.api.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class VKSticker extends VKModel implements Serializable {

    public static final String TAG = "FVKSticker";
    public int id;
    public int product_id;
    public String src_64, src_128, src_256, src_512;
    public ArrayList<String> srcs;

    public VKSticker() {
    }

    public VKSticker(int peerId, int attId) {
        this.product_id = peerId;
        this.id = attId;
    }

    public VKSticker(JSONObject source) {
        tag = VKAttachments.TYPE_STICKER;
        this.id = source.optInt("id");
        this.product_id = source.optInt("product_id");
        this.srcs = new ArrayList<>();

        JSONArray images = source.optJSONArray("images");

        for (int i = 0; i < images.length(); i++) {
            JSONObject size = images.optJSONObject(i);
            String url = size.optString("url");
            this.srcs.add(url);
        }

        this.src_64 = srcs.get(0);
        this.src_128 = srcs.get(1);
        this.src_256 = srcs.get(2);
        this.src_512 = srcs.get(4);
    }
}
