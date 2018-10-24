/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.dialer.binary.google;

import com.android.dialer.binary.basecomponent.BaseDialerRootComponent;
import com.android.dialer.calllog.CallLogModule;
import com.android.dialer.common.concurrent.DialerExecutorModule;
import com.android.dialer.configprovider.SharedPrefConfigProviderModule;
import com.android.dialer.enrichedcall.stub.StubEnrichedCallModule;
import com.android.dialer.inject.ContextModule;
import com.android.dialer.lightbringer.stub.StubLightbringerModule;
import com.android.dialer.simulator.impl.SimulatorModule;
import com.android.incallui.calllocation.impl.CallLocationModule;
import com.android.incallui.maps.impl.MapsModule;
import com.android.voicemail.impl.VoicemailModule;
import dagger.Component;
import javax.inject.Singleton;

/**
 * Root component for the Google Stub Dialer application. Unlike the AOSP variant, this component
 * can pull in modules that depend on Google Play Services like the maps module.
 */
@Singleton
@Component(
  modules = {
    CallLocationModule.class,
    CallLogModule.class,
    ContextModule.class,
    DialerExecutorModule.class,
    SharedPrefConfigProviderModule.class,
    SimulatorModule.class,
    StubEnrichedCallModule.class,
    MapsModule.class,
    VoicemailModule.class,
    StubLightbringerModule.class
  }
)
public interface GoogleStubDialerRootComponent extends BaseDialerRootComponent {}
