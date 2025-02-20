package com.zpl.capacitor;

import android.util.Log;

public class ZPLPlugin {

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }
}
