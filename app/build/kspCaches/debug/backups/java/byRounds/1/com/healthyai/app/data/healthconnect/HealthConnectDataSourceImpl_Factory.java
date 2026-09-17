package com.healthyai.app.data.healthconnect;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class HealthConnectDataSourceImpl_Factory implements Factory<HealthConnectDataSourceImpl> {
  private final Provider<Context> contextProvider;

  public HealthConnectDataSourceImpl_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public HealthConnectDataSourceImpl get() {
    return newInstance(contextProvider.get());
  }

  public static HealthConnectDataSourceImpl_Factory create(Provider<Context> contextProvider) {
    return new HealthConnectDataSourceImpl_Factory(contextProvider);
  }

  public static HealthConnectDataSourceImpl newInstance(Context context) {
    return new HealthConnectDataSourceImpl(context);
  }
}
