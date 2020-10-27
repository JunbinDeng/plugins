// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.webviewflutter;

import android.content.Context;
import android.view.View;

import java.util.Map;

import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public final class WebViewFactory extends PlatformViewFactory {
  private final BinaryMessenger messenger;
  private final View containerView;
  private FlutterWebView flutterWebView;
  private ActivityPluginBinding binding;

  WebViewFactory(BinaryMessenger messenger, View containerView) {
    super(StandardMessageCodec.INSTANCE);
    this.messenger = messenger;
    this.containerView = containerView;
  }

  @SuppressWarnings("unchecked")
  @Override
  public PlatformView create(Context context, int id, Object args) {
    Map<String, Object> params = (Map<String, Object>) args;
    this.flutterWebView = new FlutterWebView(context, messenger, id, params, containerView);
    this.flutterWebView.setActivityPluginBinding(binding);
    return flutterWebView;
  }

  public void setActivityPluginBinding(ActivityPluginBinding binding) {
    this.binding = binding;
    if (flutterWebView != null) {
      flutterWebView.setActivityPluginBinding(binding);
    }
  }
}
