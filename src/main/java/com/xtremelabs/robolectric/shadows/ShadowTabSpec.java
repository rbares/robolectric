package com.xtremelabs.robolectric.shadows;

import android.view.View;
import android.widget.TabHost;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

@SuppressWarnings({"UnusedDeclaration"})
@Implements(TabHost.TabSpec.class)
public class ShadowTabSpec {

    @Implementation
    public TabHost.TabSpec setIndicator(View view) {
        return Robolectric.newInstanceOf(TabHost.TabSpec.class);
    }
}
